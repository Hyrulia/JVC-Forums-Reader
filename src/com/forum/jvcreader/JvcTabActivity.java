package com.forum.jvcreader;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import com.forum.jvcreader.jvc.JvcUserData;

@SuppressWarnings("deprecation")
public class JvcTabActivity extends TabActivity
{
	private MainApplication app;
	private int currentTheme;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		app = (MainApplication) getApplicationContext();
		currentTheme = app.getJvcTheme();
		setTheme(currentTheme);
		if(app.isJvcSessionValid())
			setTitle(getString(R.string.app_name) + " - " + app.getJvcPseudo());

		if(JvcUserData.getBoolean(JvcUserData.PREF_HIDE_TITLE_BAR, JvcUserData.DEFAULT_HIDE_TITLE_BAR))
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) /* Let 11+ api level users go to MainActivity with home button */
	{
		if(Build.VERSION.SDK_INT >= 11 && item.getItemId() == android.R.id.home) /* ActionBar-enabled devices only */
		{
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		checkTheme();
	}

	protected void checkTheme()
	{
		if(currentTheme != app.getJvcTheme())
		{
			startActivity(getIntent());
			finish();
		}
	}
}
