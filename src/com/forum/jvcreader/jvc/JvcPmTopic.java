package com.forum.jvcreader.jvc;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;

public class JvcPmTopic
{
	public static final int ACTION_NONE = 0;
	public static final int ACTION_QUIT_TOPIC = 1;
	public static final int ACTION_CLOSE_TOPIC = 2;

	public static final int REQUEST_OK = 0;
	public static final int REQUEST_ERROR_FROM_JVC = 1;
	public static final int REQUEST_ERROR = 2;
	public static final int REQUEST_REQUIRE_CAPTCHA = 3;

	private Context context;
	private boolean sendable;
	private String sendableSubject;
	private String sendableContent;

	private String captchaCode = null;
	private String captchaSignature = null;
	private String captchaKey = null;
	private String captchaLink = null;

	private long id;
	private int colorSwitch;
	private String pseudo;
	private boolean read;
	private String subject;
	private String date;
	private int boxType;
	private boolean isAdmin;

	private ArrayList<JvcPost> requestPostList;
	private LinkedHashMap<JvcPseudo, String> requestPseudosMap;
	private String requestError;
	private int requestAvailablePreviousMessages;
	private String requestControlValue;
	private String requestTmpValue;
	private boolean requestIsTopicLocked;
	private int requestAvailableAction = ACTION_NONE;
	private String requestContent;
	private long requestNewTopicId;
	private String requestX, requestZ;
	private boolean requestCanAddRecipient = false;

	public JvcPmTopic(Context context, long id, int colorSwitch, String pseudo, boolean read, String subject, String date, int boxType, boolean isAdmin)
	{
		this.context = context;
		this.id = id;
		this.colorSwitch = colorSwitch;
		this.pseudo = pseudo;
		this.read = read;
		this.subject = subject;
		this.date = date;
		this.boxType = boxType;
		this.isAdmin = isAdmin;

		sendable = false;
	}

	public JvcPmTopic(Context context, long id, int boxType, boolean read)
	{
		this.context = context;
		this.id = id;
		this.boxType = boxType;
		this.read = read;

		sendable = false;
	}

	public JvcPmTopic(Context context, String subject, String content)
	{
		this.context = context;
		this.sendableSubject = subject;
		this.sendableContent = content;

		sendable = true;
	}

	public void saveInRecoveryMode(Bundle bundle)
	{
		bundle.putLong("JvcPmTopic_id", id);
		bundle.putInt("JvcPmTopic_boxType", boxType);
		bundle.putBoolean("JvcPmTopic_read", read);
	}

	public static JvcPmTopic restoreInRecoveryMode(Context context, Bundle bundle)
	{
		return new JvcPmTopic(context, bundle.getLong("JvcPmTopic_id"), bundle.getInt("JvcPmTopic_boxType"), bundle.getBoolean("JvcPmTopic_read"));
	}

	public boolean requestInitialTopicList(AsyncTask task) throws IOException
	{
		String content = getContentFromUrl(String.format("http://www.jeuxvideo.com/messages-prives/message.php?idd=%d&box=%d", id, boxType), "ISO-8859-1");
		if(task.isCancelled())
			return false;
		return getInitialTopicListFromContent(content);
	}

	public boolean getInitialTopicListFromContent(String content)
	{
		requestPostList = new ArrayList<JvcPost>();
		
		/* JVC Error */
		Matcher m = PatternCollection.extractPmTopicInfoMessage.matcher(content);
		if(m.find())
		{
			requestError = m.group(1);
			return true;
		}
		
		/* Participating pseudos */
		m = PatternCollection.fetchPmTopicPseudosFrame.matcher(content);
		if(m.find())
		{
			requestPseudosMap = new LinkedHashMap<JvcPseudo, String>();
			m = PatternCollection.fetchNextPmTopicPseudo.matcher(content);
			while(m.find())
			{
				requestPseudosMap.put(new JvcPseudo(m.group(3), m.group(2) != null, true), m.group(1));
			}

			if(requestPseudosMap.size() == 0)
			{
				requestError = "participating pseudos map empty";
				return true;
			}
		}
		else
		{
			requestError = "participating pseudos frame not found";
			return true;
		}
		
		/* Available action */
		if(content.contains("/bt_quitter.png\""))
		{
			requestAvailableAction = ACTION_QUIT_TOPIC;
		}
		else if(content.contains("/bt_fermer.png\""))
		{
			requestAvailableAction = ACTION_CLOSE_TOPIC;
		}
		else
		{
			requestAvailableAction = ACTION_NONE;
		}

		if(content.contains("<span id='ajout_destinataire'>"))
		{
			requestCanAddRecipient = true;

			m = PatternCollection.extractPmTopicTimeKeyValues.matcher(content);
			if(m.find())
			{
				requestX = m.group(1);
				requestZ = m.group(2);
			}
			else
			{
				requestError = "X & Z missing for adding recipient";
				return true;
			}
		}
		else
		{
			requestCanAddRecipient = false;
		}
		
		/* Fieldset */
		m = PatternCollection.extractPmTopicFieldset.matcher(content);
		if(m.find())
		{
			requestControlValue = m.group(1);
			requestTmpValue = m.group(2);
		}
		
		/* Available previous messages */
		if(content.contains("<span>Voir le  message pr\u00E9c\u00E9dent</span>"))
		{
			requestAvailablePreviousMessages = 1;
		}
		else
		{
			m = PatternCollection.extractPmTopicPreviousPostsCount.matcher(content);
			if(m.find())
			{
				requestAvailablePreviousMessages = Integer.parseInt(m.group(1));
			}
			else
			{
				requestAvailablePreviousMessages = 0;
			}
		}
		
		/* Posts */
		addPostsToList(content);
		Collections.reverse(requestPostList);
		if(requestPostList.size() == 0)
		{
			requestError = "post list empty";
			return true;
		}
		
		/* Topic lock state */
		requestIsTopicLocked = !content.contains("<div id=\"rediger\">");

		return false;
	}

