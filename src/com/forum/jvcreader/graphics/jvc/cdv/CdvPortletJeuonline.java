package com.forum.jvcreader.graphics.jvc.cdv;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.forum.jvcreader.jvc.JvcTextSpanner.JvcClickableSpan;
import com.forum.jvcreader.utils.StringHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletJeuonline extends CdvPortlet
{
	private static final Pattern patternFetchNextKeyValue = Pattern.compile("<li><strong>(.+?)</strong>(.+?)</li>");
	private static final Pattern patternFetchNextWiiGame = Pattern.compile("<dt>(?:<a.*?href='(.+?)'>)?(.+?)(?:</a>)?</dt><dd>(.+?)</dd>");

	private final LinkedHashMap<String, String> genericPairs = new LinkedHashMap<String, String>();

	private final ArrayList<String> gameLinks = new ArrayList<String>();
	private final ArrayList<String> gameNames = new ArrayList<String>();
	private final ArrayList<String> gameCodes = new ArrayList<String>();

	private String gameKey;

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternFetchNextKeyValue.matcher(StringHelper.unescapeHTML(content));
		while(m.find())
		{
			if(m.group(2).contains("<dl>"))
			{
				gameKey = m.group(1);
				Matcher m2 = patternFetchNextWiiGame.matcher(m.group(2));
				while(m2.find())
				{
					if(m2.group(1) == null)
						gameLinks.add("");
					else
						gameLinks.add(m2.group(1));
					gameNames.add(m2.group(2));
					gameCodes.add(m2.group(3));
				}
			}
			else
			{
				genericPairs.put(m.group(1), m.group(2));
			}
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

		if(genericPairs.size() > 0)
		{
			for(String key : genericPairs.keySet())
			{
				TextView tv = new TextView(context);
				tv.setTextColor(textPrimaryColor);
				tv.setTextSize(16);
				SpannableStringBuilder builder = new SpannableStringBuilder(key + genericPairs.get(key));
				builder.setSpan(new StyleSpan(Typeface.BOLD), 0, key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				tv.setText(builder);
				layout.addView(tv);
			}
		}

		final int count = gameLinks.size();
		if(count > 0)
		{
			TextView tv = new TextView(context);
			tv.setTextColor(textPrimaryColor);
			tv.setTextSize(16);
			tv.setTypeface(null, Typeface.BOLD);
			tv.setText(gameKey);
			layout.addView(tv);

			for(int i = 0; i < count; i++)
			{
				TextView tv2 = new TextView(context);
				tv2.setMovementMethod(LinkMovementMethod.getInstance());
				tv2.setTypeface(null, Typeface.BOLD);
				tv2.setTextColor(textPrimaryColor);
				tv2.setTextSize(12);
				SpannableStringBuilder builder = new SpannableStringBuilder(gameNames.get(i));
				final String link = gameLinks.get(i);
				if(link != null)
				{
					UnderlineSpan underSpan = new UnderlineSpan();
					builder.setSpan(underSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					try
					{
						builder.setSpan(new JvcClickableSpan(context, new URL(link), true), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					catch(MalformedURLException e)
					{
						builder.removeSpan(underSpan);
					}
				}
				tv2.setText(builder);
				layout.addView(tv2);

				TextView tv3 = new TextView(context);
				tv3.setTextColor(textPrimaryColor);
				tv3.setTextSize(13);
				tv3.setText(gameCodes.get(i));
				layout.addView(tv3);
			}
		}

		return layout;
	}
}
