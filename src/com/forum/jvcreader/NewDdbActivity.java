package com.forum.jvcreader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.forum.jvcreader.graphics.CaptchaDialog;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcPost;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.GlobalData;
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

public class NewDdbActivity extends JvcActivity
{
	private String url;
	private JvcPost post;
	private String reason, explication;

	private final ArrayList<String> reasonNameList = new ArrayList<String>();
	private final ArrayList<String> reasonIdList = new ArrayList<String>();

	private RadioGroup radioGroup;
	private TextView textView, remarksTextView;
	private EditText editText;
	private Button button;

	private String formDataPage, formDataK, formDataT, formDataSession, formDataKey, captchaUrl, captchaCode, modeValue;
	private Boolean ddbAlreadySent = false;
	CaptchaDialog cDialog;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_ddb);

		url = "http://www.jeuxvideo.com/cgi-bin/jvforums/avertir_moderateur.cgi";

		radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
		textView = (TextView) findViewById(R.id.textView);
		remarksTextView = (TextView) findViewById(R.id.remarksTextView);
		editText = (EditText) findViewById(R.id.editText);
		button = (Button) findViewById(R.id.button);
		button.setOnClickListener(buttonListener);

		String key;
		if(savedInstanceState == null)
		{
			Intent intent = getIntent();
			key = intent.getStringExtra("com.forum.jvcreader.DdbPostKey");
			modeValue = intent.getStringExtra("com.forum.jvcreader.CurrentBrowsingMode");

		}
		else
		{
			key = savedInstanceState.getString("com.forum.jvcreader.DdbPostKey");
			modeValue = savedInstanceState.getString("com.forum.jvcreader.CurrentBrowsingMode");
		}

		post = (JvcPost) GlobalData.getOnce(key);
		if(post == null)
		{
			Log.e("JvcForumsReader", "NewDdbActivity: post is null");
			finish();
			return;
		}

		new LoadDdbFormTask().execute();
	}

	@Override
	public void onSaveInstanceState(Bundle instanceState)
	{
		String key = "ddbPost_" + System.currentTimeMillis();
		GlobalData.set(key, post);
		instanceState.putString("com.forum.jvcreader.DdbPostKey", key);
		instanceState.putString("com.forum.jvcreader.CurrentBrowsingMode", modeValue);
	}

	private final View.OnClickListener buttonListener = new View.OnClickListener()
	{
		public void onClick(View view)
		{
			if(reason != null)
			{
				if(ddbAlreadySent)
				{
					explication = "plus un http://www.jeuxvideo.com";
				}
				else
				{
					explication = editText.getText().toString();
					if(explication == null || explication.isEmpty())
					{
						NoticeDialog.show(NewDdbActivity.this, "Vous devez renseigner le champ \"Remarques\" limité à 200 caractères.");
						return;
					}
				}

				if(captchaCode == null)
				{
					cDialog = new CaptchaDialog(NewDdbActivity.this, captchaUrl);
					cDialog.setSubmitButtonListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							captchaCode = cDialog.getCaptchaString();
							cDialog.hide();
							new SendDdbTask().execute();
						}
					});
					cDialog.show();
				}
			}
		}
	};

	private class LoadDdbFormTask extends AsyncTask<Void, Void, String>
	{
		protected String doInBackground(Void... voids)
		{
			try
			{
				MainApplication appContext = (MainApplication) getApplication();
				HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet(post.getPostDdbLink());
				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(client.execute(httpGet).getEntity()));

				if(content.contains("Veuillez vous connecter"))
				{
					throw new RuntimeException("not connected !!!");
				}
				else if(content.contains("Le formulaire est expiré"))
				{
					throw new RuntimeException("Formulaire expiré. Rafraîchissez le topic et réessayez.");
				}
				else if(content.contains("Vous êtes à l'origine de l'alerte, vous ne pouvez pas booster."))
				{
					throw new RuntimeException("Vous êtes à l'origine de l'alerte, vous ne pouvez pas booster.");
				}
				else if(content.contains("Vous avez déjà effectué un boost pour cette alerte !"))
				{
					throw new RuntimeException("Vous avez déjà effectué un boost pour cette alerte !");
				}
				else if(content.contains("<div><label>Motif :</label>Autosignalement</div>"))
				{
					fetchFormAndCaptchaData(content);

					reason = "17";
					return "_auto_ddb";
				}
				else if(content.contains("Autosignalement déjà effectué."))
				{
					throw new RuntimeException("Autosignalement déjà effectué.");
				}
				else if(content.contains("Signalement déjà fait, apportez votre voix"))
				{
					fetchFormAndCaptchaData(content);

					ddbAlreadySent = true;
					Matcher m = PatternCollection.fetchAlreadySentDdbReason.matcher(content);
					if(m.find())
					{
						reason = m.group(1);
						return "_boost_ddb";
					}
					else
					{
						throw new RuntimeException("boost: can't find already sent ddb reason");
					}
				}
				else
				{
					reasonNameList.clear();
					reasonIdList.clear();
					Matcher m = PatternCollection.fetchNextDdbReason.matcher(content);
					while(m.find())
					{
						reasonNameList.add(m.group(2));
						reasonIdList.add(m.group(1));
					}

					if(reasonNameList.size() == 0)
						throw new RuntimeException("ddb reason list empty");

					fetchFormAndCaptchaData(content);

					return null;
				}
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
				return e.getMessage();
			}
		}

		protected void onPostExecute(String result)
		{
			if(result == null)
			{
				textView.setText(R.string.chooseDdbReason);
				radioGroup.setVisibility(View.VISIBLE);
				remarksTextView.setVisibility(View.VISIBLE);
				editText.setVisibility(View.VISIBLE);
				button.setVisibility(View.VISIBLE);

				for(int i = 0; i < reasonNameList.size(); i++)
				{
					RadioButton rb = new RadioButton(NewDdbActivity.this);
					rb.setTextAppearance(NewDdbActivity.this, android.R.attr.textAppearanceMedium);
					rb.setText(reasonNameList.get(i));
					rb.setTextColor(textView.getTextColors());
					rb.setOnClickListener(new RadioButtonListener(reasonIdList.get(i)));
					radioGroup.addView(rb);
				}

				reason = null;
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(NewDdbActivity.this);
			}
			else if(result.equals("_auto_ddb"))
			{
				textView.setText("Autosignalement");
				remarksTextView.setVisibility(View.VISIBLE);
				editText.setVisibility(View.VISIBLE);
				button.setText("Demander la suppression du post");
				button.setVisibility(View.VISIBLE);
			}
			else if(result.equals("_boost_ddb"))
			{
				textView.setText("Signalement déjà fait, apportez votre voix");
				button.setText("J'approuve cette DDB");
				button.setVisibility(View.VISIBLE);
			}
			else
			{
				textView.setText(result);
			}
		}
	}

	private void fetchFormAndCaptchaData(String content) throws RuntimeException
	{
		Matcher m = PatternCollection.extractDdbFormData.matcher(content);
		if(!m.find())
			throw new RuntimeException("can't find ddb form data");
		formDataPage = m.group(1);
		formDataK = m.group(2);
		formDataT = m.group(3);
		formDataSession = m.group(4);
		formDataKey = m.group(5);

		m = PatternCollection.extractDdbCaptchaUrl.matcher(content);
		if(!m.find())
			throw new RuntimeException("can't find ddb captcha url");
		captchaUrl = m.group(1);
		captchaCode = null;
	}

	private class SendDdbTask extends AsyncTask<Void, Void, String>
	{
		protected String doInBackground(Void... voids)
		{
			try
			{
				MainApplication appContext = (MainApplication) getApplication();
				HttpClient client = appContext.getHttpClient(MainApplication.JVC_SESSION);
				HttpPost httpPost = new HttpPost(url);

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("forum", String.valueOf(post.getTopic().getForum().getForumId())));
				nameValuePairs.add(new BasicNameValuePair("numero", String.valueOf(post.getPostId())));
				nameValuePairs.add(new BasicNameValuePair("hash_mdr", "0"));
				nameValuePairs.add(new BasicNameValuePair("topic", String.valueOf(post.getTopic().getTopicId())));
				nameValuePairs.add(new BasicNameValuePair("mode", modeValue));
				nameValuePairs.add(new BasicNameValuePair("page", formDataPage));
				nameValuePairs.add(new BasicNameValuePair("k", formDataK));
				nameValuePairs.add(new BasicNameValuePair("t", formDataT));
				nameValuePairs.add(new BasicNameValuePair("session", formDataSession));
				nameValuePairs.add(new BasicNameValuePair("key", formDataKey));
				nameValuePairs.add(new BasicNameValuePair("motif", reason));
				nameValuePairs.add(new BasicNameValuePair("explication", explication));
				nameValuePairs.add(new BasicNameValuePair("code", captchaCode));
				nameValuePairs.add(new BasicNameValuePair("submit", ddbAlreadySent ? "submit" : "Valider"));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(client.execute(httpPost).getEntity()));

				Matcher m = PatternCollection.fetchDdbMessage.matcher(content);
				if(!m.find())
					throw new RuntimeException("no ddb message\n" + content);

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
			catch(Exception e)
			{
				return getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			button.setEnabled(false);
			button.setText(getString(R.string.genericLoading));
		}

		protected void onPostExecute(String result)
		{
			if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(NewDdbActivity.this);
			}
			else
			{
				textView.setText(Html.fromHtml(result));
				remarksTextView.setVisibility(View.GONE);
				radioGroup.setVisibility(View.GONE);
				editText.setVisibility(View.GONE);
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
