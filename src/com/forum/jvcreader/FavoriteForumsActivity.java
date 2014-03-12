package com.forum.jvcreader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.forum.jvcreader.graphics.MenuForumItem;
import com.forum.jvcreader.graphics.MenuSeparator;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.graphics.jvc.cdv.Cdv;
import com.forum.jvcreader.graphics.jvc.cdv.CdvPortletGenericPrefs;
import com.forum.jvcreader.jvc.JvcForum;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.utils.GlobalData;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class FavoriteForumsActivity extends JvcActivity
{
	private LinearLayout linearLayout;
	private Button importButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forum_list);
		linearLayout = (LinearLayout) findViewById(R.id.forumListLayout);
		updateForums();
	}

	@Override
	public void onResume() /* If a forum have been un-favorited */
	{
		super.onResume();
		linearLayout.removeAllViews();
		updateForums();
		invalidateOptionsMenu();
	}

	private void updateForums()
	{
		linearLayout.removeAllViews();
		LayoutInflater inflater = getLayoutInflater();

		ArrayList<MenuForumItem> menuItems = new ArrayList<MenuForumItem>();
		ArrayList<JvcForum> forumList = JvcUserData.getFavoriteForums();

		if(forumList == null)
		{
			TextView tv = new TextView(this);
			tv.setText(R.string.noFavoriteForums);
			tv.setTextSize(25);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
			tv.setLayoutParams(params);
			tv.setGravity(Gravity.CENTER);
			linearLayout.addView(tv);
		}
		else
		{
			MenuSeparator separator = new MenuSeparator(R.layout.menu_separator, getLayoutInflater());
			separator.setText(getString(R.string.mainMenuFavForumsItem).toUpperCase());
			linearLayout.addView(separator.getView());

			int size = forumList.size();
			for(int i = 0; i < size; i++)
			{
				JvcForum forum = forumList.get(i);
				MenuForumItem menuItem = new MenuForumItem(inflater);

				menuItems.add(menuItem);
				menuItem.setItemText(forum.getForumName());
				menuItem.setItemOnClickListener(new ForumListItemOnClickListener(forum));
				if(i == 0)
					menuItem.setSeparatorVisibility(View.GONE);

				linearLayout.addView(menuItem.getView());
			}
		}

		importButton = new Button(this);
		importButton.setText(R.string.importFavoriteForums);
		importButton.setOnClickListener(importButtonListener);
		linearLayout.addView(importButton);
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
					Collections.sort(JvcUserData.getFavoriteForums(), new JvcForum.NameAscendingComparator());
					JvcUserData.saveFavoriteForums();
					updateForums();
					break;

				case R.id.optionsMenuSortById:
					Collections.sort(JvcUserData.getFavoriteForums(), new JvcForum.ForumIdAscendingComparator());
					JvcUserData.saveFavoriteForums();
					updateForums();
					break;

				case R.id.optionsMenuInverseList:
					Collections.reverse(JvcUserData.getFavoriteForums());
					JvcUserData.saveFavoriteForums();
					updateForums();
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

	private final OnClickListener importButtonListener = new OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			ImportFavoriteForumsTask task = new ImportFavoriteForumsTask();
			registerTask(task);
			task.execute();
		}
	};

	private class ForumListItemOnClickListener implements OnClickListener
	{
		private JvcForum forum;

		public ForumListItemOnClickListener(JvcForum forum)
		{
			this.forum = forum;
		}

		@Override
		public void onClick(View view)
		{
			forum.setContext(FavoriteForumsActivity.this);
			GlobalData.set("forumFromPreviousActivity", forum);
			startActivity(new Intent(FavoriteForumsActivity.this, ForumActivity.class));
		}
	}

	private class ImportFavoriteForumsTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;
		private int count;

		@Override
		protected String doInBackground(Void... voids)
		{
			Cdv cdv = Cdv.createAccountInstance(FavoriteForumsActivity.this);
			String result = cdv.requestCdv(Cdv.CDV_PREFS, this);
			if(result != null || isCancelled())
				return result;

			CdvPortletGenericPrefs favForumsPortlet = (CdvPortletGenericPrefs) cdv.findPortletFromId("pt_forumspref");
			if(favForumsPortlet == null || favForumsPortlet.isHidden())
				return getString(R.string.noFavoriteForumsOnJvc);
			final ArrayList<String> links = favForumsPortlet.getPrefsLinks(), names = favForumsPortlet.getPrefsKeys();
			if(links.isEmpty() || names.isEmpty())
				return getString(R.string.noFavoriteForumsOnJvc);

			count = 0;
			for(int i = 0; i < links.size(); i++)
			{
				JvcLinkIntent jvcLink;
				try
				{
					jvcLink = new JvcLinkIntent(FavoriteForumsActivity.this, new URL(links.get(i)));
					if(jvcLink.getUrlType() != JvcLinkIntent.FORUM_URL)
						throw new MalformedURLException("Wrong URL type for " + names.get(i));
				}
				catch(MalformedURLException e)
				{
					return getString(R.string.errorWhileImportingFavoriteForums) + " : " + e.toString();
				}

				JvcForum forum = new JvcForum(FavoriteForumsActivity.this, jvcLink.getForumId());
				forum.setForumName(names.get(i));
				if(!JvcUserData.isForumInFavorites(forum))
				{
					try
					{
						JvcUserData.addToFavoriteForums(forum);
						count++;
					}
					catch(Exception e)
					{
						return getString(R.string.errorWhileImportingFavoriteForums) + " : " + e.toString();
					}
				}
			}

			if(count == 0)
			{
				return getString(R.string.allFavoriteForumsAlreadyImported);
			}

			return null;
		}

		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(FavoriteForumsActivity.this, null, getString(R.string.importingFavoriteForums));
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				updateForums();
				final String msg;
				if(count == 1)
					msg = getString(R.string.importedOneForum);
				else
					msg = String.format(getString(R.string.importedXForums), count);
				Toast.makeText(FavoriteForumsActivity.this, msg, Toast.LENGTH_LONG).show();
			}
			else
			{
				if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
				{
					Toast.makeText(FavoriteForumsActivity.this, MainApplication.handleHttpTimeout(FavoriteForumsActivity.this), Toast.LENGTH_LONG).show();
				}
				else
				{
					Toast.makeText(FavoriteForumsActivity.this, result, Toast.LENGTH_LONG).show();
				}
			}
		}
	}
}
