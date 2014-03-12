package com.forum.jvcreader;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.TableLayout.LayoutParams;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.graphics.PostItem;
import com.forum.jvcreader.jvc.JvcKickInterface;
import com.forum.jvcreader.jvc.JvcKickInterface.Entry;
import com.forum.jvcreader.jvc.JvcPost;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.GlobalData;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class KickInterfaceActivity extends JvcActivity
{
	private JvcKickInterface kickInterface;
	private String interfaceUrl;
	private boolean isForumJV;
	private String forumJVBaseUrl;

	private LayoutInflater inflater;
	private TableLayout table;
	private TextView totalKicksTv;

	private ArrayList<View> flushViewList = new ArrayList<View>();
	private boolean showNoelshackThumbnails;
	private boolean animateSmileys;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kick_interface);

		showNoelshackThumbnails = JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS);
		animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);

		interfaceUrl = getIntent().getStringExtra("com.forum.jvcreader.KickInterfaceUrl");
		if(interfaceUrl == null || interfaceUrl.length() == 0)
		{
			Log.e("JvcForumsReader", "KickInterfaceUrl not specified !");
			Log.e("JvcForumsReader", "Finishing KickInterfaceActivity...");
			finish();
		}

		isForumJV = getIntent().getBooleanExtra("com.forum.jvcreader.KickInterfaceIsForumJV", false);
		if(isForumJV)
		{
			forumJVBaseUrl = getIntent().getStringExtra("com.forum.jvcreader.ForumJVBaseUrl");
			if(forumJVBaseUrl == null || forumJVBaseUrl.length() == 0)
			{
				Log.e("JvcForumsReader", "ForumJVBaseUrl not specified !");
				Log.e("JvcForumsReader", "Finishing KickInterfaceActivity...");
				finish();
			}
		}
		else
		{
			forumJVBaseUrl = null;
		}

		kickInterface = new JvcKickInterface(this, interfaceUrl, isForumJV, forumJVBaseUrl);
		inflater = getLayoutInflater();
		table = (TableLayout) findViewById(R.id.kickInterfaceTableLayout);
		table.addView(inflater.inflate(R.layout.list_divider, null));
		totalKicksTv = (TextView) findViewById(R.id.kickInterfaceTotalKicksTextView);
		totalKicksTv.setText("TOTAL : 0 KICK");

		loadKickInterface();
	}

	public void addEntry(JvcKickInterface.Entry entry)
	{
		TableRow row = (TableRow) inflater.inflate(R.layout.kick_interface_row, null);
		row.findViewById(R.id.kirImageLayout).setOnClickListener(new SpecialOnClickListener(actionListener, entry)
		{
			@Override
			public void onClick(View v)
			{
				listener.onRemoveEntryClick(entry);
			}
		});
		((ImageView) row.findViewById(R.id.kirImageView)).setImageDrawable(CachedRawDrawables.getDrawable(R.raw.cancel_icon));

		TextView tv = (TextView) row.findViewById(R.id.kirPseudoTextView);
		tv.setText(entry.getPseudo());
		tv.setOnClickListener(new SpecialOnClickListener(actionListener, entry)
		{
			@Override
			public void onClick(View v)
			{
				listener.onShowPseudoClick(entry);
			}
		});

		tv = (TextView) row.findViewById(R.id.kirReasonTextView);
		SpannableStringBuilder builder = new SpannableStringBuilder(entry.getReason() + "\n");
		builder.append(JvcLinkIntent.makeLink(this, "Afficher", "http://www.jeuxvideo.com/", false));
		tv.setText(builder);
		tv.setOnClickListener(new SpecialOnClickListener(actionListener, entry)
		{
			@Override
			public void onClick(View v)
			{
				listener.onShowReasonClick(entry);
			}
		});

		((TextView) row.findViewById(R.id.kirTotalKicksTextView)).setText(entry.getTotalKicks());

		table.addView(row);
		flushViewList.add(row);
		View view = inflater.inflate(R.layout.list_divider, null);
		table.addView(view);
		flushViewList.add(view);
	}

	private OnKickInterfaceActionListener actionListener = new OnKickInterfaceActionListener()
	{

		@Override
		public void onRemoveEntryClick(Entry entry)
		{
			RemoveEntryTask task = new RemoveEntryTask();
			registerTask(task);
			task.execute(entry);
		}

		@Override
		public void onShowPseudoClick(Entry entry)
		{
			GlobalData.set("cdvPseudoFromLastActivity", entry.getPseudo());
			KickInterfaceActivity.this.startActivity(new Intent(KickInterfaceActivity.this, CdvActivity.class));
		}

		@Override
		public void onShowReasonClick(Entry entry)
		{
			PostItem item = new PostItem(KickInterfaceActivity.this, true, showNoelshackThumbnails, animateSmileys);
			JvcPost post = new JvcPost(null, 0, entry.getMessage() + "\n", entry.getPseudo(), false, false, 1, "", null, null);
			item.updateDataFromPost(post, false);

			final int tenDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
			LinearLayout layout = new LinearLayout(KickInterfaceActivity.this);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			layout.setBackgroundColor(Color.WHITE);

			TextView tv = new TextView(KickInterfaceActivity.this);
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.bottomMargin = params.topMargin = tenDp;
			tv.setLayoutParams(params);
			tv.setTextSize(16);
			tv.setGravity(Gravity.CENTER);
			tv.setTextColor(getResources().getColor(R.color.jvcTopicName));
			tv.setTypeface(null, Typeface.BOLD);
			tv.setText("\u00AB " + entry.getSubject() + " \u00BB");
			layout.addView(tv);
			layout.addView(item.getView());

			Dialog previewDialog = new Dialog(KickInterfaceActivity.this, R.style.FullScreenNoTitleDialogTheme);
			previewDialog.setContentView(layout);
			previewDialog.setCancelable(true);
			previewDialog.show();
		}
	};

	private void loadKickInterface()
	{
		LoadKickInterfaceTask task = new LoadKickInterfaceTask();
		registerTask(task);
		task.execute();
	}

	private class LoadKickInterfaceTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialog dialog;

		@Override
		protected String doInBackground(Void... voids)
		{
			try
			{
				if(isCancelled())
					return null;
				boolean error = kickInterface.loadInterface();
				if(error)
				{
					return kickInterface.getRequestError();
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
			catch(Exception e)
			{
				return getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			if(flushViewList.size() > 0)
			{
				for(View v : flushViewList)
				{
					ViewParent parent = v.getParent();
					if(parent != null && parent instanceof ViewGroup)
						((ViewGroup) parent).removeView(v);
				}

				flushViewList.clear();
			}

			dialog = ProgressDialog.show(KickInterfaceActivity.this, null, getString(R.string.loading));
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				final long n = kickInterface.getRequestTotalKicks();
				totalKicksTv.setText("TOTAL : " + n + " KICK" + (n > 1 ? "S" : ""));

				final ArrayList<JvcKickInterface.Entry> list = kickInterface.getRequestKickEntryList();
				if(list.size() > 0)
				{
					for(JvcKickInterface.Entry entry : list)
					{
						addEntry(entry);
					}
				}
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(KickInterfaceActivity.this);
			}
			else
			{
				NoticeDialog.show(KickInterfaceActivity.this, result);
			}
		}
	}

	private class RemoveEntryTask extends AsyncTask<JvcKickInterface.Entry, Void, String>
	{
		private ProgressDialog dialog;

		@Override
		protected String doInBackground(Entry... entry)
		{

			try
			{
				if(isCancelled())
					return null;
				boolean error = kickInterface.removeEntry(entry[0]);
				if(error)
				{
					return kickInterface.getRequestError();
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
			catch(Exception e)
			{
				return getString(R.string.errorWhileLoading) + " : " + e.toString();
			}
		}

		protected void onPreExecute()
		{
			dialog = ProgressDialog.show(KickInterfaceActivity.this, null, getString(R.string.loading));
		}

		protected void onPostExecute(String result)
		{
			dialog.dismiss();
			dialog = null;

			if(result == null)
			{
				loadKickInterface();
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				MainApplication.handleHttpTimeout(KickInterfaceActivity.this);
			}
			else
			{
				NoticeDialog.show(KickInterfaceActivity.this, result);
			}
		}
	}

	public interface OnKickInterfaceActionListener
	{
		public void onRemoveEntryClick(JvcKickInterface.Entry entry);

		public void onShowPseudoClick(JvcKickInterface.Entry entry);

		public void onShowReasonClick(JvcKickInterface.Entry entry);
	}

	public abstract class SpecialOnClickListener implements OnClickListener
	{
		protected OnKickInterfaceActionListener listener;
		protected JvcKickInterface.Entry entry;

		public SpecialOnClickListener(OnKickInterfaceActionListener listener, JvcKickInterface.Entry entry)
		{
			this.listener = listener;
			this.entry = entry;
		}

		@Override
		public abstract void onClick(View v);
	}
}
