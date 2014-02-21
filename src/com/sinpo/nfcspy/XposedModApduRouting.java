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

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findField;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public final class XposedModApduRouting implements IXposedHookLoadPackage {

	@Override
	public void handleLoadPackage(LoadPackageParam pkg) throws Throwable {
		if ("com.android.nfc".equals(pkg.packageName)) {

			final String CLASS = "com.android.nfc.cardemulation.HostEmulationManager";
			final String METHOD = "findSelectAid";

			try {

				findAndHookMethod(CLASS, pkg.classLoader, METHOD, byte[].class,
						findSelectAidHook);

			} catch (Exception e) {
			}
		}
	}

	private final static String AID_NFCSPY = "F04E4643535059";

	private final XC_MethodHook findSelectAidHook = new XC_MethodHook() {

		@Override
		protected void beforeHookedMethod(MethodHookParam param)
				throws Throwable {

			final byte[] data = (byte[]) param.args[0];
			final int len = data.length;

			if (data != null && len > 0) {
				try {
					final Object THIS = param.thisObject;
					// static final int STATE_W4_SELECT = 1;
					if (findField(THIS.getClass(), "mState").getInt(THIS) == 1)
						param.setResult(AID_NFCSPY);
					else
						// just bypass orgin call;
						param.setResult(null);
				} catch (Exception e) {
				}
			}
		}
	};
}
