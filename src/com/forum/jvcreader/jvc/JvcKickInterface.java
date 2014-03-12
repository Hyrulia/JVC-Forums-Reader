package com.forum.jvcreader.jvc;

import android.content.Context;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;

public class JvcKickInterface
{
	private Context context;

	private String url;
	private boolean isForumJV;
	private String forumJVBaseUrl;

	private String requestError;
	private ArrayList<JvcKickInterface.Entry> requestKickEntryList;
	private long requestTotalKicks;

	public JvcKickInterface(Context context, String url, boolean isForumJV, String forumJVBaseUrl)
	{
		this.context = context;
		this.url = url;
		this.isForumJV = isForumJV;
		this.forumJVBaseUrl = forumJVBaseUrl;
	}

	public boolean loadInterface() throws IOException
	{
		requestKickEntryList = new ArrayList<JvcKickInterface.Entry>();
		MainApplication app = (MainApplication) context.getApplicationContext();
		HttpClient client = app.getHttpClient(isForumJV ? MainApplication.JVFORUM_SESSION : MainApplication.JVC_SESSION);
		HttpEntity entity = client.execute(new HttpGet(url)).getEntity();
		String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(entity));

		Matcher m = PatternCollection.extractKickInterfaceTotalKicks.matcher(content);
		if(m.find())
		{
			requestTotalKicks = Long.parseLong(m.group(1));
		}
		else
		{
			requestError = "can't extract total kicks";
			return true;
		}

		m = PatternCollection.fetchNextKickInterfaceEntry.matcher(content);
		while(m.find())
		{
			Entry entry = new Entry(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), m.group(6));
			requestKickEntryList.add(entry);
		}

		return false;
	}

	public boolean removeEntry(Entry entry) throws IOException
	{
		MainApplication app = (MainApplication) context.getApplicationContext();
		HttpClient client = app.getHttpClient(isForumJV ? MainApplication.JVFORUM_SESSION : MainApplication.JVC_SESSION);
		HttpEntity entity = client.execute(new HttpGet((isForumJV ? forumJVBaseUrl : "") + entry.getDekickLink())).getEntity();
		String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(entity));

		if(content.contains("<p class=\"ok centrer\">Kick retir\u00E9 !</p>"))
			return false;

		requestError = "message result not found";
		return true;
	}

	public String getRequestError()
	{
		return requestError;
	}

	public ArrayList<JvcKickInterface.Entry> getRequestKickEntryList()
	{
		return requestKickEntryList;
	}

	public long getRequestTotalKicks()
	{
		return requestTotalKicks;
	}

	public static class Entry
	{
		private String dekickLink;
		private String pseudo;
		private String reason;
		private String subject;
		private String message;
		private String totalKicks;

		public Entry(String dekickLink, String pseudo, String reason, String subject, String message, String totalKicks)
		{
			this.dekickLink = dekickLink;
			this.pseudo = pseudo;
			this.reason = reason;
			this.subject = subject;
			this.message = message;
			this.totalKicks = totalKicks;
		}

		public String getDekickLink()
		{
			return dekickLink;
		}

		public String getPseudo()
		{
			return pseudo;
		}

		public String getReason()
		{
			return reason;
		}

		public String getSubject()
		{
			return subject;
		}

		public String getMessage()
		{
			return message;
		}

		public String getTotalKicks()
		{
			return totalKicks;
		}
	}
}