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

import static android.nfc.NfcAdapter.EXTRA_TAG;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

final class NfcManager {
	final static String KEY_DISCOVERYDELAY = "opt_discovery_delay";
	final static int DEF_DISCOVERYDELAY = 3000;

	interface TagListener {
		void onNewTagIntent(Intent intent);
	}

	static boolean hasHCE() {
		return VERSION.SDK_INT >= VERSION_CODES.KITKAT;
	}

	NfcManager(Activity activity) {
		this.activity = activity;
		nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		pendingIntent = PendingIntent.getActivity(activity, 0, new Intent(
				activity, activity.getClass())
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	void onPause() {
		if (nfcAdapter != null) {
			if (hasHCE())
				setReaderMode(false, 0);
			else
				nfcAdapter.disableForegroundDispatch(activity);
		}
	}

	void onResume(TagListener listener) {
		this.tagListener = listener;

		if (nfcAdapter != null) {
			if (hasHCE()) {
				setReaderMode(true, getDiscoveryDelay());
			} else {
				nfcAdapter.enableForegroundDispatch(activity, pendingIntent,
						TAGFILTERS, TECHLISTS);
			}
		}
	}

	boolean isEnabled() {
		return nfcAdapter != null && nfcAdapter.isEnabled();
	}

	int getDiscoveryDelay() {
		return ThisApplication.getPreference(KEY_DISCOVERYDELAY,
				DEF_DISCOVERYDELAY);
	}

	void setDiscoveryDelay(int delay) {
		ThisApplication.setPreference(KEY_DISCOVERYDELAY, delay);
		setReaderMode(true, delay);
	}

	@SuppressLint("NewApi")
	private void setReaderMode(boolean enable, int delay) {
		if (nfcAdapter == null || !hasHCE())
			return;

		if (!enable) {
			nfcAdapter.disableReaderMode(activity);
			return;
		}

		Bundle opts = new Bundle();
		opts.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000);

		int flags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
		flags |= NfcAdapter.FLAG_READER_NFC_A;

		// For the 'READ BINARY' problem of Braodcom's NFC stack.
		// Only works on Android 4.4+
		nfcAdapter.enableReaderMode(activity, new ReaderCallback() {

			@Override
			public void onTagDiscovered(Tag tag) {
				Intent i = new Intent().putExtra(EXTRA_TAG, tag);
				tagListener.onNewTagIntent(i);
			}
		}, flags, opts);
	}

	static IsoDep attachCard(Intent intent) {
		Tag tag = (Tag) intent.getParcelableExtra(EXTRA_TAG);
		return (tag != null) ? IsoDep.get(tag) : null;
	}

	static String getCardId(IsoDep isodep) {
		if (isodep != null) {
			byte[] id = isodep.getTag().getId();
			if (id != null && id.length > 0)
				return Logger.toHexString(id, 0, id.length);
		}

		return "UNKNOWN";
	}

	static void disconnect(IsoDep tag) {
		try {
			if (tag != null)
				tag.close();
		} catch (Exception e) {
		}
	}

	static byte[] transceiveApdu(IsoDep tag, byte[] cmd) {
		if (tag != null) {
			try {
				if (!tag.isConnected()) {
					tag.connect();
					tag.setTimeout(10000);
				}

				return tag.transceive(cmd);
			} catch (Exception e) {
			}
		}
		return null;
	}

	private final Activity activity;
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private TagListener tagListener;

	private static String[][] TECHLISTS;
	private static IntentFilter[] TAGFILTERS;

	static {
		try {
			TECHLISTS = new String[][] { { IsoDep.class.getName() } };

			TAGFILTERS = new IntentFilter[] { new IntentFilter(
					NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") };
		} catch (Exception e) {
		}
	}
}
