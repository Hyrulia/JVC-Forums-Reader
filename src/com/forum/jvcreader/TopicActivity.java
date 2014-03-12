package com.forum.jvcreader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.forum.jvcreader.graphics.*;
import com.forum.jvcreader.jvc.*;
import com.forum.jvcreader.utils.AsyncTaskManager;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.widgets.SwipeableScrollViewer;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

@SuppressWarnings("deprecation") /* Clipboard */

public class TopicActivity extends JvcActivity
{
	private static final int MANUAL_BROWSING = 1;
	private static final int AUTOMATIC_BROWSING = 2;

	private JvcTopic topic;
	private boolean isUpdatedTopic;
	private boolean updatedTopicsHighlight = false;
	private int highlightPostPos = -1;
	private ArrayList<JvcPost> currentPostList;
	private ArrayList<PostItem> postItemList;

	private long prefAutomaticBrowsingDelay;
	private boolean prefAutomaticCheckingEnabled;

	private SwipeableScrollViewer swipeableViewer;
	private TextView transitivePreviousText;
	private TextView transitiveNextText;

	private View lastSeparator;
	private ScrollView scrollView;
	private PageSelectionDialog pageSelectionDialog;

	private Button switchBrowsingButton;
	private LinearLayout manualBrowsingLayout;
	private LinearLayout bottomNavigationLayout;

	private ImageButton firstPageButton;
	private ImageButton bottomFirstPageButton;
	private Drawable firstPageDrawable;
	private ImageButton previousPageButton;
	private ImageButton bottomPreviousPageButton;
	private Drawable previousPageDrawable;
	private ImageButton nextPageButton;
	private ImageButton bottomNextPageButton;
	private Drawable nextPageDrawable;
	private ImageButton lastPageButton;
	private ImageButton bottomLastPageButton;
	private Drawable lastPageDrawable;
	private Button goToPageButton;
	private Button bottomGoToPageButton;

	private boolean enableSmileys;
	private boolean animateSmileys;
	private boolean showNoelshackThumbnails;

	private FrameLayout errorLayout;
	private TextView errorTextView;
	private TextView topicNameTextView;

	private FrameLayout replyLayout;
	private TopicReplyLayoutHandler replyLayoutHandler;
	private Handler updatePostListHandler = new Handler();

	private int currentListType;
	private int currentPageNumber;
	private int currentBrowsingMode;
	private long lastReadPost = -1;
	private boolean isReplyTopic;
	private boolean topicInFavorites = false;
	private boolean topicInUpdatedTopics = false;

	private boolean savedState = false;
	private Bundle savedBundle = null;
	private boolean lastRequestSucceeded = false;
	private JvcPost currentContextMenuPost;

