package com.forum.jvcreader;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.forum.jvcreader.graphics.MenuSeparator;
import com.forum.jvcreader.graphics.MenuTopicItem;
import com.forum.jvcreader.graphics.MenuTopicItem.PmTopicItemOnClickListener;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.*;
import com.forum.jvcreader.utils.AsyncTaskManager;
import com.forum.jvcreader.utils.GlobalData;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class UpdatedTopicsActivity extends JvcActivity
{
	private LinearLayout layout;
	private ArrayList<JvcTopic> topics;
	private final ArrayList<MenuTopicItem> topicItemList = new ArrayList<MenuTopicItem>();
	private final HashMap<MenuTopicItem, JvcTopic> selectedTopics = new HashMap<MenuTopicItem, JvcTopic>();
	private View topicEditingView;

	private boolean savedState = false;
	private Bundle savedBundle = null;

	private boolean isUpdatingTopics = false;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.updated_topics);

		if(savedInstanceState != null && savedInstanceState.getBoolean("savedInstanceState"))
		{
			savedState = true;
			savedBundle = savedInstanceState;
		}

		layout = (LinearLayout) findViewById(R.id.updatedTopicsLayout);
		topicEditingView = findViewById(R.id.updatedTopicsTopicEditingView);
		topicEditingView.setVisibility(View.GONE);

		loadUpdatedTopics();

		if(savedInstanceState == null && topics != null && topics.size() > 0 && !getIntent().getBooleanExtra("com.forum.jvcreader.DisableStartCheck", false))
		{
			RefreshUpdatedTopicsTask task = new RefreshUpdatedTopicsTask();
			registerTask(task);
			task.execute();
		}

		startAnimatingDrawables(layout);
	}

	public void onResume()
	{
		super.onResume();
		loadUpdatedTopics();
	}

	public void onSaveInstanceState(Bundle bundle)
	{
		bundle.putBoolean("savedInstanceState", true);
		bundle.putBoolean("isEditing", topicEditingView.getVisibility() == View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.updated_topics_activity, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		if(isUpdatingTopics)
		{
			menu.findItem(R.id.optionsMenuCheckTopics).setVisible(false);
			menu.findItem(R.id.optionsMenuEditTopics).setVisible(false);
			menu.findItem(R.id.optionsMenuAbortChecking).setVisible(true);
		}
		else
		{
			menu.findItem(R.id.optionsMenuCheckTopics).setVisible(true);
			menu.findItem(R.id.optionsMenuEditTopics).setVisible(true);
			menu.findItem(R.id.optionsMenuAbortChecking).setVisible(false);
		}

		return topics != null && topics.size() > 0 && topicEditingView.getVisibility() == View.GONE; /* Show menu only if there are topics and user is not editing them */
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.optionsMenuCheckTopics:
				RefreshUpdatedTopicsTask task = new RefreshUpdatedTopicsTask();
				registerTask(task);
				task.execute();
				break;

			case R.id.optionsMenuEditTopics:
				if(topicEditingView.getVisibility() == View.GONE)
				{
					topicEditingView.setVisibility(View.VISIBLE);
					selectedTopics.clear();
					for(MenuTopicItem menuItem : topicItemList)
					{
						menuItem.setCheckboxVisible(true);
					}
				}
				break;

			case R.id.optionsMenuAbortChecking:
				if(isUpdatingTopics)
				{
					AsyncTask currentTask = AsyncTaskManager.getCurrentTask(this, RefreshUpdatedTopicsTask.class);
					if(currentTask != null)
						currentTask.cancel(true);
				}
				break;

			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private void loadUpdatedTopics()
	{
		selectedTopics.clear();
		if(savedState)
		{
			if(savedBundle.getBoolean("isEditing"))
				topicEditingView.setVisibility(View.VISIBLE);
			savedState = false;
		}

		layout.removeAllViews();
		topicItemList.clear();
		final boolean enableSmileys = JvcUserData.getBoolean(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES, JvcUserData.DEFAULT_SMILEYS_IN_TOPIC_TITLES);
		final boolean animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);
		final boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_TOPICS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_TOPICS);
		final boolean isEditing = topicEditingView.getVisibility() == View.VISIBLE;
		topics = JvcUserData.getUpdatedTopics();
		if(topics != null && topics.size() > 0)
		{
			MenuSeparator separator = new MenuSeparator(R.layout.menu_separator, getLayoutInflater());
			separator.setText(getString(R.string.prefsUpdatedTopics));
			layout.addView(separator.getView());

			for(int i = 0; i < topics.size(); i++)
			{
				MenuTopicItem item = new MenuTopicItem(this, enableSmileys, animateSmileys, jvcLike);
				item.updateFromUpdatedTopic(topics.get(i));
				item.setCheckboxVisible(isEditing);
				if(selectedTopics.containsValue(topics.get(i)))
					item.setCheckboxChecked(true);
				item.setItemOnClickListener(new UpdatedTopicOnClickListener(topics.get(i)));
				item.setPmTopicItemOnClickListener(new PmTopicItemOnClickListener(i)
				{
					@Override
					public void onClick(boolean checked)
					{
						if(checked)
							selectedTopics.put(topicItemList.get(id), topics.get(id));
						else
							selectedTopics.remove(topicItemList.get(id));
					}
				});
				if(i == 0)
					item.setSeparatorVisibility(View.GONE);
				layout.addView(item.getView());
				topicItemList.add(item);
			}
		}
		else
		{
			TextView tv = new TextView(this);
			tv.setText(R.string.noUpdatedTopics);
			tv.setTextSize(25);
			tv.setGravity(Gravity.CENTER);
			layout.addView(tv);
		}
	}

	public void updatedTopicsTickAllButtonClick(View view)
	{
		for(int i = 0; i < topicItemList.size(); i++)
		{
			MenuTopicItem menuItem = topicItemList.get(i);
			menuItem.setCheckboxChecked(true);
			selectedTopics.put(menuItem, topics.get(i));
		}
	}

	public void updatedTopicsUntickAllButtonClick(View view)
	{
		selectedTopics.clear();
		for(MenuTopicItem menuItem : topicItemList)
		{
			menuItem.setCheckboxChecked(false);
		}
	}

	public void updatedTopicsMarkAsReadButtonClick(View view) throws IOException
	{
		for(MenuTopicItem menuItem : selectedTopics.keySet())
		{
			final JvcTopic topic = selectedTopics.get(menuItem);
			topic.getUpdatedTopicData().setAsRead();
			menuItem.updateFromUpdatedTopic(topic);
			menuItem.setCheckboxChecked(false);
			JvcUserData.updateUpdatedTopic(topic);
		}
		selectedTopics.clear();
	}

	public void updatedTopicsRemoveButtonClick(View view)
	{
		if(selectedTopics.size() > 0)
		{
			NoticeDialog.showYesNo(this, String.format(getString(R.string.updatedTopicsRemoveComfirm), selectedTopics.size()), new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();

							try
							{
								for(MenuTopicItem menuItem : selectedTopics.keySet())
								{
									JvcUserData.removeFromUpdatedTopics(selectedTopics.get(menuItem));
								}
							}
							catch(Exception e)
							{
								NoticeDialog.show(UpdatedTopicsActivity.this, getString(R.string.errorWhileLoading) + " : " + e.toString());
							}
							finally
							{
								selectedTopics.clear();
								loadUpdatedTopics();
								if(topics == null || topics.size() == 0)
								{
									for(MenuTopicItem menuItem : topicItemList)
									{
										menuItem.setCheckboxVisible(false);
									}

									topicEditingView.setVisibility(View.GONE);
								}
							}
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
	}

	public void updatedTopicsStopEditingTopicsButtonClick(View view)
	{
		for(MenuTopicItem menuItem : topicItemList)
		{
			menuItem.setCheckboxVisible(false);
		}

		topicEditingView.setVisibility(View.GONE);
	}

	private class RefreshUpdatedTopicsTask extends AsyncTask<Void, Integer, String>
	{
		private int id = 0;

		protected String doInBackground(Void... voids)
		{
			try
			{
				UpdatedTopicsManager manager = UpdatedTopicsManager.getInstance();
				manager.updateTopics(UpdatedTopicsActivity.this, topics, new Runnable()
				{
					public void run()
					{
						publishProgress(id);
						id++;
					}
				}, this);

				if(isCancelled() && AsyncTaskManager.isActivityRegistered(UpdatedTopicsActivity.this))
				{
					isUpdatingTopics = false;
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							invalidateOptionsMenu();
							loadUpdatedTopics();
						}
					});
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
			catch(IOException e)
			{
				return getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			Toast.makeText(UpdatedTopicsActivity.this, R.string.genericLoading, Toast.LENGTH_SHORT).show();
			isUpdatingTopics = true;
		}

		protected void onProgressUpdate(Integer... integers)
		{
			final int id = integers[0];
			if(id < topics.size())
			{
				layout.getChildAt(id + 1).setBackgroundColor(getResources().getColor(R.color.jvcTopicClicked));
			}
			if(id > 0 && id - 1 < topics.size())
			{
				final JvcTopic topic = topics.get(id - 1);
				topicItemList.get(id - 1).updateFromUpdatedTopic(topic);
			}
		}

		protected void onPostExecute(String result)
		{
			isUpdatingTopics = false;
			invalidateOptionsMenu();

			if(result != null)
			{
				if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
				{
					MainApplication.handleHttpTimeout(UpdatedTopicsActivity.this);
				}
				else
				{
					NoticeDialog.show(UpdatedTopicsActivity.this, result);
				}
			}
		}
	}

	private class UpdatedTopicOnClickListener implements OnClickListener
	{
		private JvcTopic topic;

		public UpdatedTopicOnClickListener(JvcTopic topic)
		{
			this.topic = topic;
		}

		@Override
		public void onClick(View view)
		{
			UpdatedTopicData data = topic.getUpdatedTopicData();

			if(data.isAccessLocked())
				return;

			Intent intent = new Intent(UpdatedTopicsActivity.this, TopicActivity.class);
			if(data.getLastError() == null)
				intent.putExtra("com.forum.jvcreader.IsUpdatedTopic", true);
			topic.getForum().setContext(getApplicationContext());
			GlobalData.set("topicFromPreviousActivity", topic);
			startActivity(intent);
		}
	}
}
