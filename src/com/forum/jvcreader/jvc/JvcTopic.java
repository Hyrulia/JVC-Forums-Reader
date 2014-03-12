package com.forum.jvcreader.jvc;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

public class JvcTopic implements Serializable
{
	public static final int REQUEST_OK = 0;
	public static final int REQUEST_CAPTCHA_REQUIRED = 1;
	public static final int REQUEST_ERROR_FROM_JVC = 2;
	public static final int REQUEST_ERROR = 3;
	public static final int REQUEST_RETRY = 4;

	private static final long serialVersionUID = 1L;
	private JvcForum forum;
	private long topicId;

	private boolean sendable;
	transient private String sendableContent;

	transient private boolean isReplyTopic = false;
	transient private int replyPageNumber;
	transient private long replyPostId;

	/* Extra info */
	private String extraTopicName = null; /* Needs to be cached whenever possible & serialized */
	transient private boolean extraIsLocked; /* Needs to be cached whenever possible */
	transient private String extraIconName;
	transient private String extraPseudo;
	transient private boolean extraIsAdmin;
	transient private int extraColorSwitch;
	transient private int extraPostCount;
	transient private String extraDate;

	/* Admin data */
	transient private String adminDeleteUrl = null;
	transient private boolean adminIsLocked;
	transient private String adminLockUrl = null;
	transient private boolean adminIsPinned;
	transient private String adminPinUrl = null;
	transient private String adminKickInterfaceUrl = null;

	/* Updated topics data */
	private UpdatedTopicData updatedTopicData = null; /* Not transient : used for updated topics */

	/* Request data */
	transient private ArrayList<JvcPost> requestResult;
	transient private long requestLastPostId;
	transient private String requestError = null;
	transient private String requestContent;
	transient private String requestCaptchaUrl;
	transient private String requestCaptchaSession;
	transient private String requestFieldset;
	transient private String requestRedirectedUrl;
	transient private int requestPageCount;
	transient private boolean requestHasFirstPage;
	transient private boolean requestHasPreviousPage;
	transient private boolean requestHasNextPage;
	transient private boolean requestHasLastPage;
	transient private boolean requestIsTopicEmpty;

	/* Constants */
	public static final int SHOW_POSTS_FROM_PAGE = 1;
	public static final int SHOW_LAST_TEN_POSTS = 3;

	transient private List<NameValuePair> nameValuePairs;
	transient private String userCaptchaCode = null;
	transient private boolean postIsMobile;

	public JvcTopic(JvcForum forum, long topicId)
	{
		sendable = false;

		this.forum = forum;
		this.topicId = topicId;
	}

	public JvcTopic(JvcForum forum, long topicId, int pageNumber, long postId)
	{
		sendable = false;

		this.forum = forum;
		this.topicId = topicId;
		isReplyTopic = true;
		replyPageNumber = pageNumber;
		replyPostId = postId;
	}

	public JvcTopic(JvcForum forum, String subject, String content, boolean isMobile)
	{
		sendable = true;

		this.forum = forum;
		extraTopicName = subject;
		sendableContent = content;
		postIsMobile = isMobile;
	}

	public void saveInRecoveryMode(Bundle bundle)
	{
		forum.saveInRecoveryMode(bundle);
		bundle.putLong("JvcTopic_topicId", topicId);
	}

	public static JvcTopic restoreInRecoveryMode(Context context, Bundle bundle)
	{
		return new JvcTopic(JvcForum.restoreInRecoveryMode(context, bundle), bundle.getLong("JvcTopic_topicId"));
	}

