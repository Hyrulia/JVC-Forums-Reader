package com.forum.jvcreader.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.jvc.JvcUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

public class CheckPrivateMessagesAlarmReceiver extends BroadcastReceiver
{
	MainApplication appContext;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		appContext = (MainApplication) context.getApplicationContext();
		if(appContext.isJvcSessionValid())
		{
			new JvcCheckPrivateMessagesTask().execute();
		}
	}

	public class JvcCheckPrivateMessagesTask extends AsyncTask<Void, Void, Integer>
	{
		protected Integer doInBackground(Void... voids)
		{
			try
			{
				HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION, false);
				HttpGet httpGet = new HttpGet("http://www.jeuxvideo.com/messages-prives/get_message_nonlu.php?skipmc=1");
				HttpResponse response = client.execute(httpGet);
				HttpEntity entity = response.getEntity();

				JSONObject json = new JSONObject(MainApplication.getEntityContent(entity));
				return json.getInt("nb_message");
			}
			catch(Exception e)
			{
				return -1;
			}
		}

		protected void onPostExecute(Integer result)
		{
			if(result != -1)
			{
				int lastPmCount = appContext.getUnreadPmCount();
				appContext.setUnreadPmCount(result);
				Intent intent = new Intent(JvcUtils.INTENT_ACTION_UPDATE_PM);
				intent.putExtra("com.forum.jvcreader.NewPmCount", result - lastPmCount);
				intent.putExtra("com.forum.jvcreader.TotalPmCount", (int) result);
				appContext.sendOrderedBroadcast(intent, null);
			}
		}
	}


}
