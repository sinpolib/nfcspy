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

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Application;
import android.content.res.XmlResourceParser;

public final class ThisApplication extends Application implements
		UncaughtExceptionHandler {
	final static int PRIORITY_HIGH = android.os.Process.THREAD_PRIORITY_URGENT_AUDIO;
	final static int PRIORITY_NORMAL = android.os.Process.THREAD_PRIORITY_FOREGROUND;
	final static int PRIORITY_LOW = android.os.Process.THREAD_PRIORITY_DEFAULT;
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		System.exit(0);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		//Thread.setDefaultUncaughtExceptionHandler(this);
		
		if (getCurrentThreadPriority() > PRIORITY_NORMAL)
			setCurrentThreadPriority(PRIORITY_NORMAL);

		instance = this;
	}
	
	static int getCurrentThreadPriority() {
		try {
			int tid = android.os.Process.myTid();
			return android.os.Process.getThreadPriority(tid);
		} catch (Exception e) {
			return PRIORITY_LOW;
		}
	}
	
	static void setCurrentThreadPriority(int priority) {
		try {
			int tid = android.os.Process.myTid();
			android.os.Process.setThreadPriority(tid, priority);
		} catch (Exception e) {
		}
	}

	static String name() {
		return getStringResource(R.string.app_name);
	}

	static String version() {
		try {
			return instance.getPackageManager().getPackageInfo(
					instance.getPackageName(), 0).versionName;
		} catch (Exception e) {
			return "1.0";
		}
	}

	static int getColorResource(int resId) {
		return instance.getResources().getColor(resId);
	}

	static String getStringResource(int resId) {
		return instance.getString(resId);
	}
	
	static XmlResourceParser getXmlResource(int resId) {
		return instance.getResources().getXml(resId);
	}
	
	private static ThisApplication instance;
}
