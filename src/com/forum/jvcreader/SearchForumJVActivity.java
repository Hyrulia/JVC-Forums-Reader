package com.forum.jvcreader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.forum.jvcreader.graphics.MenuForumItem;
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
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

public class SearchForumJVActivity extends JvcActivity
{
	private EditText editText;
	private Button button;
	private FrameLayout messageLayout;
	private TextView messageTextView;
	private LinearLayout listLayout;

	private ArrayList<JvcForum> forumList;
	private LayoutInflater layoutInflater;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_forum_by_name);

		editText = (EditText) findViewById(R.id.searchForumEditText);
		button = (Button) findViewById(R.id.searchForumButton);
		messageLayout = (FrameLayout) findViewById(R.id.searchForumTextViewLayout);
		messageLayout.setVisibility(View.GONE);
		messageTextView = (TextView) findViewById(R.id.searchForumTextView);
		listLayout = (LinearLayout) findViewById(R.id.searchForumListLayout);

		forumList = new ArrayList<JvcForum>();
		layoutInflater = getLayoutInflater();
	}

	public void searchForumButtonClick(View view)
	{
		String search = editText.getText().toString();
		if(search == null || search.length() == 0)
			search = "-";
		JvcSearchForumJVTask task = new JvcSearchForumJVTask();
		registerTask(task);
		task.execute(search);
	}

	public class JvcSearchForumJVTask extends AsyncTask<String, Void, String>
	{
		private String message;

		@Override
		protected String doInBackground(String... strings)
		{
			if(isCancelled())
				return null;

			try
			{
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(String.format("http://www.forumjv.com/recherche/%s.html", JvcUtils.encodeUrlParam(strings[0])));
				HttpResponse response = client.execute(httpGet);
				if(isCancelled())
					return null;
				String content = StringHelper.unescapeHTML(MainApplication.getEntityContent(response.getEntity()));

				Matcher m = PatternCollection.fetchForumJVSearchFrame.matcher(content);
				if(m.find())
				{
					message = m.group(1);
					String frame = m.group(2);
					if(frame != null)
					{
						m = PatternCollection.fetchNextForumJVItem.matcher(frame);
						while(m.find())
						{
							JvcForum forum = new JvcForum(SearchForumJVActivity.this, Integer.parseInt(m.group(2)), m.group(1));
							forum.setForumName(m.group(3));
							forumList.add(forum);
						}
					}
				}
				else
				{
					throw new PatternSyntaxException("cannot find ForumJV search frame", PatternCollection.fetchForumJVSearchFrame.pattern(), -1);
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

		protected void onPreExecute()
		{
			button.setEnabled(false);
			button.setText(R.string.genericLoading);
			messageLayout.setVisibility(View.GONE);
			listLayout.removeAllViews();
			forumList.clear();
		}

		protected void onPostExecute(String result)
		{
			if(result == null)
			{
				int count = forumList.size();

				for(int i = 0; i < count; i++)
				{
					JvcForum forum = forumList.get(i);
					MenuForumItem item = new MenuForumItem(layoutInflater);

					if(i == 0)
						item.setSeparatorVisibility(View.GONE);
					item.setItemText(forum.getForumName());
					item.setItemOnClickListener(new ForumOnClickListener(forum));
					listLayout.addView(item.getView());
				}

				messageLayout.setVisibility(View.VISIBLE);
				messageTextView.setText(Html.fromHtml(message));
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(SearchForumJVActivity.this);
			}
			else
			{
				NoticeDialog.show(SearchForumJVActivity.this, result);
			}

			button.setText(R.string.genericSearch);
			button.setEnabled(true);
		}
	}

	public class ForumOnClickListener implements OnClickListener
	{
		JvcForum forum;

		public ForumOnClickListener(JvcForum forum)
		{
			this.forum = forum;
		}

		public void onClick(View view)
		{
			forum.invalidateForumName();
			GlobalData.set("forumFromPreviousActivity", forum);
			startActivity(new Intent(SearchForumJVActivity.this, ForumActivity.class));
		}
	}
}
