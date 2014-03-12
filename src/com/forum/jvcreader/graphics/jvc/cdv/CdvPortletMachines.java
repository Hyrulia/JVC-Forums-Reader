package com.forum.jvcreader.graphics.jvc.cdv;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.StringHelper;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletMachines extends CdvPortlet
{
	private ArrayList<String> deviceList = new ArrayList<String>();

	private static final Pattern patternFetchNextDevice = Pattern.compile("<img src=\"http://image.jeuxvideo.com/pics/(mc/.+?\\.gif).*?/>");

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternFetchNextDevice.matcher(StringHelper.unescapeHTML(content));

		while(m.find())
		{
			deviceList.add(m.group(1));
		}

		return true;
	}

	@Override
	protected View getContentView()
	{
		LinearLayout mainLayout = new LinearLayout(context);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mainLayout.setLayoutParams(params);

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setLayoutParams(params);
		mainLayout.addView(layout);

		final int fiveDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics());

		int viewCount = 0;
		for(String deviceName : deviceList)
		{
			int drawableId = JvcUtils.getRawIdFromName(deviceName);
			if(drawableId != -1)
			{
				ImageView image = new ImageView(context);
				params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.rightMargin = fiveDp;
				image.setLayoutParams(params);
				image.setImageDrawable(CachedRawDrawables.getDrawable(drawableId));
				layout.addView(image);

				viewCount++;
				if(viewCount == 6)
				{
					layout = new LinearLayout(context);
					layout.setOrientation(LinearLayout.HORIZONTAL);
					params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					params.topMargin = fiveDp;
					layout.setLayoutParams(params);
					mainLayout.addView(layout);

					viewCount = 0;
				}
			}
			else
			{
				Log.w("JvcForumsReader", "CdvPortletMachines : device icon " + deviceName + " not found !");
			}
		}

		return mainLayout;
	}
}