	public ArrayList<JvcPost> requestPreviousMessages(int page, AsyncTask task) throws IOException
	{
		requestPostList = new ArrayList<JvcPost>();
		String content = getContentFromUrl(String.format("http://www.jeuxvideo.com/messages-prives/ajax_prec_msg.php?idd=%d&nb_clic=%d&last_position=0", id, page), "UTF-8");
		if(task.isCancelled())
			return null;

		Matcher m = PatternCollection.extractPmTopicPreviousPostsCount.matcher(content);
		if(m.find())
		{
			requestAvailablePreviousMessages = Integer.parseInt(m.group(1));
		}
		else
		{
			requestAvailablePreviousMessages = 0;
		}

		addPostsToList(content);
		Collections.reverse(requestPostList);

		return requestPostList;
	}

	public int send(HashMap<String, Boolean> recipientMap, String tmp, String control) throws ParseException, IOException
	{
		MainApplication appContext = (MainApplication) context.getApplicationContext();
		requestError = null;

		if(!sendable)
		{
			requestError = String.format("JvcPmTopic %d not sendable !", id);
			return REQUEST_ERROR;
		}

		String all_dest = "";
		for(String recipient : recipientMap.keySet())
		{
			all_dest += recipient + ';';
		}

		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("all_dest", all_dest));
		nameValuePairs.add(new BasicNameValuePair("control", control));
		nameValuePairs.add(new BasicNameValuePair("tmp", tmp));
		nameValuePairs.add(new BasicNameValuePair("newdest", ""));
		nameValuePairs.add(new BasicNameValuePair("sujet", JvcUtils.applyInverseFontCompatibility(sendableSubject)));
		nameValuePairs.add(new BasicNameValuePair("yournewmessage", JvcUtils.applyInverseFontCompatibility(sendableContent)));
		if(captchaCode != null && captchaCode.length() > 0)
		{
			nameValuePairs.add(new BasicNameValuePair("signature", captchaSignature));
			nameValuePairs.add(new BasicNameValuePair("clef", captchaKey));
			nameValuePairs.add(new BasicNameValuePair("code", captchaCode));
		}
		nameValuePairs.add(new BasicNameValuePair("Submit.x", String.valueOf((int) (Math.random() * 100))));
		nameValuePairs.add(new BasicNameValuePair("Submit.y", String.valueOf((int) (Math.random() * 15))));

		HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
		HttpPost httpPost = new HttpPost("http://www.jeuxvideo.com/messages-prives/nouveau.php");
		HttpContext httpContext = new BasicHttpContext();
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		HttpResponse response = client.execute(httpPost, httpContext);
		String content = MainApplication.getEntityContent(response.getEntity());
		String newUrl = ((HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST)).getURI().toString();

		Matcher m = PatternCollection.checkPmTopicErrorMessage.matcher(content);
		if(m.find())
		{
			requestError = m.group(1);

			m = PatternCollection.extractPmTopicCaptchaData.matcher(content);
			if(m.find())
			{
				captchaSignature = m.group(1);
				captchaKey = m.group(2);
				captchaLink = m.group(3);

				m = PatternCollection.extractPmNewFormData.matcher(content);
				if(m.find())
				{
					requestControlValue = m.group(1);
					requestTmpValue = m.group(2);
					return REQUEST_REQUIRE_CAPTCHA;
				}
				else
				{
					requestError = "no fieldset in nouveau.php";
					return REQUEST_ERROR;
				}
			}
			else
			{
				return REQUEST_ERROR_FROM_JVC;
			}
		}

		requestContent = content;
		m = PatternCollection.extractPmTopicUrlFromNewPm.matcher(newUrl);
		if(!m.find())
		{
			requestError = "can't parse new topic url";
			return REQUEST_ERROR;
		}
		requestNewTopicId = Long.parseLong(m.group(1));

