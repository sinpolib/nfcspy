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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

final class ServiceFactory {
	final static String ACTION_SERVER_START = "ACTION_SERVER_START";
	final static String ACTION_SERVER_STOP = "ACTION_SERVER_STOP";
	final static String ACTION_SERVER_SET = "ACTION_SERVER_SET";
	final static String ACTION_SERVER_CHAT = "ACTION_SERVER_CHAT";
	final static String ACTION_NFC_ATTACH = "ACTION_NFC_ATTACH";
	final static String ACTION_NFC_DETTACH = "ACTION_NFC_DETTACH";

	final static int MSG_P2P_INIT = 0x11;
	final static int MSG_P2P_SOCKET = 0x12;
	final static int MSG_P2P_CONNECT = 0x13;
	final static int MSG_P2P_DISCONN = 0x14;

	final static int MSG_SERVER_KILL = 0x21;
	final static int MSG_SERVER_START = 0x22;
	final static int MSG_SERVER_STOP = 0x23;
	final static int MSG_SERVER_VER = 0x24;

	final static int MSG_HCE_ATTACH = 0x31;
	final static int MSG_HCE_DETTACH = 0x32;
	final static int MSG_HCE_DEACTIVATED = 0x33;
	final static int MSG_HCE_APDU_CMD = 0x34;
	final static int MSG_HCE_APDU_RSP = 0x35;

	final static int MSG_CHAT_SEND = 0x41;
	final static int MSG_CHAT_RECV = 0x42;

	final static int STA_NOTCARE = 0xFFFFFFFF;
	final static int STA_UNKNOWN = 0x00;
	final static int STA_SUCCESS = 0x01;
	final static int STA_FAIL = 0x02;
	final static int STA_ERROR = 0x03;

	final static int STA_P2P_UNINIT = 0x00;
	final static int STA_P2P_INITED = 0x10;
	final static int STA_P2P_WATING = 0x11;
	final static int STA_P2P_ACCEPT = 0x12;
	final static int STA_P2P_CLIENT = 0x13;
	
	final static int ERR_APDU_RSP = 0x21;
	final static int ERR_APDU_CMD = 0x22;
	final static int ERR_P2P = 0x23;

	interface SpyService {
		void processResponseApdu(byte[] apdu);

		byte[] processCommandApdu(byte[] commandApdu, Bundle extras);
	}

	interface SpyCallback {
		void handleMessage(int type, int status, Object obj);
	}

	static void startServer(Context ctx, Messenger reply) {
		final Intent i = newServerIntent(ctx, ACTION_SERVER_START);
		ctx.startService(i.putExtra(KEY_REPLY, reply));
	}

	static void setTag2Server(Context ctx, Intent nfc) {
		final Intent i = newServerIntent(ctx, ACTION_NFC_ATTACH);
		ctx.startService(i.replaceExtras(nfc));
	}
	
	static void resetTag2Server(Context ctx) {
		ctx.startService(newServerIntent(ctx, ACTION_NFC_DETTACH));
	}
	
	static void setHighSpeed2Server(Context ctx, boolean set) {
		final Intent i = newServerIntent(ctx, ACTION_SERVER_SET);
		ctx.startService(i.putExtra(KEY_HISPD, set));
	}

	static void sendChatMessage2Server(Context ctx, String msg) {
		final Intent i = newServerIntent(ctx, ACTION_SERVER_CHAT);
		ctx.startService(i.putExtra(KEY_CHAT, msg));
	}

	static void stopServer(Context ctx) {
		ctx.stopService(newServerIntent(ctx, ACTION_SERVER_STOP));
	}

	static Messenger getReplyMessengerExtra(Intent intent) {
		return (Messenger) intent.getParcelableExtra(KEY_REPLY);
	}

	static String getChatMessageExtra(Intent intent) {
		return intent.getStringExtra(KEY_CHAT);
	}

	static boolean getHighSpeedSettingExtra(Intent intent) {
		return intent.getBooleanExtra(KEY_HISPD, false);
	}
	
	static byte[] extractDataFromMessage(Message msg) {
		Bundle data = msg.getData();
		return (data != null) ? data.getByteArray(KEY_BLOB) : null;
	}

	static void setDataToMessage(Message msg, byte[] raw) {
		Bundle data = new Bundle();
		data.putByteArray(KEY_BLOB, raw);
		msg.setData(data);
	}

	private static Intent newServerIntent(Context ctx, String action) {

		Class<?> clazz = ServiceLite.class;
		if (NfcManager.hasHCE()) {
			try {
				clazz = Class.forName("com.sinpo.nfcspy.ServiceFull");
			} catch (Exception e) {
				clazz = ServiceLite.class;
			}
		}

		return new Intent(action, null, ctx, clazz);
	}

	private final static String KEY_REPLY = "KEY_REPLY";
	private final static String KEY_HISPD = "KEY_HISPD";
	private final static String KEY_CHAT = "KEY_CHAT";
	private final static String KEY_BLOB = "KEY_BLOB";
}
