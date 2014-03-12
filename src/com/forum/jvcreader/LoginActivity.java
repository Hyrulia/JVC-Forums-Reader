package com.forum.jvcreader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.forum.jvcreader.graphics.CaptchaDialog;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcAccount;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.PatternCollection;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class LoginActivity extends JvcActivity
{
	private static final int MY_ACCOUNTS_REQUEST_CODE = 1;

	private CheckBox checkBoxRememberLogin;
	private CheckBox checkBoxRememberPassword;
	private CheckBox checkBoxAutoConnect;
	private EditText editTextLogin;
	private EditText editTextPassword;

	private AsyncTask<String, String, String> loginTask;

	private JSONObject lastJvcJson;
	private String currentLogin;
	private String currentPassword;

	private ProgressDialog connectDialog;
	private CaptchaDialog captchaDialog;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		MainApplication app = (MainApplication) getApplication();

		Boolean b = (Boolean) GlobalData.getOnce("exitMainActivity");
		if(b != null && b)
		{
			finish();
			return;
		}

		WebSettings sfrFixWebSettings = new WebView(this).getSettings();
		if(sfrFixWebSettings != null && app.getUserAgent() == null)
		{
			String defUserAgent = sfrFixWebSettings.getUserAgentString();
			app.setUserAgent(defUserAgent);
			app.invalidateHttpClients();
		}

		boolean sessionValid = ((MainApplication) getApplicationContext()).isJvcSessionValid();
		boolean justDisconnected = getIntent().getBooleanExtra("com.forum.jvcreader.Disconnected", false);

        /* UI Controls */
		checkBoxRememberLogin = (CheckBox) findViewById(R.id.rememberLoginCheckBox);
		checkBoxRememberPassword = (CheckBox) findViewById(R.id.rememberPasswordCheckBox);
		checkBoxAutoConnect = (CheckBox) findViewById(R.id.autoConnectCheckBox);
		editTextLogin = (EditText) findViewById(R.id.loginEdit);
		editTextPassword = (EditText) findViewById(R.id.passwordEdit);

        /* Fetch login preferences */
		String lastLogin = JvcUserData.getString(JvcUserData.PREF_LOGIN, "");
		String lastPassword = JvcUserData.getSecureString(JvcUserData.PREF_PASSWORD, "");
		boolean rememberLogin = JvcUserData.getBoolean(JvcUserData.PREF_REMEMBER_LOGIN, false);
		boolean rememberPassword = JvcUserData.getBoolean(JvcUserData.PREF_REMEMBER_PASSWORD, false);
		boolean autoConnect = JvcUserData.getBoolean(JvcUserData.PREF_AUTO_CONNECT, false);

		checkBoxRememberLogin.setChecked(rememberLogin);
		if(rememberLogin)
		{
			editTextLogin.setText(lastLogin);
		}

		checkBoxRememberPassword.setChecked(rememberPassword);
		if(rememberPassword)
		{
			editTextPassword.setText(lastPassword);
		}

		checkBoxAutoConnect.setChecked(autoConnect);
		if(autoConnect)
		{
			checkBoxRememberLogin.setEnabled(false);
			checkBoxRememberPassword.setEnabled(false);
			currentLogin = lastLogin;
			currentPassword = lastPassword;

			if(!sessionValid && !justDisconnected)
			{
				loginTask = new JvcLoginTask();
				registerTask(loginTask);
				loginTask.execute(lastLogin, lastPassword, "no");
			}
		}

		if(sessionValid && !justDisconnected)
		{
			startMainActivity();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) /* Disable home button here ! */
	{
		return true;
	}

	public void checkBoxRememberLoginClick(View view)
	{
		JvcUserData.startEditing();
		JvcUserData.setBoolean(JvcUserData.PREF_REMEMBER_LOGIN, checkBoxRememberLogin.isChecked());
		if(!checkBoxRememberLogin.isChecked())
		{
			JvcUserData.setString(JvcUserData.PREF_LOGIN, "");
		}
		JvcUserData.stopEditing();
	}

	public void checkBoxRememberPasswordClick(View view)
	{
		JvcUserData.startEditing();
		JvcUserData.setBoolean(JvcUserData.PREF_REMEMBER_PASSWORD, checkBoxRememberPassword.isChecked());
		if(!checkBoxRememberPassword.isChecked())
		{
			JvcUserData.setSecureString(JvcUserData.PREF_PASSWORD, "");
		}
		JvcUserData.stopEditing();
	}

	public void checkBoxAutoConnectClick(View view)
	{
		JvcUserData.startEditing();
		if(checkBoxAutoConnect.isChecked())
		{
			checkBoxRememberLogin.setEnabled(false);
			checkBoxRememberLogin.setChecked(true);
			checkBoxRememberPassword.setEnabled(false);
			checkBoxRememberPassword.setChecked(true);

			JvcUserData.setBoolean(JvcUserData.PREF_REMEMBER_LOGIN, true);
			JvcUserData.setBoolean(JvcUserData.PREF_REMEMBER_PASSWORD, true);
			JvcUserData.setBoolean(JvcUserData.PREF_AUTO_CONNECT, true);
		}
		else
		{
			checkBoxRememberLogin.setEnabled(true);
			checkBoxRememberPassword.setEnabled(true);

			JvcUserData.setBoolean(JvcUserData.PREF_AUTO_CONNECT, false);
		}
		JvcUserData.stopEditing();
	}

	public void buttonConnectClick(View view)
	{
		String login = editTextLogin.getText().toString(), password = editTextPassword.getText().toString();
		if(isLoginOrPasswordBad(login, password))
		{
			Toast.makeText(this, R.string.incorrectLoginOrPasswordFormatting, Toast.LENGTH_LONG).show();
		}
		else
		{
			JvcUserData.startEditing();
			JvcUserData.setString(JvcUserData.PREF_LOGIN, editTextLogin.getText().toString());
			if(checkBoxRememberPassword.isChecked())
			{
				JvcUserData.setSecureString(JvcUserData.PREF_PASSWORD, editTextPassword.getText().toString());
			}
			JvcUserData.stopEditing();

			currentLogin = login;
			currentPassword = password;
			loginTask = new JvcLoginTask();
			registerTask(loginTask);
			loginTask.execute(login, password, "no");
		}
	}

	private void startMainActivity()
	{
		startActivity(new Intent(this, MainActivity.class));
		finish();
	}

	public void myAccountsButtonClick(View view)
	{
		startActivityForResult(new Intent(this, MyAccountsActivity.class), 1);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == MY_ACCOUNTS_REQUEST_CODE)
		{
			if(resultCode == RESULT_OK)
			{
				String key = data.getStringExtra("com.forum.jvcreader.ChosenAccountKey");
				JvcAccount account = (JvcAccount) GlobalData.getOnce(key);
				if(account != null)
				{
					String pseudo = account.getPseudo(), password = account.getPassword();
					editTextLogin.setText(pseudo);
					editTextPassword.setText(password);

					JvcUserData.startEditing();
					JvcUserData.setString(JvcUserData.PREF_LOGIN, pseudo);
					if(checkBoxRememberPassword.isChecked())
					{
						JvcUserData.setSecureString(JvcUserData.PREF_PASSWORD, password);
					}
					JvcUserData.stopEditing();

					currentLogin = pseudo;
					currentPassword = password;
					loginTask = new JvcLoginTask();
					registerTask(loginTask);
					loginTask.execute(pseudo, password, "no");
				}
			}
		}
	}

	public class JvcLoginTask extends AsyncTask<String, String, String>
	{
		protected String doInBackground(String... loginData)
		{
			try
			{
				MainApplication context = (MainApplication) getApplicationContext();
				context.invalidateHttpClients();
				DefaultHttpClient client = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost("http://www.jeuxvideo.com/profil/ajax_connect.php");

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("pseudo", loginData[0]));
				nameValuePairs.add(new BasicNameValuePair("pass", loginData[1]));
				nameValuePairs.add(new BasicNameValuePair("retenir", "1"));
				if(loginData[2].equals("yes")) /* Login while validating a captcha */
				{
					nameValuePairs.add(new BasicNameValuePair("code", loginData[3]));
					nameValuePairs.add(new BasicNameValuePair("tk", loginData[4]));
					nameValuePairs.add(new BasicNameValuePair("session", loginData[5]));
				}
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				String content = MainApplication.getEntityContent(client.execute(httpPost).getEntity());

				try
				{
					lastJvcJson = new JSONObject(content);
				}
				catch(JSONException e)
				{
					return getString(R.string.errorWhileConnecting) + " : '" + content + "' => '" + loginData[0] + "'";
				}
				if(!lastJvcJson.isNull("err"))
					return getString(R.string.errorFromJvc) + " :\n" + lastJvcJson.getString("err");

				int operationId = lastJvcJson.getInt("operation");
				switch(operationId)
				{
					case 0: 	/* Login is successful */
						context.setJvcCookieStore(client.getCookieStore()); /* Set the session cookies */
						String cgiUrl = lastJvcJson.getString("cgi");
						Matcher m = PatternCollection.extractLoginCgiUrl.matcher(cgiUrl);
						if(m.find())
						{
							GlobalData.set("pseudo", m.group(1));
							GlobalData.set("tk", m.group(2));
							GlobalData.set("key", m.group(3));
						}
						GlobalData.set("pass", loginData[1]);

						context.setJvcPseudo(loginData[0]);
						return null;
					case 101:	/* Login failed, it's possible to retry without captcha */
						return getString(R.string.errorBadLoginOrPassword);
					case 100:	/* Login failed, you must process the captcha */
						return "_captcha";
					default:
						return getString(R.string.errorUnknownJVCOperationId);
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
				return getString(R.string.errorWhileConnecting) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			connectDialog = ProgressDialog.show(LoginActivity.this, getString(R.string.connectingMessage), getString(R.string.connectingAt) + " jeuxvideo.com...", true);
		}

		protected void onProgressUpdate(String... progress)
		{
			connectDialog.setMessage(progress[0]);
		}

		protected void onPostExecute(String result)
		{
			if(connectDialog == null)
				return;
			connectDialog.dismiss();
			connectDialog = null;

			if(result == null)
			{
				startMainActivity();
			}
			else if(result.equals("_captcha"))
			{
				try
				{
					String captchaUrl = getUrlFromJvcCaptcha(lastJvcJson.getString("code_captsha")); /* captsha ? */

					captchaDialog = new CaptchaDialog(LoginActivity.this, captchaUrl);
					captchaDialog.setSubmitButtonListener(new OnClickListener()
					{
						public void onClick(View view)
						{
							try
							{
								String code = captchaDialog.getCaptchaString();
								loginTask = new JvcLoginTask();
								registerTask(loginTask);
								loginTask.execute(currentLogin, currentPassword, "yes", code, lastJvcJson.getString("tk").replace("\"", ""), lastJvcJson.getString("session"));
								captchaDialog.dismiss();
								captchaDialog = null;
							}
							catch(Exception e)
							{
								captchaDialog.showErrorFrame(true, getString(R.string.errorWithCaptchaData) + " : " + e.toString());
							}
						}
					});
					captchaDialog.show();
				}
				catch(Exception e)
				{
					NoticeDialog.show(LoginActivity.this, getString(R.string.errorWithCaptchaData) + " : " + e.toString());
				}
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(LoginActivity.this);
			}
			else
			{
				NoticeDialog.show(LoginActivity.this, result);
			}
		}
	}

    /* Utility functions */

	public boolean isLoginOrPasswordBad(String login, String password)
	{
		return !JvcUtils.isPseudoCorrect(login) || !JvcUtils.isPasswordCorrect(password);
	}

	public String getUrlFromJvcCaptcha(String code)
	{
		Matcher m = PatternCollection.extractLoginCaptchaUrl.matcher(code);

		if(!m.find())
			return "";
		return m.group(1);
	}
}