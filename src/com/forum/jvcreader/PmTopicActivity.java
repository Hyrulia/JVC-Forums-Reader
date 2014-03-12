package com.forum.jvcreader;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.forum.jvcreader.graphics.*;
import com.forum.jvcreader.jvc.*;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.utils.GlobalData;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

@SuppressWarnings("deprecation")
public class PmTopicActivity extends JvcActivity
{
	private JvcPmTopic pmTopic;
	private TopicReplyLayoutHandler replyLayoutHandler;

	private ScrollView mainView;
	private LinearLayout listLayout;
	private TextView participatingPseudosTv;
	private View allMessagesSeparator;
	private TextView seeAllPreviousMessagesTv;
	private View messagesSeparator;
	private TextView seePreviousMessagesTv;
	private Button refreshButton;
	private View actionButtonSeparator;
	private Button actionButton;
	private FrameLayout replyLayout;

	private final ArrayList<PostItem> postItemList = new ArrayList<PostItem>();
	private ArrayList<JvcPost> postList;
	private JvcPost currentContextMenuPost;
	private int currentPage;

	private boolean savedState = false;
	private Bundle savedBundle = null;

	private ProgressDialog seeAllMessagesDialog = null;
	private PseudoPromptDialog pseudoPromptDialog;
	private int allMessagesCounter;
	private boolean stopLoadingAllMessages = false;

