package com.forum.jvcreader;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.graphics.PageSelectionDialog;
import com.forum.jvcreader.graphics.PostItem;
import com.forum.jvcreader.jvc.JvcArchivedTopic;
import com.forum.jvcreader.jvc.JvcPost;
import com.forum.jvcreader.jvc.JvcTextSpanner;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.widgets.SwipeableScrollViewer;

import java.io.IOException;
import java.util.ArrayList;

public class ArchivedTopicActivity extends JvcActivity
{
	private JvcArchivedTopic topic;
	private ArrayList<JvcPost> currentPostList;
	private ArrayList<PostItem> postItemList;

	private SwipeableScrollViewer swipeableViewer;
	private TextView transitivePreviousText;
	private TextView transitiveNextText;

	private LinearLayout viewLayout;
	private View lastSeparator;
	private ScrollView scrollView;
	private PageSelectionDialog pageSelectionDialog;

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
	private LinearLayout bottomNavigationLayout;

	private boolean enableSmileys;
	private boolean animateSmileys;

	private TextView topicNameTextView;

	private int currentPageNumber;

	private boolean savedState = false;
	private Bundle savedBundle = null;
	private JvcPost currentContextMenuPost;

	private Menu optionsMenu = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.archived_topic);

		if(savedInstanceState != null)
		{
			savedState = savedInstanceState.getBoolean("savedInstanceState");
			savedBundle = savedInstanceState;
		}

		enableSmileys = JvcUserData.getBoolean(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES, JvcUserData.DEFAULT_SMILEYS_IN_TOPIC_TITLES);
		animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);

		/* Assign UI elements */
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.topicLayout);
		viewLayout = (LinearLayout) findViewById(R.id.topicViewLayout);
		swipeableViewer = (SwipeableScrollViewer) findViewById(R.id.topicSwipeableScrollViewer);
		scrollView = (ScrollView) findViewById(R.id.topicScrollView);
		firstPageButton = (ImageButton) findViewById(R.id.topicFirstPageButton);
		previousPageButton = (ImageButton) findViewById(R.id.topicPreviousPageButton);
		nextPageButton = (ImageButton) findViewById(R.id.topicNextPageButton);
		lastPageButton = (ImageButton) findViewById(R.id.topicLastPageButton);
		bottomFirstPageButton = (ImageButton) findViewById(R.id.topicBottomFirstPageButton);
		bottomPreviousPageButton = (ImageButton) findViewById(R.id.topicBottomPreviousPageButton);
		bottomNextPageButton = (ImageButton) findViewById(R.id.topicBottomNextPageButton);
		bottomLastPageButton = (ImageButton) findViewById(R.id.topicBottomLastPageButton);
		topicNameTextView = (TextView) findViewById(R.id.topicNameTextView);
		lastSeparator = findViewById(R.id.topicLastSeparator);
		bottomNavigationLayout = (LinearLayout) findViewById(R.id.topicBottomNavigationLayout);
		bottomNavigationLayout.setVisibility(View.GONE);

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
			topic = (JvcArchivedTopic) GlobalData.get("archivedTopicFromPreviousActivity");
		}
		else
		{
			String topicKey = savedBundle.getString("topicKey");
			topic = (JvcArchivedTopic) GlobalData.getOnce(topicKey);
		}

		if(topic == null)
		{
			Log.e("JvcForumsReader", "topic is null !");
			Log.e("JvcForumsReader", "Finishing ArchivedTopicActivity...");
			finish();
			return;
		}

		/* Create post items */
		final boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_POSTS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_POSTS);
		postItemList = new ArrayList<PostItem>();
		for(int i = 0; i < topic.getPostCountPerPage(); i++)
		{
			PostItem post = new PostItem(this, jvcLike, false, animateSmileys);
			View view = post.getView();
			post.getTitleBarLayout().setId(i + 500);
			registerForContextMenu(post.getTitleBarLayout());
			linearLayout.addView(view);
			postItemList.add(post);
		}

		if(!savedState)
		{
			currentPageNumber = 1;
		}
		else
		{
			currentPageNumber = savedBundle.getInt("currentPageNumber");
		}

		/* Show post list */
		refreshTopic();
	}

	public void onSaveInstanceState(Bundle bundle)
	{
		bundle.putBoolean("savedInstanceState", true);

		String topicKey = System.currentTimeMillis() + "_topic";
		bundle.putString("topicKey", topicKey);
		GlobalData.set(topicKey, topic);

		bundle.putInt("currentPageNumber", currentPageNumber);
		bundle.putInt("scrollViewY", scrollView.getScrollY());
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
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info)
	{
		int id = view.getId();
		if(id >= 500 && id <= (500 + topic.getPostCountPerPage()))
		{
			currentContextMenuPost = currentPostList.get(id - 500);
			getMenuInflater().inflate(R.menu.post_context_menu, menu);
			menu.setHeaderTitle(String.format(getString(R.string.contextMenuPostHeader), currentContextMenuPost.getPostPseudo()));
			if(JvcUserData.isPseudoInBlacklist(currentContextMenuPost.getPostPseudo()))
			{
				menu.findItem(R.id.contextMenuIgnorePseudo).setTitle(R.string.contextMenuStopIgnorePseudo);
			}

			menu.removeItem(R.id.contextMenuDeletePost);
			menu.removeItem(R.id.contextMenuWarnAdmin);
			menu.removeItem(R.id.contextMenuQuoteAuthor);
			menu.removeItem(R.id.contextMenuQuotePost);
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
			}

			return true;
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.archived_topic_activity, menu);

		optionsMenu = menu;

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.optionsMenuRemoveArchivedTopic:
				NoticeDialog.showYesNo(this, getString(R.string.removeArchivedTopicConfirmation), new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
								dialogInterface.dismiss();
								try
								{
									JvcUserData.removeFromArchivedTopics(topic);
								}
								catch(IOException e)
								{
									NoticeDialog.show(ArchivedTopicActivity.this, getString(R.string.errorWhileLoadingUserPreferences) + " : " + e.toString());
								}
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
				break;

			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
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
			currentPageNumber = topic.getArchivedPageCount();
			refreshTopic();
		}
	}

	public void topicGoToPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			pageSelectionDialog = new PageSelectionDialog(this, topic.getArchivedPageCount());

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

	private TextView getTransitiveTextView()
	{
		return (TextView) getLayoutInflater().inflate(R.layout.transitive_text_view, null);
	}

	private final Runnable swipeableViewerChangedView = new Runnable()
	{
		public void run()
		{
			final int pageCount = topic.getArchivedPageCount();
			final int screen = swipeableViewer.getCurrentScreen();
			final boolean hasPreviousText = currentPageNumber > 1;
			final boolean hasNextText = currentPageNumber < pageCount;

			if(screen == 0 && hasPreviousText)
			{
				swipeableViewer.removeAllViews();
				swipeableViewer.addView(viewLayout);
				swipeableViewer.addView(transitivePreviousText);
				swipeableViewer.setToScreen(1);

				for(PostItem item : postItemList)
				{
					item.emptyItem();
				}
				bottomNavigationLayout.setVisibility(View.GONE);
				lastSeparator.setVisibility(View.VISIBLE);

				swipeableViewer.postDelayed(new Runnable()
				{
					public void run()
					{
						swipeableViewer.setScrollingLocked(true);
						swipeableViewer.snapToScreen(0, new Runnable()
						{
							@Override
							public void run()
							{
								if(currentPageNumber > 2)
									topicPreviousPageButtonClick(previousPageButton);
								else
									topicFirstPageButtonClick(firstPageButton);
								swipeableViewer.setScrollingLocked(false);
							}
						});
					}
				}, 10);
			}
			else if(hasNextText && ((screen == 1 && !hasPreviousText) || (screen == 2 && hasPreviousText)))
			{
				swipeableViewer.removeAllViews();
				swipeableViewer.addView(transitiveNextText);
				swipeableViewer.addView(viewLayout);
				swipeableViewer.setToScreen(0);

				for(PostItem item : postItemList)
				{
					item.emptyItem();
				}
				bottomNavigationLayout.setVisibility(View.GONE);
				lastSeparator.setVisibility(View.VISIBLE);

				swipeableViewer.postDelayed(new Runnable()
				{
					public void run()
					{
						swipeableViewer.setScrollingLocked(true);
						swipeableViewer.snapToScreen(1, new Runnable()
						{
							@Override
							public void run()
							{
								if(currentPageNumber < (topic.getArchivedPageCount() - 1))
									topicNextPageButtonClick(nextPageButton);
								else
									topicLastPageButtonClick(lastPageButton);
								swipeableViewer.setScrollingLocked(false);
							}
						});
					}
				}, 10);
			}
		}
	};

	private void refreshTopic()
	{
		bottomNavigationLayout.setVisibility(View.VISIBLE);

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

		try
		{
			currentPostList = topic.getPostsFromPage(currentPageNumber);
		}
		catch(ClassNotFoundException e)
		{
			Log.e("Jvc", "Error while fetching posts in ArchivedTopicActivity : " + e.toString());
			finish();
			return;
		}
		catch(IOException e)
		{
			Log.e("Jvc", "Error while fetching posts in ArchivedTopicActivity : " + e.toString());
			finish();
			return;
		}

		swipeableViewer.removeAllViews();
		swipeableViewer.addView(viewLayout);
		swipeableViewer.setToScreen(0);
		lastSeparator.setVisibility(View.GONE);

		String text = String.format("\u00AB %s \u00BB\nPage %d / %d", topic.getExtraTopicName(), currentPageNumber, topic.getArchivedPageCount());
		topicNameTextView.setVisibility(View.VISIBLE);
		if(enableSmileys)
			topicNameTextView.setText(JvcTextSpanner.getSpannableTextFromSmileyNames(text, animateSmileys));
		else
			topicNameTextView.setText(text);

		if(currentPageNumber > 1)
		{
			firstPageButton.setEnabled(true);
			bottomFirstPageButton.setEnabled(true);
			firstPageDrawable.setAlpha(255);

			if(currentPageNumber == 2)
			{
				transitivePreviousText.setText(String.format("%s\n1 / %d", getString(R.string.transitiveFirstPage), topic.getArchivedPageCount()));
				swipeableViewer.addView(transitivePreviousText, 0);
				swipeableViewer.setToScreen(1);
			}
		}
		if(currentPageNumber > 2)
		{
			previousPageButton.setEnabled(true);
			bottomPreviousPageButton.setEnabled(true);
			previousPageDrawable.setAlpha(255);

			transitivePreviousText.setText(String.format("%s\n%d / %d", getString(R.string.transitivePreviousPage), currentPageNumber - 1, topic.getArchivedPageCount()));
			swipeableViewer.addView(transitivePreviousText, 0);
			swipeableViewer.setToScreen(1);
		}
		if(currentPageNumber < topic.getArchivedPageCount())
		{
			lastPageButton.setEnabled(true);
			bottomLastPageButton.setEnabled(true);
			lastPageDrawable.setAlpha(255);

			if(currentPageNumber == (topic.getArchivedPageCount() - 1))
			{
				transitiveNextText.setText(String.format("%s\n%d / %d", getString(R.string.transitiveLastPage), currentPageNumber + 1, topic.getArchivedPageCount()));
				swipeableViewer.addView(transitiveNextText);
			}
		}
		if(currentPageNumber < (topic.getArchivedPageCount() - 1))
		{
			nextPageButton.setEnabled(true);
			bottomNextPageButton.setEnabled(true);
			nextPageDrawable.setAlpha(255);

			transitiveNextText.setText(String.format("%s\n%d / %d", getString(R.string.transitiveNextPage), currentPageNumber + 1, topic.getArchivedPageCount()));
			swipeableViewer.addView(transitiveNextText);
		}

		swipeableViewer.post(new Runnable()
		{
			public void run()
			{
				swipeableViewer.setToScreen(swipeableViewer.getCurrentScreen());
			}
		});

		int postCount = currentPostList.size();
		for(int i = 0; i < topic.getPostCountPerPage(); i++)
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

		if(savedState)
		{
			scrollView.post(new SmoothScrollToYRunnable(savedBundle.getInt("scrollViewY")));
			savedState = false;
		}

		if(optionsMenu != null)
			onPrepareOptionsMenu(optionsMenu);
		startAnimatingDrawables(scrollView);
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
