package com.forum.jvcreader;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import com.forum.jvcreader.jvc.JvcPmList;
import com.forum.jvcreader.utils.CachedRawDrawables;

public class PmActivity extends JvcTabActivity
{
	@SuppressWarnings("deprecation") @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pm);

		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent, activityIntent = getIntent();
		//Drawable ic;

		intent = new Intent(this, PmBoxActivity.class);
		intent.putExtra("com.forum.jvcreader.BoxType", JvcPmList.RECEIVED_MESSAGES);
		//ic = getResources().getDrawable(R.drawable.ic_tab_pm_received);
		spec = tabHost.newTabSpec("received").setIndicator(getString(R.string.pmTabReceived), CachedRawDrawables.getDrawable(R.raw.pm_received)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent(this, PmBoxActivity.class);
		intent.putExtra("com.forum.jvcreader.BoxType", JvcPmList.SENT_MESSAGES);
		//ic = getResources().getDrawable(R.drawable.ic_tab_pm_sent);
		spec = tabHost.newTabSpec("sent").setIndicator(getString(R.string.pmTabSent), CachedRawDrawables.getDrawable(R.raw.pm_sent)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent(this, PmNewActivity.class);
		if(activityIntent.hasExtra("com.forum.jvcreader.RecipientPseudo"))
		{
			intent.putExtra("com.forum.jvcreader.RecipientPseudo", activityIntent.getStringExtra("com.forum.jvcreader.RecipientPseudo"));
		}
		//ic = getResources().getDrawable(R.drawable.ic_tab_pm_new);
		spec = tabHost.newTabSpec("new").setIndicator(getString(R.string.pmTabNew), CachedRawDrawables.getDrawable(R.raw.pm_new)).setContent(intent);
		tabHost.addTab(spec);

		if(activityIntent.getBooleanExtra("com.forum.jvcreader.SendNewPm", false))
		{
			tabHost.setCurrentTab(2);
		}
		else
		{
			tabHost.setCurrentTab(0);
		}
	}
}
