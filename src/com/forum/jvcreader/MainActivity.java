package com.forum.jvcreader;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.*;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcTopic;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.jvc.UpdatedTopicsManager;
import com.forum.jvcreader.jvc.UpdatedTopicsManager.UpdateResult;
import com.forum.jvcreader.services.CheckPrivateMessagesAlarmReceiver;
import com.forum.jvcreader.services.CheckUpdatedTopicsAlarmReceiver;
import com.forum.jvcreader.utils.GlobalData;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends JvcActivity
{
	MainApplication app;
	private BroadcastReceiver updatedTopicsReceiver;
	private BroadcastReceiver pmUpdatedReceiver;

	private TextView updatedTopicsTextView;
	private TextView pmItemTextView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState, R.layout.main);

		Boolean b = (Boolean) GlobalData.getOnce("firstRun");
		if(b != null && b)
		{
			updateAlarmsFromPrefs(true, true);
		}

		updatedTopicsTextView = (TextView) findViewById(R.id.mainMenuUTItemTextView);
		pmItemTextView = (TextView) findViewById(R.id.mainMenuPMItemTextView);
		app = (MainApplication) getApplicationContext();
		UpdateResult result = app.getUpdatedTopicsUpdateResult();
		updateUtItem(result);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					if(getWindow().getDecorView().isHardwareAccelerated() && JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS))
					{
						NoticeDialog.show(MainActivity.this, getString(R.string.hardwareAccelerationNotice));
					}
				}
			}, 100);
		}

		checkPrivateMessages();

		if(!JvcUserData.getBoolean("_sfrUserWarned", false))
		{
			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
					if(manager != null)
					{
						String operator = manager.getNetworkOperatorName();
						if(operator != null && operator.length() > 0 && operator.contains("SFR"))
						{
							//NoticeDialog.show(MainActivity.this, getString(R.string.sfrGoodNews));
						}
					}

					JvcUserData.startEditing();
					JvcUserData.setBoolean("_sfrUserWarned", true);
					JvcUserData.stopEditing();
				}
			}, 100);
		}
	}

	public void onPause()
	{
		unregisterReceiver(pmUpdatedReceiver);
		unregisterReceiver(updatedTopicsReceiver);

		super.onPause();
	}

	public void onResume()
	{
		pmUpdatedReceiver = new PmReceiver();
		IntentFilter filter = new IntentFilter();
		filter.setPriority(1);
		filter.addAction(JvcUtils.INTENT_ACTION_UPDATE_PM);
		registerReceiver(pmUpdatedReceiver, filter);

		updatedTopicsReceiver = new UpdatedTopicsUpdateReceiver();
		filter = new IntentFilter();
		filter.setPriority(1);
		filter.addAction(JvcUtils.INTENT_ACTION_UPDATE_UPDATED_TOPICS);
		registerReceiver(updatedTopicsReceiver, filter);

		super.onResume();

		Boolean b = (Boolean) GlobalData.getOnce("editedPrefs");
		if(b != null && b)
		{
			/* Hardware acceleration */
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getWindow().getDecorView().isHardwareAccelerated() && JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS))
			{
				NoticeDialog.show(this, getString(R.string.hardwareAccelerationNotice));
			}

			/* Smileys sizes */
			JvcUserData.updateSmileyProperties();

			/* Android Alarms */
			Boolean b1 = (Boolean) GlobalData.getOnce("editedPref" + JvcUserData.PREF_CHECK_UPDATED_TOPICS);
			Boolean b2 = (Boolean) GlobalData.getOnce("editedPref" + JvcUserData.PREF_UPDATED_TOPICS_CHECK_DELAY);
			Boolean b3 = (Boolean) GlobalData.getOnce("editedPref" + JvcUserData.PREF_CHECK_PM);
			Boolean b4 = (Boolean) GlobalData.getOnce("editedPref" + JvcUserData.PREF_PM_CHECK_DELAY);
			updateAlarmsFromPrefs((b1 != null && b1) || (b2 != null && b2), (b3 != null && b3) || (b4 != null && b4));
			Toast.makeText(this, R.string.preferencesHaveBeenUpdated, Toast.LENGTH_LONG).show();
		}

		updatePmItem();
		updateUtItem(app.getUpdatedTopicsUpdateResult());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main_activity, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		ArrayList<JvcTopic> topics = JvcUserData.getUpdatedTopics();
		if(topics == null || topics.size() == 0)
		{
			menu.findItem(R.id.optionsMenuCheckUpdatedTopics).setEnabled(false);
			menu.findItem(R.id.optionsMenuCheckAll).setEnabled(false);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.optionsMenuCheckPrivateMessages:
				checkPrivateMessages();
				break;

			case R.id.optionsMenuCheckUpdatedTopics:
				checkUpdatedTopics();
				break;

			case R.id.optionsMenuCheckAll:
				checkPrivateMessages();
				checkUpdatedTopics();
				break;

			/* No call to super() because it would allow home button usage */
		}

		return true;
	}

	private void updateAlarmsFromPrefs(boolean updateUtAlarm, boolean updatePmAlarm)
	{
		AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

    	/* Private messages */
		if(updatePmAlarm)
		{
			PendingIntent checkPmIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, CheckPrivateMessagesAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
			long checkPrivateMessagesDelay = JvcUserData.getLong(JvcUserData.PREF_PM_CHECK_DELAY, JvcUserData.DEFAULT_PM_CHECK_DELAY);
			if(JvcUserData.getBoolean(JvcUserData.PREF_CHECK_PM, JvcUserData.DEFAULT_CHECK_PM))
			{
				manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + checkPrivateMessagesDelay, checkPrivateMessagesDelay, checkPmIntent);
			}
			else
			{
				manager.cancel(checkPmIntent);
			}
		}

    	/* Updated topics */
		if(updateUtAlarm)
		{
			PendingIntent checkTopicsIntent = PendingIntent.getBroadcast(this, 1, new Intent(this, CheckUpdatedTopicsAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
			long checkTopicsDelay = JvcUserData.getLong(JvcUserData.PREF_UPDATED_TOPICS_CHECK_DELAY, JvcUserData.DEFAULT_UPDATED_TOPICS_CHECK_DELAY);
			if(JvcUserData.getBoolean(JvcUserData.PREF_CHECK_UPDATED_TOPICS, JvcUserData.DEFAULT_CHECK_UPDATED_TOPICS))
			{
				manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + checkTopicsDelay, checkTopicsDelay, checkTopicsIntent);
			}
			else
			{
				manager.cancel(checkTopicsIntent);
			}
		}
	}

	/* Menu Items [1] */
	public void mainMenuActiveTopicsItemClick(View view)
	{
		startActivity(new Intent(this, UpdatedTopicsActivity.class));
	}

	public void mainMenuPMItemClick(View view)
	{
		startActivity(new Intent(this, PmActivity.class));
	}

	/* Menu Items [2] */
	public void mainMenuFavTopicsItemClick(View view)
	{
		startActivity(new Intent(this, FavoriteTopicsActivity.class));
	}

	public void mainMenuFavForumsItemClick(View view)
	{
		startActivity(new Intent(this, FavoriteForumsActivity.class));
	}

	public void mainMenuForumListItemClick(View view)
	{
		startActivity(new Intent(this, ForumListActivity.class));
	}

	public void mainMenuTopForumsItemClick(View view)
	{
		startActivity(new Intent(this, TopForumsActivity.class));
	}

	public void mainMenuSearchForumItemClick(View view)
	{
		startActivity(new Intent(this, SearchGameForumActivity.class));
	}

	public void mainMenuSearchForumJVItemClick(View view)
	{
		startActivity(new Intent(this, SearchForumJVActivity.class));
	}

	/* Menu Items [3] */
	public void mainMenuPreferencesItemClick(View view)
	{
		startActivity(new Intent(this, PreferencesActivity.class));
	}

	public void mainMenuDisconnectItemClick(View view)
	{
		new JvcDisconnectAndFinishTask().execute();
	}

	public void mainMenuInfoItemClick(View view)
	{
		startActivity(new Intent(this, InfoActivity.class));
	}

	private void updatePmItem()
	{
		final int count = ((MainApplication) getApplicationContext()).getUnreadPmCount();
		pmItemTextView.setText(getString(R.string.mainMenuPMItem) + " (" + count + ")");
		if(count > 0)
		{
			pmItemTextView.setTypeface(null, Typeface.BOLD);
		}
		else
		{
			pmItemTextView.setTypeface(Typeface.DEFAULT);
		}
	}

	private void updateUtItem(UpdateResult updateResult)
	{
		String text;
		int unreadPostCount = updateResult.getTotalUnreadPostCount(), unreadTopicCount = updateResult.getTotalUnreadTopicCount();

		if(unreadPostCount > 0)
		{
			((TextView) findViewById(R.id.mainMenuUTTitleTextView)).setTypeface(null, Typeface.BOLD);
		}
		else
		{
			((TextView) findViewById(R.id.mainMenuUTTitleTextView)).setTypeface(Typeface.DEFAULT);
		}

		if(unreadPostCount > 1)
		{
			text = String.format("%d %s", unreadPostCount, getString(R.string.posts));
		}
		else
		{
			text = String.format("%d %s", unreadPostCount, getString(R.string.post));
		}

		if(unreadTopicCount > 0)
		{
			if(unreadTopicCount == 1)
			{
				text += String.format("\n%s", getString(R.string.notificationTopic));
			}
			else
			{
				text += "\n" + String.format(getString(R.string.notificationTopics), unreadTopicCount);
			}
		}

		updatedTopicsTextView.setText(text.toUpperCase());
	}

	public class UpdatedTopicsUpdateReceiver extends BroadcastReceiver
	{
		public void onReceive(Context context, Intent intent)
		{
			UpdateResult result = app.getUpdatedTopicsUpdateResult();
			final int newCount = result.getNewUnreadPostCount(), topicCount = result.getTotalUnreadTopicCount();
			if(newCount > 0)
			{
				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if(topicCount == 1)
				{
					vibrator.vibrate(JvcUtils.UPDATED_TOPICS_SINGLE_NOTIFICATION_VIBRATOR_PATTERN, -1);
				}
				else
				{
					vibrator.vibrate(JvcUtils.UPDATED_TOPICS_MULTI_NOTIFICATION_VIBRATOR_PATTERN, -1);
				}
			}

			updateUtItem(result);
			abortBroadcast();
		}
	}

	public class PmReceiver extends BroadcastReceiver
	{
		public void onReceive(Context context, Intent intent)
		{
			final int newPmCount = intent.getIntExtra("com.forum.jvcreader.NewPmCount", 0);
			if(newPmCount > 0)
			{
				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(JvcUtils.PM_NOTIFICATION_VIBRATOR_PATTERN, -1);
			}

			updatePmItem();
			abortBroadcast();
		}
	}

	private void checkPrivateMessages()
	{
		JvcCheckPrivateMessagesTask task = new JvcCheckPrivateMessagesTask();
		registerTask(task);
		task.execute();
	}

	private class JvcCheckPrivateMessagesTask extends AsyncTask<Void, Void, String[]>
	{
		protected String[] doInBackground(Void... voids)
		{
			String[] arrAnswer = new String[2];

			try
			{
				HttpClient client = app.getHttpClient(MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet("http://www.jeuxvideo.com/messages-prives/get_message_nonlu.php?skipmc=1");
				HttpResponse response = client.execute(httpGet);
				HttpEntity entity = response.getEntity();

				JSONObject json = new JSONObject(MainApplication.getEntityContent(entity));
				arrAnswer[0] = "ok";
				arrAnswer[1] = json.getString("nb_message");

				if(isCancelled())
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							findViewById(R.id.mainMenuPMItemProgressBar).setVisibility(View.INVISIBLE);
						}
					});
				}

				return arrAnswer;
			}
			catch(UnknownHostException e)
			{
				arrAnswer[0] = JvcUtils.HTTP_TIMEOUT_RESULT;
				return arrAnswer;
			}
			catch(HttpHostConnectException e)
			{
				arrAnswer[0] = JvcUtils.HTTP_TIMEOUT_RESULT;
				return arrAnswer;
			}
			catch(ConnectTimeoutException e)
			{
				arrAnswer[0] = JvcUtils.HTTP_TIMEOUT_RESULT;
				return arrAnswer;
			}
			catch(SocketTimeoutException e)
			{
				arrAnswer[0] = JvcUtils.HTTP_TIMEOUT_RESULT;
				return arrAnswer;
			}
			catch(Exception e)
			{
				arrAnswer[0] = "error";
				arrAnswer[1] = getString(R.string.errorWhileCheckingPM) + " : " + e.toString();
				return arrAnswer;
			}
		}

		protected void onPreExecute()
		{
			findViewById(R.id.mainMenuPMItemProgressBar).setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(String[] result)
		{
			findViewById(R.id.mainMenuPMItemProgressBar).setVisibility(View.INVISIBLE);

			if(result[0].equals("ok"))
			{
				pmItemTextView.setText(getString(R.string.mainMenuPMItem) + " (" + result[1] + ")");
				final int count = Integer.parseInt(result[1]);
				if(count > 0)
				{
					pmItemTextView.setTypeface(null, Typeface.BOLD);
				}
				else
				{
					pmItemTextView.setTypeface(Typeface.DEFAULT);
				}

				((MainApplication) getApplicationContext()).setUnreadPmCount(count);
			}
			else if(result[0].equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(MainActivity.this);
			}
			else if(result[0].equals("error"))
			{
				pmItemTextView.setText(getString(R.string.mainMenuPMItem) + " (?)");
				Toast.makeText(MainActivity.this, result[1], Toast.LENGTH_LONG).show();
			}
		}
	}

	private void checkUpdatedTopics()
	{
		JvcCheckUpdatedTopicsTask task = new JvcCheckUpdatedTopicsTask();
		registerTask(task);
		task.execute();
	}

	private class JvcCheckUpdatedTopicsTask extends AsyncTask<Void, Integer, String>
	{
		private UpdateResult updateResult;
		private int id = 0;

		protected String doInBackground(Void... voids)
		{
			try
			{
				UpdatedTopicsManager manager = UpdatedTopicsManager.getInstance();
				updateResult = manager.updateTopics(MainActivity.this, JvcUserData.getUpdatedTopics(), runnable, this);

				if(isCancelled())
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							findViewById(R.id.mainMenuUTItemProgressBar).setVisibility(View.INVISIBLE);
						}
					});
				}

				return null;
			}
			catch(UnknownHostException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(HttpHostConnectException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(ConnectTimeoutException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(SocketTimeoutException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(Exception e)
			{
				return getString(R.string.errorWhileCheckingUpdatedTopics) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			findViewById(R.id.mainMenuUTItemProgressBar).setVisibility(View.VISIBLE);
		}

		private final Runnable runnable = new Runnable()
		{
			public void run()
			{
				publishProgress(id);
				id++;
			}
		};

		protected void onPostExecute(String result)
		{
			findViewById(R.id.mainMenuUTItemProgressBar).setVisibility(View.GONE);

			if(result == null)
			{
				updateUtItem(updateResult);
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(MainActivity.this);
				updatedTopicsTextView.setText(R.string.operationFailed);
			}
			else
			{
				Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
				updatedTopicsTextView.setText(R.string.operationFailed);
			}
		}
	}

	private class JvcDisconnectAndFinishTask extends AsyncTask<Void, String, Boolean>
	{
		private ProgressDialog dialog;

		protected Boolean doInBackground(Void... voids)
		{
			try
			{
				app.invalidateJvcSession();

				HttpClient client = app.getHttpClient(MainApplication.JVC_SESSION);
				String tk = (String) GlobalData.get("tk"), key = (String) GlobalData.get("key"); /* Clean disconnect */
				HttpGet httpGet = new HttpGet(String.format("http://www.jeuxvideo.com/cgi-bin/admin/logout.cgi?time=%s&cod=%s&url=", tk, key));
				client.execute(httpGet);

				app.invalidateHttpClients();
			}
			catch(Exception e)
			{
				Log.e("JvcForumsReader", e.toString());
				return false; /* Ignore disconnecting problems */
			}

			return true;
		}

		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(MainActivity.this, getString(R.string.disconnectingMessage), getString(R.string.disconnectingFrom) + " jeuxvideo.com", true);
		}

		protected void onProgressUpdate(String... strings)
		{
			dialog.setMessage(strings[0]);
		}

		protected void onPostExecute(Boolean result)
		{
			dialog.dismiss();
			dialog = null;

			Intent intent = new Intent(MainActivity.this, LoginActivity.class);
			intent.putExtra("com.forum.jvcreader.Disconnected", true);
			startActivity(intent);

			finish();
		}
	}
}