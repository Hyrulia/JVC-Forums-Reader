package com.forum.jvcreader;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.TableLayout.LayoutParams;
import com.forum.jvcreader.graphics.*;
import com.forum.jvcreader.jvc.*;
import com.forum.jvcreader.utils.AsyncTaskManager;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.widgets.SwipeableScrollViewer;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class ForumActivity extends JvcActivity
{
	private JvcForum forum;
	private ArrayList<MenuTopicItem> menuItemList;
	private boolean enableSmileys;

	private SwipeableScrollViewer swipeableViewer;
	private TextView transitivePreviousText;
	private TextView transitiveNextText;

	private ArrayList<JvcTopic> currentTopicList;
	private int currentListType;
	private int currentPageNumber;
	private String currentSearchString;
	private boolean hasLastRequestSucceeded = false;
	private boolean forumInFavorites = false;
	private boolean showNoelshackThumbnails;
	private boolean animateSmileys;

	private FrameLayout errorLayout;
	private TextView errorTextView;
	private TextView forumNameTextView;

	private Button searchButton;
	private Button refreshButton;

	private Button firstPageButton;
	private Button firstPageBottomButton;
	private SingleImageText firstPageSpan;
	private Button previousPageButton;
	private Button previousPageBottomButton;
	private SingleImageText previousPageSpan;
	private Button nextPageButton;
	private Button nextPageBottomButton;
	private SingleImageText nextPageSpan;

	private TextView createTopicTextView;
	private EditText topicSubjectEdit;
	private EditText topicContentEdit;
	private CheckBox topicPostAsMobileCheckBox;
	private CheckBox topicIncludeSignatureCheckBox;

	private SmileyGridDialog smileyDialog;
	private ScrollView scrollView;
	private JvcTopic currentContextTopic;

	private boolean savedState = false;
	private Bundle savedBundle = null;

	private MenuItem optItemFirstPage;
	private MenuItem optItemPreviousPage;
	private MenuItem optItemNextPage;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forum);

		if(savedInstanceState != null)
		{
			savedState = savedInstanceState.getBoolean("savedInstanceState");
			savedBundle = savedInstanceState;
		}

        /* Assign UI elements */
		swipeableViewer = (SwipeableScrollViewer) findViewById(R.id.forumSwipeableScrollViewer);
		scrollView = (ScrollView) findViewById(R.id.forumScrollView);
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.forumLayout);
		errorLayout = (FrameLayout) findViewById(R.id.forumErrorLayout);
		errorTextView = (TextView) findViewById(R.id.forumErrorTextView);
		forumNameTextView = (TextView) findViewById(R.id.forumNameTextView);
		searchButton = (Button) findViewById(R.id.forumSearchButton);
		refreshButton = (Button) findViewById(R.id.forumRefreshButton);
		createTopicTextView = (TextView) findViewById(R.id.forumCreateTopicTextView);
		topicSubjectEdit = (EditText) findViewById(R.id.forumReplyFormSubjectEditText);
		topicContentEdit = (EditText) findViewById(R.id.forumReplyFormContentEditText);
		if(JvcUserData.getBoolean(JvcUserData.PREF_SHOW_CHARACTER_COUNTER, JvcUserData.DEFAULT_SHOW_CHARACTER_COUNTER))
		{
			topicContentEdit.addTextChangedListener(characterCounterWatcher);
		}
		topicPostAsMobileCheckBox = (CheckBox) findViewById(R.id.forumReplyFormPostAsMobileCheckBox);
		topicIncludeSignatureCheckBox = (CheckBox) findViewById(R.id.forumReplyFormIncludeSignatureCheckBox);
		topicIncludeSignatureCheckBox.setChecked(false);
		if(JvcUserData.getBoolean(JvcUserData.PREF_USE_SIGNATURE, JvcUserData.DEFAULT_USE_SIGNATURE))
		{
			if(JvcUserData.getBoolean(JvcUserData.PREF_INCLUDE_SIGNATURE_BY_DEFAULT, JvcUserData.DEFAULT_INCLUDE_SIGNATURE_BY_DEFAULT))
			{
				topicIncludeSignatureCheckBox.setChecked(true);
			}
		}
		else
		{
			topicIncludeSignatureCheckBox.setVisibility(View.GONE);
		}
		showNoelshackThumbnails = JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS);
		animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);

    	/* Initialize swipeable viewer */
		swipeableViewer.removeAllViews();
		swipeableViewer.setSnapToScreenRunnable(swipeableViewerChangedView);
		swipeableViewer.addView(scrollView);
		swipeableViewer.setToScreen(0);
		transitivePreviousText = getTransitiveTextView();
		transitiveNextText = getTransitiveTextView();

    	/* Initialize page spanned buttons */
		firstPageSpan = new SingleImageText(getString(R.string.genericStart), R.raw.page_debut);
		firstPageButton = (Button) findViewById(R.id.forumFirstPageButton);
		firstPageButton.setText(firstPageSpan.getText());
		firstPageBottomButton = (Button) findViewById(R.id.forumFirstPageBottomButton);
		firstPageBottomButton.setText(firstPageSpan.getText());

		previousPageSpan = new SingleImageText(getString(R.string.genericPrevious), R.raw.page_prec);
		previousPageButton = (Button) findViewById(R.id.forumPreviousPageButton);
		previousPageButton.setText(previousPageSpan.getText());
		previousPageBottomButton = (Button) findViewById(R.id.forumPreviousPageBottomButton);
		previousPageBottomButton.setText(previousPageSpan.getText());

		nextPageSpan = new SingleImageText(getString(R.string.genericNext), R.raw.page_suiv);
		nextPageButton = (Button) findViewById(R.id.forumNextPageButton);
		nextPageButton.setText(nextPageSpan.getText());
		nextPageBottomButton = (Button) findViewById(R.id.forumNextPageBottomButton);
		nextPageBottomButton.setText(nextPageSpan.getText());
        
        /* Retrieve the JvcForum */
		if(!savedState)
		{
			forum = (JvcForum) GlobalData.get("forumFromPreviousActivity");
			if(forum == null)
			{
				Log.e("JvcForumsReader", "forum is null");
				Log.e("JvcForumsReader", "Finishing ForumActivity...");
				finish();
				return;
			}

			Intent starter = getIntent();
			currentListType = starter.getIntExtra("com.forum.jvcreader.SearchType", JvcForum.SHOW_TOPIC_LIST);
			currentPageNumber = starter.getIntExtra("com.forum.jvcreader.PageNumber", 1);
			currentSearchString = starter.getStringExtra("com.forum.jvcreader.SearchValue");

			if(currentListType != JvcForum.SHOW_TOPIC_LIST)
				searchButton.setText(R.string.genericStop);
		}
		else
		{
			forum = (JvcForum) GlobalData.getOnce(savedBundle.getString("forumKey"));
			if(forum == null)
				forum = JvcForum.restoreInRecoveryMode(this, savedBundle);

			currentListType = savedBundle.getInt("currentListType");
			currentPageNumber = savedBundle.getInt("currentPageNumber");
			currentSearchString = savedBundle.getString("currentSearchString");
			topicSubjectEdit.setText(savedBundle.getString("topicSubjectEdit"));
			topicContentEdit.setText(savedBundle.getString("topicContentEdit"));
			topicPostAsMobileCheckBox.setChecked(savedBundle.getBoolean("topicPostAsMobileCheckBox"));
			topicIncludeSignatureCheckBox.setChecked(savedBundle.getBoolean("topicIncludeSignatureCheckBox"));
			scrollView.postDelayed(new Runnable()
			{
				public void run()
				{
					scrollView.smoothScrollTo(0, savedBundle.getInt("scrollViewY"));
				}
			}, 100);
		}

		if(forum.isForumJV())
		{
			topicPostAsMobileCheckBox.setEnabled(false);
		}
		else
		{
			if(JvcUserData.getBoolean(JvcUserData.PREF_POST_AS_MOBILE_BY_DEFAULT, JvcUserData.DEFAULT_POST_AS_MOBILE_BY_DEFAULT))
			{
				topicPostAsMobileCheckBox.setChecked(true);
			}
		}
        
        /* Create the 25-item list */
		enableSmileys = JvcUserData.getBoolean(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES, JvcUserData.DEFAULT_SMILEYS_IN_TOPIC_TITLES);
		boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_TOPICS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_TOPICS);
		menuItemList = new ArrayList<MenuTopicItem>();
		for(int i = 0; i < 25; i++)
		{
			MenuTopicItem menuItem = new MenuTopicItem(this, enableSmileys, animateSmileys, jvcLike);
			menuItem.setItemOnClickListener(new TopicItemOnClickListener(i));
			menuItemList.add(menuItem);
			if(i == 0)
				menuItem.setSeparatorVisibility(View.GONE);
			final View view = menuItem.getView();
			view.setId(200 + i);
			this.registerForContextMenu(view);
			linearLayout.addView(view);
		}

		forumInFavorites = JvcUserData.isForumInFavorites(forum);

		menuItemList.get(0).setTopicName("...");
		updatePostList();
	}
	
	/* Activity lifecycle */

	public void onSaveInstanceState(Bundle bundle)
	{
		long ms = System.currentTimeMillis();
		bundle.putBoolean("savedInstanceState", true);

		String forumKey = ms + "_forum";
		bundle.putString("forumKey", forumKey);
		GlobalData.set(forumKey, forum);

		String topicListKey = ms + "_topic_list";
		bundle.putString("topicListKey", topicListKey);
		GlobalData.set(topicListKey, currentTopicList);

		bundle.putInt("currentListType", currentListType);
		bundle.putInt("currentPageNumber", currentPageNumber);
		bundle.putString("currentSearchString", currentSearchString);
		bundle.putString("topicSubjectEdit", topicSubjectEdit.getText().toString());
		bundle.putString("topicContentEdit", topicContentEdit.getText().toString());
		bundle.putBoolean("topicPostAsMobileCheckBox", topicPostAsMobileCheckBox.isChecked());
		bundle.putBoolean("topicIncludeSignatureCheckBox", topicIncludeSignatureCheckBox.isChecked());
		bundle.putInt("scrollViewY", scrollView.getScrollY());
		
		/* Recovery mode */
		forum.saveInRecoveryMode(bundle);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if(firstPageButton.isEnabled())
			firstPageSpan.setImageAlpha(255);
		else
			firstPageSpan.setImageAlpha(100);
		if(previousPageButton.isEnabled())
			previousPageSpan.setImageAlpha(255);
		else
			previousPageSpan.setImageAlpha(100);
		if(nextPageButton.isEnabled())
			nextPageSpan.setImageAlpha(255);
		else
			nextPageSpan.setImageAlpha(100);
	}

	@Override
	public void onBackPressed()
	{
		if(topicSubjectEdit.length() > 0 || topicContentEdit.length() > 0)
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
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_SEARCH)
		{
			forumSearchButtonClick(searchButton);

			return true;
		}

		return super.onKeyUp(keyCode, event);
	}

    /* Options menu */

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.forum_activity, menu);

		optItemFirstPage = menu.findItem(R.id.optionsMenuFirstPage);
		optItemPreviousPage = menu.findItem(R.id.optionsMenuPreviousPage);
		optItemNextPage = menu.findItem(R.id.optionsMenuNextPage);

		if(forum.getAdminKickInterfaceUrl() == null || forum.getAdminKickInterfaceUrl().length() == 0)
			menu.removeItem(R.id.optionsMenuSeeKickInterface);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		MenuItem favAction = menu.findItem(R.id.optionsMenuFavAction);
		if(forumInFavorites)
			favAction.setTitle(R.string.removeFromFavoriteForums);
		else
			favAction.setTitle(R.string.addToFavoriteForums);

		optItemFirstPage.setEnabled(firstPageButton.isEnabled());
		optItemPreviousPage.setEnabled(previousPageButton.isEnabled());
		optItemNextPage.setEnabled(nextPageButton.isEnabled());

		if(forum.getAdminKickInterfaceUrl() == null || forum.getAdminKickInterfaceUrl().length() == 0)
			menu.removeItem(R.id.optionsMenuSeeKickInterface);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.optionsMenuFirstPage:
				forumFirstPageButtonClick(firstPageButton);
				break;

			case R.id.optionsMenuPreviousPage:
				forumPreviousPageButtonClick(previousPageButton);
				break;

			case R.id.optionsMenuNextPage:
				forumNextPageButtonClick(nextPageButton);
				break;

			case R.id.optionsMenuRefresh:
				forumRefreshButtonClick(refreshButton);
				break;

			case R.id.optionsMenuFavAction:
				try
				{
					if(forumInFavorites)
					{
						JvcUserData.removeFromFavoriteForums(forum);
					}
					else
					{
						JvcUserData.addToFavoriteForums(forum);
					}

					forumInFavorites = !forumInFavorites;
				}
				catch(Exception e)
				{
					NoticeDialog.show(this, getString(R.string.errorWhileLoadingUserPreferences) + " : " + e.toString());
				}
				break;

			case R.id.optionsMenuCopyForumLink:
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(forum.getRequestRedirectedUrl());
				Toast.makeText(this, R.string.forumLinkCopied, Toast.LENGTH_SHORT).show();
				break;

			case R.id.optionsMenuSeeKickInterface:
				Intent intent = new Intent(this, KickInterfaceActivity.class);
				intent.putExtra("com.forum.jvcreader.KickInterfaceUrl", forum.getAdminKickInterfaceUrl());
				intent.putExtra("com.forum.jvcreader.KickInterfaceIsForumJV", forum.isForumJV());
				if(forum.isForumJV())
				{
					intent.putExtra("com.forum.jvcreader.ForumJVBaseUrl", "http://" + forum.getForumJVSubdomain() + ".forumjv.com");
				}
				startActivity(intent);
				break;

			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}
	
	/* Context menu */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		int id = v.getId();
		if(id >= 200 && id < 225) /* Arbitrary IDs */
		{
			id -= 200;
			if(id < currentTopicList.size())
			{
				currentContextTopic = currentTopicList.get(id);
				String deleteUrl = currentContextTopic.getAdminDeleteUrl();
				if(deleteUrl != null && deleteUrl.length() > 0)
				{
					MenuInflater inflater = getMenuInflater();
					if(inflater != null)
						inflater.inflate(R.menu.topic_admin_context_menu, menu);
				}
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		if(currentContextTopic == null)
			return true;

		switch(item.getItemId())
		{
			case R.id.contextMenuDeleteTopic:
				NoticeDialog.showYesNo(this, getString(R.string.dialogDeleteTopic), new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								new JvcDeleteTopicTask().execute();
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
				return true;
		}

		return true;
	}
	
	/* UI Events */

	public void forumSearchButtonClick(View view)
	{
		if(currentListType == JvcForum.SHOW_TOPIC_LIST)
		{
			final TopicSearchDialog dialog = new TopicSearchDialog(this);

			dialog.setSearchMyTopicsButtonListener(new OnClickListener()
			{
				public void onClick(View view)
				{
					searchButton.setText(R.string.genericStop);
					currentListType = JvcForum.SEARCH_PSEUDOS;
					currentPageNumber = 1;
					currentSearchString = ((MainApplication) getApplicationContext()).getJvcPseudo();
					updatePostList();
					dialog.dismiss();
				}
			});

			dialog.setSearchButtonListener(new TopicSearchDialogOnClickListener(dialog)
			{
				public void onClick(View view)
				{
					String searchString = this.dialog.getSearchString();
					if(searchString != null && searchString.length() > 0)
					{
						searchButton.setText(R.string.genericStop);
						currentListType = this.dialog.getListType();
						currentPageNumber = 1;
						currentSearchString = this.dialog.getSearchString();
						updatePostList();
						this.dialog.dismiss();
					}
				}
			});

			dialog.show();
		}
		else
		{
			searchButton.setText(R.string.genericSearch);
			currentListType = JvcForum.SHOW_TOPIC_LIST;
			currentPageNumber = 1;
			currentSearchString = null;
			updatePostList();
		}
	}

	public void forumFirstPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			currentPageNumber = 1;
			updatePostList();
		}
	}

	public void forumPreviousPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			currentPageNumber--;
			updatePostList();
		}
	}

	public void forumNextPageButtonClick(View view)
	{
		if(view.isEnabled())
		{
			currentPageNumber++;
			updatePostList();
		}
	}

	public void forumRefreshButtonClick(View view)
	{
		if(view.isEnabled())
		{
			updatePostList();
		}
	}

	private final TextWatcher characterCounterWatcher = new TextWatcher()
	{
		@Override
		public void afterTextChanged(Editable s)
		{
			final int count = s.length();
			String str;
			if(count > 1)
				str = String.format(getString(R.string.characterCounterX), count);
			else
				str = String.format(getString(R.string.characterCounterSingular), count);
			createTopicTextView.setText(getString(R.string.createTopicSeparator) + " (" + str + ")");
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

	public void forumReplyFormSmileysButtonClick(View view)
	{
		smileyDialog = new SmileyGridDialog(this, new OnClickListener()
		{
			public void onClick(View view)
			{
				final int pos = topicContentEdit.getSelectionStart();
				String append = "";

				if(topicContentEdit.length() > 0 && pos > 0)
				{
					char lastChar = topicContentEdit.getText().charAt(pos - 1);
					if(lastChar != '\n' && lastChar != ' ')
						append += ' ';
				}

				append += smileyDialog.getSelectedSmiley();

				if(pos < topicContentEdit.length())
				{
					char nextChar = topicContentEdit.getText().charAt(pos);
					if(nextChar != '\n' && nextChar != ' ')
						append += ' ';
				}

				topicContentEdit.getText().replace(pos, pos, append);

				smileyDialog.dismiss();
				smileyDialog = null;
			}
		});

		smileyDialog.show();
	}

	public void forumReplyFormPreviewButtonClick(View view)
	{
		if(topicSubjectEdit.length() > 0)
		{
			if(topicContentEdit.length() > 0)
			{
				MainApplication appContext = (MainApplication) getApplicationContext();
				PostItem item = new PostItem(this, true, showNoelshackThumbnails, animateSmileys);
				String date = new SimpleDateFormat("d MMMM y \u00E0 HH:mm:ss", Locale.FRANCE).format(new Date());
				JvcPost post = new JvcPost(null, 0, getPostTextWithSignature() + "\n", appContext.getJvcPseudo(), false, topicPostAsMobileCheckBox.isChecked(), 1, date, null, null);
				item.updateDataFromPost(post, true);

				final int tenDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
				LinearLayout layout = new LinearLayout(this);
				layout.setOrientation(LinearLayout.VERTICAL);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setBackgroundColor(Color.WHITE);

				TextView tv = new TextView(this);
				LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.bottomMargin = params.topMargin = tenDp;
				tv.setLayoutParams(params);
				tv.setTextSize(16);
				tv.setGravity(Gravity.CENTER);
				tv.setTextColor(getResources().getColor(R.color.jvcTopicName));
				tv.setTypeface(null, Typeface.BOLD);
				String title = "\u00AB " + topicSubjectEdit.getText().toString() + " \u00BB\nPage 1 / 1";
				if(enableSmileys)
					tv.setText(JvcTextSpanner.getSpannableTextFromSmileyNames(title, JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS)));
				else
					tv.setText(title);
				layout.addView(tv);

				layout.addView(item.getView());

				Dialog previewDialog = new Dialog(this, R.style.FullScreenNoTitleDialogTheme);
				previewDialog.setContentView(layout);
				previewDialog.setCancelable(true);
				previewDialog.setOnDismissListener(new OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						setAnimationView(scrollView);
					}
				});
				previewDialog.show();
				setAnimationView(layout);
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

	public void forumReplyFormSendButtonClick(View view)
	{
		if(topicSubjectEdit.length() > 0)
		{
			if(topicContentEdit.length() > 0)
			{
				JvcTopic topic = new JvcTopic(forum, topicSubjectEdit.getText().toString(), getPostTextWithSignature(), topicPostAsMobileCheckBox.isChecked() && !forum.isForumJV());
				new JvcSendTopicTask().execute(topic);
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
		if(topicIncludeSignatureCheckBox.isChecked())
		{
			String s;

			if(JvcUserData.getBoolean(JvcUserData.PREF_SIGNATURE_AT_START, JvcUserData.DEFAULT_SIGNATURE_AT_START))
			{
				s = JvcUserData.getString(JvcUserData.PREF_SIGNATURE, JvcUserData.DEFAULT_SIGNATURE) + "\n\n";
				s += topicContentEdit.getText().toString();
			}
			else
			{
				s = topicContentEdit.getText().toString() + "\n\n";
				s += JvcUserData.getString(JvcUserData.PREF_SIGNATURE, JvcUserData.DEFAULT_SIGNATURE);
			}

			return s;
		}
		else
		{
			return topicContentEdit.getText().toString();
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
			final int screen = swipeableViewer.getCurrentScreen();
			final boolean hasPreviousText = forum.requestHasFirstPage() || forum.requestHasPreviousPage();

			if(screen == 0 && hasPreviousText)
			{
				swipeableViewer.removeAllViews();
				swipeableViewer.addView(scrollView);
				swipeableViewer.addView(transitivePreviousText);
				swipeableViewer.setToScreen(1);

				if(forum.requestHasPreviousPage())
					forumPreviousPageButtonClick(previousPageButton);
				else
					forumFirstPageButtonClick(firstPageButton);
				for(MenuTopicItem item : menuItemList)
				{
					item.emptyItem();
				}
				menuItemList.get(0).setTopicName("...");

				swipeableViewer.postDelayed(new Runnable()
				{
					public void run()
					{
						swipeableViewer.snapToScreen(0);
					}
				}, 10);
			}
			else if(forum.requestHasNextPage() && ((screen == 1 && !hasPreviousText) || (screen == 2 && hasPreviousText)))
			{
				swipeableViewer.removeAllViews();
				swipeableViewer.addView(transitiveNextText);
				swipeableViewer.addView(scrollView);
				swipeableViewer.setToScreen(0);

				forumNextPageButtonClick(nextPageButton);
				for(MenuTopicItem item : menuItemList)
				{
					item.emptyItem();
				}
				menuItemList.get(0).setTopicName("...");

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

	public abstract class TopicSearchDialogOnClickListener implements OnClickListener
	{
		protected TopicSearchDialog dialog;

		public TopicSearchDialogOnClickListener(TopicSearchDialog dialog)
		{
			this.dialog = dialog;
		}
	}

	public class TopicItemOnClickListener implements OnClickListener
	{
		private int itemId;

		public TopicItemOnClickListener(int id)
		{
			itemId = id;
		}

		@Override
		public void onClick(View view)
		{
			if(hasLastRequestSucceeded && itemId < currentTopicList.size())
			{
				topicSubjectEdit.clearFocus(); /* Avoid focus of EditText */
				topicContentEdit.clearFocus(); /* When resuming the Activity */

				JvcTopic topic = currentTopicList.get(itemId);
				topic.invalidateTopicName();
				GlobalData.set("topicFromPreviousActivity", topic);
				startActivity(new Intent(ForumActivity.this, TopicActivity.class));
			}
		}
	}

	private void updatePostList()
	{
		JvcUpdateTopicListTask task = new JvcUpdateTopicListTask();
		registerTask(task);
		task.execute();
	}

	public class JvcUpdateTopicListTask extends AsyncTask<Void, Void, String>
	{
		@SuppressWarnings("unchecked")
		protected String doInBackground(Void... voids)
		{
			try
			{
				if(savedState && forum.getRequestError() == null)
				{
					savedState = false;
					ArrayList<JvcTopic> topicList = (ArrayList<JvcTopic>) GlobalData.getOnce(savedBundle.getString("topicListKey"));

					if(topicList != null)
					{
						if(!isCancelled())
							currentTopicList = topicList;
						return null;
					}
				}

				boolean error = forum.requestTopicList(currentListType, currentPageNumber, currentSearchString);
				if(error)
				{
					return forum.getRequestError();
				}
				else if(isCancelled() && AsyncTaskManager.isActivityRegistered(ForumActivity.this))
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							refreshButton.setText(R.string.genericRefresh);
							refreshButton.setEnabled(true);
						}
					});
				}

				currentTopicList = forum.getRequestResult();
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
			catch(IOException e)
			{
				return getString(R.string.errorWhileLoadingTopicList) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			scrollView.scrollTo(0, 0);

			hasLastRequestSucceeded = false;
			swipeableViewer.setScrollingLocked(true);

			errorLayout.setVisibility(View.GONE);
			refreshButton.setText(R.string.genericLoading);
			refreshButton.setEnabled(false);

			firstPageButton.setEnabled(false);
			firstPageBottomButton.setEnabled(false);
			firstPageSpan.setImageAlpha(100);

			previousPageButton.setEnabled(false);
			previousPageBottomButton.setEnabled(false);
			previousPageSpan.setImageAlpha(100);

			nextPageButton.setEnabled(false);
			nextPageBottomButton.setEnabled(false);
			nextPageSpan.setImageAlpha(100);
		}

		protected void onPostExecute(String result)
		{
			swipeableViewer.removeAllViews();
			swipeableViewer.addView(scrollView);
			swipeableViewer.setToScreen(0);

			refreshButton.setText(R.string.genericRefresh);
			refreshButton.setEnabled(true);

			forumNameTextView.setText("\u00AB " + forum.getForumName() + " \u00BB");

			if(result == null)
			{
				hasLastRequestSucceeded = true;

				if(forum.requestHasFirstPage())
				{
					firstPageButton.setEnabled(true);
					firstPageBottomButton.setEnabled(true);
					firstPageSpan.setImageAlpha(255);

					if(!forum.requestHasPreviousPage())
					{
						transitivePreviousText.setText(getString(R.string.transitiveFirstPage));
						swipeableViewer.addView(transitivePreviousText, 0);
						swipeableViewer.setToScreen(1);
					}
				}
				if(forum.requestHasPreviousPage())
				{
					previousPageButton.setEnabled(true);
					previousPageBottomButton.setEnabled(true);
					previousPageSpan.setImageAlpha(255);

					transitivePreviousText.setText(String.format("%s\n\nPage %d", getString(R.string.transitivePreviousPage), currentPageNumber - 1));
					swipeableViewer.addView(transitivePreviousText, 0);
					swipeableViewer.setToScreen(1);
				}
				if(forum.requestHasNextPage())
				{
					nextPageButton.setEnabled(true);
					nextPageBottomButton.setEnabled(true);
					nextPageSpan.setImageAlpha(255);

					transitiveNextText.setText(String.format("%s\n\nPage %d", getString(R.string.transitiveNextPage), currentPageNumber + 1));
					swipeableViewer.addView(transitiveNextText);
				}

				swipeableViewer.setScrollingLocked(false);
				swipeableViewer.post(new Runnable() /* Necessary for orientation switching */
				{
					public void run()
					{
						swipeableViewer.setToScreen(swipeableViewer.getCurrentScreen());
					}
				});

				for(int i = 0; i < 25; i++)
				{
					if(i < currentTopicList.size())
					{
						menuItemList.get(i).updateDataFromTopic(currentTopicList.get(i));
					}
					else
					{
						menuItemList.get(i).emptyItem();
					}
				}

				invalidateOptionsMenu();
				startAnimatingDrawables(scrollView);
			}
			else
			{
				errorLayout.setVisibility(View.VISIBLE);
				errorTextView.setText(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT) ? MainApplication.handleHttpTimeout(ForumActivity.this) : Html.fromHtml(result));

				for(int i = 0; i < 25; i++)
				{
					menuItemList.get(i).emptyItem();
				}
			}
		}
	}

	private class JvcSendTopicTask extends AsyncTask<JvcTopic, Void, Integer>
	{
		private ProgressDialog dialog;
		private CaptchaDialog captcha;

		private JvcTopic topic;
		private String exceptionResult;

		private String customMessage = getString(R.string.publishingTopic);

		public void setCustomMessage(String message)
		{
			customMessage = message;
		}

		protected Integer doInBackground(JvcTopic... topics)
		{
			try
			{
				topic = topics[0];
				return topic.publishOnForum();
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
			catch(Exception e)
			{
				exceptionResult = e.toString();
				return -1;
			}
		}

		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(ForumActivity.this, getString(R.string.genericSending), customMessage);
			dialog.setCancelable(true);
			dialog.setOnDismissListener(new OnDismissListener()
			{
				@Override
				public void onDismiss(DialogInterface dialog)
				{
					cancel(true);
				}
			});
		}

		protected void onPostExecute(Integer result)
		{
			try
			{
				dialog.dismiss();
				dialog = null;
			}
			catch(IllegalArgumentException e)
			{
				Log.w("JvcForumsReader", "Couldn't dismiss SendTopicTask dialog");
			}

			switch(result)
			{
				case -1:
					if(exceptionResult.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
					{
						MainApplication.handleHttpTimeout(ForumActivity.this);
					}
					else
					{
						NoticeDialog.show(ForumActivity.this, exceptionResult);
					}
					break;

				case JvcPost.REQUEST_OK:
					JvcActivity.hideKeyboard(ForumActivity.this);
					topicSubjectEdit.setText("");
					topicContentEdit.setText("");
					updatePostList();
					scrollView.postDelayed(new Runnable()
					{
						public void run()
						{
							topicSubjectEdit.clearFocus();
							topicContentEdit.clearFocus();
							scrollView.smoothScrollTo(0, 0);
						}
					}, 250);
					break;

				case JvcPost.REQUEST_CAPTCHA_REQUIRED:
					captcha = new CaptchaDialog(ForumActivity.this, topic.getRequestCaptchaUrl());
					captcha.setDialogTitle(topic.getRequestError());
					captcha.setSubmitButtonListener(new OnClickListener()
					{
						public void onClick(View view)
						{
							topic.prepareCaptcha(captcha.getCaptchaString());
							new JvcSendTopicTask().execute(topic);
							captcha.dismiss();
							captcha = null;
						}
					});
					captcha.show();
					break;

				case JvcPost.REQUEST_RETRY:
					JvcSendTopicTask task = new JvcSendTopicTask();
					task.setCustomMessage(getString(R.string.formExpiryRetry));
					task.execute(topic);
					break;

				case JvcPost.REQUEST_ERROR:
				case JvcPost.REQUEST_ERROR_FROM_JVC:
					NoticeDialog.show(ForumActivity.this, topic.getRequestError());
					break;
			}
		}
	}

	private class JvcDeleteTopicTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;

		protected String doInBackground(Void... voids)
		{
			try
			{
				MainApplication app = (MainApplication) getApplicationContext();
				HttpClient client;
				if(forum.isForumJV())
					client = app.getHttpClient(MainApplication.JVFORUM_SESSION);
				else
					client = app.getHttpClient(MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet(currentContextTopic.getAdminDeleteUrl());
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
			dialog = ProgressDialog.show(ForumActivity.this, "", getString(R.string.deletingTopic));
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				updatePostList();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(ForumActivity.this);
			}
			else
			{
				NoticeDialog.show(ForumActivity.this, result);
			}
		}
	}
}
