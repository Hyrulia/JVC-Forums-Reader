package com.forum.jvcreader.graphics.jvc.cdv;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import com.forum.jvcreader.utils.StringHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletNbpost extends CdvPortlet
{
	private static final Pattern patternExtractHeaderTitle = Pattern.compile("(.+)<span>(.+?)</span>");
	private static final Pattern patternExtractBarWidth = Pattern.compile("<p id=\"nbpost\"><img width=\"([0-9]+)\"");

	private int width;

	@Override
	protected CharSequence getHeaderTitle() /* Custom colored title for pt_nbpost */
	{
		Matcher m = patternExtractHeaderTitle.matcher(StringHelper.unescapeHTML(name));
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
		Matcher m = patternExtractBarWidth.matcher(StringHelper.unescapeHTML(content));
		if(m.find())
		{
			width = Integer.parseInt(m.group(1));
			return true;
		}

		return false;
	}

	@Override
	protected View getContentView()
	{
		ProgressBar bar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		bar.setLayoutParams(params);
		bar.setMax(322);
		bar.setProgress(width);
		return bar;
	}
}
