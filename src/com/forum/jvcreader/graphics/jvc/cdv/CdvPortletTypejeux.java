package com.forum.jvcreader.graphics.jvc.cdv;

import android.view.View;
import android.widget.TextView;
import com.forum.jvcreader.utils.StringHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletTypejeux extends CdvPortlet
{
	private static final Pattern patternFetchGameTypes = Pattern.compile("<p>(.+?)</p>");

	private String gameTypes;

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternFetchGameTypes.matcher(content);
		if(m.find())
		{
			gameTypes = StringHelper.unescapeHTML(m.group(1));
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	protected View getContentView()
	{
		TextView desc = new TextView(context);
		desc.setText(gameTypes);
		desc.setTextSize(15);
		desc.setTextColor(textPrimaryColor);

		return desc;
	}
}
