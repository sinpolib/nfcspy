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

import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_CONNECT;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_DISCONN;
import static com.sinpo.nfcspy.ServiceFactory.STA_FAIL;
import static com.sinpo.nfcspy.ServiceFactory.STA_P2P_ACCEPT;
import static com.sinpo.nfcspy.ServiceFactory.STA_P2P_CLIENT;
import static com.sinpo.nfcspy.ServiceFactory.STA_SUCCESS;
import static com.sinpo.nfcspy.ThisApplication.PRIORITY_HIGH;
import static com.sinpo.nfcspy.ThisApplication.setCurrentThreadPriority;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

final class WiFiP2PSocket extends Thread {
	static final int BUF_SIZE = 512;

	ServerSocket server;
	Socket client;

	private ServiceFactory.SpyCallback callback;
	private DataInputStream iStream;
	private DataOutputStream oStream;

	WiFiP2PSocket(ServiceFactory.SpyCallback callback) {
		this.callback = callback;
	}

	synchronized boolean isConnected() {

		if (client == null)
			return false;

		return (server != null) ? server.isBound() : client.isConnected();
	}

	boolean sendData(int type, byte[] data) {
		if (data != null && data.length > 0 && oStream != null) {

			try {
				oStream.writeInt(type);
				oStream.writeInt(data.length);
				oStream.write(data);
				oStream.flush();
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	synchronized void close() {
		iStream = null;
		oStream = null;
		try {
			if (client != null && !client.isClosed())
				client.close();
		} catch (Throwable e) {
		}
		client = null;

		try {
			if (server != null && !server.isClosed())
				server.close();
		} catch (Throwable e) {
		}
		server = null;
	}

	@Override
	public void run() {
		synchronized (this) {
			notifyAll();
		}

		setCurrentThreadPriority(PRIORITY_HIGH);

		try {
			iStream = new DataInputStream(client.getInputStream());
			oStream = new DataOutputStream(new BufferedOutputStream(
					client.getOutputStream(), BUF_SIZE));

			while (true) {

				int type = iStream.readInt();
				int len = iStream.readInt();

				byte[] data = new byte[len];

				iStream.readFully(data);

				callback.handleMessage(type, STA_SUCCESS, data);
			}
		} catch (Exception e) {
		} finally {
			close();
			callback.handleMessage(MSG_P2P_DISCONN, STA_SUCCESS, null);
		}
	}
}

final class SocketConnector extends Thread {

	private final ServiceFactory.SpyCallback callback;
	final WiFiP2PManager context;

	SocketConnector(WiFiP2PManager ctx, ServiceFactory.SpyCallback cb) {
		callback = cb;
		context = ctx;
	}

	@Override
	public void run() {
		final WiFiP2PManager ctx = context;

		ctx.closeSocket();

		WiFiP2PSocket p2p = new WiFiP2PSocket(callback);
		ctx.p2pSocket = p2p;

		Socket client = null;
		ServerSocket server = null;

		try {

			if (ctx.isGroupOwner) {
				fireCallback(STA_P2P_ACCEPT);

				server = new ServerSocket(WiFiP2PManager.PORT);
				p2p.server = server;

				client = server.accept();
				p2p.client = client;
			} else {
				fireCallback(STA_P2P_CLIENT);

				server = null;
				p2p.server = server;

				client = new Socket();
				p2p.client = client;

				client.bind(null);
				client.connect(new InetSocketAddress(ctx.groupOwnerAddress,
						WiFiP2PManager.PORT), 8000);
			}

		} catch (Exception e) {

			safeClose(client);
			client = null;
			p2p.client = null;

			safeClose(server);
			p2p.server = null;

			ctx.p2pSocket = null;
		}

		if (client != null && client.isConnected()) {

			try {
				tuneupSocket(client);

				p2p.start();

				synchronized (p2p) {
					p2p.wait();
				}

				fireCallback(STA_SUCCESS);
			} catch (Exception e) {
				fireCallback(STA_FAIL);
			}

		} else {
			fireCallback(STA_FAIL);
		}
	}

	protected void fireCallback(int sta) {
		callback.handleMessage(MSG_P2P_CONNECT, sta, null);
	}

	private static void tuneupSocket(Socket skt) throws SocketException {
		skt.setTcpNoDelay(true);
		skt.setTrafficClass(0x04 | 0x10);

		if (skt.getSendBufferSize() < WiFiP2PSocket.BUF_SIZE)
			skt.setSendBufferSize(WiFiP2PSocket.BUF_SIZE);

		if (skt.getReceiveBufferSize() < WiFiP2PSocket.BUF_SIZE)
			skt.setReceiveBufferSize(WiFiP2PSocket.BUF_SIZE);
	}

	private static void safeClose(Closeable obj) {
		try {
			if (obj != null)
				obj.close();
		} catch (Throwable e) {
		}
	}
}
