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

import static com.sinpo.nfcspy.ServiceFactory.STA_FAIL;
import static com.sinpo.nfcspy.ServiceFactory.STA_SUCCESS;
import static com.sinpo.nfcspy.ServiceFactory.STA_UNKNOWN;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

abstract class WiFiP2PCommand {
	final static int CMD_DiscoverPeers = 0x01000000;
	final static int CMD_StopPeerDiscovery = 0x02000000;
	final static int CMD_RequestPeers = 0x03000000;
	final static int CMD_ConnectPeer = 0x04000000;
	final static int CMD_CancelConnect = 0x05000000;
	final static int CMD_RemoveGroup = 0x06000000;
	final static int CMD_RequestConnectionInfo = 0x07000000;

	WiFiP2PCommand(ServiceFactory.SpyCallback cb) {
		callback = cb;
		status = STA_UNKNOWN;
	}

	void setListener(ServiceFactory.SpyCallback cb) {
		callback = cb;
	}

	boolean isFinished() {
		return status != STA_UNKNOWN;
	}

	boolean isSuccessed() {
		return status == STA_SUCCESS;
	}

	void execute(WiFiP2PManager ctx) {
		if (!ctx.isInited()) {
			onFinished(STA_FAIL);
		} else {
			executeImp(ctx);
		}
	}

	protected void onFinished(int sta) {
		status = sta;

		if (callback != null)
			callback.handleMessage(id(), sta, null);
	}

	protected void onFinished(int sta, Object obj) {
		status = sta;

		if (callback != null)
			callback.handleMessage(id(), sta, obj);
	}

	abstract protected int id();

	abstract protected void executeImp(WiFiP2PManager ctx);

	protected int status;
	protected int error;
	private ServiceFactory.SpyCallback callback;
}

abstract class WiFiP2PAction extends WiFiP2PCommand implements ActionListener {

	WiFiP2PAction(ServiceFactory.SpyCallback callback) {
		super(callback);
	}

	public void onSuccess() {
		onFinished(STA_SUCCESS);
	}

	public void onFailure(int reason) {
		error = reason;
		onFinished(STA_FAIL);
	}
}

final class Wifip2pRequestConnectionInfo extends WiFiP2PCommand implements
		ConnectionInfoListener {

	Wifip2pRequestConnectionInfo(ServiceFactory.SpyCallback callback) {
		super(callback);
	}

	@Override
	protected void executeImp(WiFiP2PManager ctx) {
		this.ctx = ctx;
		ctx.wifip2p.requestConnectionInfo(ctx.channel, this);
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {

		if (info.groupFormed) {
			ctx.groupOwnerAddress = info.groupOwnerAddress;
			ctx.isGroupOwner = info.isGroupOwner;
			ctx.isWifiP2pConnected = true;
			onFinished(STA_SUCCESS, info);
		} else {
			ctx.groupOwnerAddress = null;
			ctx.isGroupOwner = false;
			ctx.isWifiP2pConnected = false;
			onFinished(STA_FAIL);
		}
	}

	private WiFiP2PManager ctx;

	@Override
	protected int id() {
		return CMD_RequestConnectionInfo;
	}
}

final class Wifip2pDiscoverPeers extends WiFiP2PAction {

	Wifip2pDiscoverPeers(ServiceFactory.SpyCallback callback) {
		super(callback);
	}

	@Override
	protected int id() {
		return CMD_DiscoverPeers;
	}

	@Override
	protected void executeImp(WiFiP2PManager ctx) {
		ctx.wifip2p.discoverPeers(ctx.channel, this);
	}
}

final class Wifip2pStopPeerDiscovery extends WiFiP2PAction {

	Wifip2pStopPeerDiscovery(ServiceFactory.SpyCallback callback) {
		super(callback);
	}

	@Override
	protected int id() {
		return CMD_StopPeerDiscovery;
	}

	@Override
	protected void executeImp(WiFiP2PManager ctx) {
		ctx.wifip2p.stopPeerDiscovery(ctx.channel, this);
	}
}

final class Wifip2pRequestPeers extends WiFiP2PCommand implements
		PeerListListener {

	Wifip2pRequestPeers(ServiceFactory.SpyCallback callback) {
		super(callback);
	}

	@Override
	protected int id() {
		return CMD_RequestPeers;
	}

	@Override
	protected void executeImp(WiFiP2PManager ctx) {
		ctx.wifip2p.requestPeers(ctx.channel, this);
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		onFinished(STA_SUCCESS, peers.getDeviceList());
	}
}

final class Wifip2pConnectPeer extends WiFiP2PAction {

	Wifip2pConnectPeer(WifiP2pDevice peer, ServiceFactory.SpyCallback callback) {
		super(callback);
		this.peer = peer;
	}

	@Override
	protected int id() {
		return CMD_ConnectPeer;
	}

	@Override
	protected void executeImp(WiFiP2PManager ctx) {

		final WifiP2pConfig config = new WifiP2pConfig();
		config.wps.setup = android.net.wifi.WpsInfo.PBC;
		config.deviceAddress = peer.deviceAddress;
		ctx.wifip2p.connect(ctx.channel, config, this);
	}

	private final WifiP2pDevice peer;
}

final class Wifip2pCancelConnect extends WiFiP2PAction {

	Wifip2pCancelConnect(ServiceFactory.SpyCallback callback) {
		super(callback);
	}

	@Override
	protected int id() {
		return CMD_CancelConnect;
	}

	@Override
	protected void executeImp(WiFiP2PManager ctx) {
		ctx.wifip2p.cancelConnect(ctx.channel, this);
	}
}

final class Wifip2pRemoveGroup extends WiFiP2PAction {

	Wifip2pRemoveGroup(ServiceFactory.SpyCallback callback) {
		super(callback);
	}

	@Override
	protected int id() {
		return CMD_RemoveGroup;
	}

	@Override
	protected void executeImp(WiFiP2PManager ctx) {
		ctx.wifip2p.removeGroup(ctx.channel, this);
	}
}
