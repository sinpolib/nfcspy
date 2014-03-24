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

import static com.sinpo.nfcspy.ServiceFactory.ERR_APDU_CMD;
import static com.sinpo.nfcspy.ServiceFactory.ERR_APDU_RSP;
import static com.sinpo.nfcspy.ServiceFactory.MSG_CHAT_RECV;
import static com.sinpo.nfcspy.ServiceFactory.MSG_CHAT_SEND;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_APDU_CMD;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_APDU_RSP;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_ATTACH;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_DEACTIVATED;
import static com.sinpo.nfcspy.ServiceFactory.MSG_HCE_DETTACH;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_CONNECT;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_DISCONN;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_INIT;
import static com.sinpo.nfcspy.ServiceFactory.MSG_P2P_SOCKET;
import static com.sinpo.nfcspy.ServiceFactory.MSG_SERVER_VER;
import static com.sinpo.nfcspy.ServiceFactory.STA_ERROR;
import static com.sinpo.nfcspy.ServiceFactory.STA_FAIL;
import static com.sinpo.nfcspy.ServiceFactory.STA_NOTCARE;
import static com.sinpo.nfcspy.ServiceFactory.STA_P2P_ACCEPT;
import static com.sinpo.nfcspy.ServiceFactory.STA_P2P_CLIENT;
import static com.sinpo.nfcspy.ServiceFactory.STA_SUCCESS;

import java.io.File;
import java.io.FileOutputStream;

