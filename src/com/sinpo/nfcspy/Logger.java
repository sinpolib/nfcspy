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

import java.util.ArrayList;

import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;

final class Logger {

	static CharSequence fmtHeader(CharSequence notes) {
		final StringBuilder header = new StringBuilder();
		final String CMT = "## ";

		final Time time = new Time();
		time.setToNow();

		header.append(CMT).append(ThisApplication.name()).append(" Log File v")
				.append(ThisApplication.version()).append('\n');

		header.append(CMT).append("System: Android ")
				.append(Build.VERSION.RELEASE).append(", ").append(Build.BRAND)
				.append(' ').append(Build.MODEL).append('\n');

		header.append(CMT).append("DateTime: ").append(fmtDate(time))
				.append(' ').append(fmtTime(time)).append('\n');

		header.append(CMT).append('\n');

		if (!TextUtils.isEmpty(notes)) {
			String[] lines = notes.toString().split("\\n");
			for (String line : lines)
				header.append(CMT).append(line).append('\n');

			header.append(CMT).append('\n');
		}

		header.append('\n');

		return header;
	}

	static CharSequence fmtChatIn(CharSequence msg) {
		return fmt(R.color.sta_chat_in, fmt(">> %s %s", now(), msg));
	}

	static CharSequence fmtChatOut(CharSequence msg) {
		return fmt(R.color.sta_chat_out, fmt("<< %s %s", now(), msg));
	}

	static CharSequence fmtMsg(String fm, Object... args) {
		return fmt(R.color.sta_msg, fmt("!!!! %s %s", now(), fmt(fm, args)));
	}

	static CharSequence fmtInfo(String fm, Object... args) {
		return fmt(R.color.sta_info, fmt("## %s %s", now(), fmt(fm, args)));
	}

	static CharSequence fmtWarn(String fm, Object... args) {
		return fmt(R.color.sta_warn, fmt("!!!! %s %s", now(), fmt(fm, args)));
	}

	static CharSequence fmtError(String fm, Object... args) {
		return fmt(R.color.sta_error, fmt("!!!! %s %s", now(), fmt(fm, args)));
	}

	static CharSequence fmtApdu(int delay, boolean isCmd, byte[] apdu) {
		long stamp = System.currentTimeMillis();
		return fmtApdu(currentCardId, stamp, delay, false, isCmd, apdu);
	}

	static CharSequence fmtApdu(String id, long stamp, long delay,
			boolean isHCE, boolean isCmd, byte[] raw) {

		final StringBuilder info = apduFormatter;
		info.setLength(0);

		final CharSequence hex, cmt;
		if (raw == null || raw.length == 0) {

			if (raw == ATTACH_MARKER)
				return fmtNfcAttachMessage(id, stamp);

			if (raw == DEACTIVE_MARKER)
				return fmtHceDeactivatedMessage(stamp);

			hex = "<NULL>";
			cmt = "No Data";
		} else {
			hex = toHexString(raw, 0, raw.length);
			cmt = ApduParser.parse(isCmd, raw);
		}

		final Time tm = new Time();
		tm.set(stamp);

		info.append('[').append(isCmd ? 'C' : 'R').append(' ');
		info.append(id).append(']').append(' ');
		info.append(fmtTime(tm)).append(' ').append(hex).append(' ');
		info.append(cmt).append(' ').append(isHCE ? '+' : '~');

		if (delay < 0 || delay > MAX_APDUDELAY_MS)
			info.append('?');
		else
			info.append(delay);

		info.append('m').append('s');
		
		return fmt(isCmd ? R.color.sta_apdu_in : R.color.sta_apdu_out, info);
	}

	static CharSequence fmtNfcAttachMessage(String cardId, long stamp) {
		Time time = new Time();
		time.set(stamp);

		return fmt(
				R.color.sta_msg,
				fmt(ThisApplication
						.getStringResource(R.string.event_nfc_attach),
						fmtTime(time), cardId, fmtDate(time)));
	}

