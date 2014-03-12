package com.forum.jvcreader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import com.forum.jvcreader.graphics.MenuForumItem;
import com.forum.jvcreader.graphics.MenuSeparator;
import com.forum.jvcreader.jvc.JvcForum;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopForumsActivity extends JvcActivity
{
	private static final Pattern patternFetchTopForumsList = Pattern.compile("<ul class=\"liste_liens\">\n<li><strong>\n((.+\n+)+?)(</strong>)?</ul>");
	private static final Pattern patternFetchNextTopForum = Pattern.compile("<a href=\".+/0-([0-9]+)-0-1-0-1-0-.+.htm\">(.+)</a>");

	private ArrayList<MenuForumItem> menuItemList;
	private ArrayList<JvcForum> forumList;
	private int[] forumIdList;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forum_list);

		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.forumListLayout);
		LayoutInflater inflater = getLayoutInflater();

		menuItemList = new ArrayList<MenuForumItem>();
		forumList = new ArrayList<JvcForum>();

		MenuSeparator separator = new MenuSeparator(R.layout.menu_separator, getLayoutInflater());
		separator.setText(getString(R.string.mainMenuTopForumsItem).toUpperCase());
		linearLayout.addView(separator.getView());

		for(int i = 0; i < 15; i++)
		{
			MenuForumItem menuItem = new MenuForumItem(inflater);
			if(i == 0)
				menuItem.setSeparatorVisibility(View.GONE);
			menuItem.setItemOnClickListener(new ForumListItemOnClickListener(i));
			menuItemList.add(menuItem);
			linearLayout.addView(menuItem.getView());

			menuItem.getView().setVisibility(View.GONE);
		}

		JvcUpdateTopForumsListTask task = new JvcUpdateTopForumsListTask();
		registerTask(task);
		task.execute();
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
			if(forumIdList[0] != -1)
			{
				JvcForum forum = forumList.get(itemId);
				forum.invalidateForumName();
				GlobalData.set("forumFromPreviousActivity", forum);
				startActivity(new Intent(TopForumsActivity.this, ForumActivity.class));
			}
		}
	}

	private class JvcUpdateTopForumsListTask extends AsyncTask<Void, Void, String>
	{
		protected String doInBackground(Void... voids)
		{
			try
			{
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet("http://www.jeuxvideo.com/forums.htm");
				HttpResponse response = client.execute(httpGet);
				if(isCancelled())
					return null;
				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(response.getEntity()));

				Matcher m = patternFetchTopForumsList.matcher(content);
				if(m.find())
				{
					m = patternFetchNextTopForum.matcher(m.group(1));
					while(m.find())
					{
						JvcForum forum = new JvcForum(TopForumsActivity.this, Integer.parseInt(m.group(1)));
						forum.setForumName(m.group(2));
						forumList.add(forum);
					}

					return null;
				}

				return getString(R.string.cannotExtractData);
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
				return e.toString();
			}
		}

		protected void onPreExecute()
		{
			forumIdList = new int[1];
			forumIdList[0] = -1;

			menuItemList.get(0).setItemText(R.string.genericLoading);
			menuItemList.get(0).getView().setVisibility(View.VISIBLE);
			forumList.clear();
		}

		protected void onPostExecute(String result)
		{
			if(result == null)
			{
				int size = forumList.size();
				forumIdList = new int[size];
				for(int i = 0; i < size; i++)
				{
					JvcForum forum = forumList.get(i);
					MenuForumItem item = menuItemList.get(i);

					forumIdList[i] = forum.getForumId();
					item.setItemText(forum.getForumName());
					item.getView().setVisibility(View.VISIBLE);
				}
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MenuForumItem item = menuItemList.get(0);
				item.setItemText(MainApplication.handleHttpTimeout(TopForumsActivity.this));
				item.getView().setVisibility(View.VISIBLE);
			}
			else
			{
				MenuForumItem item = menuItemList.get(0);
				item.setItemText(result);
				item.getView().setVisibility(View.VISIBLE);
			}
		}
	}
}
