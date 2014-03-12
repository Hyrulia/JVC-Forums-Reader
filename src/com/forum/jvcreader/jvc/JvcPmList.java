package com.forum.jvcreader.jvc;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

public class JvcPmList
{
	public static final int RECEIVED_MESSAGES = 1;
	public static final int SENT_MESSAGES = 2;

	private Context context;
	private String url;
	private int boxType;

	private int requestUnreadPmCount = 0;
	private String requestError;
	private JvcPmListPaginationData requestPaginationData;

	public JvcPmList(Context context, int boxType)
	{
		this.context = context;
		this.boxType = boxType;
		switch(boxType)
		{
			case RECEIVED_MESSAGES:
				url = "http://www.jeuxvideo.com/messages-prives/boite-reception.php";
				break;

			case SENT_MESSAGES:
				url = "http://www.jeuxvideo.com/messages-prives/envoyes.php";
				break;
		}
	}

	public void saveInRecoveryMode(Bundle bundle)
	{
		bundle.putInt("JvcPmList_boxType", boxType);
	}

	public static JvcPmList restoreInRecoveryMode(Context context, Bundle bundle)
	{
		return new JvcPmList(context, bundle.getInt("JvcPmList_boxType"));
	}

	public ArrayList<JvcPmTopic> requestList(int page, AsyncTask task) throws ParseException, IOException
	{
		requestUnreadPmCount = 0;
		ArrayList<JvcPmTopic> list = new ArrayList<JvcPmTopic>();

		MainApplication appContext = (MainApplication) context.getApplicationContext();
		HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
		HttpGet httpGet;
		if(page > 1)
			httpGet = new HttpGet(url + "?page=" + page);
		else
			httpGet = new HttpGet(url);
		HttpResponse response = client.execute(httpGet);
		if(task.isCancelled())
			return null;
		String content = MainApplication.getEntityContent(response.getEntity());

		Matcher m = PatternCollection.extractPmTopicListPageData.matcher(content);
		if(!m.find())
		{
			return list; /* Empty list : no topics */
		}
		requestPaginationData = new JvcPmListPaginationData(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), content.contains("class=\"p_prec\""), content.contains("Suivant</a>"));

		m = PatternCollection.fetchNextPmTopic.matcher(content);
		while(m.find())
		{
			final boolean read = m.group(5) != null;
			if(!read)
				requestUnreadPmCount++;
			JvcPmTopic topic = new JvcPmTopic(context, Long.parseLong(m.group(4)), Integer.parseInt(m.group(1)), m.group(3), read, StringHelper.unescapeHTML(m.group(6)), m.group(7), boxType, m.group(2) != null);
			list.add(topic);
		}

		return list;
	}

	public void removeTopics(int page, HashMap<JvcPmTopic, Boolean> topics) throws IOException
	{
		MainApplication appContext = (MainApplication) context.getApplicationContext();
		HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
		HttpPost httpPost = new HttpPost(url);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		if(page > 1)
			nameValuePairs.add(new BasicNameValuePair("page", String.valueOf(page)));
		for(JvcPmTopic topic : topics.keySet())
		{
			nameValuePairs.add(new BasicNameValuePair("cbox[]", String.valueOf(topic.getId())));
		}
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

		client.execute(httpPost);
	}

	public String getRequestError()
	{
		return requestError;
	}

	public JvcPmListPaginationData getPaginationData()
	{
		return requestPaginationData;
	}

	public static class JvcPmListPaginationData
	{
		public int rangeStart;
		public int rangeEnd;
		public int messagesCount;
		public boolean hasPreviousPage;
		public boolean hasNextPage;

		public JvcPmListPaginationData(int start, int end, int count, boolean prev, boolean next)
		{
			rangeStart = start;
			rangeEnd = end;
			messagesCount = count;
			hasPreviousPage = prev;
			hasNextPage = next;
		}
	}
}
