package com.forum.jvcreader;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;
import com.forum.jvcreader.graphics.*;
import com.forum.jvcreader.graphics.PseudoPromptDialog.OnPseudoChosenListener;
import com.forum.jvcreader.jvc.*;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.widgets.CancelableLabel;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;

public class PmNewActivity extends JvcActivity
{
	private MainApplication app;
	private PseudoPromptDialog pseudoDialog;
	private boolean showNoelshackThumbnails;
	private boolean animateSmileys;

	private LinkedHashMap<String, Boolean> recipientMap;
	private LinearLayout recipientListLayout;

	private EditText subjectEditText;
	private EditText contentEditText;
	private CheckBox includeSignatureCheckBox;

	private String currentContextMenuPseudo;
	private SmileyGridDialog smileyDialog;

	private boolean savedState = false;
	private Bundle savedBundle = null;

	@SuppressWarnings("unchecked")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pm_new);
		app = (MainApplication) getApplicationContext();
		Intent appIntent = getIntent();
		String recipientPseudo = appIntent.getStringExtra("com.forum.jvcreader.RecipientPseudo");

		if(savedInstanceState != null)
		{
			savedState = savedInstanceState.getBoolean("savedInstanceState");
			savedBundle = savedInstanceState;
		}

        /* Assign UI Elements */
		TextView tv = (TextView) findViewById(R.id.pmNewSenderTextView);
		tv.setText(JvcTextSpanner.getColoredSpannableText(String.format(getString(R.string.pmNewSender), app.getJvcPseudo()), getResources().getColor(R.color.jvcAdminPseudo)));
		recipientListLayout = (LinearLayout) findViewById(R.id.pmNewRecipientListLayout);
		recipientListLayout.setVisibility(View.GONE);
		subjectEditText = (EditText) findViewById(R.id.pmNewSubjectEditText);
		contentEditText = (EditText) findViewById(R.id.pmNewContentEditText);
		includeSignatureCheckBox = (CheckBox) findViewById(R.id.pmNewIncludeSignatureCheckBox);
		boolean useSignature = JvcUserData.getBoolean(JvcUserData.PREF_USE_SIGNATURE, JvcUserData.DEFAULT_USE_SIGNATURE);
		if(!useSignature)
		{
			includeSignatureCheckBox.setChecked(false);
			includeSignatureCheckBox.setVisibility(View.GONE);
		}
		showNoelshackThumbnails = JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS);
		animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);

        /* Initialize data */
		if(appIntent.getAction() != null && appIntent.getAction().equals(Intent.ACTION_SEND))
		{
			if(!app.isJvcSessionValid())
			{
				startActivity(new Intent(PmNewActivity.this, LoginActivity.class));
				finish();
				return;
			}

			recipientMap = new LinkedHashMap<String, Boolean>();
			Bundle extras = appIntent.getExtras();

			if(extras.containsKey(Intent.EXTRA_SUBJECT))
				subjectEditText.setText(extras.getString(Intent.EXTRA_SUBJECT));
			if(extras.containsKey(Intent.EXTRA_TEXT))
				contentEditText.setText(extras.getString(Intent.EXTRA_TEXT));
		}
		else if(!savedState)
		{
			recipientMap = new LinkedHashMap<String, Boolean>();
			if(recipientPseudo != null)
			{
				recipientMap.put(recipientPseudo, false);
				addPseudoToRecipientListLayout(recipientPseudo, false);
			}
		}
		else
		{
			recipientMap = (LinkedHashMap<String, Boolean>) GlobalData.get(savedBundle.getString("recipientMapKey"));
			if(recipientMap == null)
			{
				recipientMap = new LinkedHashMap<String, Boolean>();
				if(recipientPseudo != null)
				{
					recipientMap.put(recipientPseudo, false);
					addPseudoToRecipientListLayout(recipientPseudo, false);
				}
			}
			else
			{
				for(String pseudo : recipientMap.keySet())
				{
					addPseudoToRecipientListLayout(pseudo, recipientMap.get(pseudo));
				}
			}

			subjectEditText.setText(savedBundle.getString("subjectEditText"));
			contentEditText.setText(savedBundle.getString("contentEditText"));
			includeSignatureCheckBox.setChecked(savedBundle.getBoolean("includeSignatureCheckBox"));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle bundle)
	{
		bundle.putBoolean("savedInstanceState", true);

		String recipientMapKey = System.currentTimeMillis() + "_recipient_map";
		bundle.putString("recipientMapKey", recipientMapKey);
		GlobalData.set(recipientMapKey, recipientMap);

		bundle.putString("subjectEditText", subjectEditText.getText().toString());
		bundle.putString("contentEditText", contentEditText.getText().toString());
		bundle.putBoolean("includeSignatureCheckBox", includeSignatureCheckBox.isChecked());
	}

	@Override
	public void onBackPressed()
	{
		if(subjectEditText.length() > 0 || contentEditText.length() > 0)
		{
			NoticeDialog.showYesNo(this, getString(R.string.noticeDialogCancelPost), new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							dialogInterface.dismiss();
							finish();
						}
					}, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							dialogInterface.dismiss();
						}
					}
			);
		}
		else
		{
			finish();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info)
	{
		currentContextMenuPseudo = ((CancelableLabel) view).getText().toString();
		getMenuInflater().inflate(R.menu.post_context_menu, menu);
		menu.setHeaderTitle(currentContextMenuPseudo);
		menu.removeItem(R.id.contextMenuWarnAdmin);
		menu.removeItem(R.id.contextMenuQuotePost);
		menu.removeItem(R.id.contextMenuCopyPost);
		menu.removeItem(R.id.contextMenuCopyPermanentLink);
		menu.removeItem(R.id.contextMenuDeletePost);
		menu.removeItem(R.id.contextMenuKickPseudo);
		if(JvcUserData.isPseudoInBlacklist(currentContextMenuPseudo))
		{
			menu.findItem(R.id.contextMenuIgnorePseudo).setTitle(R.string.contextMenuStopIgnorePseudo);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		if(currentContextMenuPseudo != null && currentContextMenuPseudo.length() > 0)
		{
			switch(item.getItemId())
			{
				case R.id.contextMenuShowProfile:
					GlobalData.set("cdvPseudoFromLastActivity", currentContextMenuPseudo);
					startActivity(new Intent(this, CdvActivity.class));
					break;

				case R.id.contextMenuQuoteAuthor:
					contentEditText.requestFocus();
					contentEditText.append(currentContextMenuPseudo + " :d) ");
					contentEditText.post(new Runnable()
					{
						public void run()
						{
							contentEditText.setSelection(contentEditText.length());
						}
					});
					break;

				case R.id.contextMenuIgnorePseudo:
					try
					{
						if(!JvcUserData.isPseudoInBlacklist(currentContextMenuPseudo))
						{
							JvcUserData.setPseudoInBlacklist(currentContextMenuPseudo);
						}
						else
						{
							JvcUserData.removePseudoFromBlacklist(currentContextMenuPseudo);
						}
					}
					catch(IOException e)
					{
						NoticeDialog.show(this, e.toString());
					}
					break;
			}

			return true;
		}

		return super.onContextItemSelected(item);
	}

	private void addPseudoToRecipientListLayout(String pseudo, boolean isAdmin)
	{
		final int fiveDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		if(recipientListLayout.getChildCount() > 0)
			params.leftMargin = fiveDp;
		params.gravity = Gravity.CENTER;
		CancelableLabel label = new CancelableLabel(this);
		label.setText(pseudo);
		if(isAdmin)
			label.setTextColor(getResources().getColor(R.color.jvcAdminPseudo));
		label.setLayoutParams(params);
		registerForContextMenu(label);
		label.setCancelOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				recipientMap.remove(((CancelableLabel) view).getText());
				unregisterForContextMenu(view);
				recipientListLayout.removeView(view);
				if(recipientListLayout.getChildCount() > 0)
				{
					CancelableLabel label = (CancelableLabel) recipientListLayout.getChildAt(0);
					LayoutParams params = (LayoutParams) label.getLayoutParams();
					params.leftMargin = 0;
					label.setLayoutParams(params);
				}
				else
				{
					recipientListLayout.setVisibility(View.GONE);
				}
			}
		});
		recipientListLayout.addView(label);
		recipientListLayout.setVisibility(View.VISIBLE);
	}

	public void pmNewAddRecipientButtonClick(View view)
	{
		pseudoDialog = new PseudoPromptDialog(this, new OnPseudoChosenListener()
		{
			@Override
			public void onPseudoChosen(JvcPseudo pseudo)
			{
				if(!recipientMap.containsKey(pseudo.getPseudo()))
				{
					recipientMap.put(pseudo.getPseudo(), pseudo.isAdmin());
					addPseudoToRecipientListLayout(pseudo.getPseudo(), pseudo.isAdmin());
					pseudoDialog.dismiss();
					pseudoDialog = null;
				}
			}
		}, false);
		pseudoDialog.show();
	}

	public void pmNewSmileysButtonClick(View view)
	{
		smileyDialog = new SmileyGridDialog(this, new OnClickListener()
		{
			public void onClick(View view)
			{
				final int pos = contentEditText.getSelectionStart();
				String append = "";

				if(contentEditText.length() > 0 && pos > 0)
				{
					char lastChar = contentEditText.getText().charAt(pos - 1);
					if(lastChar != '\n' && lastChar != ' ')
						append += ' ';
				}

				append += smileyDialog.getSelectedSmiley();

				if(pos < contentEditText.length())
				{
					char nextChar = contentEditText.getText().charAt(pos);
					if(nextChar != '\n' && nextChar != ' ')
						append += ' ';
				}

				contentEditText.getText().replace(pos, pos, append);

				smileyDialog.dismiss();
				smileyDialog = null;
			}
		});

		smileyDialog.show();
	}

	public void pmNewPreviewButtonClick(View view)
	{
		if(contentEditText.length() > 0)
		{
			MainApplication appContext = (MainApplication) getApplicationContext();
			PostItem item = new PostItem(this, true, showNoelshackThumbnails, animateSmileys);
			String date = new SimpleDateFormat("d MMMM y \u00E0 HH:mm:ss", Locale.FRANCE).format(new Date());
			JvcPost post = new JvcPost(null, 0, getPostTextWithSignature() + "\n", appContext.getJvcPseudo(), false, false, 1, date, null, null);

			item.updateDataFromPost(post, true);
			Dialog previewDialog = new Dialog(this, R.style.FullScreenNoTitleDialogTheme);
			previewDialog.setContentView(item.getView());
			startAnimatingDrawables(item.getView());
			previewDialog.setCancelable(true);
			previewDialog.setOnDismissListener(new OnDismissListener()
			{
				@Override
				public void onDismiss(DialogInterface dialogInterface)
				{
					stopAnimatingDrawables();
				}
			});
			previewDialog.show();
		}
		else
		{
			Toast.makeText(this, R.string.postEmpty, Toast.LENGTH_LONG).show();
		}
	}

	public void pmNewSendButtonClick(View view)
	{
		if(recipientMap.size() == 0)
		{
			Toast.makeText(this, R.string.noRecipient, Toast.LENGTH_LONG).show();
			return;
		}

		if(subjectEditText.length() > 0)
		{
			if(contentEditText.length() > 0)
			{
				JvcPmTopic topic = new JvcPmTopic(this, subjectEditText.getText().toString(), getPostTextWithSignature());
				sendPmTopic(topic);
			}
			else
			{
				Toast.makeText(this, R.string.postEmpty, Toast.LENGTH_LONG).show();
			}
		}
		else
		{
			Toast.makeText(this, R.string.subjectEmpty, Toast.LENGTH_LONG).show();
		}
	}

	private String getPostTextWithSignature()
	{
		if(includeSignatureCheckBox.isChecked())
		{
			String s;

			if(JvcUserData.getBoolean(JvcUserData.PREF_SIGNATURE_AT_START, JvcUserData.DEFAULT_SIGNATURE_AT_START))
			{
				s = JvcUserData.getString(JvcUserData.PREF_SIGNATURE, JvcUserData.DEFAULT_SIGNATURE) + "\n\n";
				s += contentEditText.getText().toString();
			}
			else
			{
				s = contentEditText.getText().toString() + "\n\n";
				s += JvcUserData.getString(JvcUserData.PREF_SIGNATURE, JvcUserData.DEFAULT_SIGNATURE);
			}

			return s;
		}
		else
		{
			return contentEditText.getText().toString();
		}
	}

	private void sendPmTopic(JvcPmTopic topic)
	{
		JvcSendPmTopicTask task = new JvcSendPmTopicTask(topic);
		registerTask(task);
		task.execute();
	}

	private class JvcSendPmTopicTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;
		private int requestResult;
		private JvcPmTopic topic;

		public JvcSendPmTopicTask(JvcPmTopic topic)
		{
			this.topic = topic;
		}

		protected String doInBackground(Void... voids)
		{
			if(isCancelled())
				return null;

			try
			{
				if(topic.getCaptchaLink() != null && topic.getCaptchaLink().length() > 0) /* Already have tmp & control from captcha warning */
				{
					requestResult = topic.send(recipientMap, topic.getRequestTmpValue(), topic.getRequestControlValue());
				}
				else
				{
					HttpClient client = app.getHttpClient(MainApplication.JVC_SESSION);
					HttpGet httpGet = new HttpGet("http://www.jeuxvideo.com/messages-prives/nouveau.php");
					HttpResponse response = client.execute(httpGet);
					if(isCancelled())
						return null;
					String content = MainApplication.getEntityContent(response.getEntity());

					Matcher m = PatternCollection.extractPmNewFormData.matcher(content);
					if(!m.find())
					{
						return getString(R.string.errorWhileLoading) + " : no fieldset in nouveau.php";
					}

					requestResult = topic.send(recipientMap, m.group(2), m.group(1));
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
				return getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			dialog = new ProgressDialog(PmNewActivity.this);
			dialog.setMessage(getString(R.string.publishingPm));
			dialog.setCancelable(true);
			dialog.setOnDismissListener(new OnDismissListener()
			{
				@Override
				public void onDismiss(DialogInterface dialog)
				{
					cancel(true);
				}
			});
			dialog.show();
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				switch(requestResult)
				{
					case JvcPmTopic.REQUEST_OK:
						JvcActivity.hideKeyboard(PmNewActivity.this);
						recipientMap.clear();
						recipientListLayout.removeAllViews();
						recipientListLayout.setVisibility(View.GONE);
						subjectEditText.setText("");
						contentEditText.setText("");
						includeSignatureCheckBox.setChecked(false);

						JvcPmTopic newTopic = new JvcPmTopic(PmNewActivity.this, topic.getRequestNewTopicId(), JvcPmList.SENT_MESSAGES, true);
						GlobalData.set("pmTopicFromPreviousActivity", newTopic);
						Intent intent = new Intent(PmNewActivity.this, PmTopicActivity.class);
						intent.putExtra("com.forum.jvcreader.RecycledTopicContent", topic.getRequestContent());
						startActivity(intent);
						break;

					case JvcPmTopic.REQUEST_REQUIRE_CAPTCHA:
						final CaptchaDialog dialog = new CaptchaDialog(PmNewActivity.this, topic.getCaptchaLink());
						dialog.setSubmitButtonListener(new OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								topic.prepareForCaptcha(dialog.getCaptchaString());
								dialog.dismiss();
								sendPmTopic(topic);
							}
						});
						dialog.setDialogTitle(topic.getRequestError());
						dialog.show();
						break;

					case JvcPmTopic.REQUEST_ERROR_FROM_JVC:
					case JvcPmTopic.REQUEST_ERROR:
						NoticeDialog.show(PmNewActivity.this, topic.getRequestError());
						break;
				}
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(PmNewActivity.this);
			}
			else
			{
				NoticeDialog.show(PmNewActivity.this, result);
			}
		}
	}
}
