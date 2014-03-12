package com.forum.jvcreader.jvc;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import com.forum.jvcreader.ArchivedTopicsActivity;
import com.forum.jvcreader.MainActivity;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.GlobalData;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class JvcArchiveTopicService extends IntentService
{
	NotificationManager manager;
	Notification onGoingNotification;

	public JvcArchiveTopicService()
	{
		super("JvcArchiveTopicService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		final PendingIntent mainActivityIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		String topicKey = intent.getExtras().getString("ArchivedTopicActivityKey");
		final JvcArchivedTopic topic = (JvcArchivedTopic) GlobalData.getOnce(topicKey);
		if(topic == null)
			return;

		GlobalData.set("archivingTopic", true);
		manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		onGoingNotification = new Notification(R.drawable.ic_menu_save, getString(R.string.downloadingTopic), System.currentTimeMillis());
		onGoingNotification.setLatestEventInfo(this, getString(R.string.downloadingTopic), "", mainActivityIntent);
		onGoingNotification.flags = Notification.FLAG_ONGOING_EVENT;
		onGoingNotification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		manager.notify(JvcUtils.NOTIFICATION_ID_DOWNLOADING_TOPIC, onGoingNotification);

		String result = archiveTopic(topic, new JvcArchivedTopic.OnPageArchivedListener()
		{
			@Override
			public void onUpdateState(int state, int arg1)
			{
				String title;

				switch(state)
				{
					case JvcArchivedTopic.OnPageArchivedListener.FETCHING_AUTHOR_NAME:
						title = getString(R.string.fetchingAuthorName);
						break;

					case JvcArchivedTopic.OnPageArchivedListener.FETCHING_PAGE:
						title = String.format(getString(R.string.pageXOutOfX), arg1, topic.getInitialPageEnd());
						break;

					case JvcArchivedTopic.OnPageArchivedListener.SAVING_TOPIC:
						title = getString(R.string.savingTopic);
						break;

					default:
						title = getString(R.string.errorWhileLoading);
						break;
				}

				onGoingNotification.setLatestEventInfo(JvcArchiveTopicService.this, getString(R.string.downloadingTopic), title, mainActivityIntent);
				manager.notify(JvcUtils.NOTIFICATION_ID_DOWNLOADING_TOPIC, onGoingNotification);
			}
		});

		manager.cancel(JvcUtils.NOTIFICATION_ID_DOWNLOADING_TOPIC);

		String message, titleMessage;
		boolean error = false;

		if(result == null)
		{
			message = getString(R.string.clickToOpenArchivedTopics);
		}
		else
		{
			error = true;

			try
			{
				topic.deleteTopicContent();
			}
			catch(IOException e)
			{
				Log.e("JvcForumsReader", "JvcArchiveTopicService error : can't delete topic content");
			}

			if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				message = MainApplication.handleHttpTimeout(this);
			}
			else
			{
				message = result;
			}
		}

		titleMessage = error ? getString(R.string.errorWhileLoading) : getString(R.string.topicArchived);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, ArchivedTopicsActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification(R.drawable.ic_menu_save, titleMessage, System.currentTimeMillis());
		notification.setLatestEventInfo(this, titleMessage, message, pendingIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.defaults = Notification.DEFAULT_VIBRATE;
		manager.notify(JvcUtils.NOTIFICATION_ID_DOWNLOADED_TOPIC, notification);

		GlobalData.remove("archivingTopic");
	}

	private String archiveTopic(JvcArchivedTopic topic, JvcArchivedTopic.OnPageArchivedListener listener)
	{
		try
		{
			boolean success = topic.archivePages(listener);

			if(success)
			{
				return null;
			}
			else
			{
				return getString(R.string.noAnyPosts);
			}
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
		catch(IOException e)
		{
			return getString(R.string.errorWhileLoading) + " : " + e.toString();
		}
		catch(JvcErrorException e)
		{
			return e.getMessage();
		}
	}

	@Override
	public void onDestroy()
	{
		manager.cancel(JvcUtils.NOTIFICATION_ID_DOWNLOADING_TOPIC);
		GlobalData.remove("archivingTopic");
	}
}