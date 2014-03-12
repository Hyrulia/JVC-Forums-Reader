package com.forum.jvcreader.graphics.jvc.cdv;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import com.forum.jvcreader.R;

public class CdvPortletUnknown extends CdvPortlet
{
	@Override
	protected boolean parseContent()
	{
		return true;
	}

	@Override
	protected View getContentView()
	{
		TextView tv = new TextView(context);
		tv.setGravity(Gravity.CENTER);
		tv.setText(R.string.errorPortletNotHandled);
		tv.setTextColor(Color.RED);
		tv.setTextSize(18);
		return tv;
	}
}
