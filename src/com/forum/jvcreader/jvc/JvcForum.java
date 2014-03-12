package com.forum.jvcreader.jvc;

import android.content.Context;
import android.os.Bundle;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

public class JvcForum implements Serializable
{
	transient private Context context;

	private static final long serialVersionUID = 1L;
	private int forumId;
	private boolean isForumJV;
	private String forumJVSubdomain = null;
	private String baseUrl;
	private String baseUrlMobile;

	/* JVC-Dependent data */
	private String cacheForumName = null; /* Cached */
	transient private ArrayList<JvcTopic> requestResult;
	transient private String requestError = null;
	transient private String requestFieldset;
	transient private String requestRedirectedUrl;
	transient private boolean requestHasFirstPage;
	transient private boolean requestHasPreviousPage;
	transient private boolean requestHasNextPage;

	/* Admin controls */
	transient private String adminKickInterfaceUrl = null;

	/* Constants */
	public static final int SHOW_TOPIC_LIST = 0;
	public static final int SEARCH_PSEUDOS = 1;
	public static final int SEARCH_TOPICS = 2;
	public static final int SHOW_REPLY_FORM = 3;
	public static final int SEARCH_TOPIC_POSTS = 4;

	public JvcForum(Context context, int forumId)
	{
		this.context = context;
		this.forumId = forumId;
		isForumJV = false;
		baseUrl = "http://www.jeuxvideo.com/forums/";
		baseUrlMobile = "http://m.jeuxvideo.com/forums/";
	}

	public JvcForum(Context context, int forumId, String forumJVSubdomain)
	{
		this.context = context;
		this.forumId = forumId;
		isForumJV = true;
		this.forumJVSubdomain = forumJVSubdomain;
		baseUrl = "http://" + forumJVSubdomain + ".forumjv.com/";
	}

	public void saveInRecoveryMode(Bundle bundle)
	{
		bundle.putInt("JvcForum_forumId", forumId);
		if(isForumJV)
			bundle.putString("JvcForum_forumJVSubdomain", forumJVSubdomain);
	}

	public static JvcForum restoreInRecoveryMode(Context context, Bundle bundle)
	{
		int forumId = bundle.getInt("JvcForum_forumId");
		if(bundle.containsKey("JvcForum_forumJVSubdomain"))
		{
			return new JvcForum(context, forumId, bundle.getString("JvcForum_forumJVSubdomain"));
		}
		else
		{
			return new JvcForum(context, forumId);
		}
	}

	public boolean requestTopicList(int type, int pageNumber, String text) throws IOException
	{
		requestError = null;
		ArrayList<JvcTopic> topicList = new ArrayList<JvcTopic>();
		String content = getContentFromUrl(makeUrlFromParams(type, pageNumber, text, false));

		checkAndCacheForumName(content);

		Matcher m = PatternCollection.fetchKickInterfaceUrl.matcher(content);
		if(m.find())
		{
			adminKickInterfaceUrl = m.group(1);
		}

		requestHasFirstPage = content.contains("class=\"p_debut\"");
		requestHasPreviousPage = content.contains("class=\"p_prec\"");
		requestHasNextPage = content.contains("class=\"p_suiv\"");

		requestFieldset = null;
		m = PatternCollection.extractFormFieldset.matcher(content);
		if(m.find())
		{
			requestFieldset = m.group(1);
		}

		m = PatternCollection.checkErrorMessage.matcher(content);
		if(m.find())
		{
			requestError = m.group(1);
			return true;
		}

		m = PatternCollection.fetchNextTopic.matcher(content); /* m.group(3) is topic's delete url for mod or null if not admin */
		if(type == JvcForum.SEARCH_TOPIC_POSTS) /* Special processing for those */
		{
			if(m.find())
			{
				Matcher postMatcher = null;
				while(true)
				{
					JvcTopic topic = new JvcTopic(this, Long.parseLong(m.group(4)));
					topic.setExtras(StringHelper.unescapeHTML(m.group(5)), Integer.parseInt(m.group(1)), m.group(2), m.group(7), !(m.group(6).length() == 0), Integer.parseInt(m.group(8)), m.group(9));
					if(m.group(3) != null)
						topic.setAdminDeleteUrl(StringHelper.unescapeHTML(m.group(3)));
					topicList.add(topic);

					int replyStart = m.end(9), replyEnd;
					boolean lastTopic = false;
					if(m.find())
					{
						replyEnd = m.start();
					}
					else
					{
						replyEnd = content.length() - 1;
						lastTopic = true;
					}

					if(postMatcher == null)
					{
						postMatcher = PatternCollection.fetchNextTopicReply.matcher(content.subSequence(replyStart, replyEnd));
					}
					else
					{
						postMatcher.reset(content.subSequence(replyStart, replyEnd));
					}

					Matcher urlMatcher = null;
					while(postMatcher.find())
					{
						if(urlMatcher == null)
						{
							urlMatcher = PatternCollection.extractTopicUrl.matcher(StringHelper.unescapeHTML(postMatcher.group(2)));
						}
						else
						{
							urlMatcher.reset(postMatcher.group(2));
						}

						if(!urlMatcher.find())
						{
							throw new PatternSyntaxException("cannot extract topic URL", PatternCollection.extractTopicUrl.pattern(), -1);
						}

						JvcTopic replyTopic = new JvcTopic(this, Long.parseLong(urlMatcher.group(1)), Integer.parseInt(urlMatcher.group(2)), Long.parseLong(urlMatcher.group(3)));
						replyTopic.setExtras(StringHelper.unescapeHTML(postMatcher.group(3)), Integer.parseInt(postMatcher.group(1)), null, postMatcher.group(5), !(postMatcher.group(4).length() == 0), 0, null);
						topicList.add(replyTopic);
					}

					if(lastTopic)
						break;
				}
			}
		}
		else /* Regular processing */
		{
			while(m.find())
			{
				JvcTopic topic = new JvcTopic(this, Long.parseLong(m.group(4)));
				topic.setExtras(StringHelper.unescapeHTML(m.group(5)), Integer.parseInt(m.group(1)), m.group(2), m.group(7), !(m.group(6).length() == 0), Integer.parseInt(m.group(8)), m.group(9));
				if(m.group(3) != null)
					topic.setAdminDeleteUrl(StringHelper.unescapeHTML(m.group(3)));
				topicList.add(topic);
			}
		}

		requestResult = topicList;
		if(requestResult.size() == 0)
		{
			requestError = "Empty list";
			return true;
		}

		return false;
	}