	public boolean requestPosts(int type, int pageNumber) throws IOException
	{
		requestIsTopicEmpty = false;
		requestError = null;

		if(!sendable)
		{
			ArrayList<JvcPost> postList = new ArrayList<JvcPost>();
			String content;
			if(!forum.isForumJV())
			{
				content = getContentFromUrl(makeUrlFromParams(type, pageNumber), MainApplication.JVC_SESSION);
			}
			else
			{
				content = getContentFromUrl(makeUrlFromParams(type, pageNumber), MainApplication.JVFORUM_SESSION);
			}

			checkAndCacheExtras(type, content);
			requestFieldset = null;

			Matcher m = PatternCollection.fetchTopicKickInterfaceUrl.matcher(content);
			if(m.find())
			{
				adminKickInterfaceUrl = m.group(1);
			}

			if(type == JvcTopic.SHOW_POSTS_FROM_PAGE)
			{
				m = PatternCollection.checkTopicAdminControls.matcher(content);
				if(m.find())
				{
					adminIsLocked = m.group(1) != null;
					adminLockUrl = m.group(2);
					adminIsPinned = m.group(3) != null;
					adminPinUrl = m.group(4);
				}

				m = PatternCollection.extractPageCount.matcher(content);
				if(m.find())
				{
					requestPageCount = Integer.parseInt(m.group(1));
				}
				else
				{
					requestPageCount = 1;
				}

				requestHasFirstPage = content.contains("class=\"p_debut\"");
				requestHasPreviousPage = content.contains("class=\"p_prec\"");
				requestHasNextPage = content.contains("class=\"p_suiv\"");
				requestHasLastPage = content.contains("class=\"p_fin\"");
			}
			else if(type == JvcTopic.SHOW_LAST_TEN_POSTS)
			{
				m = PatternCollection.extractFormFieldset.matcher(content);
				if(m.find())
				{
					requestFieldset = m.group(1);
				}

				adminLockUrl = null;
				adminPinUrl = null;

				requestPageCount = 1;
				requestHasFirstPage = false;
				requestHasPreviousPage = false;
				requestHasNextPage = false;
				requestHasLastPage = false;
			}

			m = PatternCollection.checkErrorMessage.matcher(content);
			if(m.find())
			{
				requestError = m.group(1);
				return true;
			}

			if(pageNumber == 1)
			{
				m = PatternCollection.fetchSuiteSujetClass.matcher(content);
				if(m.find())
				{
					JvcPost post = new JvcPost(this, StringHelper.unescapeHTML(m.group(1)), StringHelper.unescapeHTML(m.group(2)), StringHelper.unescapeHTML(m.group(3)));
					postList.add(post);
				}
			}

			m = PatternCollection.fetchNextPost.matcher(content);
			while(m.find())
			{
				String link;
				try
				{
					link = StringHelper.unescapeHTML(m.group(12));
				}
				catch(Exception e)
				{
					link = "";
				}
				JvcPost post = new JvcPost(this, Long.parseLong(m.group(1)), StringHelper.unescapeHTML(m.group(11)), m.group(6), m.group(5) != null, m.group(7) != null, Integer.parseInt(m.group(2)), m.group(8), link, StringHelper.unescapeHTML(m.group(10)));
				if(m.group(3) != null)
				{
					post.setAdminDeleteData(StringHelper.unescapeHTML(m.group(3)), m.group(4) != null);
				}
				if(m.group(9) != null)
				{
					post.setAdminKickUrl(StringHelper.unescapeHTML(m.group(9)));
				}
				if(m.group(13) != null)
				{
					post.setNextTopicData(StringHelper.unescapeHTML(m.group(13)), StringHelper.unescapeHTML(m.group(14)), StringHelper.unescapeHTML(m.group(15)));
				}
				postList.add(post);
			}

			requestResult = postList;
			if(requestResult.isEmpty())
			{
				Log.e("JvcForumsReader", "JvcTopic (topic empty) : " + content);
				requestIsTopicEmpty = true;
				requestError = getForum().getContext().getString(R.string.postListEmpty);
				return true;
			}

			requestLastPostId = requestResult.get(requestResult.size() - 1).getPostId();

			return false;
		}
		else
		{
			requestError = "Not immutable";
			return true;
		}
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

	public int publishOnForum() throws Exception
	{
		MainApplication context = (MainApplication) forum.getContext().getApplicationContext();
		boolean forumJv = forum.isForumJV();
		requestError = null;

		if(!sendable)
		{
			requestError = String.format("JvcTopic %d not sendable !", topicId);
			return REQUEST_ERROR;
		}

		String fieldset;
		if(!postIsMobile || (userCaptchaCode != null))
		{
			fieldset = forum.getRequestFieldset();
			if(fieldset == null)
			{
				requestError = forum.getContext().getString(R.string.pleaseRetryPostMessage);
				return REQUEST_ERROR;
			}
		}
		else /* Fetch mobile fieldset */
		{
			String content = getContentFromUrl(forum.makeUrlFromParams(JvcForum.SHOW_REPLY_FORM, 1, null, true), MainApplication.JVC_MOBILE_SESSION);
			Matcher m = PatternCollection.extractMobileFormFieldset.matcher(content);
			if(m.find())
			{
				fieldset = m.group(1);
			}
			else
			{
				requestError = "Assertion";
				return REQUEST_ERROR;
			}
		}

		nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("yournewmessage", JvcUtils.applyInverseFontCompatibility(sendableContent)));
		nameValuePairs.add(new BasicNameValuePair("newsujet", JvcUtils.applyInverseFontCompatibility(extraTopicName)));
		if(userCaptchaCode != null)
		{
			nameValuePairs.add(new BasicNameValuePair("session", requestCaptchaSession));
			nameValuePairs.add(new BasicNameValuePair("code", userCaptchaCode));
			userCaptchaCode = null;
		}

		XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
		ContentHandler handler = new DefaultHandler()
		{
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
			{
				if(qName.equals("input"))
				{
					nameValuePairs.add(new BasicNameValuePair(attributes.getValue("name"), attributes.getValue("value")));
				}
			}
		};
		xmlReader.setContentHandler(handler);
		xmlReader.parse(new InputSource(new StringReader(fieldset)));

		HttpClient client;
		HttpPost httpPost;
		if(postIsMobile)
		{
			client = context.getHttpClient(MainApplication.JVC_MOBILE_SESSION);
			httpPost = new HttpPost(JvcPost.JVC_FORUM_MOBILE_CGI_URL);
		}
		else
		{
			if(!forumJv)
			{
				client = context.getHttpClient(MainApplication.JVC_SESSION);
				httpPost = new HttpPost(JvcPost.JVC_FORUM_CGI_URL);
			}
			else
			{
				client = context.getHttpClient(MainApplication.JVFORUM_SESSION);
				httpPost = new HttpPost(forum.getBaseUrl() + "cgi-bin/jvforums/forums.cgi");
			}
		}
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		HttpResponse response = client.execute(httpPost);
		HttpEntity entity = response.getEntity();
		requestContent = MainApplication.getEntityContent(entity);

		Matcher m;
		if(!postIsMobile)
		{
			m = PatternCollection.checkErrorMessage.matcher(requestContent);
			if(m.find())
				requestError = m.group(1).replace("<br />", "");

			if(forumJv)
			{
				m = PatternCollection.extractForumJVCaptchaData.matcher(requestContent);
			}
			else
			{
				m = PatternCollection.extractCaptchaData.matcher(requestContent);
			}
			if(m.find())
			{
				forum.updateFieldsetFromContent(requestContent);
				requestCaptchaSession = m.group(1);
				requestCaptchaUrl = m.group(2);
				return REQUEST_CAPTCHA_REQUIRED;
			}
			else
			{
				if(requestError != null)
				{
					if(requestError.contains(JvcPost.FORM_EXPIRED_ERROR_MESSAGE))
					{
						return REQUEST_RETRY;
					}
					else
					{
						return REQUEST_ERROR_FROM_JVC;
					}
				}
			}
		}
		else
		{
			m = PatternCollection.checkMobileErrorMessage.matcher(requestContent);
			if(m.find())
				requestError = m.group(1).replace("</li>\n<li>", "\n");

			m = PatternCollection.extractMobileCaptchaData.matcher(requestContent);
			if(m.find())
			{
				forum.updateMobileFieldsetFromContent(requestContent);
				requestCaptchaSession = m.group(1);
				requestCaptchaUrl = m.group(2);
				return REQUEST_CAPTCHA_REQUIRED;
			}
			else
			{
				if(requestError != null)
				{
					if(requestError.contains(JvcPost.FORM_EXPIRED_ERROR_MESSAGE))
					{
						return REQUEST_RETRY;
					}
					else
					{
						return REQUEST_ERROR_FROM_JVC;
					}
				}
			}
		}

		return REQUEST_OK;
	}

