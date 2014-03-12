package com.forum.jvcreader;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.forum.jvcreader.graphics.MenuForumItem;
import com.forum.jvcreader.graphics.MenuSeparator;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcTopic;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.GlobalData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class FavoriteTopicsActivity extends JvcActivity
{
	private LinearLayout linearLayout;
	private Button archivedTopicsButton;

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
		ArrayList<JvcTopic> topicList = JvcUserData.getFavoriteTopics();

		if(topicList == null)
		{
			TextView tv = new TextView(this);
			tv.setText(R.string.noFavoriteTopics);
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
			separator.setText(getString(R.string.mainMenuFavTopicsItem).toUpperCase());
			linearLayout.addView(separator.getView());

			int size = topicList.size();
			for(int i = 0; i < size; i++)
			{
				JvcTopic topic = topicList.get(i);
				MenuForumItem menuItem = new MenuForumItem(inflater);

				menuItems.add(menuItem);
				menuItem.setItemText(topic.getExtraTopicName());
				menuItem.setItemOnClickListener(new TopicListItemOnClickListener(topic));
				if(i == 0)
					menuItem.setSeparatorVisibility(View.GONE);

				linearLayout.addView(menuItem.getView());
			}
		}

		archivedTopicsButton = new Button(this);
		archivedTopicsButton.setText(R.string.openArchivedTopics);
		archivedTopicsButton.setOnClickListener(archivedTopicsButtonListener);
		linearLayout.addView(archivedTopicsButton);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.sort_options_menu, menu);

		return JvcUserData.getFavoriteForums() != null;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		return JvcUserData.getFavoriteForums() != null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		try
		{
			switch(item.getItemId())
			{
				case R.id.optionsMenuSortByName:
					Collections.sort(JvcUserData.getFavoriteTopics(), new JvcTopic.NameAscendingComparator());
					JvcUserData.saveFavoriteTopics();
					updateTopics();
					break;

				case R.id.optionsMenuSortById:
					Collections.sort(JvcUserData.getFavoriteTopics(), new JvcTopic.TopicIdAscendingComparator());
					JvcUserData.saveFavoriteTopics();
					updateTopics();
					break;

				case R.id.optionsMenuInverseList:
					Collections.reverse(JvcUserData.getFavoriteTopics());
					JvcUserData.saveFavoriteTopics();
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
			startActivity(new Intent(FavoriteTopicsActivity.this, ArchivedTopicsActivity.class));
		}
	};

	private class TopicListItemOnClickListener implements OnClickListener
	{
		private JvcTopic topic;

		public TopicListItemOnClickListener(JvcTopic topic)
		{
			this.topic = topic;
		}

		@Override
		public void onClick(View view)
		{
			topic.getForum().setContext(FavoriteTopicsActivity.this);
			GlobalData.set("topicFromPreviousActivity", topic);
			startActivity(new Intent(FavoriteTopicsActivity.this, TopicActivity.class));
		}
	}
}
