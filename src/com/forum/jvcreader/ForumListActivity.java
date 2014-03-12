package com.forum.jvcreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import com.forum.jvcreader.graphics.MenuForumItem;
import com.forum.jvcreader.graphics.MenuSeparator;
import com.forum.jvcreader.jvc.JvcForum;
import com.forum.jvcreader.utils.GlobalData;

import java.util.ArrayList;
import java.util.Arrays;

public class ForumListActivity extends JvcActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forum_list);

		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.forumListLayout);
		LayoutInflater inflater = getLayoutInflater();

		ArrayList<MenuForumItem> menuItems = new ArrayList<MenuForumItem>();
		ArrayList<String> forumNames = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.forumListNames)));
		int[] forumIds = getResources().getIntArray(R.array.forumListIds);

		for(int i = 0; i < forumNames.size(); i++)
		{
			MenuForumItem menuItem = new MenuForumItem(inflater);

			if(forumNames.get(i).compareTo("#category") == 0)
			{
				MenuSeparator separator = new MenuSeparator(R.layout.menu_separator, inflater);
				i++;
				separator.setText(forumNames.get(i));
				linearLayout.addView(separator.getView());
				i++;
				menuItem.setSeparatorVisibility(View.GONE);
			}

			menuItems.add(menuItem);
			menuItem.setItemText(forumNames.get(i));
			menuItem.setItemOnClickListener(new ForumListItemOnClickListener(forumIds[menuItems.size() - 1]));

			linearLayout.addView(menuItem.getView());
		}
	}

	private class ForumListItemOnClickListener implements OnClickListener
	{
		private int itemId;

		public ForumListItemOnClickListener(int id)
		{
			itemId = id;
		}

		@Override
		public void onClick(View view)
		{
			GlobalData.set("forumFromPreviousActivity", new JvcForum(ForumListActivity.this, itemId));
			startActivity(new Intent(ForumListActivity.this, ForumActivity.class));
		}
	}
}