	public UpdatedTopicData getUpdatedTopicData()
	{
		return updatedTopicData;
	}

	public void setUpdatedTopicData(UpdatedTopicData data)
	{
		updatedTopicData = data;
	}

	public ArrayList<JvcPost> getRequestResult()
	{
		return requestResult;
	}

	public long getRequestLastPostId()
	{
		return requestLastPostId;
	}

	public String getRequestError()
	{
		return requestError;
	}

	public String getRequestContent()
	{
		return requestContent;
	}

	public String getRequestCaptchaUrl()
	{
		return requestCaptchaUrl;
	}

	public String getRequestCaptchaSession()
	{
		return requestCaptchaSession;
	}

	public String getRequestFieldset()
	{
		return requestFieldset;
	}

	public String getRequestRedirectedUrl()
	{
		return requestRedirectedUrl;
	}

	public int getRequestPageCount()
	{
		return requestPageCount;
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

	public boolean requestHasLastPage()
	{
		return requestHasLastPage;
	}

	public boolean requestIsTopicEmpty()
	{
		return requestIsTopicEmpty;
	}

	private void checkAndCacheExtras(int type, String content)
	{
		if(extraTopicName == null)
		{
			Matcher matcher = PatternCollection.fetchTopicName.matcher(content);
			if(matcher.find())
			{
				extraTopicName = StringHelper.unescapeHTML(matcher.group(1));
			}
		}

		if(type == JvcTopic.SHOW_LAST_TEN_POSTS)
		{
			extraIsLocked = false;
		}
		else
		{
			Matcher matcher = PatternCollection.findReplyButton.matcher(content);
			extraIsLocked = !matcher.find();
		}
	}

	private String makeUrlFromParams(int requestType, int pageNumber)
	{
		return forum.getBaseUrl() + String.format("%d-%d-%d-%d-0-1-0-0.htm", requestType, forum.getForumId(), topicId, pageNumber);
	}

	public String makeUrlFromParams(int requestType, int pageNumber, boolean mobile)
	{
		if(mobile)
		{
			return forum.getBaseUrlMobile() + String.format("%d-%d-%d-%d-0-1-0-0.htm", requestType, forum.getForumId(), topicId, pageNumber);
		}
		else
		{
			return makeUrlFromParams(requestType, pageNumber);
		}
	}

	public String getContentFromUrl(String url, int auth_type) throws IOException
	{
		MainApplication app = (MainApplication) forum.getContext().getApplicationContext();
		HttpClient client = app.getHttpClient(auth_type);
		HttpContext httpContext = new BasicHttpContext();
		HttpEntity entity = client.execute(new HttpGet(url), httpContext).getEntity();
		String redirectedUrl = ((HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST)).getURI().toString();
		if(forum.isForumJV())
		{
			requestRedirectedUrl = forum.getBaseUrl() + redirectedUrl.substring(1);
		}
		else
		{
			requestRedirectedUrl = "http://www.jeuxvideo.com" + redirectedUrl;
		}

		return MainApplication.getEntityContent(entity);
	}

	public JvcForum getForum()
	{
		return forum;
	}

	public long getTopicId()
	{
		return topicId;
	}

	public boolean isReplyTopic()
	{
		return isReplyTopic;
	}

	public int getReplyPageNumber()
	{
		return replyPageNumber;
	}

	public long getReplyPostId()
	{
		return replyPostId;
	}

	public void setReplyPostId(long id)
	{
		replyPostId = id;
	}

	public void setExtras(String topicName, int colorSwitch, String iconName, String pseudo, boolean isAdmin, int postCount, String date)
	{
		extraTopicName = topicName;
		extraColorSwitch = colorSwitch;
		extraIconName = iconName;
		extraPseudo = pseudo;
		extraIsAdmin = isAdmin;
		extraPostCount = postCount;
		extraDate = date;
	}

	public String getExtraTopicName()
	{
		if(extraTopicName == null)
			return "(?)";

		return extraTopicName;
	}

	public void invalidateTopicName()
	{
		extraTopicName = null;
	}

	public boolean isTopicLocked()
	{
		return extraIsLocked;
	}

	public int getExtraColorSwitch()
	{
		return extraColorSwitch;
	}

	public String getExtraIconName()
	{
		if(isReplyTopic)
		{
			return "for_fleche.png";
		}

		return extraIconName;
	}

	public String getExtraPseudo()
	{
		return extraPseudo;
	}

	public boolean getExtraIsAdmin()
	{
		return extraIsAdmin;
	}

	public int getExtraPostCount()
	{
		return extraPostCount;
	}

	public String getExtraDate()
	{
		return extraDate;
	}

	public void setAdminDeleteUrl(String deleteUrl)
	{
		adminDeleteUrl = deleteUrl;
	}

	public String getAdminDeleteUrl()
	{
		return adminDeleteUrl;
	}

	public boolean getAdminIsLocked()
	{
		return adminIsLocked;
	}

	public String getAdminLockUrl()
	{
		return adminLockUrl;
	}

	public boolean getAdminIsPinned()
	{
		return adminIsPinned;
	}

	public String getAdminPinUrl()
	{
		return adminPinUrl;
	}

	public String getAdminKickInterfaceUrl()
	{
		return adminKickInterfaceUrl;
	}

	public void prepareCaptcha(String captchaString)
	{
		userCaptchaCode = captchaString;
	}

	public static class NameAscendingComparator implements Comparator<JvcTopic>
	{
		@Override
		public int compare(JvcTopic jvcTopic, JvcTopic jvcTopic1)
		{
			return jvcTopic.getExtraTopicName().compareToIgnoreCase(jvcTopic1.getExtraTopicName());
		}
	}

	public static class TopicIdAscendingComparator implements Comparator<JvcTopic>
	{
		@Override
		public int compare(JvcTopic jvcTopic, JvcTopic jvcTopic1)
		{
			return (int) (jvcTopic1.getTopicId() - jvcTopic.getTopicId());
		}
	}
}
