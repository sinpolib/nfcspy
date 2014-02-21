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

import static android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_DEVICE;
import static android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION;

import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityManageP2P extends ActivityBase {

	public void startDiscover(View v) {
		resetPeers();

		if (!WiFiP2PManager.isWiFiEnabled(this)) {
			showMessage(R.string.status_no_wifi);
			return;
		}

		new CommandHelper(new Wifip2pDiscoverPeers(eventHelper), p2p,
				getString(R.string.info_discovering)).execute();
	}

	public void disconnectPeer(View v) {
		new Wifip2pCancelConnect(eventHelper).execute(p2p);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage_p2p);

		peerdevs = new ArrayList<WifiP2pDevice>();
		peers = new ArrayAdapter<CharSequence>(this, R.layout.listitem_peer);
		eventHelper = new EventHelper();

		ListView lv = ((ListView) findViewById(R.id.list));
		lv.setAdapter(peers);
		lv.setOnItemClickListener(eventHelper);

		thisDevice = (TextView) findViewById(R.id.txtThisdevice);
	}

	@Override
	protected void onResume() {

		p2p = new WiFiP2PManager();
		p2p.init(this, eventHelper);
		registerReceiver(eventHelper, getWiFiP2PIntentFilter());

		super.onResume();
	}

	@Override
	protected void onPause() {
		closeProgressDialog();

		new Wifip2pStopPeerDiscovery(null).execute(p2p);

		unregisterReceiver(eventHelper);
		p2p.reset();
		p2p = null;

		resetPeers();

		super.onPause();
	}

	@SuppressWarnings("unchecked")
	boolean handleMessage(int type, int status, Object obj) {

		if (type == WiFiP2PCommand.CMD_RequestPeers) {

			resetPeers();

			if (obj != null) {

				for (WifiP2pDevice dev : (Collection<WifiP2pDevice>) obj) {
					peerdevs.add(dev);
					peers.add(getWifiP2pDeviceInfo(dev));
				}
			}
			return true;
		}

		if (type == WiFiP2PCommand.CMD_CancelConnect) {
			new Wifip2pRemoveGroup(eventHelper).execute(p2p);
			return true;
		}

		if (type == WiFiP2PCommand.CMD_RemoveGroup) {
			new Wifip2pDiscoverPeers(eventHelper).execute(p2p);
			return true;
		}

		return true;
	}

	void handlePeerListClick(int index) {
		WifiP2pDevice dev = peerdevs.get(index);

		if (dev.status == WifiP2pDevice.CONNECTED
				|| dev.status == WifiP2pDevice.INVITED) {
			disconnectPeer(dev);
		} else if (dev.status != WifiP2pDevice.UNAVAILABLE) {
			connectPeer(dev);
		}
	}

	void handleBroadcast(Intent intent) {
		closeProgressDialog();

		final String action = intent.getAction();
		if (WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
			int state = intent.getIntExtra(EXTRA_WIFI_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				p2p.isWifiP2pEnabled = true;
			} else {
				showMessage(R.string.event_p2p_disable);
				resetData();
			}
		} else if (WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			new Wifip2pRequestPeers(eventHelper).execute(p2p);

		} else if (WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
			WifiP2pDevice me = (WifiP2pDevice) intent
					.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE);

			thisDevice.setText(getWifiP2pDeviceInfo(me));
		}
	}

	void showMessage(int resId) {
		showMessage(getString(resId));
	}

	void showMessage(CharSequence msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	void showProgressDialog(CharSequence msg) {
		closeProgressDialog();

		ProgressDialog pd = new ProgressDialog(this,
				ProgressDialog.THEME_HOLO_LIGHT);
		progressDialog = pd;

		pd.setMessage(msg);
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.setIndeterminate(true);
		pd.setCancelable(true);
		pd.setCanceledOnTouchOutside(false);
		pd.show();
	}

	void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	void onChannelDisconnected() {
		showMessage(R.string.event_p2p_disable);
		resetData();
		p2p.reset();
	}

	private void connectPeer(WifiP2pDevice peer) {

		CharSequence dev = getWifiP2pDeviceInfo2(peer);
		CharSequence msg = Logger.fmt(getString(R.string.info_connect), dev);
		CharSequence msg2 = Logger
				.fmt(getString(R.string.info_connecting), dev);
		CommandHelper cmd = new CommandHelper(new Wifip2pConnectPeer(peer,
				eventHelper), p2p, msg2);

		new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
				.setTitle(R.string.lab_p2p_connect).setMessage(msg)
				.setNegativeButton(R.string.action_cancel, cmd)
				.setPositiveButton(R.string.action_ok, cmd).show();
	}

	private void disconnectPeer(WifiP2pDevice peer) {

		CharSequence dev = getWifiP2pDeviceInfo2(peer);
		CharSequence msg = Logger.fmt(getString(R.string.info_disconnect), dev);
		CommandHelper cmd = new CommandHelper(new Wifip2pCancelConnect(
				eventHelper), p2p, null);

		new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
				.setTitle(R.string.lab_p2p_disconnect).setMessage(msg)
				.setNegativeButton(R.string.action_cancel, cmd)
				.setPositiveButton(R.string.action_ok, cmd).show();
	}

	private void resetPeers() {
		peerdevs.clear();
		peers.clear();
	}

	private void resetData() {
		p2p.isWifiP2pEnabled = false;
		thisDevice.setText(null);
		resetPeers();
	}

	private CharSequence getWifiP2pDeviceInfo(WifiP2pDevice dev) {
		CharSequence sta = getWifiP2pDeviceStatus(dev);
		return Logger.fmt("%s [%s] [%s]", dev.deviceName, dev.deviceAddress,
				sta);
	}

	private CharSequence getWifiP2pDeviceInfo2(WifiP2pDevice dev) {
		return Logger.fmt("%s [%s]", dev.deviceName, dev.deviceAddress);
	}

	private CharSequence getWifiP2pDeviceStatus(WifiP2pDevice dev) {

		switch (dev.status) {
		case WifiP2pDevice.CONNECTED:
			return getString(R.string.status_p2p_connected);
		case WifiP2pDevice.INVITED:
			return getString(R.string.status_p2p_invited);
		case WifiP2pDevice.FAILED:
			return getString(R.string.status_p2p_failed);
		case WifiP2pDevice.AVAILABLE:
			return getString(R.string.status_p2p_available);
		case WifiP2pDevice.UNAVAILABLE:
		default:
			return getString(R.string.status_p2p_unavailable);
		}
	}

	private ArrayAdapter<CharSequence> peers;
	private ArrayList<WifiP2pDevice> peerdevs;
	private TextView thisDevice;
	private ProgressDialog progressDialog;
	private EventHelper eventHelper;
	private WiFiP2PManager p2p;

	private final class CommandHelper implements OnClickListener {
		private final WiFiP2PCommand command;
		private final WiFiP2PManager ctx;
		private final CharSequence message;

		CommandHelper(WiFiP2PCommand cmd, WiFiP2PManager p2p, CharSequence msg) {
			ctx = p2p;
			command = cmd;
			message = msg;
		}

		void execute() {
			command.execute(ctx);

			if (message != null)
				showProgressDialog(message);
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();

			if (which == DialogInterface.BUTTON_POSITIVE)
				execute();
		}
	}

	private final class EventHelper extends BroadcastReceiver implements
			ServiceFactory.SpyCallback, ChannelListener, OnItemClickListener {

		@Override
		public void handleMessage(int type, int status, Object obj) {
			ActivityManageP2P.this.handleMessage(type, status, obj);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			ActivityManageP2P.this.handleBroadcast(intent);
		}

		@Override
		public void onChannelDisconnected() {
			ActivityManageP2P.this.onChannelDisconnected();
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			ActivityManageP2P.this.handlePeerListClick(position);
		}
	}

	private final IntentFilter getWiFiP2PIntentFilter() {
		final IntentFilter ret = new IntentFilter();
		ret.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
		ret.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
		ret.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);
		ret.addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		return ret;
	}
}