	private Menu optionsMenu = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.topic);

		if(savedInstanceState != null)
		{
			savedState = savedInstanceState.getBoolean("savedInstanceState");
			savedBundle = savedInstanceState;
		}

		isUpdatedTopic = getIntent().getBooleanExtra("com.forum.jvcreader.IsUpdatedTopic", false);
		enableSmileys = JvcUserData.getBoolean(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES, JvcUserData.DEFAULT_SMILEYS_IN_TOPIC_TITLES);
		animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);
		showNoelshackThumbnails = JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS);

    	/* Assign UI elements */
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.topicLayout);
		swipeableViewer = (SwipeableScrollViewer) findViewById(R.id.topicSwipeableScrollViewer);
		scrollView = (ScrollView) findViewById(R.id.topicScrollView);
		switchBrowsingButton = (Button) findViewById(R.id.topicSwitchBrowsingButton);
		manualBrowsingLayout = (LinearLayout) findViewById(R.id.topicManualBrowsingLayout);
		firstPageButton = (ImageButton) findViewById(R.id.topicFirstPageButton);
		previousPageButton = (ImageButton) findViewById(R.id.topicPreviousPageButton);
		nextPageButton = (ImageButton) findViewById(R.id.topicNextPageButton);
		lastPageButton = (ImageButton) findViewById(R.id.topicLastPageButton);
		goToPageButton = (Button) findViewById(R.id.topicGoToPageButton);
		bottomFirstPageButton = (ImageButton) findViewById(R.id.topicBottomFirstPageButton);
		bottomPreviousPageButton = (ImageButton) findViewById(R.id.topicBottomPreviousPageButton);
		bottomNextPageButton = (ImageButton) findViewById(R.id.topicBottomNextPageButton);
		bottomLastPageButton = (ImageButton) findViewById(R.id.topicBottomLastPageButton);
		bottomGoToPageButton = (Button) findViewById(R.id.topicBottomGoToPageButton);
		errorLayout = (FrameLayout) findViewById(R.id.topicErrorLayout);
		errorTextView = (TextView) findViewById(R.id.topicErrorTextView);
		topicNameTextView = (TextView) findViewById(R.id.topicNameTextView);
		lastSeparator = findViewById(R.id.topicLastSeparator);
		bottomNavigationLayout = (LinearLayout) findViewById(R.id.topicBottomNavigationLayout);
		bottomNavigationLayout.setVisibility(View.GONE);
		replyLayout = (FrameLayout) findViewById(R.id.topicReplyLayout);
		replyLayout.setVisibility(View.GONE);

    	/* Initialize swipeable viewer */
		swipeableViewer.setSnapToScreenRunnable(swipeableViewerChangedView);
		swipeableViewer.setInitialPosition(0);
		transitivePreviousText = getTransitiveTextView();
		transitiveNextText = getTransitiveTextView();

    	/* Initialize image buttons */
		firstPageDrawable = CachedRawDrawables.getDrawable(R.raw.page_debut);
		previousPageDrawable = CachedRawDrawables.getDrawable(R.raw.page_prec);
		nextPageDrawable = CachedRawDrawables.getDrawable(R.raw.page_suiv);
		lastPageDrawable = CachedRawDrawables.getDrawable(R.raw.page_fin);

		firstPageButton.setImageDrawable(firstPageDrawable);
		bottomFirstPageButton.setImageDrawable(firstPageDrawable);
		previousPageButton.setImageDrawable(previousPageDrawable);
		bottomPreviousPageButton.setImageDrawable(previousPageDrawable);
		nextPageButton.setImageDrawable(nextPageDrawable);
		bottomNextPageButton.setImageDrawable(nextPageDrawable);
		lastPageButton.setImageDrawable(lastPageDrawable);
		bottomLastPageButton.setImageDrawable(lastPageDrawable);
    	
		/* Fetch topic */
		if(!savedState)
		{
			topic = (JvcTopic) GlobalData.get("topicFromPreviousActivity");
			if(topic == null)
			{
				Log.e("JvcForumsReader", "topic is null !");
				Log.e("JvcForumsReader", "Finishing TopicActivity...");
				finish();
				return;
			}
			isReplyTopic = topic.isReplyTopic();
		}
		else
		{
			String topicKey = savedBundle.getString("topicKey");
			topic = (JvcTopic) GlobalData.getOnce(topicKey);
			if(topic == null)
				topic = JvcTopic.restoreInRecoveryMode(this, savedBundle);
			isReplyTopic = false;
		}
    	
	    /* Create 20 unique post items */
		final boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_POSTS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_POSTS);
		postItemList = new ArrayList<PostItem>();
		for(int i = 0; i < 20; i++)
		{
			PostItem post = new PostItem(this, jvcLike, showNoelshackThumbnails, animateSmileys);
			View view = post.getView();
			post.getTitleBarLayout().setId(i + 500);
			registerForContextMenu(post.getTitleBarLayout());
			linearLayout.addView(view);
			postItemList.add(post);
		}
	    
	    /* Initialize reply layout */
		replyLayoutHandler = new TopicReplyLayoutHandler(this, replyRunnableHandler, replyLayout, topic);

		boolean loadTopic = true;

		if(!savedState)
		{
			if(isUpdatedTopic)
			{
				final UpdatedTopicData data = topic.getUpdatedTopicData();
				if(data.isNew() && data.getNewPostCount() > 10)
				{
					loadTopic = false;
					AlertDialog.Builder dialog = new AlertDialog.Builder(this);
					dialog.setCancelable(false);
					dialog.setTitle(R.string.updatedTopic);
					dialog.setMessage(R.string.updatedTopicsChoiceDialogMessage);
					dialog.setPositiveButton(R.string.updatedTopicsFirstUnreadPost, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							highlightPostPos = data.computeFirstNewPostPos();
							currentListType = JvcTopic.SHOW_POSTS_FROM_PAGE;
							currentPageNumber = data.computeFirstNewPostPage();
							currentBrowsingMode = TopicActivity.MANUAL_BROWSING;
							refreshTopic();
							dialog.dismiss();
						}
					});
					dialog.setNegativeButton(R.string.updatedTopicsLastTenPosts, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							updatedTopicsHighlight = true;
							currentPageNumber = 1;
							currentListType = JvcTopic.SHOW_LAST_TEN_POSTS;
							currentBrowsingMode = TopicActivity.AUTOMATIC_BROWSING;
							manualBrowsingLayout.setVisibility(View.GONE);
							bottomNavigationLayout.setVisibility(View.GONE);
							switchBrowsingButton.setText(R.string.switchToManualBrowsing);
							refreshTopic();
							dialog.dismiss();
						}
					});
					dialog.show();
				}
				else
				{
					updatedTopicsHighlight = true;
					currentPageNumber = 1;
					currentListType = JvcTopic.SHOW_LAST_TEN_POSTS;
					currentBrowsingMode = TopicActivity.AUTOMATIC_BROWSING;
					manualBrowsingLayout.setVisibility(View.GONE);
					bottomNavigationLayout.setVisibility(View.GONE);
					switchBrowsingButton.setText(R.string.switchToManualBrowsing);
				}
			}
			else
			{
				currentListType = JvcTopic.SHOW_POSTS_FROM_PAGE;
				if(isReplyTopic)
					currentPageNumber = topic.getReplyPageNumber();
				else
					currentPageNumber = getIntent().getIntExtra("com.forum.jvcreader.PageNumber", 1);
				currentBrowsingMode = TopicActivity.MANUAL_BROWSING;
			}
		}
		else
		{
			replyLayoutHandler.restoreState(savedBundle);
			currentListType = savedBundle.getInt("currentListType");
			currentPageNumber = savedBundle.getInt("currentPageNumber");
			currentBrowsingMode = savedBundle.getInt("currentBrowsingMode");
			if(currentBrowsingMode == AUTOMATIC_BROWSING)
			{
				manualBrowsingLayout.setVisibility(View.GONE);
				bottomNavigationLayout.setVisibility(View.GONE);
				switchBrowsingButton.setText(R.string.switchToManualBrowsing);
				if(replyLayoutHandler.isReplying())
					switchBrowsingButton.setEnabled(false);
			}
		}

		prefAutomaticCheckingEnabled = JvcUserData.getBoolean(JvcUserData.PREF_CHECK_POSTS_DURING_AUTOMATIC_BROWSING, JvcUserData.DEFAULT_CHECK_POSTS_DURING_AUTOMATIC_BROWSING);
		prefAutomaticBrowsingDelay = JvcUserData.getLong(JvcUserData.PREF_AUTOMATIC_BROWSING_CHECK_DELAY, JvcUserData.DEFAULT_AUTOMATIC_BROWSING_CHECK_DELAY);
	    
	    /* Initialize favorite & updated topics buttons */
		topicInFavorites = JvcUserData.isTopicInFavorites(topic);
		topicInUpdatedTopics = JvcUserData.isTopicInUpdatedTopics(topic);
        
        /* Request post list */
		if(loadTopic)
			refreshTopic();
	}

	public void onSaveInstanceState(Bundle bundle)
	{
		bundle.putBoolean("savedInstanceState", true);

		String topicKey = System.currentTimeMillis() + "_topic";
		bundle.putString("topicKey", topicKey);
		GlobalData.set(topicKey, topic);

		String postListKey = System.currentTimeMillis() + "_post_list";
		bundle.putString("postListKey", postListKey);
		GlobalData.set(postListKey, currentPostList);

		replyLayoutHandler.saveState(bundle);
		bundle.putInt("currentListType", currentListType);
		bundle.putInt("currentPageNumber", currentPageNumber);
		bundle.putInt("currentBrowsingMode", currentBrowsingMode);
		bundle.putInt("scrollViewY", scrollView.getScrollY());
		
		/* Recovery mode */
		topic.saveInRecoveryMode(bundle);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		updatePostListHandler.removeCallbacks(updatePostListRunnable);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if(firstPageButton.isEnabled())
			firstPageDrawable.setAlpha(255);
		else
			firstPageDrawable.setAlpha(100);
		if(previousPageButton.isEnabled())
			previousPageDrawable.setAlpha(255);
		else
			previousPageDrawable.setAlpha(100);
		if(nextPageButton.isEnabled())
			nextPageDrawable.setAlpha(255);
		else
			nextPageDrawable.setAlpha(100);
		if(lastPageButton.isEnabled())
			lastPageDrawable.setAlpha(255);
		else
			lastPageDrawable.setAlpha(100);

		if(currentBrowsingMode == TopicActivity.AUTOMATIC_BROWSING && lastRequestSucceeded && prefAutomaticCheckingEnabled)
		{
			updatePostListHandler.post(updatePostListRunnable); /* Resume auto-checking */
		}
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

	private final Handler replyRunnableHandler = new Handler(new Callback()
	{
		public boolean handleMessage(Message msg)
		{
			switch(msg.what)
			{
				case TopicReplyLayoutHandler.SET_BROWSING_TO_AUTOMATIC:
					if(currentBrowsingMode == TopicActivity.MANUAL_BROWSING)
					{
						manualBrowsingLayout.setVisibility(View.GONE);
						bottomNavigationLayout.setVisibility(View.GONE);
						switchBrowsingButton.setText(R.string.switchToManualBrowsing);
						currentListType = JvcTopic.SHOW_LAST_TEN_POSTS;
						currentBrowsingMode = TopicActivity.AUTOMATIC_BROWSING;
					}

					scrollView.post(new SmoothScrollToViewRunnable(replyLayout, true));
					updatePostListHandler.post(updatePostListRunnable);
					switchBrowsingButton.setEnabled(false);
					break;

				case TopicReplyLayoutHandler.CANCEL_REPLY:
					switchBrowsingButton.setEnabled(true);
					break;

				case TopicReplyLayoutHandler.REFRESH_TOPIC:
					if(currentBrowsingMode == TopicActivity.MANUAL_BROWSING)
						refreshTopic();
					else if(currentBrowsingMode == TopicActivity.AUTOMATIC_BROWSING)
						updatePostListHandler.post(updatePostListRunnable);
					break;

				case TopicReplyLayoutHandler.PREVIEW_POST_ANIMATE:
					if(msg.obj != null && msg.obj instanceof View)
					{
						setAnimationView((View) msg.obj);
					}
					break;

				case TopicReplyLayoutHandler.PREVIEW_POST_CLOSE:
					setAnimationView(scrollView);
					break;
			}

			return true;
		}
	});

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info)
	{
		int id = view.getId();
		if(id >= 500 && id <= 520)
		{
			currentContextMenuPost = currentPostList.get(id - 500);
			getMenuInflater().inflate(R.menu.post_context_menu, menu);
			menu.setHeaderTitle(String.format(getString(R.string.contextMenuPostHeader), currentContextMenuPost.getPostPseudo()));
			if(currentBrowsingMode != TopicActivity.MANUAL_BROWSING)
				menu.findItem(R.id.contextMenuCopyPermanentLink).setEnabled(false);
			if(JvcUserData.isPseudoInBlacklist(currentContextMenuPost.getPostPseudo()))
			{
				menu.findItem(R.id.contextMenuIgnorePseudo).setTitle(R.string.contextMenuStopIgnorePseudo);
			}

			if(currentContextMenuPost.getAdminDeleteUrl() == null)
				menu.removeItem(R.id.contextMenuDeletePost);
			if(currentContextMenuPost.getAdminKickUrl() == null)
				menu.removeItem(R.id.contextMenuKickPseudo);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		if(currentContextMenuPost != null)
		{
			switch(item.getItemId())
			{
				case R.id.contextMenuShowProfile:
					GlobalData.set("cdvPseudoFromLastActivity", currentContextMenuPost.getPostPseudo());
					startActivity(new Intent(this, CdvActivity.class));
					break;

				case R.id.contextMenuDeletePost:
					if(currentContextMenuPost.getAdminDeleteRequestConfirm())
					{
						NoticeDialog.showYesNo(this, getString(R.string.dialogDeleteTopic), new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialog, int which)
									{
										dialog.dismiss();
										JvcDeletePostTask task = new JvcDeletePostTask();
										registerTask(task);
										task.execute();
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
						JvcDeletePostTask task = new JvcDeletePostTask();
						registerTask(task);
						task.execute();
					}
					break;

				case R.id.contextMenuWarnAdmin:
					if(topic.getForum().isForumJV())
					{
						DdbDialog dialog = new DdbDialog(this, currentContextMenuPost, currentBrowsingMode == MANUAL_BROWSING ? "1" : "3");
						dialog.show();
					}
					else
					{
						String key = "ddbPost_" + System.currentTimeMillis();
						GlobalData.set(key, currentContextMenuPost);
						Intent intent = new Intent(this, NewDdbActivity.class);
						intent.putExtra("com.forum.jvcreader.DdbPostKey", key);
						intent.putExtra("com.forum.jvcreader.CurrentBrowsingMode", currentBrowsingMode == MANUAL_BROWSING ? "1" : "3");
						startActivity(intent);
					}
					break;

				case R.id.contextMenuQuoteAuthor:
					if(!topic.isTopicLocked())
						replyLayoutHandler.quoteText(String.format("%s :d) ", currentContextMenuPost.getPostPseudo()));
					break;

				case R.id.contextMenuQuotePost:
					if(!topic.isTopicLocked())
						replyLayoutHandler.quoteText(String.format("%s\n%s | %s\n\n%s%s\n\n", JvcUtils.QUOTE_DELIMITER, currentContextMenuPost.getPostPseudo(), currentContextMenuPost.getPostDate(), currentContextMenuPost.getTextualPostData(), JvcUtils.QUOTE_DELIMITER));
					break;

				case R.id.contextMenuCopyPost:
					clipboard.setText(currentContextMenuPost.getTextualPostData());
					Toast.makeText(this, getString(R.string.postCopied), Toast.LENGTH_LONG).show();
					break;

				case R.id.contextMenuCopyPermanentLink:
					clipboard.setText(currentContextMenuPost.getPostPermanentLink());
					Toast.makeText(this, getString(R.string.permanentLinkCopied), Toast.LENGTH_LONG).show();
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

					final int postCount = currentPostList.size();
					for(int i = 0; i < postCount; i++)
					{
						postItemList.get(i).updateDataFromPost(currentPostList.get(i), false);
					}
					break;

				case R.id.contextMenuKickPseudo:
					new KickDialog(this, currentContextMenuPost).show();
					break;
			}

			return true;
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.topic_activity, menu);

		if(topic.getAdminLockUrl() == null || topic.getAdminLockUrl().length() == 0)
			menu.removeItem(R.id.optionsMenuLockTopic);
		if(topic.getAdminPinUrl() == null || topic.getAdminPinUrl().length() == 0)
			menu.removeItem(R.id.optionsMenuPinTopic);
		if(topic.getAdminKickInterfaceUrl() == null || topic.getAdminKickInterfaceUrl().length() == 0)
			menu.removeItem(R.id.optionsMenuSeeKickInterface);

		if(JvcUserData.isTopicInArchivedTopics(topic))
		{
			MenuItem item = menu.findItem(R.id.optionsMenuArchiveTopic);
			item.setTitle(R.string.archivedTopic);
			item.setEnabled(false);
		}

		optionsMenu = menu;

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		MenuItem action = menu.findItem(R.id.optionsMenuFavActionTopics);
		if(!topicInFavorites)
			action.setTitle(R.string.addToFavoriteTopics);
		else
			action.setTitle(R.string.removeFromFavoriteTopics);

		action = menu.findItem(R.id.optionsMenuUpdatedActionTopics);
		if(!topicInUpdatedTopics)
			action.setTitle(R.string.addToUpdatedTopics);
		else
			action.setTitle(R.string.removeFromUpdatedTopics);
		action.setEnabled(lastRequestSucceeded && !topic.isTopicLocked());

		action = menu.findItem(R.id.optionsMenuLockTopic);
		if(action != null)
		{
			if(topic.getAdminIsLocked())
			{
				action.setTitle(R.string.optionsMenuUnlockTopic);
			}
			else
			{
				action.setTitle(R.string.optionsMenuLockTopic);
			}
		}

		action = menu.findItem(R.id.optionsMenuPinTopic);
		if(action != null)
		{
			if(topic.getAdminIsPinned())
			{
				action.setTitle(R.string.optionsMenuUnpinTopic);
			}
			else
			{
				action.setTitle(R.string.optionsMenuPinTopic);
			}
		}

		action = menu.findItem(R.id.optionsMenuArchiveTopic);
		if(JvcUserData.isTopicInArchivedTopics(topic))
		{
			action.setTitle(R.string.topicArchived);
			action.setEnabled(false);
		}
		else
		{
			action.setTitle(R.string.optionsMenuArchiveTopic);
			action.setEnabled(GlobalData.get("archivingTopic") == null && currentBrowsingMode == MANUAL_BROWSING && lastRequestSucceeded);
		}

		if(topic.getAdminKickInterfaceUrl() == null || topic.getAdminKickInterfaceUrl().length() == 0)
			menu.removeItem(R.id.optionsMenuSeeKickInterface);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.optionsMenuGoToForum:
				GlobalData.set("forumFromPreviousActivity", topic.getForum());
				startActivity(new Intent(this, ForumActivity.class));
				break;

			case R.id.optionsMenuLockTopic:
				JvcDoActionTask lockTask = new JvcDoActionTask();
				registerTask(lockTask);
				lockTask.execute(topic.getAdminLockUrl());
				break;

			case R.id.optionsMenuPinTopic:
				JvcDoActionTask pinTask = new JvcDoActionTask();
				registerTask(pinTask);
				pinTask.execute(topic.getAdminPinUrl());
				break;

			case R.id.optionsMenuFavActionTopics:
				try
				{
					if(topicInFavorites)
					{
						JvcUserData.removeFromFavoriteTopics(topic);
					}
					else
					{
						JvcUserData.addToFavoriteTopics(topic);
					}

					topicInFavorites = !topicInFavorites;
				}
				catch(Exception e)
				{
					NoticeDialog.show(this, getString(R.string.errorWhileLoadingUserPreferences) + " : " + e.toString());
				}
				break;

			case R.id.optionsMenuUpdatedActionTopics:
				try
				{
					if(topicInUpdatedTopics)
					{
						JvcUserData.removeFromUpdatedTopics(topic);
						topicInUpdatedTopics = false;
					}
					else
					{
						JvcAddToUpdatedTopicsTask task = new JvcAddToUpdatedTopicsTask();
						registerTask(task);
						task.execute();
					}
				}
				catch(Exception e)
				{
					NoticeDialog.show(this, getString(R.string.errorWhileLoadingUserPreferences) + " : " + e.toString());
				}
				break;

			case R.id.optionsMenuCopyTopicLink:
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(topic.getRequestRedirectedUrl());
				Toast.makeText(this, R.string.topicLinkCopied, Toast.LENGTH_SHORT).show();
				break;

			case R.id.optionsMenuSeeKickInterface:
				Intent intent = new Intent(this, KickInterfaceActivity.class);
				intent.putExtra("com.forum.jvcreader.KickInterfaceUrl", topic.getAdminKickInterfaceUrl());
				intent.putExtra("com.forum.jvcreader.KickInterfaceIsForumJV", topic.getForum().isForumJV());
				if(topic.getForum().isForumJV())
				{
					intent.putExtra("com.forum.jvcreader.ForumJVBaseUrl", "http://" + topic.getForum().getForumJVSubdomain() + ".forumjv.com");
				}
				startActivity(intent);
				break;

			case R.id.optionsMenuArchiveTopic:
				ArchiveTopicDialog dialog = new ArchiveTopicDialog(this, topic);
				dialog.setOnArchivedTopicSubmitListener(new ArchiveTopicDialog.OnArchivedTopicSubmitListener()
				{
					@Override
					public void onArchivedTopicSubmit(JvcArchivedTopic topic)
					{
						String topicKey = "temp_archived_topic_" + topic.hashCode();
						GlobalData.set(topicKey, topic);

						Intent intent = new Intent(TopicActivity.this, JvcArchiveTopicService.class);
						intent.putExtra("ArchivedTopicActivityKey", topicKey);
						startService(intent);
						Toast.makeText(TopicActivity.this, R.string.archivingTopicNotice, Toast.LENGTH_LONG).show();
					}
				});
				dialog.show();
				break;

			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}

	/* UI Events */

	public void topicSwitchBrowsingButtonClick(View view)
	{
		if(currentBrowsingMode == TopicActivity.MANUAL_BROWSING) /* Switch to automatic */
		{
			manualBrowsingLayout.setVisibility(View.GONE);
			bottomNavigationLayout.setVisibility(View.GONE);
			switchBrowsingButton.setText(R.string.switchToManualBrowsing);
			currentListType = JvcTopic.SHOW_LAST_TEN_POSTS;
			currentBrowsingMode = TopicActivity.AUTOMATIC_BROWSING;

			updatePostListHandler.post(updatePostListRunnable);
		}
		else if(currentBrowsingMode == TopicActivity.AUTOMATIC_BROWSING) /* Switch to manual */
		{
			manualBrowsingLayout.setVisibility(View.VISIBLE);
			bottomNavigationLayout.setVisibility(View.VISIBLE);
			switchBrowsingButton.setText(R.string.switchToAutomaticBrowsing);
			currentListType = JvcTopic.SHOW_POSTS_FROM_PAGE;
			currentBrowsingMode = TopicActivity.MANUAL_BROWSING;

			updatePostListHandler.removeCallbacks(updatePostListRunnable);
			refreshTopic();
		}
	}

	public void topicFirstPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			scrollView.scrollTo(0, 0);
			currentPageNumber = 1;
			refreshTopic();
		}
	}

	public void topicPreviousPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			scrollView.scrollTo(0, 0);
			currentPageNumber--;
			refreshTopic();
		}
	}

	public void topicNextPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			scrollView.scrollTo(0, 0);
			currentPageNumber++;
			refreshTopic();
		}
	}

	public void topicLastPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			scrollView.scrollTo(0, 0);
			currentPageNumber = topic.getRequestPageCount();
			refreshTopic();
		}
	}

	public void topicGoToPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			pageSelectionDialog = new PageSelectionDialog(this, topic.getRequestPageCount());

			pageSelectionDialog.setButtonOnClickListener(new OnClickListener()
			{
				public void onClick(View view)
				{
					scrollView.scrollTo(0, 0);
					currentPageNumber = pageSelectionDialog.getSelectedPageNumber();
					refreshTopic();
					pageSelectionDialog.dismiss();
					pageSelectionDialog = null;
				}
			});

			pageSelectionDialog.show();
		}
	}

	private void refreshTopic()
	{
		if(AsyncTaskManager.isActivityRegistered(this))
		{
			JvcUpdatePostListTask task = new JvcUpdatePostListTask();
			registerTask(task);
			task.execute();
		}
	}

	private TextView getTransitiveTextView()
	{
		TextView tv = (TextView) getLayoutInflater().inflate(R.layout.transitive_text_view, null);

		return tv;
	}

	private int findPostPosByPostId(ArrayList<JvcPost> list, long id)
	{
		for(int i = 0; i < list.size(); i++)
		{
			if(list.get(i).getPostId() == id)
			{
				return i;
			}
		}

		return -1;
	}

	public void smoothScrollToPostId(long postId)
	{
		int count = currentPostList.size();
		for(int i = 0; i < count; i++)
		{
			if(currentPostList.get(i).getPostId() == postId)
			{
				View view = postItemList.get(i).getView();
				view.setBackgroundResource(R.drawable.jvc_post_box_highlighted);
				scrollView.post(new SmoothScrollToViewRunnable(view));
				return;
			}
		}

		Toast.makeText(this, String.format(getString(R.string.postNotFound), postId), Toast.LENGTH_LONG).show();
	}

	public void smoothScrollToPostPos(int postPos)
	{
		int count = currentPostList.size();
		if(postPos > count - 1)
			return;

		View view = postItemList.get(postPos).getView();
		view.setBackgroundResource(R.drawable.jvc_post_box_highlighted);
		scrollView.post(new SmoothScrollToViewRunnable(view));
	}

	private final Runnable swipeableViewerChangedView = new Runnable()
	{
		public void run()
		{
			final int screen = swipeableViewer.getCurrentScreen();
			final boolean hasPreviousText = topic.requestHasFirstPage() || topic.requestHasPreviousPage();
			final boolean hasNextText = topic.requestHasLastPage() || topic.requestHasNextPage();

			if(screen == 0 && hasPreviousText)
			{
				swipeableViewer.removeAllViews();
				swipeableViewer.addView(scrollView);
				swipeableViewer.addView(transitivePreviousText);
				swipeableViewer.setToScreen(1);

				if(topic.requestHasPreviousPage())
					topicPreviousPageButtonClick(previousPageButton);
				else
					topicFirstPageButtonClick(firstPageButton);
				for(PostItem item : postItemList)
				{
					item.emptyItem();
				}
				bottomNavigationLayout.setVisibility(View.GONE);
				lastSeparator.setVisibility(View.VISIBLE);
				replyLayout.setVisibility(View.GONE);

				swipeableViewer.postDelayed(new Runnable()
				{
					public void run()
					{
						swipeableViewer.snapToScreen(0);
					}
				}, 10);
			}
			else if(hasNextText && ((screen == 1 && !hasPreviousText) || (screen == 2 && hasPreviousText)))
			{
				swipeableViewer.removeAllViews();
				swipeableViewer.addView(transitiveNextText);
				swipeableViewer.addView(scrollView);
				swipeableViewer.setToScreen(0);

				if(topic.requestHasNextPage())
					topicNextPageButtonClick(nextPageButton);
				else
					topicLastPageButtonClick(lastPageButton);
				for(PostItem item : postItemList)
				{
					item.emptyItem();
				}
				bottomNavigationLayout.setVisibility(View.GONE);
				lastSeparator.setVisibility(View.VISIBLE);
				replyLayout.setVisibility(View.GONE);

				swipeableViewer.postDelayed(new Runnable()
				{
					public void run()
					{
						swipeableViewer.snapToScreen(1);
					}
				}, 10);
			}
		}
	};

	private final Runnable updatePostListRunnable = new Runnable()
	{
		public void run()
		{
			if(currentBrowsingMode == TopicActivity.AUTOMATIC_BROWSING)
			{
				refreshTopic();
				updatePostListHandler.removeCallbacks(this);
				if(prefAutomaticCheckingEnabled)
					updatePostListHandler.postDelayed(this, prefAutomaticBrowsingDelay);
			}
		}
	};

	public class JvcUpdatePostListTask extends AsyncTask<Void, Void, String>
	{
		@SuppressWarnings("unchecked")
		protected String doInBackground(Void... voids)
		{
			try
			{
				if(savedState && topic.getRequestError() == null)
				{
					ArrayList<JvcPost> postList = (ArrayList<JvcPost>) GlobalData.getOnce(savedBundle.getString("postListKey"));

					if(postList != null && !isCancelled())
					{
						currentPostList = postList;
						return null;
					}
				}

				boolean error = topic.requestPosts(currentListType, currentPageNumber);
				if(isCancelled())
					return null;

				if(error)
				{
					return topic.getRequestError();
				}
				else
				{
					currentPostList = topic.getRequestResult();

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
			catch(IOException e)
			{
				return getString(R.string.errorWhileLoadingTopic) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			for(PostItem item : postItemList)
			{
				item.getLoader().clearAndStopLoading();
			}

			errorLayout.setVisibility(View.GONE);
			replyLayoutHandler.setRefreshButtonState(false);
			goToPageButton.setEnabled(false);
			bottomGoToPageButton.setEnabled(false);
			swipeableViewer.setScrollingLocked(true);

			firstPageDrawable.setAlpha(100);
			previousPageDrawable.setAlpha(100);
			nextPageDrawable.setAlpha(100);
			lastPageDrawable.setAlpha(100);

			firstPageButton.setEnabled(false);
			bottomFirstPageButton.setEnabled(false);
			previousPageButton.setEnabled(false);
			bottomPreviousPageButton.setEnabled(false);
			nextPageButton.setEnabled(false);
			bottomNextPageButton.setEnabled(false);
			lastPageButton.setEnabled(false);
			bottomLastPageButton.setEnabled(false);

			if(currentBrowsingMode == TopicActivity.AUTOMATIC_BROWSING)
				topicNameTextView.setText(R.string.genericLoading);
			else if(currentBrowsingMode == TopicActivity.MANUAL_BROWSING)
				topicNameTextView.setText(getString(R.string.genericLoading) + "\n ");
		}

		protected void onPostExecute(String result)
		{
			swipeableViewer.removeAllViews();
			swipeableViewer.addView(scrollView);
			swipeableViewer.setToScreen(0);

			replyLayoutHandler.setRefreshButtonState(true);

			if(result == null)
			{
				if(replyLayout.getVisibility() == View.GONE)
				{
					replyLayout.setVisibility(View.VISIBLE);
				}
				lastSeparator.setVisibility(View.GONE);
				lastRequestSucceeded = true;
				UpdatedTopicData data = topic.getUpdatedTopicData();

				goToPageButton.setEnabled(true);
				bottomGoToPageButton.setEnabled(true);

				if(topic.isTopicLocked())
				{
					switchBrowsingButton.setEnabled(false);
				}
				else
				{
					if(!replyLayoutHandler.isReplying())
						switchBrowsingButton.setEnabled(true);
				}

				String text = "";
				if(currentBrowsingMode == TopicActivity.MANUAL_BROWSING)
				{
					text = String.format("\u00AB %s \u00BB\nPage %d / %d", topic.getExtraTopicName(), currentPageNumber, topic.getRequestPageCount());
					bottomNavigationLayout.setVisibility(View.VISIBLE);
				}
				else if(currentBrowsingMode == TopicActivity.AUTOMATIC_BROWSING)
				{
					text = String.format("\u00AB %s \u00BB", topic.getExtraTopicName());
				}

				topicNameTextView.setVisibility(View.VISIBLE);
				if(enableSmileys)
					topicNameTextView.setText(JvcTextSpanner.getSpannableTextFromSmileyNames(text, animateSmileys));
				else
					topicNameTextView.setText(text);

				if(topic.requestHasFirstPage())
				{
					firstPageButton.setEnabled(true);
					bottomFirstPageButton.setEnabled(true);
					firstPageDrawable.setAlpha(255);

					if(!topic.requestHasPreviousPage())
					{
						transitivePreviousText.setText(String.format("%s\n1 / %d", getString(R.string.transitiveFirstPage), topic.getRequestPageCount()));
						swipeableViewer.addView(transitivePreviousText, 0);
						swipeableViewer.setToScreen(1);
					}
				}
				if(topic.requestHasPreviousPage())
				{
					previousPageButton.setEnabled(true);
					bottomPreviousPageButton.setEnabled(true);
					previousPageDrawable.setAlpha(255);

					transitivePreviousText.setText(String.format("%s\n%d / %d", getString(R.string.transitivePreviousPage), currentPageNumber - 1, topic.getRequestPageCount()));
					swipeableViewer.addView(transitivePreviousText, 0);
					swipeableViewer.setToScreen(1);
				}
				if(topic.requestHasLastPage())
				{
					lastPageButton.setEnabled(true);
					bottomLastPageButton.setEnabled(true);
					lastPageDrawable.setAlpha(255);

					if(!topic.requestHasNextPage())
					{
						transitiveNextText.setText(String.format("%s\n%d / %d", getString(R.string.transitiveLastPage), currentPageNumber + 1, topic.getRequestPageCount()));
						swipeableViewer.addView(transitiveNextText);
					}
				}
				if(topic.requestHasNextPage())
				{
					nextPageButton.setEnabled(true);
					bottomNextPageButton.setEnabled(true);
					nextPageDrawable.setAlpha(255);

					transitiveNextText.setText(String.format("%s\n%d / %d", getString(R.string.transitiveNextPage), currentPageNumber + 1, topic.getRequestPageCount()));
					swipeableViewer.addView(transitiveNextText);
				}

				swipeableViewer.setScrollingLocked(false);
				swipeableViewer.post(new Runnable()
				{
					public void run()
					{
						swipeableViewer.setToScreen(swipeableViewer.getCurrentScreen());
					}
				});

				int postCount = currentPostList.size();
				for(int i = 0; i < 20; i++)
				{
					if(i < postCount)
					{
						postItemList.get(i).updateDataFromPost(currentPostList.get(i), false);
					}
					else
					{
						postItemList.get(i).emptyItem();
					}
				}

				if(!savedState)
				{
					if(isReplyTopic)
					{
						smoothScrollToPostId(topic.getReplyPostId());
						isReplyTopic = false;
					}
					else if(isUpdatedTopic && highlightPostPos != -1)
					{
						smoothScrollToPostPos(highlightPostPos);
						highlightPostPos = -1;
					}
					else if(currentBrowsingMode == TopicActivity.AUTOMATIC_BROWSING)
					{
						if(updatedTopicsHighlight)
						{
							int minIndex = findPostPosByPostId(currentPostList, data.getLastPostId());
							final int maxIndex = currentPostList.size();

							if(minIndex == -1) /* Last new post not found, highlight all posts */
							{
								for(int i = 0; i < maxIndex; i++)
								{
									postItemList.get(i).setHighlighted();
								}
							}
							else /* Last new post found, highlight concerned & next posts */
							{
								minIndex++;

								for(int i = minIndex; i < maxIndex; i++)
								{
									postItemList.get(i).setHighlighted();
								}

								final int realNewPostCount = maxIndex - minIndex;
								if(realNewPostCount > data.getNewPostCount()) /* There are more posts than expected */
								{
									final int diff = realNewPostCount - data.getNewPostCount();
									data.addAlreadyReadPosts(diff, currentPostList.get(maxIndex - 1).getPostId());
								}
							}

							scrollView.post(new SmoothScrollToViewRunnable(postItemList.get(postCount - 1).getView(), true));
							lastReadPost = currentPostList.get(postCount - 1).getPostId();
							updatedTopicsHighlight = false;
						}
						else
						{
							if(lastReadPost != -1)
							{
								int readPosts = 0;
								for(int i = 0; i < postCount; i++)
								{
									JvcPost post = currentPostList.get(i);
									PostItem postItem = postItemList.get(i);
									if(post.getPostId() > lastReadPost)
									{
										readPosts++;
										postItem.setHighlighted();
									}
								}

								if(data != null && isUpdatedTopic && readPosts > 0 && currentPostList.size() > 0)
								{
									JvcPost post = currentPostList.get(currentPostList.size() - 1);
									if(post != null)
									{
										long id = post.getPostId();
										if(id != 0)
										{
											data.addAlreadyReadPosts(readPosts, id);
										}
									}
								}
							}

							long lastReadPost = topic.getRequestLastPostId();
							if(lastReadPost > TopicActivity.this.lastReadPost)
								scrollView.post(new SmoothScrollToViewRunnable(postItemList.get(postCount - 1).getView(), true));
							TopicActivity.this.lastReadPost = lastReadPost;
						}
					}
				}
				else
				{
					scrollView.post(new SmoothScrollToYRunnable(savedBundle.getInt("scrollViewY")));
					savedState = false;
				}

				replyLayoutHandler.setReplyingEnabled(true);
				replyLayoutHandler.updateLockState(topic.isTopicLocked());

				if(isUpdatedTopic && data != null && data.isNew())
				{
					data.setAsRead();

					try
					{
						JvcUserData.updateUpdatedTopic(topic);
					}
					catch(IOException e)
					{
						NoticeDialog.show(TopicActivity.this, e.toString());
					}
				}

				invalidateOptionsMenu();
				startAnimatingDrawables(scrollView);
			}
			else
			{
				lastSeparator.setVisibility(View.VISIBLE);
				lastRequestSucceeded = false;
				replyLayoutHandler.setReplyingEnabled(false);
				for(int i = 0; i < 20; i++)
				{
					postItemList.get(i).emptyItem();
				}
				errorLayout.setVisibility(View.VISIBLE);
				errorTextView.setText(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT) ? MainApplication.handleHttpTimeout(TopicActivity.this) : result);
				topicNameTextView.setVisibility(View.GONE);
			}
		}
	}

	private class JvcAddToUpdatedTopicsTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;

		@Override
		protected String doInBackground(Void... voids)
		{
			try
			{
				boolean error = topic.requestPosts(JvcTopic.SHOW_POSTS_FROM_PAGE, topic.getRequestPageCount());
				if(error || isCancelled())
					return getString(R.string.errorWhileLoading) + " : " + topic.getRequestError();

				ArrayList<JvcPost> result = topic.getRequestResult();
				long totalCount = (topic.getRequestPageCount() - 1) * JvcUtils.POSTS_PER_PAGE + result.size();
				UpdatedTopicData data = new UpdatedTopicData(totalCount, result.get(result.size() - 1).getPostId());
				data.setLastError(null);
				topic.setUpdatedTopicData(data);

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
			dialog = new ProgressDialog(TopicActivity.this);
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.setMessage(getString(R.string.updatedTopicsAddingTopic));
			dialog.show();
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				try
				{
					JvcUserData.addToUpdatedTopics(topic);
					topicInUpdatedTopics = true;

					Toast.makeText(TopicActivity.this, getString(R.string.updatedTopicsAddedTopic), Toast.LENGTH_LONG).show();
				}
				catch(Exception e)
				{
					NoticeDialog.show(TopicActivity.this, getString(R.string.errorWhileLoading) + " : " + e.toString());
				}
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(TopicActivity.this);
			}
			else
			{
				NoticeDialog.show(TopicActivity.this, result);
			}
		}
	}

	private class JvcDeletePostTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;

		protected String doInBackground(Void... voids)
		{
			try
			{
				MainApplication app = (MainApplication) getApplicationContext();
				HttpClient client;
				if(topic.getForum().isForumJV())
					client = app.getHttpClient(MainApplication.JVFORUM_SESSION);
				else
					client = app.getHttpClient(MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet(currentContextMenuPost.getAdminDeleteUrl());
				client.execute(httpGet);

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
			dialog = ProgressDialog.show(TopicActivity.this, "", getString(R.string.deletingPost));
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				refreshTopic();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(TopicActivity.this);
			}
			else
			{
				NoticeDialog.show(TopicActivity.this, result);
			}
		}
	}

	private class JvcDoActionTask extends AsyncTask<String, Void, String>
	{
		private ProgressDialog dialog;

		@Override
		protected String doInBackground(String... params)
		{
			try
			{
				if(params == null || params.length == 0 || params[0] == null)
					return "(null)";

				MainApplication app = (MainApplication) getApplicationContext();
				HttpClient client;
				if(topic.getForum().isForumJV())
					client = app.getHttpClient(MainApplication.JVFORUM_SESSION);
				else
					client = app.getHttpClient(MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet(params[0]);
				client.execute(httpGet);

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

		@Override
		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(TopicActivity.this, "", getString(R.string.sendingRequest));
		}

		@Override
		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				refreshTopic();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(TopicActivity.this);
			}
			else
			{
				NoticeDialog.show(TopicActivity.this, result);
			}
		}
	}

	public class SmoothScrollToViewRunnable implements Runnable
	{
		private View view;
		private boolean bottom;

		public SmoothScrollToViewRunnable(View view)
		{
			this.view = view;
			bottom = false;
		}

		public SmoothScrollToViewRunnable(View view, boolean bottom)
		{
			this.view = view;
			this.bottom = bottom;
		}

		public void run()
		{
			if(bottom)
				scrollView.smoothScrollTo(0, view.getBottom());
			else
				scrollView.smoothScrollTo(0, view.getTop());
		}
	}

	public class SmoothScrollToYRunnable implements Runnable
	{
		private int y;

		public SmoothScrollToYRunnable(int y)
		{
			this.y = y;
		}

		public void run()
		{
			scrollView.smoothScrollTo(0, y);
		}
	}
}