	private boolean jvcLike;
	private boolean showNoelshackThumbnails;
	private boolean animateSmileys;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pm_topic);

		if(savedInstanceState != null)
		{
			savedState = savedInstanceState.getBoolean("savedInstanceState");
			savedBundle = savedInstanceState;
		}

		String recycledContent = getIntent().getStringExtra("com.forum.jvcreader.RecycledTopicContent");
		jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_POSTS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_POSTS);
		showNoelshackThumbnails = JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS);
		animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);
		
		/* Assign UI Elements */
		mainView = (ScrollView) findViewById(R.id.pmTopicMainView);
		listLayout = (LinearLayout) findViewById(R.id.pmTopicPostListLayout);
		participatingPseudosTv = (TextView) findViewById(R.id.pmTopicParticipatingPseudosTextView);
		participatingPseudosTv.setMovementMethod(LinkMovementMethod.getInstance());
		participatingPseudosTv.setTextColor(participatingPseudosTv.getTextColors().getDefaultColor());
		allMessagesSeparator = findViewById(R.id.pmTopicSeeAllMessagesSeparator);
		seeAllPreviousMessagesTv = (TextView) findViewById(R.id.pmTopicSeeAllPreviousMessagesTextView);
		messagesSeparator = findViewById(R.id.pmTopicSeeMessagesSeparator);
		seePreviousMessagesTv = (TextView) findViewById(R.id.pmTopicSeePreviousMessagesTextView);
		refreshButton = (Button) findViewById(R.id.pmTopicRefreshButton);
		actionButtonSeparator = findViewById(R.id.pmTopicActionButtonSeparator);
		actionButtonSeparator.setVisibility(View.GONE);
		actionButton = (Button) findViewById(R.id.pmTopicActionButton);
		actionButton.setVisibility(View.GONE);
		replyLayout = (FrameLayout) findViewById(R.id.pmTopicReplyLayout);
		
		/* Initialize PM Topic */
		if(!savedState)
		{
			pmTopic = (JvcPmTopic) GlobalData.get("pmTopicFromPreviousActivity");
			if(pmTopic == null)
			{
				Log.e("JvcForumsReader", "pmTopic is null");
				Log.e("JvcForumsReader", "Finishing PmTopicActivity...");
				finish();
				return;
			}
		}
		else
		{
			String pmTopicKey = savedBundle.getString("pmTopicKey");
			pmTopic = (JvcPmTopic) GlobalData.getOnce(pmTopicKey);
			if(pmTopic == null)
				pmTopic = JvcPmTopic.restoreInRecoveryMode(this, savedBundle);
		}

        /* Initialize reply layout */
		replyLayoutHandler = new TopicReplyLayoutHandler(this, replyRunnableHandler, replyLayout, pmTopic);
		if(savedState)
			replyLayoutHandler.restoreState(savedBundle);

		if(recycledContent != null)
			loadInitialPmTopicList(recycledContent);
		else
			loadInitialPmTopicList();
	}
	
	/* Activity lifecycle */

	public void onSaveInstanceState(Bundle bundle)
	{
		bundle.putBoolean("savedInstanceState", true);

		String pmTopicKey = System.currentTimeMillis() + "_pm_topic";
		bundle.putString("pmTopicKey", pmTopicKey);
		GlobalData.set(pmTopicKey, pmTopic);

		String postListKey = System.currentTimeMillis() + "_post_list";
		bundle.putString("postListKey", postListKey);
		GlobalData.set(postListKey, postList);

		replyLayoutHandler.saveState(bundle);
		bundle.putInt("scrollViewY", mainView.getScrollY());
		
		/* Recovery mode */
		pmTopic.saveInRecoveryMode(bundle);
	}

	@Override
	public void onBackPressed()
	{
		if(replyLayoutHandler.isReplying())
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
	
	/* Context menu */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info)
	{
		int id = view.getId();
		currentContextMenuPost = postList.get(id);
		getMenuInflater().inflate(R.menu.post_context_menu, menu);
		menu.setHeaderTitle(String.format(getString(R.string.contextMenuPostHeader), currentContextMenuPost.getPostPseudo()));
		menu.removeItem(R.id.contextMenuCopyPermanentLink);
		menu.removeItem(R.id.contextMenuDeletePost);
		menu.removeItem(R.id.contextMenuKickPseudo);
		if(JvcUserData.isPseudoInBlacklist(currentContextMenuPost.getPostPseudo()))
		{
			menu.findItem(R.id.contextMenuIgnorePseudo).setTitle(R.string.contextMenuStopIgnorePseudo);
		}
		if(currentContextMenuPost.getType() == JvcPost.TYPE_GENERIC)
		{
			menu.findItem(R.id.contextMenuWarnAdmin).setEnabled(false);
			menu.findItem(R.id.contextMenuQuotePost).setEnabled(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		if(currentContextMenuPost != null)
		{
			switch(item.getItemId())
			{
				case R.id.contextMenuShowProfile:
					GlobalData.set("cdvPseudoFromLastActivity", currentContextMenuPost.getPostPseudo());
					startActivity(new Intent(this, CdvActivity.class));
					break;

				case R.id.contextMenuWarnAdmin:
					PmDdbDialog dialog = new PmDdbDialog(this, currentContextMenuPost, pmTopic);
					dialog.show();
					break;

				case R.id.contextMenuQuoteAuthor:
					if(!pmTopic.requestIsTopicLocked())
						replyLayoutHandler.quoteText(String.format("%s :d) ", currentContextMenuPost.getPostPseudo()));
					break;

				case R.id.contextMenuQuotePost:
					if(!pmTopic.requestIsTopicLocked())
						replyLayoutHandler.quoteText(String.format("%s\n%s | %s\n\n%s%s\n\n", JvcUtils.QUOTE_DELIMITER, currentContextMenuPost.getPostPseudo(), currentContextMenuPost.getPostDate(), currentContextMenuPost.getTextualPostData(), JvcUtils.QUOTE_DELIMITER));
					break;

				case R.id.contextMenuCopyPost:
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setText(currentContextMenuPost.getTextualPostData());
					Toast.makeText(this, getString(R.string.postCopied), Toast.LENGTH_LONG).show();
					break;

				case R.id.contextMenuIgnorePseudo:
					final String pseudo = currentContextMenuPost.getPostPseudo();

					try
					{
						if(!JvcUserData.isPseudoInBlacklist(pseudo))
						{
							JvcUserData.setPseudoInBlacklist(pseudo);
						}
						else
						{
							JvcUserData.removePseudoFromBlacklist(pseudo);
						}
					}
					catch(IOException e)
					{
						NoticeDialog.show(this, e.toString());
					}

					final int postCount = postList.size();
					for(int i = 0; i < postCount; i++)
					{
						postItemList.get(i).updateDataFromPost(postList.get(i), false);
					}
					break;
			}

			return true;
		}

		return super.onContextItemSelected(item);
	}
	
	/* Options menu */

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.pm_topic_activity, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		return pmTopic.getRequestCanAddRecipient();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.optionsMenuAddRecipient:
				pseudoPromptDialog = new PseudoPromptDialog(PmTopicActivity.this, new PseudoPromptDialog.OnPseudoChosenListener()
				{
					@Override
					public void onPseudoChosen(JvcPseudo pseudo)
					{
						pseudoPromptDialog.dismiss();
						pseudoPromptDialog = null;
						JvcAddRecipientTask task = new JvcAddRecipientTask();
						registerTask(task);
						task.execute(pseudo.getPseudo());
					}
				}, false);
				pseudoPromptDialog.show();
				break;

			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}

	/* Reply layout handler */
	private final Handler replyRunnableHandler = new Handler(new Callback()
	{
		public boolean handleMessage(Message msg)
		{
			switch(msg.what)
			{
				case TopicReplyLayoutHandler.REFRESH_PM_TOPIC:
					JvcPost post = (JvcPost) msg.obj;
					loadInitialPmTopicList(post.getLastContent());
					break;

				case TopicReplyLayoutHandler.SCROLL_DOWN_END:
					if(postItemList.size() > 0)
					{
						mainView.post(new Runnable()
						{
							public void run()
							{
								mainView.scrollTo(0, replyLayout.getBottom());
							}
						});
					}
					break;

				case TopicReplyLayoutHandler.PREVIEW_POST_ANIMATE:
					if(msg.obj != null && msg.obj instanceof View)
					{
						setAnimationView((View) msg.obj);
					}
					break;

				case TopicReplyLayoutHandler.PREVIEW_POST_CLOSE:
					setAnimationView(mainView);
					break;
			}

			return true;
		}
	});

	public void pmTopicRefreshButtonClick(View view)
	{
		loadInitialPmTopicList();
	}

	public void pmTopicActionButtonClick(View view)
	{
		NoticeDialog.showYesNo(this, getString(R.string.pmTopicActionDialog), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						final int action = pmTopic.getRequestAvailableAction();
						if(action != JvcPmTopic.ACTION_NONE)
						{
							JvcPmTopicExecuteActionTask task = new JvcPmTopicExecuteActionTask(action);
							registerTask(task);
							task.execute();
						}
					}
				}, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				}
		);
	}

	public void pmTopicSeeAllPreviousMessagesClick(View view)
	{
		seeAllMessagesDialog = new ProgressDialog(this);
		seeAllMessagesDialog.setCancelable(true);
		seeAllMessagesDialog.setOnDismissListener(new OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				stopLoadingAllMessages = true;
			}
		});
		seeAllMessagesDialog.setTitle(R.string.genericLoading);
		seeAllMessagesDialog.setIndeterminate(true);
		seeAllMessagesDialog.setMessage(String.format(getString(R.string.pmTopicLoadedMessages), 0));
		seeAllMessagesDialog.show();

		allMessagesCounter = 25;
		currentPage++;
		loadPreviousMessages(true);
	}

	public void pmTopicSeePreviousMessagesClick(View view)
	{
		currentPage++;
		loadPreviousMessages(false);
	}

	private void orderColorSwitches()
	{
		if(!jvcLike)
			return;

		boolean firstBg = false;
		for(int i = postItemList.size() - 1; i >= 0; i--)
		{
			if(firstBg)
				postItemList.get(i).getView().setBackgroundResource(R.drawable.jvc_post_box1);
			else
				postItemList.get(i).getView().setBackgroundResource(R.drawable.jvc_post_box2);

			firstBg = !firstBg;
		}
	}

	private void loadInitialPmTopicList(String... strings)
	{
		JvcLoadInitialPmTopicListTask task = new JvcLoadInitialPmTopicListTask();
		registerTask(task);
		task.execute(strings);
	}

	private class JvcLoadInitialPmTopicListTask extends AsyncTask<String, Void, String>
	{
		@SuppressWarnings("unchecked")
		protected String doInBackground(String... strings)
		{
			if(isCancelled())
				return null;

			try
			{
				if(savedState && pmTopic.getRequestError() == null)
				{
					postList = (ArrayList<JvcPost>) GlobalData.getOnce(savedBundle.getString("postListKey"));
					if(postList != null)
						return null;
				}

				boolean error;
				if(strings != null && strings.length > 0 && strings[0] != null && strings[0].length() > 0)
				{
					error = pmTopic.getInitialTopicListFromContent(strings[0]);
				}
				else
				{
					error = pmTopic.requestInitialTopicList(this);
				}

				if(error || isCancelled())
					return getString(R.string.errorWhileLoading) + " : " + pmTopic.getRequestError();
				postList = pmTopic.getRequestPostList();

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
			mainView.scrollTo(0, 0);
			currentPage = 0;

			listLayout.removeAllViews();
			refreshButton.setText(R.string.genericLoading);
			refreshButton.setEnabled(false);
			actionButton.setEnabled(false);

			participatingPseudosTv.setVisibility(View.GONE);
			allMessagesSeparator.setVisibility(View.GONE);
			seeAllPreviousMessagesTv.setVisibility(View.GONE);
			seePreviousMessagesTv.setText(R.string.genericLoading);
			seePreviousMessagesTv.setEnabled(false);
			messagesSeparator.setVisibility(View.VISIBLE);
			seePreviousMessagesTv.setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(String result)
		{
			refreshButton.setText(R.string.genericRefresh);
			refreshButton.setEnabled(true);

			if(result == null)
			{
				/* Available action */
				final int action = pmTopic.getRequestAvailableAction();
				if(action == JvcPmTopic.ACTION_CLOSE_TOPIC)
					actionButton.setText(R.string.pmTopicActionClose);
				if(action != JvcPmTopic.ACTION_NONE)
				{
					actionButton.setEnabled(true);
					actionButton.setVisibility(View.VISIBLE);
					actionButtonSeparator.setVisibility(View.VISIBLE);
				}
				else
				{
					actionButton.setVisibility(View.GONE);
					actionButtonSeparator.setVisibility(View.GONE);
				}
				
				/* Participating pseudos */
				LinkedHashMap<JvcPseudo, String> pseudosMap = pmTopic.getRequestPseudosMap();
				SpannableStringBuilder builder = new SpannableStringBuilder(getString(R.string.between) + "  ");
				int i = 0, lastId = pseudosMap.size() - 1;
				for(JvcPseudo jvcPseudo : pseudosMap.keySet())
				{
					Spannable pseudo = JvcLinkIntent.makeLink(PmTopicActivity.this, jvcPseudo.getPseudo(), pseudosMap.get(jvcPseudo), true);

					if(i == lastId)
					{
						builder.append(getString(R.string.and)).append("  ");
						builder.append(pseudo);
					}
					else
					{
						builder.append(pseudo);
						builder.append("  ");
					}

					i++;
				}
				participatingPseudosTv.setText(builder);
				participatingPseudosTv.setVisibility(View.VISIBLE);
				
				/* Previous messages */
				final int prevPosts = pmTopic.getRequestAvailablePreviousMessages();
				if(prevPosts > 0)
				{
					if(prevPosts == 25)
					{
						allMessagesSeparator.setVisibility(View.VISIBLE);
						seeAllPreviousMessagesTv.setVisibility(View.VISIBLE);
					}
					if(prevPosts == 1)
						seePreviousMessagesTv.setText(R.string.pmTopicSeeThePreviousMessage);
					else
						seePreviousMessagesTv.setText(String.format(getString(R.string.pmTopicSeePreviousMessages), prevPosts));
					seePreviousMessagesTv.setEnabled(true);
				}
				else
				{
					messagesSeparator.setVisibility(View.GONE);
					seePreviousMessagesTv.setVisibility(View.GONE);
				}
				
				/* Post list */
				final boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_POSTS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_POSTS);
				for(i = 0; i < postList.size(); i++)
				{
					PostItem item = new PostItem(PmTopicActivity.this, jvcLike, showNoelshackThumbnails, animateSmileys);
					item.updateDataFromPost(postList.get(i), false);
					postItemList.add(item);
					View view = item.getView();
					view.setId(i);
					registerForContextMenu(view);
					listLayout.addView(view, 0);
				}
				
				/* Scroll view */
				if(!savedState)
				{
					mainView.post(new Runnable()
					{
						public void run()
						{
							mainView.scrollTo(0, replyLayout.getBottom());
						}
					});
				}
				else
				{
					mainView.post(new Runnable()
					{
						public void run()
						{
							mainView.scrollTo(0, savedBundle.getInt("scrollViewY"));
						}
					});
					savedState = false;
				}

				replyLayoutHandler.updateLockState(pmTopic.requestIsTopicLocked());

				MainApplication app = (MainApplication) getApplicationContext();
				if(!pmTopic.isTopicRead() && app.getUnreadPmCount() > 0)
				{
					app.setUnreadPmCount(app.getUnreadPmCount() - 1);
					pmTopic.acknowledgeReadTopic();
				}

				startAnimatingDrawables(mainView);
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				seePreviousMessagesTv.setText(MainApplication.handleHttpTimeout(PmTopicActivity.this));
			}
			else
			{
				seePreviousMessagesTv.setText(R.string.operationFailed);
				NoticeDialog.show(PmTopicActivity.this, result);
			}
		}
	}

	private void loadPreviousMessages(Boolean... params)
	{
		JvcLoadPreviousMessagesTask task = new JvcLoadPreviousMessagesTask();
		registerTask(task);
		task.execute(params);
	}

	private class JvcLoadPreviousMessagesTask extends AsyncTask<Boolean, Void, String>
	{
		private ArrayList<JvcPost> previousList;
		private boolean repeat = false;

		@Override
		protected String doInBackground(Boolean... booleans)
		{
			if(isCancelled())
				return null;
			repeat = booleans[0];

			try
			{
				previousList = pmTopic.requestPreviousMessages(currentPage, this);
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
			stopLoadingAllMessages = false;

			refreshButton.setText(R.string.genericLoading);
			refreshButton.setEnabled(false);

			allMessagesSeparator.setVisibility(View.GONE);
			seeAllPreviousMessagesTv.setVisibility(View.GONE);
			seePreviousMessagesTv.setText(R.string.genericLoading);
			seePreviousMessagesTv.setEnabled(false);
			messagesSeparator.setVisibility(View.VISIBLE);
			seePreviousMessagesTv.setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(String result)
		{
			refreshButton.setText(R.string.genericRefresh);
			refreshButton.setEnabled(true);

			if(result == null)
			{
				final int prevPosts = pmTopic.getRequestAvailablePreviousMessages();
				if(prevPosts > 0)
				{
					if(prevPosts == 25)
					{
						allMessagesSeparator.setVisibility(View.VISIBLE);
						seeAllPreviousMessagesTv.setVisibility(View.VISIBLE);
					}
					seePreviousMessagesTv.setText(String.format(getString(R.string.pmTopicSeePreviousMessages), prevPosts));
					seePreviousMessagesTv.setEnabled(true);
				}
				else
				{
					messagesSeparator.setVisibility(View.GONE);
					seePreviousMessagesTv.setVisibility(View.GONE);
				}

				final boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_POSTS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_POSTS);
				for(int i = 0; i < previousList.size(); i++)
				{
					JvcPost post = previousList.get(i);
					PostItem item = new PostItem(PmTopicActivity.this, jvcLike, showNoelshackThumbnails, animateSmileys);
					item.updateDataFromPost(post, false);
					postItemList.add(item);
					postList.add(post);
					View view = item.getView();
					view.setId(postList.size() - 1);
					registerForContextMenu(view);
					listLayout.addView(view, 0);
				}

				if(!repeat)
				{
					orderColorSwitches();
				}
				else
				{
					if(prevPosts > 0 && !stopLoadingAllMessages)
					{
						currentPage++;
						allMessagesCounter += prevPosts;
						seeAllMessagesDialog.setMessage(String.format(getString(R.string.pmTopicLoadedMessages), allMessagesCounter));
						loadPreviousMessages(true);
					}
					else
					{
						seeAllMessagesDialog.dismiss();
						seeAllMessagesDialog = null;
						mainView.post(new Runnable()
						{
							public void run()
							{
								orderColorSwitches();
							}
						});
					}
				}
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				seePreviousMessagesTv.setText(MainApplication.handleHttpTimeout(PmTopicActivity.this));
			}
			else
			{
				seePreviousMessagesTv.setText(R.string.operationFailed);
				NoticeDialog.show(PmTopicActivity.this, result);
			}
		}
	}

	private class JvcPmTopicExecuteActionTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;
		private int action;

		public JvcPmTopicExecuteActionTask(int action)
		{
			super();
			this.action = action;
		}

		protected String doInBackground(Void... voids)
		{
			if(isCancelled())
				return null;

			try
			{
				HttpPost httpPost;
				if(action == JvcPmTopic.ACTION_CLOSE_TOPIC)
				{
					httpPost = new HttpPost("http://www.jeuxvideo.com/messages-prives/fermer_discussion.php");
				}
				else if(action == JvcPmTopic.ACTION_QUIT_TOPIC)
				{
					httpPost = new HttpPost("http://www.jeuxvideo.com/messages-prives/quitter_discussion.php");
				}
				else
				{
					return getString(R.string.errorWhileLoading) + " : unknown action id " + action;
				}

				MainApplication app = (MainApplication) getApplicationContext();
				HttpClient client = app.getHttpClient(MainApplication.JVC_SESSION);
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("id_discussion", String.valueOf(pmTopic.getId())));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
				client.execute(httpPost);

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
			dialog = new ProgressDialog(PmTopicActivity.this);
			dialog.setMessage(action == JvcPmTopic.ACTION_CLOSE_TOPIC ? getString(R.string.pmTopicActionClosing) : getString(R.string.pmTopicActionQuitting));
			dialog.setCancelable(false);
			dialog.show();
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				loadInitialPmTopicList();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(PmTopicActivity.this);
			}
			else
			{
				NoticeDialog.show(PmTopicActivity.this, result);
			}
		}
	}

	private class JvcAddRecipientTask extends AsyncTask<String, Void, String>
	{
		ProgressDialog dialog;

		@Override
		protected String doInBackground(String... pseudos)
		{
			if(isCancelled())
				return null;
			int error = pmTopic.addRecipients(Arrays.asList(pseudos));
			if(isCancelled())
				return null;

			if(error == JvcPmTopic.REQUEST_OK)
			{
				return null;
			}
			else
			{
				return pmTopic.getRequestError();
			}
		}

		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(PmTopicActivity.this, "", getString(R.string.sendingRequest));
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				loadInitialPmTopicList();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(PmTopicActivity.this);
			}
			else
			{
				NoticeDialog.show(PmTopicActivity.this, result);
			}
		}
	}
}
