package com.forum.jvcreader.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.forum.jvcreader.PmActivity;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;

public class PmReceivedNotifierReceiver extends BroadcastReceiver
{
	@SuppressWarnings("deprecation") @Override
	public void onReceive(Context context, Intent intent)
	{
		final int newPmCount = intent.getIntExtra("com.forum.jvcreader.NewPmCount", 0);
		final int totalPmCount = intent.getIntExtra("com.forum.jvcreader.TotalPmCount", 0);

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if(newPmCount > 0 && totalPmCount > 0 && JvcUserData.getBoolean(JvcUserData.PREF_RECEIVE_NOTIFICATION_ON_PM, JvcUserData.DEFAULT_RECEIVE_NOTIFICATION_ON_PM))
		{
			Notification notification = new Notification(R.drawable.message, getUnreadPmNotificationText(context, newPmCount, false), System.currentTimeMillis());
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, PmActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(context, context.getString(R.string.app_name), getUnreadPmNotificationText(context, totalPmCount, true), pendingIntent);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS;
			notification.number = totalPmCount;
			notification.vibrate = JvcUtils.PM_NOTIFICATION_VIBRATOR_PATTERN;
			manager.notify(JvcUtils.NOTIFICATION_ID_CHECK_PRIVATE_MESSAGES, notification);
		}
		else if(totalPmCount == 0)
		{
			manager.cancel(JvcUtils.NOTIFICATION_ID_CHECK_PRIVATE_MESSAGES);
		}

		abortBroadcast();
	}

	private String getUnreadPmNotificationText(Context context, int count, boolean isLong)
	{
		if(isLong)
		{
			if(count == 1)
				return context.getString(R.string.notificationNewPMLong);
			else
				return String.format(context.getString(R.string.notificationNewPMLongPlural), count);
		}
		else
		{
			if(count == 1)
				return context.getString(R.string.notificationNewPMShort);
			else
				return String.format(context.getString(R.string.notificationNewPMShortPlural), count);
		}
	}
}
