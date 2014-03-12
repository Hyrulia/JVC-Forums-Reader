package com.forum.jvcreader.graphics.jvc.cdv;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletContribsautres extends CdvPortlet
{
	private static final Pattern patternExtractHeaderTitle = Pattern.compile("(.+)<span>(.+?)</span>");

	@Override
	protected CharSequence getHeaderTitle()
	{
		Matcher m = patternExtractHeaderTitle.matcher(name);
		if(m.find())
		{
			SpannableStringBuilder builder = new SpannableStringBuilder(m.group(1));
			builder.append(m.group(2));

			builder.setSpan(new ForegroundColorSpan(textPrimaryColor), m.group(1).length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			return builder;
		}
		else
		{
			return name;
		}
	}

	@Override
	protected boolean parseContent()
	{
		/* TODO CDV contribsautres */
		return false;
	}

	@Override
	protected View getContentView()
	{
		return null;
	}
}
