package com.forum.jvcreader.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.jvc.UpdatedTopicsManager;
import com.forum.jvcreader.jvc.UpdatedTopicsManager.UpdateResult;

public class CheckUpdatedTopicsAlarmReceiver extends BroadcastReceiver
{
	MainApplication appContext;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		appContext = (MainApplication) context.getApplicationContext();
		if(appContext.isJvcSessionValid())
		{
			appContext.invalidateHttpClients();
			new JvcCheckUpdatedTopicsTask().execute();
		}
	}

	private class JvcCheckUpdatedTopicsTask extends AsyncTask<Void, Void, UpdateResult>
	{
		@Override
		protected UpdateResult doInBackground(Void... voids)
		{
			try
			{
				UpdatedTopicsManager manager = UpdatedTopicsManager.getInstance();
				return manager.updateTopics(appContext, JvcUserData.getUpdatedTopics());
			}
			catch(Exception e)
			{
				return null;
			}
		}

		protected void onPostExecute(UpdateResult result)
		{
			if(result != null)
			{
				Intent intent = new Intent(JvcUtils.INTENT_ACTION_UPDATE_UPDATED_TOPICS);
				intent.putExtra("com.forum.jvcreader.TotalUnreadPostCount", result.getTotalUnreadPostCount());
				intent.putExtra("com.forum.jvcreader.TotalUnreadTopicCount", result.getTotalUnreadTopicCount());
				intent.putExtra("com.forum.jvcreader.NewUnreadPostCount", result.getNewUnreadPostCount());
				appContext.sendOrderedBroadcast(intent, null);
			}
		}
	}
}
