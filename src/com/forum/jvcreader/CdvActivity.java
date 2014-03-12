package com.forum.jvcreader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.forum.jvcreader.graphics.jvc.cdv.Cdv;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.GlobalData;

public class CdvActivity extends JvcActivity
{
	private int currentCdvTab;
	private TextView alternateTv;

	private Cdv cdv;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		alternateTv = new TextView(this);
		alternateTv.setTextSize(18);
		alternateTv.setGravity(Gravity.CENTER);
		alternateTv.setTextColor(getResources().getColor(R.color.jvcAdminPseudo));
		setContentView(alternateTv);

		String pseudo = (String) GlobalData.get("cdvPseudoFromLastActivity");
		if(pseudo == null || pseudo.length() == 0)
		{
			Log.e("JvcForumsReader", "CDV Pseudo from last activity is null !");
			Log.e("JvcForumsReader", "Finishing CdvActivity...");
			finish();
		}
		else
		{
			cdv = new Cdv(CdvActivity.this, pseudo);
			currentCdvTab = Cdv.CDV_PROFILE;
			loadCdv();
		}
	}

	private final OnClickListener lProfile = new OnClickListener()
	{
		public void onClick(View view)
		{
			currentCdvTab = Cdv.CDV_PROFILE;
			loadCdv();
		}
	};

	private final OnClickListener lPrefs = new OnClickListener()
	{
		public void onClick(View view)
		{
			currentCdvTab = Cdv.CDV_PREFS;
			loadCdv();
		}
	};

	private final OnClickListener lContributions = new OnClickListener()
	{
		public void onClick(View view)
		{
			currentCdvTab = Cdv.CDV_CONTRIBUTIONS;
			loadCdv();
		}
	};

	private void loadCdv()
	{
		JvcLoadCDVTask task = new JvcLoadCDVTask();
		registerTask(task);
		task.execute();
	}

	private class JvcLoadCDVTask extends AsyncTask<Void, Void, String>
	{
		protected String doInBackground(Void... voids)
		{
			return cdv.requestCdv(currentCdvTab, this);
		}

		protected void onPreExecute()
		{
			setContentView(alternateTv);
			alternateTv.setText(R.string.genericLoading);
		}

		protected void onPostExecute(String result)
		{
			if(result == null)
			{
				View currentView = cdv.getView(lProfile, lPrefs, lContributions);
				startAnimatingDrawables(currentView);
				setContentView(currentView);
			}
			else if(result.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
			{
				alternateTv.setText(MainApplication.handleHttpTimeout(CdvActivity.this));
			}
			else
			{
				alternateTv.setText(result);
			}
		}
	}
}