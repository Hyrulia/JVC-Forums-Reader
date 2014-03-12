package com.forum.jvcreader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.AsyncTaskManager;

public class JvcActivity extends Activity
{
	private MainApplication app;
	private int currentTheme;

	private boolean isAnimatingDrawables;
	private View animationView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		onCreate(savedInstanceState, -1);
	}

	public void onCreate(Bundle savedInstanceState, int contentViewRes)
	{
		app = (MainApplication) getApplicationContext();
		AsyncTaskManager.registerActivity(this);

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

		if(contentViewRes != -1)
		{
			setContentView(contentViewRes);
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		app.finishDrawableAnimation();

		AsyncTaskManager.killAllTasks(this);
	}

	@Override
	public void onDestroy()
	{
		AsyncTaskManager.killAllTasks(this);
		AsyncTaskManager.unregisterActivity(this);

		super.onDestroy();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		checkTheme();

		if(isAnimatingDrawables && animationView != null)
		{
			app.requestDrawableAnimation(animationView);
		}
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

	protected void checkTheme()
	{
		if(currentTheme != app.getJvcTheme())
		{
			startActivity(getIntent());
			finish();
		}
	}

	protected void startAnimatingDrawables(View view)
	{
		app.requestDrawableAnimation(view);
		isAnimatingDrawables = true;
		animationView = view;
	}

	protected void stopAnimatingDrawables()
	{
		app.finishDrawableAnimation();
		isAnimatingDrawables = false;
	}

	protected void setAnimationView(View view)
	{
		app.setAnimationView(view);
		animationView = view;
	}

	protected void registerTask(AsyncTask task)
	{
		AsyncTaskManager.addTask(this, task);
	}

	public void invalidateOptionsMenu()
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			super.invalidateOptionsMenu();
		}
	}

	public static void hideKeyboard(Activity activity)
	{
		InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		View focus = activity.getCurrentFocus();
		if(focus != null)
			inputManager.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

}
