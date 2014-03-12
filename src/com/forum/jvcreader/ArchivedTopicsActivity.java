package com.forum.jvcreader;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.forum.jvcreader.graphics.MenuForumItem;
import com.forum.jvcreader.graphics.MenuSeparator;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcArchivedTopic;
import com.forum.jvcreader.jvc.JvcTopic;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.InternalStorageHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchivedTopicsActivity extends JvcActivity
{
	private LinearLayout linearLayout;
	private Button deleteUnusedFilesButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forum_list); /* Actual layout used to show topics */
		linearLayout = (LinearLayout) findViewById(R.id.forumListLayout);
		updateTopics();
	}

	@Override
	public void onResume() /* If a topic have been un-favorited */
	{
		super.onResume();
		linearLayout.removeAllViews();
		updateTopics();
		invalidateOptionsMenu();
	}

	private void updateTopics()
	{
		linearLayout.removeAllViews();
		LayoutInflater inflater = getLayoutInflater();

		ArrayList<MenuForumItem> menuItems = new ArrayList<MenuForumItem>();
		ArrayList<JvcArchivedTopic> topicList = JvcUserData.getArchivedTopics();

		if(topicList == null)
		{
			TextView tv = new TextView(this);
			tv.setText(R.string.noArchivedTopics);
			tv.setTextSize(25);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			params.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
			tv.setLayoutParams(params);
			tv.setGravity(Gravity.CENTER);
			linearLayout.addView(tv);
		}
		else
		{
			MenuSeparator separator = new MenuSeparator(R.layout.menu_separator, getLayoutInflater());
			separator.setText(getString(R.string.openArchivedTopics).toUpperCase());
			linearLayout.addView(separator.getView());

			int size = topicList.size();
			for(int i = 0; i < size; i++)
			{
				JvcArchivedTopic topic = topicList.get(i);
				MenuForumItem menuItem = new MenuForumItem(inflater);

				menuItems.add(menuItem);
				menuItem.setItemText(topic.getExtraTopicName());
				menuItem.setItemOnClickListener(new TopicListItemOnClickListener(topic));
				if(i == 0)
					menuItem.setSeparatorVisibility(View.GONE);

				linearLayout.addView(menuItem.getView());
			}
		}

		deleteUnusedFilesButton = new Button(this);
		deleteUnusedFilesButton.setText(R.string.deleteUnusedFiles);
		deleteUnusedFilesButton.setOnClickListener(archivedTopicsButtonListener);
		linearLayout.addView(deleteUnusedFilesButton);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.sort_options_menu, menu);

		return JvcUserData.getArchivedTopics() != null;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		return JvcUserData.getArchivedTopics() != null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		try
		{
			ArrayList<JvcArchivedTopic> topics = JvcUserData.getArchivedTopics();

			switch(item.getItemId())
			{
				case R.id.optionsMenuSortByName:
					Collections.sort(topics, new JvcTopic.NameAscendingComparator());
					JvcUserData.saveArchivedTopics(topics);
					updateTopics();
					break;

				case R.id.optionsMenuSortById:
					Collections.sort(topics, new JvcTopic.TopicIdAscendingComparator());
					JvcUserData.saveArchivedTopics(topics);
					updateTopics();
					break;

				case R.id.optionsMenuInverseList:
					Collections.reverse(topics);
					JvcUserData.saveArchivedTopics(topics);
					updateTopics();
					break;

				default:
					return super.onOptionsItemSelected(item);
			}
		}
		catch(IOException e)
		{
			NoticeDialog.show(this, e.toString());
		}

		return true;
	}

	private final OnClickListener archivedTopicsButtonListener = new OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			final HashMap<Long, Boolean> archivedTopicIdsMap = new HashMap<Long, Boolean>();
			ArrayList<JvcArchivedTopic> topicList = JvcUserData.getArchivedTopics();
			if(topicList != null && topicList.size() > 0)
			{
				for(JvcArchivedTopic topic : topicList)
				{
					archivedTopicIdsMap.put(topic.getTopicId(), true);
				}
			}

			final Pattern filePattern = Pattern.compile("ArchivedTopic_([0-9]+)_[0-9]+\\.jvc");
			File dir = getFilesDir();

			String fileList[] = dir.list(new FilenameFilter()
			{
				@Override
				public boolean accept(File file, String s)
				{
					Matcher m = filePattern.matcher(s);
					if(m.find())
					{
						return !archivedTopicIdsMap.containsKey(Long.parseLong(m.group(1)));
					}

					return false;
				}
			});

			String toastMessage;

			if(fileList != null && fileList.length > 0)
			{
				for(int i = 0; i < fileList.length; i++)
				{
					InternalStorageHelper.deleteFile(ArchivedTopicsActivity.this, fileList[i]);
				}

				toastMessage = String.format(getString(R.string.deletedXFiles), fileList.length);
			}
			else
			{
				toastMessage = getString(R.string.noFileToDelete);
			}

			Toast.makeText(ArchivedTopicsActivity.this, toastMessage, Toast.LENGTH_LONG).show();
		}
	};

	private class TopicListItemOnClickListener implements OnClickListener
	{
		private JvcArchivedTopic topic;

		public TopicListItemOnClickListener(JvcArchivedTopic topic)
		{
			this.topic = topic;
		}

		@Override
		public void onClick(View view)
		{
			topic.getForum().setContext(getApplicationContext());
			GlobalData.set("archivedTopicFromPreviousActivity", topic);
			startActivity(new Intent(ArchivedTopicsActivity.this, ArchivedTopicActivity.class));
		}
	}
}
