package com.forum.jvcreader;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.forum.jvcreader.graphics.MenuTopicItem;
import com.forum.jvcreader.graphics.MenuTopicItem.PmTopicItemOnClickListener;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcPmList;
import com.forum.jvcreader.jvc.JvcPmList.JvcPmListPaginationData;
import com.forum.jvcreader.jvc.JvcPmTopic;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.widgets.SwipeableScrollViewer;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class PmBoxActivity extends JvcActivity
{
	private ArrayList<MenuTopicItem> menuItemList;
	private ArrayList<JvcPmTopic> pmTopicList = null;

	private final HashMap<JvcPmTopic, Boolean> selectedTopics = new HashMap<JvcPmTopic, Boolean>();
	private JvcPmListPaginationData currentPaginationData = null;

	private JvcPmList pmList;
	private int currentPage;

	private SwipeableScrollViewer swipeableViewer;
	private ScrollView mainView;
	private TextView transitivePreviousText;
	private TextView transitiveNextText;

	private TextView headerTextView;
	private Button refreshButton;
	private Button removeButton;

	private boolean savedState = false;
	private Bundle savedBundle = null;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pm_box);

		if(savedInstanceState != null)
		{
			savedState = savedInstanceState.getBoolean("savedInstanceState");
			savedBundle = savedInstanceState;
		}

        /* Assign UI Elements & PM List */
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.pmReceivedTopicListLayout);
		swipeableViewer = (SwipeableScrollViewer) findViewById(R.id.pmReceivedSwipeableScrollViewer);
		mainView = (ScrollView) findViewById(R.id.pmReceivedMainView);
		headerTextView = (TextView) findViewById(R.id.pmReceivedHeaderTextView);
		refreshButton = (Button) findViewById(R.id.pmReceivedRefreshButton);
		removeButton = (Button) findViewById(R.id.pmReceivedRemoveButton);
		removeButton.setEnabled(false);

        /* Initialize PM List */
		if(!savedState)
		{
			if(!getIntent().hasExtra("com.forum.jvcreader.BoxType"))
			{
				Log.e("JvcForumsReader", "Box type is null");
				Log.e("JvcForumsReader", "Finishing PmBoxActivity...");
				finish();
				return;
			}

			pmList = new JvcPmList(this, getIntent().getIntExtra("com.forum.jvcreader.BoxType", JvcPmList.RECEIVED_MESSAGES));
			currentPage = 1;
		}
		else
		{
			pmList = (JvcPmList) GlobalData.getOnce(savedBundle.getString("pmListKey"));
			if(pmList == null)
				pmList = JvcPmList.restoreInRecoveryMode(this, savedBundle);

			currentPage = savedBundle.getInt("currentPage");
			mainView.postDelayed(new Runnable()
			{
				public void run()
				{
					mainView.smoothScrollTo(0, savedBundle.getInt("scrollViewY"));
				}
			}, 100);
		}

        /* Initialize swipeable viewer */
		swipeableViewer.setSnapToScreenRunnable(swipeableViewerChangedView);
		swipeableViewer.setInitialPosition(0);
		transitivePreviousText = getTransitiveTextView();
		transitiveNextText = getTransitiveTextView();
        
    	/* Menu Item List */
		boolean enableSmileys = JvcUserData.getBoolean(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES, JvcUserData.DEFAULT_SMILEYS_IN_TOPIC_TITLES);
		boolean animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);
		boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_TOPICS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_TOPICS);
		menuItemList = new ArrayList<MenuTopicItem>();
		View view;
		for(int i = 0; i < 25; i++)
		{
			MenuTopicItem menuItem = new MenuTopicItem(this, enableSmileys, animateSmileys, jvcLike);
			menuItem.setItemOnClickListener(new TopicItemOnClickListener(i));
			menuItem.setPmTopicItemOnClickListener(new TopicCheckBoxOnClickListener(i));
			menuItemList.add(menuItem);
			if(i == 0)
				menuItem.setSeparatorVisibility(View.GONE);
			view = menuItem.getView();
			view.setId(i + 200);
			linearLayout.addView(view);
		}
        
        /* Load topics */
		menuItemList.get(0).setTopicName("...");
		loadPmList();
	}
	
	/* Activity lifecycle */

	public void onSaveInstanceState(Bundle bundle)
	{
		long ms = System.currentTimeMillis();
		bundle.putBoolean("savedInstanceState", true);

		String pmListKey = ms + "_pm_list";
		bundle.putString("pmListKey", pmListKey);
		GlobalData.set(pmListKey, pmList);

		String pmTopicListKey = ms + "_pm_topic_list";
		bundle.putString("pmTopicListKey", pmTopicListKey);
		GlobalData.set(pmTopicListKey, pmTopicList);

		bundle.putInt("currentPage", currentPage);
		bundle.putInt("scrollViewY", mainView.getScrollY());
		
		/* Recovery mode */
		pmList.saveInRecoveryMode(bundle);
	}

	public void onResume()
	{
		super.onResume();

		if(pmTopicList != null)
		{
			final int size = pmTopicList.size();
			if(size > 0)
			{
				for(int i = 0; i < size; i++)
				{
					JvcPmTopic topic = pmTopicList.get(i);
					menuItemList.get(i).updateDataFromPmTopic(topic, selectedTopics.containsKey(topic));
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.pm_box_activity, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		if(currentPaginationData == null)
		{
			menu.findItem(R.id.optionsMenuPreviousPage).setEnabled(false);
			menu.findItem(R.id.optionsMenuNextPage).setEnabled(false);
		}
		else
		{
			if(!currentPaginationData.hasPreviousPage)
				menu.findItem(R.id.optionsMenuPreviousPage).setEnabled(false);
			else
				menu.findItem(R.id.optionsMenuPreviousPage).setEnabled(true);
			if(!currentPaginationData.hasNextPage)
				menu.findItem(R.id.optionsMenuNextPage).setEnabled(false);
			else
				menu.findItem(R.id.optionsMenuNextPage).setEnabled(true);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.optionsMenuTickAll:
				if(pmTopicList != null && pmTopicList.size() > 0)
				{
					for(int i = 0; i < pmTopicList.size(); i++)
					{
						JvcPmTopic topic = pmTopicList.get(i);
						selectedTopics.put(topic, true);
						menuItemList.get(i).updateDataFromPmTopic(topic, true);
					}
					removeButton.setEnabled(true);
				}
				break;

			case R.id.optionsMenuUntickAll:
				selectedTopics.clear();
				for(int i = 0; i < pmTopicList.size(); i++)
				{
					menuItemList.get(i).updateDataFromPmTopic(pmTopicList.get(i), false);
				}
				removeButton.setEnabled(false);
				break;

			case R.id.optionsMenuPreviousPage:
				if(currentPaginationData.hasPreviousPage)
				{
					currentPage--;
					loadPmList();
				}
				break;

			case R.id.optionsMenuNextPage:
				if(currentPaginationData.hasNextPage)
				{
					currentPage++;
					loadPmList();
				}
				break;

			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private TextView getTransitiveTextView()
	{
		TextView tv = (TextView) getLayoutInflater().inflate(R.layout.transitive_text_view, null);

		return tv;
	}

	public void pmReceivedRefreshButtonClick(View view)
	{
		loadPmList();
	}

	public void pmReceivedRemoveButtonClick(View view)
	{
		final int count = selectedTopics.size();
		NoticeDialog.showYesNo(this, String.format(getString(R.string.confirmPmTopicDelete), count), new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						JvcRemoveSelectedTopicsTask task = new JvcRemoveSelectedTopicsTask();
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

	private final Runnable swipeableViewerChangedView = new Runnable()
	{
		public void run()
		{
			final int screen = swipeableViewer.getCurrentScreen();
			final boolean hasPreviousText = currentPaginationData.hasPreviousPage;
			final boolean hasNextText = currentPaginationData.hasNextPage;

			if(screen == 0 && hasPreviousText)
			{
				swipeableViewer.removeAllViews();
				swipeableViewer.addView(mainView);
				swipeableViewer.addView(transitivePreviousText);
				swipeableViewer.setToScreen(1);

				currentPage--;
				loadPmList();
				for(MenuTopicItem item : menuItemList)
				{
					item.emptyItem();
				}

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
				swipeableViewer.addView(mainView);
				swipeableViewer.setToScreen(0);

				currentPage++;
				loadPmList();
				for(MenuTopicItem item : menuItemList)
				{
					item.emptyItem();
				}

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

	private class TopicItemOnClickListener implements OnClickListener
	{
		private int id;

		public TopicItemOnClickListener(int id)
		{
			this.id = id;
		}

		public void onClick(View view)
		{
			if(pmTopicList != null && id < pmTopicList.size())
			{
				GlobalData.set("pmTopicFromPreviousActivity", pmTopicList.get(id));
				startActivity(new Intent(PmBoxActivity.this, PmTopicActivity.class));
			}
		}
	}

	private class TopicCheckBoxOnClickListener extends PmTopicItemOnClickListener
	{
		public TopicCheckBoxOnClickListener(int id)
		{
			super(id);
		}

		@Override
		public void onClick(boolean checked)
		{
			if(checked)
			{
				if(selectedTopics.size() == 0)
					removeButton.setEnabled(true);
				menuItemList.get(id).updateDataFromPmTopic(pmTopicList.get(id), true);
				selectedTopics.put(pmTopicList.get(id), true);
			}
			else
			{
				selectedTopics.remove(pmTopicList.get(id));
				if(selectedTopics.size() == 0)
					removeButton.setEnabled(false);
				menuItemList.get(id).updateDataFromPmTopic(pmTopicList.get(id), false);
			}
		}
	}

	private void loadPmList()
	{
		JvcLoadPmListTask task = new JvcLoadPmListTask();
		registerTask(task);
		task.execute();
	}

	private class JvcLoadPmListTask extends AsyncTask<Void, Void, String>
	{
		@SuppressWarnings("unchecked")
		protected String doInBackground(Void... voids)
		{
			try
			{
				if(isCancelled())
					return null;

				if(savedState && pmList.getRequestError() == null)
				{
					savedState = false;
					currentPaginationData = pmList.getPaginationData();
					pmTopicList = (ArrayList<JvcPmTopic>) GlobalData.getOnce(savedBundle.getString("pmTopicListKey"));
					if(pmTopicList != null)
						return null;
				}

				pmTopicList = pmList.requestList(currentPage, this);
				if(pmTopicList == null || isCancelled())
					return getString(R.string.errorWhileLoading) + " : " + pmList.getRequestError();
				else if(pmTopicList.size() == 0)
					return getString(R.string.noPmTopic);

				currentPaginationData = pmList.getPaginationData();
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
				return getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			swipeableViewer.setScrollingLocked(true);
			mainView.scrollTo(0, 0);

			headerTextView.setText(R.string.genericLoading);
			refreshButton.setEnabled(false);
			refreshButton.setText(R.string.genericLoading);
			selectedTopics.clear();
			removeButton.setEnabled(false);
		}

		protected void onPostExecute(String result)
		{
			swipeableViewer.removeAllViews();
			swipeableViewer.addView(mainView);
			swipeableViewer.setToScreen(0);

			refreshButton.setEnabled(true);
			refreshButton.setText(R.string.genericRefresh);

			if(result == null)
			{
				headerTextView.setText(String.format(getString(R.string.pmBoxHeaderText), currentPaginationData.rangeStart, currentPaginationData.rangeEnd, currentPaginationData.messagesCount));

				if(currentPaginationData.hasPreviousPage)
				{
					transitivePreviousText.setText(String.format("%s\nPage %d", getString(R.string.transitivePreviousPage), currentPage - 1));
					swipeableViewer.addView(transitivePreviousText, 0);
					swipeableViewer.setToScreen(1);
				}
				if(currentPaginationData.hasNextPage)
				{
					transitiveNextText.setText(String.format("%s\nPage %d", getString(R.string.transitiveNextPage), currentPage + 1));
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

				final int size = pmTopicList.size();
				for(int i = 0; i < 25; i++)
				{
					if(i < size)
					{
						JvcPmTopic topic = pmTopicList.get(i);
						final Boolean selected = selectedTopics.get(i);
						menuItemList.get(i).updateDataFromPmTopic(topic, selected == null ? false : selected);
					}
					else
					{
						menuItemList.get(i).setCheckboxChecked(false);
						menuItemList.get(i).emptyItem();
					}
				}

				startAnimatingDrawables(mainView);
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				headerTextView.setText(MainApplication.handleHttpTimeout(PmBoxActivity.this));
				for(MenuTopicItem item : menuItemList)
				{
					item.emptyItem();
				}
			}
			else
			{
				if(pmTopicList != null && pmTopicList.size() == 0)
					headerTextView.setText(R.string.pmNoMessages);
				else
					headerTextView.setText(R.string.operationFailed);
				for(MenuTopicItem item : menuItemList)
				{
					item.emptyItem();
				}
				NoticeDialog.show(PmBoxActivity.this, result);
			}
		}
	}

	private class JvcRemoveSelectedTopicsTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;

		@Override
		protected String doInBackground(Void... voids)
		{
			try
			{
				if(isCancelled())
					return null;
				pmList.removeTopics(currentPage, selectedTopics);

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
				return getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			dialog = new ProgressDialog(PmBoxActivity.this);
			dialog.setCancelable(true);
			dialog.setOnDismissListener(new OnDismissListener()
			{
				@Override
				public void onDismiss(DialogInterface dialog)
				{
					cancel(true);
				}
			});
			dialog.setMessage(getString(R.string.removingTopics));
			dialog.show();
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				int unreadTopics = 0;
				for(JvcPmTopic topic : selectedTopics.keySet())
				{
					if(!topic.isTopicRead())
					{
						unreadTopics++;
					}
				}
				if(unreadTopics > 0)
				{
					MainApplication app = (MainApplication) getApplicationContext();
					final int finalCount = app.getUnreadPmCount() - unreadTopics;
					if(finalCount >= 0)
						app.setUnreadPmCount(finalCount);
				}

				loadPmList();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(PmBoxActivity.this);
			}
			else
			{
				NoticeDialog.show(PmBoxActivity.this, result);
			}
		}
	}
}