		return REQUEST_OK;
	}

	public int addRecipients(List<String> recipientList)
	{
		if(!requestCanAddRecipient)
		{
			requestError = "can't add recipient";
			return REQUEST_ERROR;
		}

		String all_dest = "";
		for(String recipient : recipientList)
		{
			all_dest += recipient + ';';
		}

		String url = "http://www.jeuxvideo.com/messages-prives/ajax_add_destinataire.php?id_discussion=" + id;
		url += "&tab_pseudo=" + all_dest + "&x=" + requestX + "&z=" + requestZ;

		try
		{
			MainApplication app = (MainApplication) context.getApplicationContext();
			HttpClient client = app.getHttpClient(MainApplication.JVC_SESSION);
			HttpGet httpGet = new HttpGet(url);
			client.execute(httpGet);
		}
		catch(UnknownHostException e)
		{
			requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			return REQUEST_ERROR;
		}
		catch(HttpHostConnectException e)
		{
			requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			return REQUEST_ERROR;
		}
		catch(ConnectTimeoutException e)
		{
			requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			return REQUEST_ERROR;
		}
		catch(SocketTimeoutException e)
		{
			requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			return REQUEST_ERROR;
		}
		catch(Exception e)
		{
			requestError = context.getString(R.string.errorWhileLoading) + " : " + e.toString();
			return REQUEST_ERROR;
		}

		return REQUEST_OK;
	}

	public boolean getRequestCanAddRecipient()
	{
		return requestCanAddRecipient;
	}

	public ArrayList<JvcPost> getRequestPostList()
	{
		return requestPostList;
	}

	public LinkedHashMap<JvcPseudo, String> getRequestPseudosMap()
	{
		return requestPseudosMap;
	}

	public String getRequestError()
	{
		return requestError;
	}

	public int getRequestAvailablePreviousMessages()
	{
		return requestAvailablePreviousMessages;
	}

	public String getRequestControlValue()
	{
		return requestControlValue;
	}

	public String getRequestTmpValue()
	{
		return requestTmpValue;
	}

	public boolean requestIsTopicLocked()
	{
		return requestIsTopicLocked;
	}

	public int getRequestAvailableAction()
	{
		return requestAvailableAction;
	}

	public String getRequestContent()
	{
		return requestContent;
	}

	public long getRequestNewTopicId()
	{
		return requestNewTopicId;
	}

	private void addPostsToList(String content)
	{
		Matcher m = PatternCollection.fetchNextPmPost.matcher(content);
		while(m.find())
		{
			JvcPost post = null;

			if(m.group(1) != null) /* Regular post */
			{
				int colorSwitch = Integer.parseInt(m.group(1)) == 1 ? 2 : 1;
				String postData = StringHelper.unescapeHTML(m.group(8).replace("<br />", "").replace("<img", " <img"));
				post = new JvcPost(null, id, postData + '\n', m.group(4), m.group(3) != null, m.group(5) != null, colorSwitch, m.group(6), null, StringHelper.unescapeHTML(m.group(7)));
			}
			else if(m.group(9) != null) /* Generic post */
			{
				int colorSwitch = Integer.parseInt(m.group(9)) == 1 ? 2 : 1;
				post = new JvcPost(null, id, StringHelper.unescapeHTML(m.group(11)), m.group(10), false, false, colorSwitch, null, null, null);
				post.setType(JvcPost.TYPE_GENERIC);
			}

			requestPostList.add(post);
		}
	}

	private String getContentFromUrl(String url, String charset) throws IOException
	{
		MainApplication appContext = (MainApplication) context.getApplicationContext();
		HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
		HttpGet httpGet = new HttpGet(url);
		HttpResponse response = client.execute(httpGet);
		HttpEntity entity = response.getEntity();

		return MainApplication.getEntityContent(entity, charset);
	}

	public void acknowledgeReadTopic()
	{
		read = true;
	}

	public Context getContext()
	{
		return context;
	}

	public int getColorSwitch()
	{
		return colorSwitch;
	}

	public long getId()
	{
		return id;
	}

	public String getPseudo()
	{
		return pseudo;
	}

	public boolean isTopicRead()
	{
		return read;
	}

	public String getSubject()
	{
		return subject;
	}

	public String getDate()
	{
		return date;
	}

	public int getBoxType()
	{
		return boxType;
	}

	public boolean isPseudoAdmin()
	{
		return isAdmin;
	}

	public void updateFieldset(String control, String tmp)
	{
		requestControlValue = control;
		requestTmpValue = tmp;
	}

	public String getCaptchaLink()
	{
		return captchaLink;
	}

	public void prepareForCaptcha(String code)
	{
		captchaCode = code;
	}
}
