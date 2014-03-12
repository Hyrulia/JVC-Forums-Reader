package com.forum.jvcreader.graphics.jvc.cdv;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.PmActivity;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.PatternCollection;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;

public class Cdv
{
	public static final int CDV_PROFILE = 0;
	public static final int CDV_PREFS = 1;
	public static final int CDV_CONTRIBUTIONS = 2;

	private Context context;
	private ArrayList<CdvPortlet> portletList;

	private boolean gender; /* 0 = male, 1 = female */
	private String pseudo;
	private String rank;

	private Button profileButton;
	private Button prefsButton;
	private Button contributionsButton;

	private boolean hasPrefs, hasContributions;

	private boolean cookiesEnabled = false;

	public Cdv(Context context, String pseudo)
	{
		this.context = context;
		this.pseudo = pseudo;

		portletList = new ArrayList<CdvPortlet>();
	}

	public static Cdv createAccountInstance(Context context)
	{
		MainApplication app = (MainApplication) context.getApplicationContext();
		Cdv cdv = new Cdv(context, app.getJvcPseudo());
		cdv.setCookiesEnabled(true);

		return cdv;
	}

	public void setCookiesEnabled(boolean enabled)
	{
		cookiesEnabled = enabled;
	}

	public CdvPortlet findPortletFromId(String portletId)
	{
		for(CdvPortlet portlet : portletList)
		{
			if(portlet.getId().equals(portletId))
			{
				return portlet;
			}
		}

		return null;
	}

	public String requestCdv(int id, AsyncTask task)
	{
		Resources r = context.getResources();

		try
		{
			String append = "";
			switch(id)
			{
				case CDV_PREFS:
					append = "-coups-de-coeur";
					break;
				case CDV_CONTRIBUTIONS:
					append = "-contributions";
					break;
				default:
					break;
			}

			HttpClient client;
			if(cookiesEnabled)
			{
				MainApplication app = (MainApplication) context.getApplicationContext();
				client = app.getHttpClient(MainApplication.JVC_SESSION);
			}
			else
			{
				client = new DefaultHttpClient();
			}
			HttpGet httpGet = new HttpGet(String.format("http://www.jeuxvideo.com/profil/%s%s.html", JvcUtils.encodeUrlParam(pseudo.toLowerCase()), append));
			HttpResponse rep = client.execute(httpGet);
			String content = MainApplication.getEntityContent(rep.getEntity());

			if(content.contains("id=\"e404\"") || task.isCancelled())
				return r.getString(R.string.errorNoSuchPseudo); /* Profile page is not existing */
			Matcher m = PatternCollection.extractCDVBanInformation.matcher(content);
			if(m.find())
				return m.group(1); /* Pseudo is banned */

			m = PatternCollection.extractCDVHeader.matcher(content);
			if(m.find())
			{
				gender = m.group(1).equals("f");
				pseudo = m.group(2);
				rank = m.group(3);
			}

			portletList.clear();

			if(id != Cdv.CDV_CONTRIBUTIONS)
			{
				if(id == Cdv.CDV_PROFILE)
				{
					hasPrefs = content.contains("<li id=\"o_ccoeurs\"><a href");
					hasContributions = content.contains("<li id=\"o_contrib\"><a href");

					m = PatternCollection.fetchCDVMainPortlet.matcher(content);
					if(m.find())
					{
						portletList.add(CdvPortlet.getCdvPortlet(this, m.group(1), false, m.group(2), m.group(3)));
						portletList.add(CdvPortlet.getCdvPortlet(this, m.group(4), false, m.group(5), m.group(6)));
					}
				}

				m = PatternCollection.fetchNextCDVPortlet.matcher(content);
				while(m.find())
				{
					portletList.add(CdvPortlet.getCdvPortlet(this, m.group(1), !(m.group(2).length() == 0), m.group(4), m.group(5)));
				}
			}
			else
			{
				m = PatternCollection.fetchCDVContribPortlet1.matcher(content);
				if(m.find())
				{
					portletList.add(CdvPortlet.getCdvPortlet(this, m.group(1), false, m.group(2), m.group(3)));
				}

				m = PatternCollection.fetchCDVContribPortlet2.matcher(content);
				if(m.find())
				{
					portletList.add(CdvPortlet.getCdvPortlet(this, m.group(1), false, m.group(2), m.group(3)));
				}
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
		catch(InstantiationException e)
		{
			return r.getString(R.string.errorWhileLoadingCDV) + " : " + e.toString();
		}
		catch(IllegalAccessException e)
		{
			return r.getString(R.string.errorWhileLoadingCDV) + " : " + e.toString();
		}
		catch(IOException e)
		{
			return r.getString(R.string.errorWhileLoadingCDV) + " : " + e.toString();
		}
	}

	public View getView(OnClickListener lProfile, OnClickListener lPrefs, OnClickListener lContributions)
	{
		ScrollView view = new ScrollView(context);

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		final int tenDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
		layout.setPadding(tenDp, tenDp, tenDp, tenDp);

		layout.addView(getPseudoView(lProfile, lPrefs, lContributions));

		for(CdvPortlet portlet : portletList)
		{
			if(!portlet.isHidden())
			{
				layout.addView(portlet.getView());
			}
		}

		view.addView(layout);

		return view;
	}

	public Context getContext()
	{
		return context;
	}

	public String getPseudo()
	{
		return pseudo;
	}

	public View getPseudoView(OnClickListener lProfile, OnClickListener lPrefs, OnClickListener lContributions)
	{
		LinearLayout layout = (LinearLayout) ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cdv_header, null);

		TextView pseudoTv = (TextView) layout.findViewById(R.id.cdvHeaderPseudoTextView);
		pseudoTv.setText(pseudo);
		if(gender)
			pseudoTv.setTextColor(context.getResources().getColor(R.color.jvcGenderFemale));
		else
			pseudoTv.setTextColor(context.getResources().getColor(R.color.jvcGenderMale));

		Drawable rankDrawable = CachedRawDrawables.getDrawable(JvcUtils.getRawIdFromName(rank));
		((ImageView) layout.findViewById(R.id.cdvHeaderRankImageView)).setImageDrawable(rankDrawable);

		layout.findViewById(R.id.cdvHeaderSendPmButton).setOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(context, PmActivity.class);
				intent.putExtra("com.forum.jvcreader.SendNewPm", true);
				intent.putExtra("com.forum.jvcreader.RecipientPseudo", pseudo);
				context.startActivity(intent);
			}
		});

		profileButton = (Button) layout.findViewById(R.id.cdvHeaderProfileButton);
		profileButton.setOnClickListener(lProfile);

		prefsButton = (Button) layout.findViewById(R.id.cdvHeaderPrefsButton);
		prefsButton.setOnClickListener(lPrefs);
		prefsButton.setEnabled(hasPrefs);

		contributionsButton = (Button) layout.findViewById(R.id.cdvHeaderContributionsButton);
		contributionsButton.setOnClickListener(lContributions);
		contributionsButton.setEnabled(hasContributions);

		return layout;
	}
}
