package com.forum.jvcreader.jvc;

import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.PatternCollection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class JvcPost implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final String JVC_FORUM_CGI_URL = "http://www.jeuxvideo.com/cgi-bin/jvforums/forums.cgi";
	public static final String JVC_FORUM_MOBILE_CGI_URL = "http://m.jeuxvideo.com/cgi-bin/jvforums/forums.cgi";
	public static final String FORM_EXPIRED_ERROR_MESSAGE = "Le formulaire est expir\u00E9";

	public static final int REQUEST_OK = 0;
	public static final int REQUEST_CAPTCHA_REQUIRED = 1;
	public static final int REQUEST_ERROR_FROM_JVC = 2;
	public static final int REQUEST_ERROR = 3;
	public static final int REQUEST_RETRY = 4;

	public static final int TYPE_REGULAR = 0;
	public static final int TYPE_GENERIC = 1;
	public static final int TYPE_PREVIOUS_TOPIC = 2;

	private int type = TYPE_REGULAR;
	private long postId;
	transient private JvcTopic topic;
	transient private boolean sendable = false;

	private String postData;
	private String postPseudo;
	private boolean postIsAdmin;
	private boolean postIsMobile;
	private int postColorSwitch;
	private String postDate;
	private String postPermanentLink;
	transient private String postDdbLink;

	transient private String otherTopicNotice = null; /* If notice != null then post has next topic message */
	transient private String otherTopicUrl;
	transient private String otherTopicName;

	transient private String adminDeleteUrl = null;
	transient private boolean adminDeleteRequestConfirm;
	transient private String adminKickUrl = null;

	transient private String requestError;
	transient private String requestCaptchaSignature; /* For PMs */
	transient private String requestCaptchaKey; /* For PMs */
	transient private String requestCaptchaUrl;
	transient private String requestCaptchaSession; /* For topics */
	transient private String lastContent;

	transient private String userCaptchaCode = null;
	transient private List<NameValuePair> nameValuePairs;

	public JvcPost(JvcTopic topic, String data, boolean isMobile)
	{
		sendable = true;

		this.topic = topic;
		postData = data;
		postIsMobile = isMobile;
	}

	public JvcPost(JvcTopic topic, long postId, String data, String pseudo, boolean isAdmin, boolean isMobile, int colorSwitch, String date, String permanentLink, String ddbLink)
	{
		this.topic = topic;
		sendable = false;
		this.postId = postId;

		postData = data;
		postPseudo = pseudo;
		postIsAdmin = isAdmin;
		postIsMobile = isMobile;
		postColorSwitch = colorSwitch;
		postDate = date;
		postPermanentLink = permanentLink;
		postDdbLink = ddbLink;
	}

	public JvcPost(JvcTopic topic, String notice, String url, String name)
	{
		this.topic = topic;
		sendable = false;
		type = TYPE_PREVIOUS_TOPIC;

		otherTopicNotice = notice;
		otherTopicUrl = url;
		otherTopicName = name;

		postData = "";
		postPseudo = "";
		postIsAdmin = false;
		postIsMobile = false;
		postColorSwitch = 2;
		postDate = "";
		postPermanentLink = null;
		postDdbLink = null;
	}

	public void setNextTopicData(String notice, String url, String name)
	{
		otherTopicNotice = notice;
		otherTopicUrl = url;
		otherTopicName = name;
	}

	public String getOtherTopicNotice()
	{
		return otherTopicNotice;
	}

	public String getOtherTopicUrl()
	{
		return otherTopicUrl;
	}

	public String getOtherTopicName()
	{
		return otherTopicName;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public int getType()
	{
		return type;
	}

	public void prepareCaptcha(String code)
	{
		userCaptchaCode = code;
	}

	public int publishOnTopic() throws IOException, SAXException
	{
		MainApplication context = (MainApplication) topic.getForum().getContext().getApplicationContext();
		boolean forumJv = topic.getForum().isForumJV();
		requestError = null;

		if(!sendable)
		{
			requestError = String.format("JvcPost %d not sendable !", postId);
			return REQUEST_ERROR;
		}

		String fieldset;
		if(!postIsMobile || (userCaptchaCode != null))
		{
			fieldset = topic.getRequestFieldset();
			if(fieldset == null)
			{
				requestError = context.getString(R.string.pleaseRetryPostMessage);
				return REQUEST_ERROR;
			}
		}
		else /* Fetch mobile fieldset for the first time only */
		{
			String content = topic.getContentFromUrl(topic.makeUrlFromParams(JvcTopic.SHOW_LAST_TEN_POSTS, 0, true), MainApplication.JVC_MOBILE_SESSION);
			Matcher m = PatternCollection.extractMobileFormFieldset.matcher(content);
			if(m.find())
			{
				fieldset = m.group(1);
			}
			else
			{
				requestError = "no mobile fieldset";
				return REQUEST_ERROR;
			}
		}

		nameValuePairs = new ArrayList<NameValuePair>();

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

		nameValuePairs.add(new BasicNameValuePair("yournewmessage", JvcUtils.applyInverseFontCompatibility(postData)));
		if(userCaptchaCode != null)
		{
			nameValuePairs.add(new BasicNameValuePair("session", requestCaptchaSession));
			nameValuePairs.add(new BasicNameValuePair("code", userCaptchaCode));
			userCaptchaCode = null;
		}
		nameValuePairs.add(new BasicNameValuePair("Submit.x", String.valueOf((int) Math.floor(Math.random() * 50) + 1)));
		nameValuePairs.add(new BasicNameValuePair("Submit.y", String.valueOf((int) Math.floor(Math.random() * 8) + 1)));

		HttpClient client;
		HttpPost httpPost;
		if(postIsMobile)
		{
			client = context.getHttpClient(MainApplication.JVC_MOBILE_SESSION);
			httpPost = new HttpPost(JVC_FORUM_MOBILE_CGI_URL);
		}
		else
		{
			if(!forumJv)
			{
				client = context.getHttpClient(MainApplication.JVC_SESSION);
				httpPost = new HttpPost(JVC_FORUM_CGI_URL);
			}
			else
			{
				client = context.getHttpClient(MainApplication.JVFORUM_SESSION);
				httpPost = new HttpPost(topic.getForum().getBaseUrl() + "cgi-bin/jvforums/forums.cgi");
			}
		}
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		HttpResponse response = client.execute(httpPost);
		HttpEntity entity = response.getEntity();
		String content = MainApplication.getEntityContent(entity);

		Matcher m;
		if(!postIsMobile)
		{
			m = PatternCollection.checkErrorMessage.matcher(content);
			if(m.find())
				requestError = m.group(1).replace("<br />", "");

			if(forumJv)
			{
				m = PatternCollection.extractForumJVCaptchaData.matcher(content);
			}
			else
			{
				m = PatternCollection.extractCaptchaData.matcher(content);
			}
			if(m.find())
			{
				topic.updateFieldsetFromContent(content);
				requestCaptchaSession = m.group(1);
				requestCaptchaUrl = m.group(2);
				return REQUEST_CAPTCHA_REQUIRED;
			}
			else
			{
				if(requestError != null)
				{
					if(requestError.contains(FORM_EXPIRED_ERROR_MESSAGE))
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
			m = PatternCollection.checkMobileErrorMessage.matcher(content);
			if(m.find())
				requestError = m.group(1).replace("</li>\n<li>", "\n");

			m = PatternCollection.extractMobileCaptchaData.matcher(content);
			if(m.find())
			{
				topic.updateMobileFieldsetFromContent(content);
				requestCaptchaSession = m.group(1);
				requestCaptchaUrl = m.group(2);
				return REQUEST_CAPTCHA_REQUIRED;
			}
			else
			{
				if(requestError != null)
				{
					if(requestError.contains(FORM_EXPIRED_ERROR_MESSAGE))
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

	public int publishOnPmTopic(JvcPmTopic topic) throws IOException
	{
		MainApplication context = (MainApplication) topic.getContext().getApplicationContext();
		requestError = null;

		if(!sendable)
		{
			requestError = String.format("JvcPost %d not sendable !", postId);
			return REQUEST_ERROR;
		}

		final String jvcControlValue = topic.getRequestControlValue();
		final String jvcTmpValue = topic.getRequestTmpValue();

		if(jvcControlValue == null || jvcControlValue.length() == 0 || jvcTmpValue == null || jvcTmpValue.length() == 0)
		{
			requestError = context.getString(R.string.pleaseRetryPostMessage);
			return REQUEST_ERROR;
		}

		nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("control", jvcControlValue));
		nameValuePairs.add(new BasicNameValuePair("tmp", jvcTmpValue));
		nameValuePairs.add(new BasicNameValuePair("box", String.valueOf(topic.getBoxType())));
		nameValuePairs.add(new BasicNameValuePair("idd", String.valueOf(topic.getId())));
		nameValuePairs.add(new BasicNameValuePair("yournewmessage", JvcUtils.applyInverseFontCompatibility(postData)));
		if(userCaptchaCode != null)
		{
			nameValuePairs.add(new BasicNameValuePair("signature", requestCaptchaSignature));
			nameValuePairs.add(new BasicNameValuePair("clef", requestCaptchaKey));
			nameValuePairs.add(new BasicNameValuePair("code", userCaptchaCode));
			userCaptchaCode = null;
		}
		nameValuePairs.add(new BasicNameValuePair("Submit.x", String.valueOf((int) (Math.random() * 100))));
		nameValuePairs.add(new BasicNameValuePair("Submit.y", String.valueOf((int) (Math.random() * 15))));

		HttpClient client = context.getHttpClient(MainApplication.JVC_SESSION);
		HttpPost httpPost = new HttpPost("http://www.jeuxvideo.com/messages-prives/message.php");
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		HttpResponse response = client.execute(httpPost);
		HttpEntity entity = response.getEntity();
		String content = MainApplication.getEntityContent(entity);
		lastContent = content;

		Matcher m = PatternCollection.checkPmTopicErrorMessage.matcher(content);
		if(m.find())
		{
			requestError = m.group(1);
		}

		m = PatternCollection.extractPmTopicCaptchaData.matcher(content);
		if(m.find())
		{
			if(requestError == null)
				requestError = context.getString(R.string.captchaTextView);
			requestCaptchaSignature = m.group(1);
			requestCaptchaKey = m.group(2);
			requestCaptchaUrl = m.group(3);

			m = PatternCollection.extractPmTopicFieldset.matcher(content);
			if(m.find())
			{
				topic.updateFieldset(m.group(1), m.group(2));
			}
			else
			{
				requestError = "could not find field set in order to proceed captcha";
				return REQUEST_ERROR;
			}

			return REQUEST_CAPTCHA_REQUIRED;
		}
		else if(requestError != null)
		{
			if(requestError.contains(FORM_EXPIRED_ERROR_MESSAGE))
			{
				return REQUEST_RETRY;
			}
			else
			{
				return REQUEST_ERROR_FROM_JVC;
			}
		}

		return REQUEST_OK;
	}

	public String getRequestError()
	{
		return requestError;
	}

	public String getRequestCaptchaUrl()
	{
		return requestCaptchaUrl;
	}

	public long getPostId()
	{
		return postId;
	}

	public String getPostData()
	{
		return postData;
	}

	public String getTextualPostData()
	{
		String text;
		text = postData.replaceAll("<img src=\".+?\" alt=\"(.+?)\" />", "$1"); /* Smileys */
		text = text.replaceAll("<a href=\"(.+?)\".*>.+?</a>", "$1"); /* Links */
		text = text.replaceAll(" ?<br /> ?", "\n"); /* Line returns */
		return text;
	}

	public String getPostPseudo()
	{
		return postPseudo;
	}

	public boolean isAdminPost()
	{
		return postIsAdmin;
	}

	public boolean isMobilePost()
	{
		return postIsMobile;
	}

	public int getPostColorSwitch()
	{
		return postColorSwitch;
	}

	public String getPostDate()
	{
		return postDate;
	}

	public String getPostPermanentLink()
	{
		return postPermanentLink;
	}

	public String getPostDdbLink()
	{
		return postDdbLink;
	}

	public JvcTopic getTopic()
	{
		return topic;
	}

	public String getLastContent()
	{
		return lastContent;
	}

	public void setAdminDeleteData(String deleteUrl, boolean deleteRequestConfirm)
	{
		adminDeleteUrl = deleteUrl;
		adminDeleteRequestConfirm = deleteRequestConfirm;
	}

	public String getAdminDeleteUrl()
	{
		return adminDeleteUrl;
	}

	public boolean getAdminDeleteRequestConfirm()
	{
		return adminDeleteRequestConfirm;
	}

	public void setAdminKickUrl(String kickUrl)
	{
		adminKickUrl = kickUrl;
	}

	public String getAdminKickUrl()
	{
		return adminKickUrl;
	}
}
