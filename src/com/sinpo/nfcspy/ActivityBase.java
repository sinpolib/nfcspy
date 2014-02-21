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

import android.app.Activity;
import android.os.Handler;

abstract class ActivityBase extends Activity {

	@Override
	public void onBackPressed() {
		if (!safeExit.isLocked())
			super.onBackPressed();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus)
			safeExit.unlockDelayed(800);
		else
			safeExit.setLock(true);
	}

	private final SafeExitLock safeExit = new SafeExitLock();

	private final static class SafeExitLock extends Handler implements Runnable {
		private boolean lock;

		@Override
		public void run() {
			setLock(false);
		}

		boolean isLocked() {
			return lock;
		}

		void setLock(boolean lock) {
			this.lock = lock;
		}

		void unlockDelayed(long delayMillis) {
			postDelayed(this, delayMillis);
		}

		SafeExitLock() {
			setLock(true);
		}
	}
}
