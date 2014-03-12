package com.forum.jvcreader.graphics;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.forum.jvcreader.JvcActivity;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.*;
import com.forum.jvcreader.utils.AsyncTaskManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TopicReplyLayoutHandler
{
	public static final int SET_BROWSING_TO_AUTOMATIC = 0;
	public static final int CANCEL_REPLY = 1;
	public static final int REFRESH_TOPIC = 2;
	public static final int SCROLL_DOWN_END = 3;
	public static final int REFRESH_PM_TOPIC = 4;
	public static final int PREVIEW_POST_ANIMATE = 5;
	public static final int PREVIEW_POST_CLOSE = 6;

	public static final int FORUM_TOPIC = 0;
	public static final int PM_TOPIC = 1;

	private Activity activity;
	SmileyGridDialog smileyDialog;

	private Handler handler;
	private FrameLayout replyLayout;
	private JvcTopic topic;
	private JvcPmTopic pmTopic;
	private int topicType;

	private View replyView;
	private TextView separatorTextView;
	private LinearLayout replyFormLayout;
	private EditText replyEditText;
	private CheckBox postAsMobileCheckBox;
	private CheckBox includeSignatureCheckBox;
	private Button replySmileysButton;
	private Button replyPreviewButton;
	private Button replySendButton;

	private Button refreshButton;
	private Button replyButton;

	private boolean isReplying;
	private boolean isLocked;

	private boolean postAsMobileByDefault;
	private boolean useSignature;
	private boolean includeSignatureByDefault;
	private boolean showCharacterCounter;

	public TopicReplyLayoutHandler(Activity activity, Handler handler, FrameLayout replyLayout, JvcTopic topic)
	{
		this.topic = topic;
		topicType = FORUM_TOPIC;
		instantiate(activity, handler, replyLayout);
	}

	public TopicReplyLayoutHandler(Activity activity, Handler handler, FrameLayout replyLayout, JvcPmTopic topic)
	{
		pmTopic = topic;
		topicType = PM_TOPIC;
		instantiate(activity, handler, replyLayout);
	}

	private void instantiate(Activity activity, Handler handler, FrameLayout replyLayout)
	{
		LayoutInflater inflater = activity.getLayoutInflater();
		replyView = inflater.inflate(R.layout.topic_reply_view, null);
		replyLayout.addView(replyView);
		replyLayout.setVisibility(View.GONE);

		postAsMobileByDefault = JvcUserData.getBoolean(JvcUserData.PREF_POST_AS_MOBILE_BY_DEFAULT, JvcUserData.DEFAULT_POST_AS_MOBILE_BY_DEFAULT);
		useSignature = JvcUserData.getBoolean(JvcUserData.PREF_USE_SIGNATURE, JvcUserData.DEFAULT_USE_SIGNATURE);
		includeSignatureByDefault = JvcUserData.getBoolean(JvcUserData.PREF_INCLUDE_SIGNATURE_BY_DEFAULT, JvcUserData.DEFAULT_INCLUDE_SIGNATURE_BY_DEFAULT);
		showCharacterCounter = JvcUserData.getBoolean(JvcUserData.PREF_SHOW_CHARACTER_COUNTER, JvcUserData.DEFAULT_SHOW_CHARACTER_COUNTER);

		replyFormLayout = (LinearLayout) replyView.findViewById(R.id.topicReplyFormLayout);
		replyFormLayout.setVisibility(View.GONE);
		separatorTextView = (TextView) replyView.findViewById(R.id.topicReplyFormSeparatorTextView);
		replyEditText = (EditText) replyView.findViewById(R.id.topicReplyFormEditText);
		if(showCharacterCounter)
		{
			replyEditText.addTextChangedListener(characterCounterWatcher);
		}
		postAsMobileCheckBox = (CheckBox) replyView.findViewById(R.id.topicReplyFormPostAsMobileCheckBox);
		if((topicType == FORUM_TOPIC && topic.getForum().isForumJV()) || topicType == PM_TOPIC)
			postAsMobileCheckBox.setEnabled(false);
		includeSignatureCheckBox = (CheckBox) replyView.findViewById(R.id.topicReplyFormIncludeSignatureCheckBox);
		if(!useSignature)
			includeSignatureCheckBox.setVisibility(View.GONE);
		replySmileysButton = (Button) replyView.findViewById(R.id.topicReplyFormSmileysButton);
		replySmileysButton.setOnClickListener(replySmileysButtonListener);
		replyPreviewButton = (Button) replyView.findViewById(R.id.topicReplyFormPreviewButton);
		replyPreviewButton.setOnClickListener(replyPreviewButtonListener);
		replySendButton = (Button) replyView.findViewById(R.id.topicReplyFormSendButton);
		replySendButton.setOnClickListener(replySendButtonListener);

		refreshButton = (Button) replyView.findViewById(R.id.topicRefreshButton);
		if(topicType == FORUM_TOPIC)
		{
			refreshButton.setOnClickListener(refreshButtonListener);
		}
		else
		{
			refreshButton.setVisibility(View.GONE);
		}
		replyButton = (Button) replyView.findViewById(R.id.topicReplyButton);
		replyButton.setOnClickListener(replyButtonListener);

		this.activity = activity;
		this.handler = handler;
		this.replyLayout = replyLayout;
	}

	public void saveState(Bundle bundle)
	{
		bundle.putBoolean("replyHandlerIsLocked", isLocked);
		bundle.putBoolean("replyHandlerIsReplying", isReplying);
		bundle.putString("replyHandlerEditText", replyEditText.getText().toString());
		bundle.putBoolean("replyHandlerPostAsMobileCheckBox", postAsMobileCheckBox.isChecked());
		bundle.putBoolean("replyHandlerIncludeSignatureCheckBox", includeSignatureCheckBox.isChecked());
	}

	public void restoreState(Bundle bundle)
	{
		isLocked = bundle.getBoolean("replyHandlerIsLocked");
		isReplying = bundle.getBoolean("replyHandlerIsReplying");
		replyEditText.setText(bundle.getString("replyHandlerEditText"));
		postAsMobileCheckBox.setChecked(bundle.getBoolean("replyHandlerPostAsMobileCheckBox"));
		includeSignatureCheckBox.setChecked(bundle.getBoolean("replyHandlerIncludeSignatureCheckBox"));

		if(isLocked)
		{
			replyFormLayout.setVisibility(View.GONE);
			replyButton.setEnabled(false);
			replyButton.setText(getLockedTopicText());
		}
		else
		{
			if(isReplying)
			{
				replyFormLayout.setVisibility(View.VISIBLE);
				replyEditText.requestFocus();
				replyButton.setText(R.string.genericCancel);
			}
			else
			{
				replyFormLayout.setVisibility(View.GONE);
				replyButton.setText(R.string.genericAnswer);
			}
		}
	}

	private String getLockedTopicText()
	{
		if(topicType == PM_TOPIC)
			return activity.getString(R.string.pmTopicClosed);
		else if(topicType == FORUM_TOPIC)
			return activity.getString(R.string.topicLocked);
		else
			return "(error)";
	}

	private final TextWatcher characterCounterWatcher = new TextWatcher()
	{
		@Override
		public void afterTextChanged(Editable s)
		{
			final int count = s.length();
			String str;
			if(count > 1)
				str = String.format(activity.getString(R.string.characterCounterX), count);
			else
				str = String.format(activity.getString(R.string.characterCounterSingular), count);
			separatorTextView.setText(activity.getString(R.string.answerSeparator) + " (" + str + ")");
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}
	};

	private final OnClickListener refreshButtonListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			handler.sendEmptyMessage(REFRESH_TOPIC);
		}
	};

	private final OnClickListener replyButtonListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			if(!isLocked)
			{
				if(!isReplying)
				{
					replyEditText.setText("");
					postAsMobileCheckBox.setChecked(postAsMobileByDefault);
					includeSignatureCheckBox.setChecked(includeSignatureByDefault);
					replyFormLayout.setVisibility(View.VISIBLE);
					replyEditText.requestFocus();
					replyButton.setText(R.string.genericCancel);
					isReplying = true;

					if(topicType == FORUM_TOPIC)
						handler.sendEmptyMessage(SET_BROWSING_TO_AUTOMATIC);
					else if(topicType == PM_TOPIC)
						handler.sendEmptyMessage(SCROLL_DOWN_END);
				}
				else
				{
					if(replyEditText.length() > 0)
					{
						NoticeDialog.showYesNo(activity, activity.getString(R.string.noticeDialogCancelPost), new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialog, int which)
									{

										replyFormLayout.setVisibility(View.GONE);
										replyButton.setText(R.string.genericAnswer);
										isReplying = false;

										if(topicType == FORUM_TOPIC)
											handler.sendEmptyMessage(CANCEL_REPLY);

										dialog.dismiss();
									}
								}, new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialog, int which)
									{
										dialog.dismiss();
									}
								}
						);
					}
					else
					{
						replyFormLayout.setVisibility(View.GONE);
						replyButton.setText(R.string.genericAnswer);
						isReplying = false;

						if(topicType == FORUM_TOPIC)
							handler.sendEmptyMessage(CANCEL_REPLY);
					}
				}
			}
		}
	};

	private final OnClickListener replySmileysButtonListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			if(smileyDialog != null)
				return;

			smileyDialog = new SmileyGridDialog(activity, new OnClickListener()
			{
				public void onClick(View view)
				{
					final int pos = replyEditText.getSelectionStart();
					String append = "";

					if(replyEditText.length() > 0 && pos > 0)
					{
						char lastChar = replyEditText.getText().charAt(pos - 1);
						if(lastChar != '\n' && lastChar != ' ')
							append += ' ';
					}

					append += smileyDialog.getSelectedSmiley();

					if(pos < replyEditText.length())
					{
						char nextChar = replyEditText.getText().charAt(pos);
						if(nextChar != '\n' && nextChar != ' ')
							append += ' ';
					}

					replyEditText.getText().replace(pos, pos, append);

					smileyDialog.dismiss();
					smileyDialog = null;
				}
			});

			smileyDialog.getDialog().setOnDismissListener(new OnDismissListener()
			{

				@Override
				public void onDismiss(DialogInterface dialogInterface)
				{
					smileyDialog = null;
				}
			});

			smileyDialog.show();
		}
	};

	private final OnClickListener replyPreviewButtonListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			if(replyEditText.length() > 0)
			{
				MainApplication appContext = (MainApplication) activity.getApplicationContext();
				PostItem item = new PostItem(activity, true, JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS), JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS));
				String date = new SimpleDateFormat("d MMMM y \u00E0 HH:mm:ss", Locale.FRANCE).format(new Date());
				JvcPost post = new JvcPost(null, 0, getPostTextWithSignature() + "\n", appContext.getJvcPseudo(), false, postAsMobileCheckBox.isChecked(), 1, date, null, null);

				item.updateDataFromPost(post, true);
				Dialog previewDialog = new Dialog(activity, R.style.FullScreenNoTitleDialogTheme);
				previewDialog.setContentView(item.getView());
				previewDialog.setCancelable(true);
				previewDialog.setOnDismissListener(new OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						handler.sendEmptyMessage(PREVIEW_POST_CLOSE);
					}
				});
				previewDialog.show();
				handler.sendMessage(Message.obtain(handler, PREVIEW_POST_ANIMATE, item.getView()));
			}
			else
			{
				Toast.makeText(activity, R.string.postEmpty, Toast.LENGTH_LONG).show();
			}
		}
	};

	private final OnClickListener replySendButtonListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			if(replyEditText.length() > 0)
			{
				boolean postAsMobile = false;
				if(topicType == FORUM_TOPIC)
					postAsMobile = postAsMobileCheckBox.isChecked() && !topic.getForum().isForumJV();
				JvcPost post = new JvcPost(topic, getPostTextWithSignature(), postAsMobile);
				JvcSendPostTask task = new JvcSendPostTask();
				AsyncTaskManager.addTask(activity, task);
				task.execute(post);
			}
			else
			{
				Toast.makeText(activity, R.string.postEmpty, Toast.LENGTH_LONG).show();
			}
		}
	};

	public void quoteText(String text)
	{
		if(isReplying)
		{
			replyEditText.append(text);
		}
		else
		{
			replyEditText.requestFocus();
			replyEditText.setText(text);
			postAsMobileCheckBox.setChecked(postAsMobileByDefault);
			includeSignatureCheckBox.setChecked(includeSignatureByDefault);
			replyFormLayout.setVisibility(View.VISIBLE);
			replyButton.setText(R.string.genericCancel);
			isReplying = true;

			if(topicType == FORUM_TOPIC)
				handler.sendEmptyMessage(SET_BROWSING_TO_AUTOMATIC);
			else if(topicType == PM_TOPIC)
				handler.sendEmptyMessage(SCROLL_DOWN_END);
		}

		replyEditText.post(new Runnable()
		{
			public void run()
			{
				replyEditText.setSelection(replyEditText.length());
			}
		});
	}

	private String getPostTextWithSignature()
	{
		if(useSignature && includeSignatureCheckBox.isChecked())
		{
			String s;

			if(JvcUserData.getBoolean(JvcUserData.PREF_SIGNATURE_AT_START, JvcUserData.DEFAULT_SIGNATURE_AT_START))
			{
				s = JvcUserData.getString(JvcUserData.PREF_SIGNATURE, JvcUserData.DEFAULT_SIGNATURE) + "\n\n";
				s += replyEditText.getText().toString();
			}
			else
			{
				s = replyEditText.getText().toString() + "\n\n";
				s += JvcUserData.getString(JvcUserData.PREF_SIGNATURE, JvcUserData.DEFAULT_SIGNATURE);
			}

			return s;
		}
		else
		{
			return replyEditText.getText().toString();
		}
	}

	public void setRefreshButtonState(boolean state)
	{
		if(state)
		{
			refreshButton.setText(R.string.genericRefresh);
			refreshButton.setEnabled(true);
		}
		else
		{
			refreshButton.setText(R.string.genericLoading);
			refreshButton.setEnabled(false);
		}
	}

	public void updateLockState(boolean isLocked)
	{
		if(isLocked && !this.isLocked)
		{
			this.isLocked = true;

			replyFormLayout.setVisibility(View.GONE);
			replyButton.setEnabled(false);
			replyButton.setText(getLockedTopicText());
		}
		else if(!isLocked && this.isLocked)
		{
			this.isLocked = false;

			replyButton.setEnabled(true);
			replyButton.setText(R.string.genericAnswer);
		}

		replyLayout.setVisibility(View.VISIBLE);
	}

	public void setReplyingEnabled(boolean enabled)
	{
		if(!isLocked)
			replyButton.setEnabled(enabled);
	}

	public boolean isReplying()
	{
		return isReplying;
	}

	private class JvcSendPostTask extends AsyncTask<JvcPost, Void, Integer>
	{
		private ProgressDialog dialog;
		private CaptchaDialog captcha;

		private JvcPost post;
		private String exceptionResult;

		private String customMessage = activity.getString(R.string.publishingPost);

		public void setCustomMessage(String message)
		{
			customMessage = message;
		}

		protected Integer doInBackground(JvcPost... posts)
		{
			try
			{
				post = posts[0];
				if(topicType == FORUM_TOPIC)
					return post.publishOnTopic();
				else if(topicType == PM_TOPIC)
					return post.publishOnPmTopic(pmTopic);
				else
				{
					exceptionResult = "unknown topic type";
					return -1;
				}
			}

			catch(UnknownHostException e)
			{
				exceptionResult = JvcUtils.HTTP_TIMEOUT_RESULT;
				return -1;
			}
			catch(HttpHostConnectException e)
			{
				exceptionResult = JvcUtils.HTTP_TIMEOUT_RESULT;
				return -1;
			}
			catch(ConnectTimeoutException e)
			{
				exceptionResult = JvcUtils.HTTP_TIMEOUT_RESULT;
				return -1;
			}
			catch(SocketTimeoutException e)
			{
				exceptionResult = JvcUtils.HTTP_TIMEOUT_RESULT;
				return -1;
			}
			catch(IOException e)
			{
				exceptionResult = e.toString();
				return -1;
			}
			catch(SAXException e)
			{
				exceptionResult = e.toString();
				return -1;
			}
		}

		protected void onPreExecute()
		{
			replyButton.setEnabled(false);
			dialog = ProgressDialog.show(activity, activity.getString(R.string.genericSending), customMessage);
			dialog.setCancelable(true);
			dialog.setOnDismissListener(new OnDismissListener()
			{
				@Override
				public void onDismiss(DialogInterface dialog)
				{
					replyButton.setEnabled(true);
					cancel(true);
				}
			});
		}

		protected void onPostExecute(Integer result)
		{
			dialog.dismiss();
			dialog = null;

			switch(result)
			{
				case -1:
					if(exceptionResult.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
					{
						MainApplication.handleHttpTimeout(activity);
					}
					else
					{
						NoticeDialog.show(activity, exceptionResult);
					}

					replyButton.setEnabled(true);
					break;

				case JvcPost.REQUEST_OK:
					JvcActivity.hideKeyboard(activity);
					if(topicType == PM_TOPIC)
						handler.sendMessage(Message.obtain(handler, REFRESH_PM_TOPIC, post));
					else if(topicType == FORUM_TOPIC)
						handler.sendEmptyMessage(REFRESH_TOPIC);
					replyFormLayout.setVisibility(View.GONE);
					replyEditText.setText("");
					postAsMobileCheckBox.setChecked(false);
					replyButton.setText(R.string.genericAnswer);
					isReplying = false;
					replyButton.setEnabled(true);
					break;

				case JvcPost.REQUEST_CAPTCHA_REQUIRED:
					captcha = new CaptchaDialog(activity, post.getRequestCaptchaUrl());
					captcha.setDialogTitle(post.getRequestError());
					captcha.setSubmitButtonListener(new OnClickListener()
					{
						public void onClick(View view)
						{
							post.prepareCaptcha(captcha.getCaptchaString());
							captcha.dismiss();
							captcha = null;
							JvcSendPostTask task = new JvcSendPostTask();
							AsyncTaskManager.addTask(activity, task);
							task.execute(post);
						}
					});
					captcha.show();
					break;

				case JvcPost.REQUEST_RETRY:
					JvcSendPostTask task = new JvcSendPostTask();
					AsyncTaskManager.addTask(activity, task);
					task.setCustomMessage(activity.getString(R.string.formExpiryRetry));
					task.execute(post);
					break;

				case JvcPost.REQUEST_ERROR:
				case JvcPost.REQUEST_ERROR_FROM_JVC:
					NoticeDialog.show(activity, post.getRequestError());
					replyButton.setEnabled(true);
					break;
			}
		}
	}
}
