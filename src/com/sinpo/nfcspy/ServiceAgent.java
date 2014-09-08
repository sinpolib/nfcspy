/* NFC Spy is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFC Spy is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package com.sinpo.nfcspy;

import static android.net.wifi.p2p.WifiP2pManager.EXTRA_NETWORK_INFO;
import static android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED;
import static com.sinpo.nfcspy.ServiceFactory.ACTION_NFC_ATTACH;
import static com.sinpo.nfcspy.ServiceFactory.ACTION_NFC_DETTACH;
import static com.sinpo.nfcspy.ServiceFactory.ACTION_SERVER_CHAT;
import static com.sinpo.nfcspy.ServiceFactory.ACTION_SERVER_SET;
import static com.sinpo.nfcspy.ServiceFactory.ACTION_SERVER_START;
import static com.sinpo.nfcspy.ServiceFactory.ACTION_SERVER_STOP;
import static com.sinpo.nfcspy.ServiceFactory.ERR_APDU_CMD;
import static com.sinpo.nfcspy.ServiceFactory.ERR_APDU_RSP;
import static com.sinpo.nfcspy.ServiceFactory.ERR_P2P;
import static com.sinpo.nfcspy.ServiceFactory.MSG_CHAT_RECV;
import static com.sinpo.nfcspy.ServiceFactory.MSG_CHAT_SEND;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_APDU_CMD;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_APDU_RSP;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_ATTACH;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_DEACTIVATED;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_CONNECT;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_DISCONN;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_INIT;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_SOCKET;
import static com.sinpo.nfcspy.ServiceFactory.MSG_SERVER_KILL;
import static com.sinpo.nfcspy.ServiceFactory.MSG_SERVER_VER;
import static com.sinpo.nfcspy.ServiceFactory.STA_ERROR;
import static com.sinpo.nfcspy.ServiceFactory.STA_FAIL;
import static com.sinpo.nfcspy.ServiceFactory.STA_NOTCARE;
import static com.sinpo.nfcspy.ServiceFactory.STA_P2P_WATING;
import static com.sinpo.nfcspy.ServiceFactory.STA_SUCCESS;
import static com.sinpo.nfcspy.WiFiP2PCommand.CMD_RequestConnectionInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;

final class ServiceAgent extends BroadcastReceiver implements ChannelListener,
		ServiceFactory.SpyCallback {

	void handleIntent(Intent intent) {
		final String action = intent.getAction();

		if (ACTION_NFC_ATTACH.equals(action))
			onCardAttaching(intent);
		else if (ACTION_NFC_DETTACH.equals(action))
			onCardDettached(intent);
		else if (ACTION_SERVER_START.equals(action))
			onStartCommand(intent);
		else if (ACTION_SERVER_STOP.equals(action))
			onStopCommand(intent);
		else if (ACTION_SERVER_CHAT.equals(action))
			onSendChatMessage(intent);
		else if (ACTION_SERVER_SET.equals(action))
			onSetCommand(intent);
	}

	@Override
	public void handleMessage(int type, int status, Object obj) {
		switch (type) {
		case MSG_HCE_APDU_CMD:
			processCommandApdu((byte[]) obj);
			break;

		case MSG_HCE_APDU_RSP:
			processResponseApdu((byte[]) obj);
			break;

		case MSG_HCE_ATTACH:
			Logger.logNfcAttach(new String((byte[]) obj));
			sendBinary2Activity(MSG_HCE_ATTACH, (byte[]) obj);
			break;

		case MSG_HCE_DEACTIVATED:
			NfcManager.disconnect(isodep);
			Logger.logHceDeactive();
			sendStatus2Activity(MSG_HCE_DEACTIVATED, STA_NOTCARE, 0);
			break;

		case MSG_SERVER_VER:
			if (obj == null)
				sendBinary2Peer(MSG_SERVER_VER, ThisApplication.version()
						.getBytes());
			else
				sendBinary2Activity(MSG_SERVER_VER, (byte[]) obj);
			break;

		case MSG_CHAT_SEND:
			sendBinary2Activity(MSG_CHAT_RECV, (byte[]) obj);
			break;

		case CMD_RequestConnectionInfo:
			if (lock()) {
				if (status == STA_SUCCESS) {
					new SocketConnector(p2p, this).start();
				} else {
					sendStatus2Activity(MSG_P2P_SOCKET, status, 0);
					unlock();
				}
			}
			break;

		case MSG_P2P_CONNECT:
			if (status == STA_SUCCESS)
				sendVersion2Peer();
			// go through
		case MSG_P2P_DISCONN:
			if (status == STA_SUCCESS || status == STA_FAIL)
				unlock();

			sendStatus2Activity(type, status, STA_NOTCARE);
			break;

		case MSG_SERVER_KILL:
			stopSelf(false);
			break;
		}
	}

	@Override
	public void onReceive(Context context, Intent i) {
		delayAction(MSG_SERVER_KILL, 120000);

		final String action = i.getAction();

		if (WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

			if (i.getIntExtra(EXTRA_WIFI_STATE, -1) != WIFI_P2P_STATE_ENABLED)
				disconnect();

		} else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

			if (((NetworkInfo) i.getParcelableExtra(EXTRA_NETWORK_INFO))
					.isConnected()) {
				new Wifip2pRequestConnectionInfo(this).execute(p2p);
			}
		}
	}

	private void onStartCommand(Intent intent) {

		Messenger messenger = ServiceFactory.getReplyMessengerExtra(intent);

		if ((outbox = messenger) != null) {

			if (p2p.isConnected()) {
				sendStatus2Activity(MSG_P2P_CONNECT, STA_SUCCESS, STA_NOTCARE);
				return;
			}

			if (!p2p.isInited()) {
				p2p.init(service, this);
				if (!hasRegistered) {
					service.registerReceiver(this, P2PFILTER);
					hasRegistered = true;
				}
				sendStatus2Activity(MSG_P2P_INIT, STA_SUCCESS, STA_NOTCARE);
			}

			sendStatus2Activity(MSG_P2P_CONNECT, STA_P2P_WATING, STA_NOTCARE);

			delayAction(MSG_SERVER_KILL, 90000);

			new Wifip2pRequestConnectionInfo(this).execute(p2p);
		}
	}

	private void processCommandApdu(byte[] cmd) {
		final long stampCmd = System.currentTimeMillis();

		if (cmd != null && cmd.length > 0) {

			final byte[] rsp = NfcManager.transceiveApdu(isodep, cmd);

			final long stampRsp = System.currentTimeMillis();

			final boolean valid = (rsp != null && rsp.length > 0);

			boolean sent = false;

			if (valid)
				sent = sendBinary2Peer(MSG_HCE_APDU_RSP, rsp);

			final long delayCmd = Logger.logApdu(stampCmd, false, true, cmd);
			final long delayRsp = Logger.logApdu(stampRsp, false, false, rsp);

			if (!highSpeed) {

				sendBinary2Activity(MSG_HCE_APDU_CMD, (int) delayCmd, cmd);

				if (valid)
					sendBinary2Activity(MSG_HCE_APDU_RSP, (int) delayRsp, rsp);
			}

			if (!valid)
				sendStatus2Activity(MSG_HCE_APDU_RSP, STA_ERROR, ERR_APDU_RSP);

			if (!sent)
				sendStatus2Activity(MSG_HCE_APDU_RSP, STA_ERROR, ERR_P2P);

		} else {
			Logger.logApdu(stampCmd, false, true, null);
			sendStatus2Activity(MSG_HCE_APDU_CMD, STA_ERROR, ERR_APDU_CMD);
		}
	}

	private void processResponseApdu(byte[] rsp) {

		((ServiceFactory.SpyService) service).processResponseApdu(rsp);
		Logger.logApdu(System.currentTimeMillis(), true, false, rsp);
	}

	private void onCardAttaching(Intent intent) {
		isodep = NfcManager.attachCard(intent);
		if (isodep != null) {
			String id = NfcManager.getCardId(isodep);
			Logger.logNfcAttach(id);

			byte[] raw = id.getBytes();
			sendBinary2Peer(MSG_HCE_ATTACH, raw);
			sendBinary2Activity(MSG_HCE_ATTACH, raw);
		}
	}

	private void onSetCommand(Intent intent) {
		highSpeed = ServiceFactory.getHighSpeedSettingExtra(intent);
	}

	private void onSendChatMessage(Intent intent) {
		byte[] msg = ServiceFactory.getChatMessageExtra(intent).getBytes();
		if (sendBinary2Peer(MSG_CHAT_SEND, msg))
			sendBinary2Activity(MSG_CHAT_SEND, msg);
	}

	private void onStopCommand(Intent intent) {
		stopSelf(true);
	}

	private void onCardDettached(Intent intent) {
		isodep = null;
	}

	void onDeactivated(int reason) {
		sendBinary2Peer(MSG_HCE_DEACTIVATED, new byte[1]);
		Logger.logHceDeactive();

		if (!highSpeed)
			sendStatus2Activity(MSG_HCE_DEACTIVATED, STA_NOTCARE, 0);
	}

	byte[] processCommandApdu(byte[] apdu, Bundle ignore) {
		sendBinary2Peer(MSG_HCE_APDU_CMD, apdu);
		Logger.logApdu(System.currentTimeMillis(), true, true, apdu);
		return null;
	}

	void onDestroy() {
		if (hasRegistered) {
			service.unregisterReceiver(this);
			hasRegistered = false;
		}

		onCardDettached(null);
		disconnect();
	}

	private void stopSelf(boolean force) {
		if (force || !p2p.isConnected())
			service.stopSelf();
	}

	@Override
	public void onChannelDisconnected() {
		disconnect();
	}

	private void disconnect() {
		p2p.reset();
	}

	private void sendVersion2Peer() {
		// random delay between a few seconds
		int delay = (int) (SystemClock.uptimeMillis() & 0x3FF);
		delayAction(MSG_SERVER_VER, delay + 1000);
	}

	private boolean sendBinary2Peer(int type, byte[] raw) {
		return p2p.sendData(type, raw);
	}

	private void sendBinary2Activity(int type, byte[] raw) {
		sendBinary2Activity(type, STA_NOTCARE, raw);
	}

	private void sendBinary2Activity(int type, int arg, byte[] raw) {
		try {
			Message msg = Message.obtain(null, type, raw.length, arg);
			ServiceFactory.setDataToMessage(msg, raw);
			outbox.send(msg);
		} catch (Exception e) {
		}
	}

	private void sendStatus2Activity(int type, int status, int detail) {
		try {
			outbox.send(Message.obtain(null, type, status, detail));
		} catch (Exception e) {
		}
	}

	private void delayAction(int type, long millis) {
		handler.postDelayed(new Action(this, type, STA_NOTCARE), millis);
	}

	ServiceAgent(Service service) {
		this.service = service;
		this.handler = new Handler();
		this.p2p = new WiFiP2PManager();
		outbox = null;
		Logger.open();
	}

	synchronized boolean lock() {
		if (!lock) {
			lock = true;
			return true;
		}
		return false;
	}

	synchronized void unlock() {
		lock = false;
	}

	private final static class Action implements Runnable {
		private final ServiceFactory.SpyCallback callback;
		private final int type, status;

		@Override
		public void run() {
			callback.handleMessage(type, status, null);
		}

		Action(ServiceFactory.SpyCallback cback, int type, int status) {
			this.callback = cback;
			this.type = type;
			this.status = status;
		}
	}

	private final Service service;
	private final WiFiP2PManager p2p;
	private final Handler handler;

	private IsoDep isodep;
	private Messenger outbox;
	private boolean lock;
	private boolean hasRegistered;

	private static boolean highSpeed;

	private final static IntentFilter P2PFILTER;
	static {
		P2PFILTER = new IntentFilter();
		P2PFILTER.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
		P2PFILTER.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
	}
}
