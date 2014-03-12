package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcPmTopic;
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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PmDdbDialog extends Dialog
{
	private Context context;

	private JvcPmTopic topic;
	private JvcPost post;
	private String reason;

	private final ArrayList<String> reasonNameList = new ArrayList<String>();
	private final ArrayList<String> reasonIdList = new ArrayList<String>();

	private RadioGroup radioGroup;
	private TextView textView;
	private Button button;

	private String formDataIdMessage, formDataTs, formDataTk;

	public PmDdbDialog(Context context, JvcPost post, JvcPmTopic topic)
	{
		super(context);

		setTitle(context.getString(R.string.ddbDialogTitle));
		setContentView(R.layout.ddb_dialog);

		radioGroup = (RadioGroup) findViewById(R.id.ddbDialogRadioGroup);
		textView = (TextView) findViewById(R.id.ddbDialogTextView);
		button = (Button) findViewById(R.id.ddbDialogButton);
		button.setOnClickListener(buttonListener);

		this.context = context;
		this.topic = topic;
		this.post = post;

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
				HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet(post.getPostDdbLink());
				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(client.execute(httpGet).getEntity()));

				if(content.contains("<input type=\"text\""))
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
					throw new RuntimeException("ddb reason list empty");

				m = PatternCollection.extractPmDdbFormData.matcher(content);
				if(!m.find())
					throw new RuntimeException("can't find pm ddb form data");
				formDataIdMessage = m.group(1);
				formDataTs = m.group(2);
				formDataTk = m.group(3);

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
				return context.getString(R.string.errorWhileLoading) + " : " + e.toString();
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
				HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
				HttpPost httpPost = new HttpPost("http://www.jeuxvideo.com/messages-prives/alerte.php");

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("id_discussion", String.valueOf(topic.getId())));
				nameValuePairs.add(new BasicNameValuePair("id_message", formDataIdMessage));
				nameValuePairs.add(new BasicNameValuePair("ts", formDataTs));
				nameValuePairs.add(new BasicNameValuePair("tk", formDataTk));
				nameValuePairs.add(new BasicNameValuePair("motif", reason));
				nameValuePairs.add(new BasicNameValuePair("submit", "Valider"));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(client.execute(httpPost).getEntity()));

				Matcher m = PatternCollection.fetchDdbMessage.matcher(content);
				if(!m.find())
					throw new RuntimeException("no ddb message");

				return m.group(1);
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
			catch(IOException e)
			{
				return context.getResources().getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			button.setEnabled(false);
			button.setText(context.getResources().getString(R.string.genericLoading));
		}

		protected void onPostExecute(String result)
		{
			if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
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
