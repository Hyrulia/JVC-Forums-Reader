package com.forum.jvcreader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.forum.jvcreader.graphics.MenuForumItem;
import com.forum.jvcreader.graphics.MenuSeparator;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcForum;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchGameForumActivity extends JvcActivity
{
	private EditText editText;
	private Button button;
	private FrameLayout messageLayout;
	private LinearLayout listLayout;

	private ArrayList<LinkedHashMap<String, String>> gameList;
	private ArrayList<String> categoryNameList;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_forum_by_name);

		editText = (EditText) findViewById(R.id.searchForumEditText);
		button = (Button) findViewById(R.id.searchForumButton);
		messageLayout = (FrameLayout) findViewById(R.id.searchForumTextViewLayout);
		messageLayout.setVisibility(View.GONE);
		listLayout = (LinearLayout) findViewById(R.id.searchForumListLayout);

		gameList = new ArrayList<LinkedHashMap<String, String>>();
		categoryNameList = new ArrayList<String>();
	}

	public void searchForumButtonClick(View view)
	{
		String search = editText.getText().toString();
		if(search == null || search.length() == 0)
			search = "-";
		JvcSearchGameListTask task = new JvcSearchGameListTask();
		registerTask(task);
		task.execute(search);
	}

	private class JvcSearchGameListTask extends AsyncTask<String, Void, String>
	{
		Matcher gameMatcher = null;

		@Override
		protected String doInBackground(String... strings)
		{
			if(isCancelled())
				return null;

			try
			{
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(String.format("http://www.jeuxvideo.com/recherche/jeux/%s.htm", JvcUtils.encodeUrlParam(strings[0])));
				HttpResponse response = client.execute(httpGet);
				if(isCancelled())
					return null;
				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(response.getEntity()));

				if(content.contains("<li class=\"actif\"><a href=\"http://www.jeuxvideo.com/recherche/jeux/"))
				{
					handleMCFrame(content, PatternCollection.fetchNewMCFrame, PatternCollection.fetchNextNewMCGameFrame);
					handleMCFrame(content, PatternCollection.fetchOldMCFrame, PatternCollection.fetchNextOldMCGameFrame);
				}
				else
				{
					return getString(R.string.zeroGamesFound);
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
			catch(Exception e)
			{
				return getString(R.string.errorWhileSearchingForums) + " : " + e.toString();
			}

			return null;
		}

		private void handleMCFrame(String content, Pattern fetchFrame, Pattern fetchGameFrame)
		{
			Matcher m = fetchFrame.matcher(content);
			if(m.find())
			{
				m = fetchGameFrame.matcher(m.group(1));
				while(m.find())
				{
					categoryNameList.add(m.group(1));
					LinkedHashMap<String, String> hashMap = new LinkedHashMap<String, String>();

					if(gameMatcher == null)
						gameMatcher = PatternCollection.fetchNextGame.matcher(m.group(2));
					else
						gameMatcher.reset(m.group(2));
					while(gameMatcher.find())
					{
						hashMap.put(gameMatcher.group(2), gameMatcher.group(1));
					}

					gameList.add(hashMap);
				}
			}
		}

		protected void onPreExecute()
		{
			button.setEnabled(false);
			button.setText(R.string.genericLoading);
			messageLayout.setVisibility(View.GONE);
			listLayout.removeAllViews();

			gameList.clear();
			categoryNameList.clear();
		}

		protected void onPostExecute(String result)
		{
			if(result == null)
			{
				LayoutInflater inflater = getLayoutInflater();

				for(int i = 0; i < categoryNameList.size(); i++)
				{
					MenuSeparator separator = new MenuSeparator(R.layout.menu_separator, inflater);
					separator.setText(categoryNameList.get(i).toUpperCase());
					listLayout.addView(separator.getView());

					MenuForumItem partialItem = null;
					LinkedHashMap<String, String> hashMap = gameList.get(i);
					for(String key : hashMap.keySet())
					{
						MenuForumItem item = new MenuForumItem(inflater);
						item.setItemText(key);
						item.setItemOnClickListener(new GameListItemOnClickListener(hashMap.get(key)));
						if(partialItem == null)
							partialItem = item;
						listLayout.addView(item.getView());
					}
					if(partialItem != null)
						partialItem.setSeparatorVisibility(View.GONE);
				}
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(SearchGameForumActivity.this);
			}
			else
			{
				NoticeDialog.show(SearchGameForumActivity.this, result);
			}

			button.setText(R.string.genericSearch);
			button.setEnabled(true);
		}
	}

	private class GameListItemOnClickListener implements OnClickListener
	{
		private String url;

		public GameListItemOnClickListener(String url)
		{
			this.url = url;
		}

		@Override
		public void onClick(View view)
		{
			new JvcGoToForumFromGameUrlTask().execute(url);
		}
	}

	public class JvcGoToForumFromGameUrlTask extends AsyncTask<String, Void, String>
	{
		private JvcForum forum;
		private ProgressDialog dialog;

		@Override
		protected String doInBackground(String... strings)
		{
			try
			{
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(strings[0]);
				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(client.execute(httpGet).getEntity()));

				Matcher m = PatternCollection.extractGameForumUrl.matcher(content);
				if(m.find())
				{
					forum = new JvcForum(SearchGameForumActivity.this, Integer.parseInt(m.group(1)));
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
			catch(Exception e)
			{
				return getString(R.string.errorWhileFetchingForum) + " : " + e.toString();
			}

			return null;
		}

		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(SearchGameForumActivity.this, null, getString(R.string.fetchingForum));
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				GlobalData.set("forumFromPreviousActivity", forum);
				startActivity(new Intent(SearchGameForumActivity.this, ForumActivity.class));
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(SearchGameForumActivity.this);
			}
			else
			{
				NoticeDialog.show(SearchGameForumActivity.this, result);
			}
		}
	}
}
