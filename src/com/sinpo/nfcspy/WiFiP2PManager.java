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

import static android.content.Context.WIFI_P2P_SERVICE;
import static com.sinpo.nfcspy.ServiceFactory.STA_P2P_INITED;
import static com.sinpo.nfcspy.ServiceFactory.STA_P2P_UNINIT;

import java.net.InetAddress;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;

final class WiFiP2PManager {
	// TODO maybe choooooose by setting panel
	final static int PORT = 2013;

	static boolean isWiFiEnabled(Context ctx) {
		return ((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE))
				.isWifiEnabled();
	}

	WiFiP2PManager() {
		reset();
	}

	void init(Context ctx, ChannelListener lsn) {
		wifip2p = (WifiP2pManager) ctx.getSystemService(WIFI_P2P_SERVICE);

		channel = wifip2p.initialize(ctx, ctx.getMainLooper(), lsn);

		if (wifip2p != null && channel != null) {
			isWifiP2pEnabled = true;
			status = STA_P2P_INITED;
		} else {
			isWifiP2pEnabled = false;
			status = STA_P2P_UNINIT;
		}
	}

	boolean isInited() {
		return status != STA_P2P_UNINIT;
	}

	boolean isConnected() {
		return (p2pSocket != null) && p2pSocket.isConnected();
	}

	boolean sendData(int type, byte[] data) {
		return isConnected() ? p2pSocket.sendData(type, data) : false;
	}

	void closeSocket() {
		if (p2pSocket != null) {
			p2pSocket.close();
			p2pSocket = null;
		}
	}

	void reset() {

		closeSocket();

		peerName = null;
		groupOwnerAddress = null;
		isGroupOwner = false;
		isWifiP2pConnected = false;
		channel = null;
		status = STA_P2P_UNINIT;
	}

	String peerName;
	InetAddress groupOwnerAddress;
	boolean isGroupOwner;
	boolean isWifiP2pConnected;
	boolean isWifiP2pEnabled;
	WiFiP2PSocket p2pSocket = null;

	private int status;
	WifiP2pManager wifip2p;
	Channel channel;
}