	static CharSequence fmtHceDeactivatedMessage(long stamp) {
		Time time = new Time();
		time.set(stamp);

		return fmt(
				ThisApplication.getStringResource(R.string.status_deactivated),
				fmtTime(time), fmtDate(time));
	}

	static CharSequence fmt(int colorResId, CharSequence msg) {
		final int color = ThisApplication.getColorResource(colorResId);
		final SpannableString ret = new SpannableString(msg);
		final ForegroundColorSpan span = new ForegroundColorSpan(color);

		ret.setSpan(span, 0, msg.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		return ret;
	}

	static CharSequence fmt(String fm, Object... args) {
		return String.format(fm, args);
	}

	static String now() {
		Time time = new Time();
		time.setToNow();
		return fmtTime(time);
	}

	static String fmtDate(Time tm) {
		return String.format("%04d-%02d-%02d", tm.year, tm.month + 1,
				tm.monthDay);
	}

	static String fmtTime(Time tm) {
		return String.format("%02d:%02d:%02d", tm.hour, tm.minute, tm.second);
	}

	static String toHexString(byte[] d, int s, int n) {
		final char[] ret = new char[n * 2];
		final int e = s + n;

		int x = 0;
		for (int i = s; i < e; ++i) {
			final byte v = d[i];
			ret[x++] = HEX[0x0F & (v >> 4)];
			ret[x++] = HEX[0x0F & v];
		}
		return new String(ret);
	}

	private final static byte[] ATTACH_MARKER = new byte[0];
	private final static byte[] DEACTIVE_MARKER = new byte[0];

	private final static char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	static void logNfcAttach(String cardId) {
		currentCardId = cardId;
		logApdu(System.currentTimeMillis(), true, true, ATTACH_MARKER);
	}

	static void logHceDeactive() {
		logApdu(System.currentTimeMillis(), true, true, DEACTIVE_MARKER);
	}

	static long logApdu(long stamp, boolean isHCE, boolean isCmd, byte[] apdu) {

		ApduItem it = new ApduItem(currentCardId, stamp, isHCE, isCmd, apdu);
		if (isFull())
			apduLogs.set(cursor, it);
		else
			apduLogs.add(it);

		if (++cursor == MAX_LOGS)
			cursor = 0;

		return it.delay;
	}

	static void clearCachedLogs() {
		apduLogs.clear();
		cursor = 0;
	}

	static int getCachedLogsCount() {
		return apduLogs.size();
	}

	static CharSequence getCachedLog(int index) {
		if (isFull())
			index = (index + cursor - 1) % MAX_LOGS;

		ApduItem i = apduLogs.get(index);
		return fmtApdu(i.cardId, i.timestamp, i.delay, i.isHCE, i.isCmd, i.apdu);
	}

	static String getCurrentLogFileName() {
		Time t = new Time();
		t.setToNow();
		return String.format("nfcspy_%04d%02d%02d_%02d%02d%02d.log", t.year,
				t.month + 1, t.monthDay, t.hour, t.minute, t.second);
	}

	static void setCurrentCardId(String id) {
		currentCardId = id;
	}

	static void open() {
		ApduParser.init();
	}

	private static boolean isFull() {
		return apduLogs.size() == MAX_LOGS;
	}

	private static final long MAX_APDUDELAY_MS = 1999;
	private static final int MAX_LOGS = 512;

	private static int cursor = 0;
	private static String currentCardId = "";

	private static final StringBuilder apduFormatter = new StringBuilder();
	private static final ArrayList<ApduItem> apduLogs = new ArrayList<ApduItem>(
			MAX_LOGS);

	private static final class ApduItem {
		private static long lastApduStamp = 0;

		ApduItem(String id, long stamp, boolean isHCE, boolean isCmd, byte[] raw) {
			this.cardId = id;
			this.isHCE = isHCE;
			this.isCmd = isCmd;
			this.apdu = raw;
			this.timestamp = stamp;
			this.delay = stamp - lastApduStamp;

			lastApduStamp = stamp;
		}

		final byte[] apdu;
		final String cardId;
		final boolean isHCE;
		final boolean isCmd;
		final long timestamp;
		final long delay;
	}
}
