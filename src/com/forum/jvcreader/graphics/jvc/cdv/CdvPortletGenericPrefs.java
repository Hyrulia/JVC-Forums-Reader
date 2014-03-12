package com.forum.jvcreader.graphics.jvc.cdv;

import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.utils.StringHelper;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletGenericPrefs extends CdvPortlet
{
	private static final Pattern patternFetchNextPref = Pattern.compile("<li><a.*?href=\"(.+?)\".*?>(.+?)</a>(?: <strong>(.+?)</strong>)?</li>");

	private final ArrayList<String> prefsLinks = new ArrayList<String>();
	private final ArrayList<String> prefsKeys = new ArrayList<String>();
	private final ArrayList<String> prefsValues = new ArrayList<String>();

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternFetchNextPref.matcher(StringHelper.unescapeHTML(content));
		while(m.find())
		{
			prefsLinks.add(m.group(1));
			prefsKeys.add(m.group(2));
			if(m.group(3) != null)
				prefsValues.add(m.group(3));
			else
				prefsValues.add("");
		}

		return true;
	}

	@Override
	protected View getContentView()
	{
		LinearLayout layout = new LinearLayout(context);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		layout.setLayoutParams(params);
		layout.setOrientation(LinearLayout.VERTICAL);

		final int count = prefsLinks.size();
		if(count > 0)
		{
			for(int i = 0; i < count; i++)
			{
				TextView tv = new TextView(context);
				tv.setMovementMethod(LinkMovementMethod.getInstance());
				tv.setTextColor(textPrimaryColor);
				tv.setTextSize(16);
				SpannableStringBuilder builder = JvcLinkIntent.makeLink(context, prefsKeys.get(i), prefsLinks.get(i), true);
				builder.append(" ").append(prefsValues.get(i));
				tv.setText(builder);
				layout.addView(tv);
			}
		}

		return layout;
	}

	public ArrayList<String> getPrefsLinks()
	{
		return prefsLinks;
	}

	public ArrayList<String> getPrefsKeys()
	{
		return prefsKeys;
	}
}