import com.sinpo.nfcspy.NfcManager.TagListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityMain extends ActivityBase implements Handler.Callback,
		TagListener {

	public ActivityMain() {
		inbox = new Messenger(new Handler(this));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		messages = new ArrayAdapter<CharSequence>(this,
				R.layout.listitem_message);

		((ListView) findViewById(R.id.list)).setAdapter(messages);

		messageView = (TextView) findViewById(R.id.txtChatLine);

		nfc = new NfcManager(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_copy:
			copyLogs();
			return true;
		case R.id.action_save:
			saveLogs();
			return true;
		case R.id.action_share:
			shareLogs();
			return true;
		case R.id.action_settings:
			startActivity(new Intent(this, ActivityManageP2P.class));
			return true;
		case R.id.action_viewlogs:
			showHCELogs();
			return true;
		case R.id.action_clearlogs:
			clearHCELogs();
			return true;
		case R.id.action_discoverydelay:
			setDiscoveryDelay();
			return true;
		case R.id.action_about:
			showHelp();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void startServer(View ignore) {
		if (!nfc.isEnabled()) {
			messages.add(Logger.fmtError(getString(R.string.status_no_nfc)));
			return;
		}

		if (!WiFiP2PManager.isWiFiEnabled(this)) {
			messages.add(Logger.fmtError(getString(R.string.status_no_wifi)));
			return;
		}

		if (!NfcManager.hasHCE())
			messages.add(Logger.fmtWarn(getString(R.string.status_no_hce)));

		ServiceFactory.startServer(this, inbox);
	}

	public void stopServer(View ignore) {
		ServiceFactory.stopServer(this);
	}

	public void clearList(View ignore) {
		messages.clear();
	}

	public void enableHighSpeed(View v) {
		ServiceFactory.setHighSpeed2Server(this, ((CheckBox) v).isChecked());
	}

	public void sendChatMessage(View ignore) {
		final String msg = messageView.getText().toString();
		if (!TextUtils.isEmpty(msg))
			ServiceFactory.sendChatMessage2Server(this, msg);
	}

	@Override
	protected void onResume() {
		super.onResume();
		nfc.onResume(this);
	}

	@Override
	protected void onPause() {
		nfc.onPause();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		nfc = null;
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		onNewTagIntent(intent);
	}

	@Override
	public void onNewTagIntent(Intent intent) {
		ServiceFactory.setTag2Server(this, intent);
	}

	@Override
	public boolean handleMessage(Message msg) {

		switch (msg.what) {

		case MSG_HCE_APDU_CMD:
		case MSG_HCE_APDU_RSP:
			printHceApdu(msg);
			break;
		case MSG_HCE_ATTACH:
			printNfcAttachMessage(msg);
			break;
		case MSG_HCE_DEACTIVATED:
			printHceDeactivatedMessage();
			break;
		case MSG_HCE_DETTACH:
			printStatusMessage(R.string.event_nfc_lost, STA_ERROR, 0);
			break;
		case MSG_CHAT_SEND:
		case MSG_CHAT_RECV:
			printChatMessage(msg);
			break;
		case MSG_P2P_SOCKET:
			printStatusMessage(R.string.status_getaddr, msg.arg1, msg.arg2);
			break;
		case MSG_P2P_CONNECT:
			printStatusMessage(R.string.status_connect, msg.arg1, msg.arg2);
			break;
		case MSG_P2P_DISCONN:
			printStatusMessage(R.string.status_disconn, msg.arg1, msg.arg2);
			break;
		case MSG_P2P_INIT:
			printStatusMessage(R.string.status_init, msg.arg1, msg.arg2);
			break;
		case MSG_SERVER_VER:
			printVersionInfo(msg);
			break;
		default:
			return false;
		}
		return true;
	}

	private void printStatusMessage(int descId, int status, int error) {
		final CharSequence desc;
		if (status == STA_NOTCARE) {
			desc = Logger.fmtInfo(getString(descId));
		} else if (status == STA_SUCCESS) {
			String sta = getString(R.string.status_success);
			desc = Logger.fmtInfo("%s%s", getString(descId), sta);
		} else if (status == STA_FAIL) {
			String sta = getString(R.string.status_failure);
			desc = Logger.fmtError("%s%s", getString(descId), sta);
		} else if (status == STA_ERROR) {
			desc = Logger.fmtError(getString(descId));
		} else {

			if (status == STA_P2P_ACCEPT)
				descId = R.string.status_p2p_accept;
			else if (status == STA_P2P_CLIENT)
				descId = R.string.status_p2p_client;

			String sta = getString(R.string.status_waitting);
			desc = Logger.fmtWarn("%s %s", getString(descId), sta);
		}

		messages.add(desc);
	}

	private void printChatMessage(Message msg) {

		byte[] raw = ServiceFactory.extractDataFromMessage(msg);
		if (raw != null && raw.length > 0) {
			String txt = new String(raw);

			if (MSG_CHAT_SEND == msg.what) {
				messages.add(Logger.fmtChatOut(txt));
				messageView.setText(null);
			} else {
				messages.add(Logger.fmtChatIn(txt));
			}
		}
	}

	private void printVersionInfo(Message msg) {

		byte[] raw = ServiceFactory.extractDataFromMessage(msg);
		if (raw != null && raw.length > 0) {
			String peer = new String(raw);
			String me = ThisApplication.version();
			if (me.equals(peer)) {
				String fmt = getString(R.string.event_p2p_version);
				messages.add(Logger.fmtInfo(fmt, me));
			} else {
				String fmt = getString(R.string.event_p2p_version2);
				messages.add(Logger.fmtWarn(fmt, peer, me));
			}
		}
	}

	private void printNfcAttachMessage(Message msg) {

		byte[] raw = ServiceFactory.extractDataFromMessage(msg);
		if (raw != null && raw.length > 0) {
			long stamp = System.currentTimeMillis();
			messages.add(Logger.fmtNfcAttachMessage(new String(raw), stamp));
		}
	}

	private void printHceDeactivatedMessage() {
		long stamp = System.currentTimeMillis();
		messages.add(Logger.fmtHceDeactivatedMessage(stamp));
	}

	private void printHceApdu(Message msg) {
		byte[] apdu = ServiceFactory.extractDataFromMessage(msg);
		if (apdu != null && apdu.length > 0) {
			boolean isCmd = (MSG_HCE_APDU_CMD == msg.what);
			messages.add(Logger.fmtApdu(msg.arg2, isCmd, apdu));
		} else {
			String hint;
			if (STA_ERROR == msg.arg1) {
				if (ERR_APDU_CMD == msg.arg2)
					hint = getString(R.string.event_p2p_connect);
				else if (ERR_APDU_RSP == msg.arg2)
					hint = getString(R.string.event_nfc_rsp);
				else
					hint = getString(R.string.event_p2p_connect);
			} else {
				hint = getString(R.string.event_nfc_lost);
			}

			messages.add(Logger.fmtError(hint));
		}
	}

	private void shareLogs() {
		CharSequence logs = getAllLogs();
		if (!TextUtils.isEmpty(logs)) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_TEXT, logs);
			intent.setType("text/plain");
			startActivity(intent);
		}
	}

	private void copyLogs() {
		CharSequence logs = getAllLogs();
		if (!TextUtils.isEmpty(logs)) {
			((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
					.setText(logs);

			Toast.makeText(this, R.string.event_log_copy, Toast.LENGTH_LONG)
					.show();
		}
	}

	private void showHCELogs() {
		int n = Logger.getCachedLogsCount();
		for (int i = 0; i < n; ++i)
			messages.add(Logger.getCachedLog(i));
	}

	private void clearHCELogs() {
		Logger.clearCachedLogs();
	}

	private void setDiscoveryDelay() {

		final EditText input = new EditText(this);
		input.setHint(this.getString(R.string.hint_discoverydelay));
		input.setText(Integer.toString(nfc.getDiscoveryDelay()));
		input.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
		input.setKeyListener(DigitsKeyListener.getInstance("01234567890"));
		input.setSingleLine(true);

		SetDelayHelper helper = new SetDelayHelper(nfc, input);

		new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
				.setTitle(R.string.action_discoverydelay)
				.setMessage(R.string.lab_discoverydelay).setView(input)
				.setPositiveButton(R.string.action_ok, helper)
				.setNegativeButton(R.string.action_cancel, helper).show();
	}

	private void showHelp() {
		CharSequence title = Logger.fmt("%s v%s", ThisApplication.name(),
				ThisApplication.version());
		CharSequence info = Html.fromHtml(getString(R.string.info_about));

		TextView tv = (TextView) getLayoutInflater().inflate(
				R.layout.dialog_message, null);
		tv.setLinksClickable(true);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(info);

		new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
				.setTitle(title).setView(tv)
				.setNeutralButton(R.string.action_ok, null).show();
	}

	private void saveLogs() {
		CharSequence logs = getAllLogs();
		if (!TextUtils.isEmpty(logs)) {
			View root = getLayoutInflater().inflate(R.layout.dialog_savelog,
					null);
			EditText name = (EditText) root.findViewById(R.id.file);
			name.setText(Logger.getCurrentLogFileName());

			EditText note = (EditText) root.findViewById(R.id.note);

			SaveLogHelper helper = new SaveLogHelper(logs, name, note);

			new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
					.setTitle(R.string.action_save).setView(root)
					.setNegativeButton(R.string.action_cancel, helper)
					.setPositiveButton(R.string.action_ok, helper).show();
		}
	}

	private CharSequence getAllLogs() {
		StringBuilder ret = new StringBuilder();

		final ArrayAdapter<CharSequence> messages = this.messages;
		final int count = messages.getCount();
		for (int i = 0; i < count; ++i) {
			if (i > 0)
				ret.append("\n\n");

			ret.append(messages.getItem(i));
		}

		return ret;
	}

	private static final class SetDelayHelper implements
			DialogInterface.OnClickListener {

		SetDelayHelper(NfcManager nfc, EditText input) {
			this.nfc = nfc;
			this.input = input;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				CharSequence text = input.getText();
				if (TextUtils.isEmpty(text))
					text = "";

				int delay = -1;
				try {
					delay = Integer.parseInt(text.toString().trim());
				} catch (Exception e) {
					delay = -1;
				}

				if (delay < 100)
					delay = nfc.getDiscoveryDelay();

				nfc.setDiscoveryDelay(delay);
			}

			nfc = null;
			input = null;
			dialog.dismiss();
		}

		private NfcManager nfc;
		private EditText input;
	}

	private static final class SaveLogHelper implements
			DialogInterface.OnClickListener {
		CharSequence logs;
		EditText nameView, noteView;

		SaveLogHelper(CharSequence logs, EditText name, EditText note) {
			this.logs = logs;
			this.nameView = name;
			this.noteView = note;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE)
				saveLogs();

			dialog.dismiss();
			nameView = null;
			noteView = null;
			logs = null;
		}

		private void saveLogs() {
			String file = nameView.getText().toString();
			if (TextUtils.isEmpty(file))
				return;

			Context ctx = nameView.getContext();
			String msg;
			try {
				File root = Environment.getExternalStorageDirectory();
				File path = new File(root, "/nfcspy/logs");

				File logf = new File(path, file);
				file = logf.getAbsolutePath();

				path.mkdirs();

				FileOutputStream os = new FileOutputStream(file);

				CharSequence note = Logger.fmtHeader(noteView.getText());
				if (!TextUtils.isEmpty(note))
					os.write(note.toString().getBytes());

				os.write(logs.toString().getBytes());
				os.close();

				msg = ctx.getString(R.string.event_log_save);
			} catch (Exception e) {
				msg = ctx.getString(R.string.event_log_notsave);
			}

			Toast.makeText(ctx, Logger.fmt(msg, file), Toast.LENGTH_LONG)
					.show();
		}
	}

	private NfcManager nfc;
	private Messenger inbox;
	private ArrayAdapter<CharSequence> messages;
	private TextView messageView;
}
