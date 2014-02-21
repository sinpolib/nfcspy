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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

@SuppressLint("NewApi")
public final class ServiceFull extends HostApduService implements
		ServiceFactory.SpyService {
	private ServiceAgent agent;

	@Override
	public void onCreate() {
		super.onCreate();
		agent = new ServiceAgent(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		agent.handleIntent(intent);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		agent.onDestroy();
		agent = null;
		super.onDestroy();
	}

	@Override
	public void onDeactivated(int reason) {
		if (DEACTIVATION_LINK_LOSS == reason)
			agent.onDeactivated(reason);
	}

	@Override
	public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
		return agent.processCommandApdu(commandApdu, extras);
	}

	@Override
	public void processResponseApdu(byte[] apdu) {
		sendResponseApdu(apdu);
	}
}
