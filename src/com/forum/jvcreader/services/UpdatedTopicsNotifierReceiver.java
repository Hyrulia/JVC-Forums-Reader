package com.forum.jvcreader.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.forum.jvcreader.R;
import com.forum.jvcreader.UpdatedTopicsActivity;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;

public class UpdatedTopicsNotifierReceiver extends BroadcastReceiver
{
	@SuppressWarnings("deprecation") @Override
	public void onReceive(Context context, Intent intent)
	{
		final int unreadPostCount = intent.getIntExtra("com.forum.jvcreader.TotalUnreadPostCount", 0);
		final int unreadTopicCount = intent.getIntExtra("com.forum.jvcreader.TotalUnreadTopicCount", 0);
		final int newUnreadPostCount = intent.getIntExtra("com.forum.jvcreader.NewUnreadPostCount", 0);
		final boolean isMultiTopic = unreadTopicCount > 1;

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if(newUnreadPostCount > 0 && JvcUserData.getBoolean(JvcUserData.PREF_RECEIVE_NOTIFICATION_ON_TOPIC_UPDATED, JvcUserData.DEFAULT_RECEIVE_NOTIFICATION_ON_TOPIC_UPDATED))
		{
			int drawableId;
			if(isMultiTopic)
				drawableId = R.drawable.topic_dossier2;
			else
				drawableId = R.drawable.topic_dossier1;
			Notification notification = new Notification(drawableId, getNotificationText(context, unreadPostCount, unreadTopicCount, false), System.currentTimeMillis());
			Intent updatedTopicsIntent = new Intent(context, UpdatedTopicsActivity.class);
			updatedTopicsIntent.putExtra("com.forum.jvcreader.DisableStartCheck", true);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, updatedTopicsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(context, context.getString(R.string.app_name), getNotificationText(context, unreadPostCount, unreadTopicCount, true), pendingIntent);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS;
			notification.number = unreadPostCount;
			notification.vibrate = JvcUtils.UPDATED_TOPICS_SINGLE_NOTIFICATION_VIBRATOR_PATTERN;
			manager.notify(JvcUtils.NOTIFICATION_ID_CHECK_UPDATED_TOPICS, notification);
		}
		else if(unreadPostCount == 0)
		{
			manager.cancel(JvcUtils.NOTIFICATION_ID_CHECK_UPDATED_TOPICS);
		}

		abortBroadcast();
	}

	private String getNotificationText(Context context, int unreadPostCount, int unreadTopicCount, boolean isLong)
	{
		String text;

		if(isLong)
		{
			if(unreadPostCount == 1)
				text = context.getString(R.string.notificationNewPostLong);
			else
				text = String.format(context.getString(R.string.notificationNewPostsLong), unreadPostCount);
		}
		else
		{
			if(unreadPostCount == 1)
				text = context.getString(R.string.notificationNewPostShort);
			else
				text = String.format(context.getString(R.string.notificationNewPostsShort), unreadPostCount);
		}

		text += " ";
		if(unreadTopicCount == 1)
			text += context.getString(R.string.notificationTopic);
		else
			text += String.format(context.getString(R.string.notificationTopics), unreadTopicCount);

		return text;
	}
}
