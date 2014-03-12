package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.widget.*;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcPost;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class KickDialog extends Dialog
{
	private Context context;

	private String url;
	private JvcPost post;
	private String reason;
	private boolean isForumJV;

	private final ArrayList<String> reasonNameList = new ArrayList<String>();
	private final ArrayList<String> reasonIdList = new ArrayList<String>();

	private RadioGroup radioGroup;
	private TextView textView;
	private Button button;

	private String formDataPage, formDataK;

	public KickDialog(Context context, JvcPost post)
	{
		super(context);

		setTitle(context.getResources().getString(R.string.kickDialogTitle));
		setContentView(R.layout.ddb_dialog);

		radioGroup = (RadioGroup) findViewById(R.id.ddbDialogRadioGroup);
		textView = (TextView) findViewById(R.id.ddbDialogTextView);
		button = (Button) findViewById(R.id.ddbDialogButton);
		button.setOnClickListener(buttonListener);

		isForumJV = post.getTopic().getForum().isForumJV();
		this.context = context;
		this.post = post;

		url = post.getAdminKickUrl();
		int index = url.indexOf('?');
		if(index != -1)
		{
			url = url.substring(0, index);
		}

		setOnShowListener(new OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialogInterface)
			{
				new LoadDdbFormTask().execute();
			}
		});
	}

	private final View.OnClickListener buttonListener = new View.OnClickListener()
	{
		public void onClick(View view)
		{
			if(reason != null)
			{
				new SendDdbTask().execute();
			}
		}
	};

	private class LoadDdbFormTask extends AsyncTask<Void, Void, String>
	{
		protected String doInBackground(Void... voids)
		{
			try
			{
				MainApplication appContext = (MainApplication) context.getApplicationContext();
				HttpClient client = appContext.getHttpClient(isForumJV ? MainApplication.JVFORUM_SESSION : MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet(post.getAdminKickUrl());
				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(client.execute(httpGet).getEntity()));

				if(content.contains("Vous n'avez pas le droit d'\u00EAtre ici"))
				{
					throw new RuntimeException("not connected !");
				}

				reasonNameList.clear();
				reasonIdList.clear();
				Matcher m = PatternCollection.fetchNextDdbReason.matcher(content);
				while(m.find())
				{
					reasonNameList.add(m.group(1));
					reasonIdList.add(m.group(2));
				}

				if(reasonNameList.size() == 0)
					throw new RuntimeException("kick reason list empty");

				m = PatternCollection.extractKickFormData.matcher(content);
				if(!m.find())
					throw new RuntimeException("can't find kick form data");
				formDataPage = m.group(1);
				formDataK = m.group(2);

				return null;
			}
			catch(UnknownHostException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(HttpHostConnectException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(ConnectTimeoutException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(SocketTimeoutException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(Exception e)
			{
				return context.getResources().getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPostExecute(String result)
		{
			if(result == null)
			{
				textView.setText(R.string.chooseDdbReason);
				for(int i = 0; i < reasonNameList.size(); i++)
				{
					RadioButton rb = new RadioButton(context);
					rb.setTextAppearance(context, android.R.attr.textAppearanceMedium);
					rb.setText(reasonNameList.get(i));
					rb.setTextColor(textView.getTextColors());
					rb.setOnClickListener(new RadioButtonListener(reasonIdList.get(i)));
					radioGroup.addView(rb);
				}
				button.setVisibility(View.VISIBLE);
				reason = null;
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				dismiss();
				MainApplication.handleHttpTimeout(context);
			}
			else
			{
				dismiss();
				NoticeDialog.show(context, result);
			}
		}
	}

	private class SendDdbTask extends AsyncTask<Void, Void, String>
	{
		protected String doInBackground(Void... voids)
		{
			try
			{
				MainApplication appContext = (MainApplication) context.getApplicationContext();
				HttpClient client = appContext.getHttpClient(isForumJV ? MainApplication.JVFORUM_SESSION : MainApplication.JVC_SESSION);
				HttpPost httpPost = new HttpPost(url);

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("Dargor", "Valider"));
				nameValuePairs.add(new BasicNameValuePair("forum", String.valueOf(post.getTopic().getForum().getForumId())));
				nameValuePairs.add(new BasicNameValuePair("topic", String.valueOf(post.getTopic().getTopicId())));
				nameValuePairs.add(new BasicNameValuePair("numero", String.valueOf(post.getPostId())));
				nameValuePairs.add(new BasicNameValuePair("page", formDataPage));
				nameValuePairs.add(new BasicNameValuePair("k", formDataK));
				nameValuePairs.add(new BasicNameValuePair("motif", reason));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(client.execute(httpPost).getEntity()));

				Matcher m = PatternCollection.fetchKickFormError.matcher(content);
				if(m.find())
				{
					return m.group(1);
				}
				else if(content.contains("ERROR"))
				{
					return content;
				}

				return null;
			}
			catch(UnknownHostException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(HttpHostConnectException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(ConnectTimeoutException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(SocketTimeoutException e)
			{
				return JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(Exception e)
			{
				return context.getResources().getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			button.setEnabled(false);
			button.setText(context.getString(R.string.genericLoading));
		}

		protected void onPostExecute(String result)
		{
			if(result == null)
			{
				dismiss();
				Toast.makeText(context, String.format(context.getString(R.string.pseudoKicked), post.getPostPseudo()), Toast.LENGTH_SHORT).show();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				dismiss();
				MainApplication.handleHttpTimeout(context);
			}
			else
			{
				textView.setText(Html.fromHtml(result));
				radioGroup.setVisibility(View.GONE);
				button.setVisibility(View.GONE);
			}
		}
	}

	private class RadioButtonListener implements View.OnClickListener
	{
		private String id;

		public RadioButtonListener(String id)
		{
			this.id = id;
		}

		public void onClick(View view)
		{
			reason = id;
		}
	}
}