	public ArrayList<JvcTopic> getRequestResult()
	{
		return requestResult;
	}

	public String getRequestError()
	{
		return requestError;
	}

	public String getRequestFieldset()
	{
		return requestFieldset;
	}

	public String getRequestRedirectedUrl()
	{
		return requestRedirectedUrl;
	}

	public boolean requestHasFirstPage()
	{
		return requestHasFirstPage;
	}

	public boolean requestHasPreviousPage()
	{
		return requestHasPreviousPage;
	}

	public boolean requestHasNextPage()
	{
		return requestHasNextPage;
	}

	private void checkAndCacheForumName(String content) /* If forum name isn't cached, cache it */
	{
		if(cacheForumName == null)
		{
			Matcher m;
			if(!isForumJV)
			{
				m = PatternCollection.fetchForumName.matcher(content);
			}
			else
			{
				m = PatternCollection.fetchForumJVName.matcher(content);
			}
			if(m.find())
			{
				cacheForumName = m.group(1);
			}
		}
	}

	public String makeUrlFromParams(int requestType, int pageNumber, String textParam, boolean isMobile) throws UnsupportedEncodingException
	{
		String url, encodedTextParam;

		if(textParam == null || textParam.length() == 0)
		{
			encodedTextParam = "0";
		}
		else
		{
			encodedTextParam = JvcUtils.encodeUrlParam(textParam);
		}

		if(isMobile && !isForumJV)
		{
			url = baseUrlMobile;
		}
		else
		{
			url = baseUrl;
		}

		int auxType = 0;
		if(requestType == JvcForum.SHOW_REPLY_FORM)
		{
			auxType = requestType;
			requestType = 0;
		}

		url += String.format("%d-%d-0-1-0-%d-%d-%s.htm", auxType, forumId, ((pageNumber - 1) * 25) + 1, requestType, encodedTextParam);

		return url;
	}

	private String getContentFromUrl(String url) throws IOException
	{
		MainApplication appContext = (MainApplication) context.getApplicationContext();
		HttpClient client = appContext.getHttpClient(isForumJV ? MainApplication.JVFORUM_SESSION : MainApplication.JVC_SESSION);
		HttpGet httpGet = new HttpGet(url);
		HttpContext httpContext = new BasicHttpContext();
		HttpResponse response = client.execute(httpGet, httpContext);
		HttpEntity entity = response.getEntity();
		String redirectedUrl = ((HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST)).getURI().toString();
		if(isForumJV)
		{
			requestRedirectedUrl = baseUrl + redirectedUrl.substring(1);
		}
		else
		{
			requestRedirectedUrl = "http://www.jeuxvideo.com" + redirectedUrl;
		}

		return MainApplication.getEntityContent(entity);
	}

	public void updateFieldsetFromContent(String content)
	{
		Matcher m = PatternCollection.extractFormFieldset.matcher(content);
		if(m.find())
		{
			requestFieldset = m.group(1);
		}
	}

	public void updateMobileFieldsetFromContent(String content)
	{
		Matcher m = PatternCollection.extractMobileFormFieldset.matcher(content);
		if(m.find())
		{
			requestFieldset = m.group(1);
		}
	}

	public Context getContext()
	{
		return context;
	}

	public void setContext(Context context)
	{
		this.context = context;
	}

	public int getForumId()
	{
		return forumId;
	}

	public boolean isForumJV()
	{
		return isForumJV;
	}

	public String getForumJVSubdomain()
	{
		return forumJVSubdomain;
	}

	public String getBaseUrl()
	{
		return baseUrl;
	}

	public String getBaseUrlMobile()
	{
		return baseUrlMobile;
	}

	public String getForumName()
	{
		if(cacheForumName == null)
			return "(?)";
		return cacheForumName;
	}

	public void setForumName(String name)
	{
		cacheForumName = name;
	}

	public void invalidateForumName()
	{
		cacheForumName = null;
	}

	public String getAdminKickInterfaceUrl()
	{
		return adminKickInterfaceUrl;
	}

	public static class NameAscendingComparator implements Comparator<JvcForum>
	{
		@Override
		public int compare(JvcForum jvcForum, JvcForum jvcForum1)
		{
			return jvcForum.getForumName().compareToIgnoreCase(jvcForum1.getForumName());
		}
	}

	public static class ForumIdAscendingComparator implements Comparator<JvcForum>
	{
		@Override
		public int compare(JvcForum jvcForum, JvcForum jvcForum1)
		{
			return jvcForum1.getForumId() - jvcForum.getForumId();
		}
	}
}
