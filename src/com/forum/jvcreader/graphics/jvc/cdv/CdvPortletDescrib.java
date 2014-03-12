package com.forum.jvcreader.graphics.jvc.cdv;

import android.graphics.Typeface;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.forum.jvcreader.jvc.JvcTextSpanner;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.noelshack.NoelshackSpansLoader;
import com.forum.jvcreader.utils.StringHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletDescrib extends CdvPortlet
{
	private static final Pattern patternExtractDescriptionMessage = Pattern.compile("<p>([^\u0007]+?)</p>");

	private String description = null;
	private NoelshackSpansLoader loader;

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternExtractDescriptionMessage.matcher(content);
		if(m.find())
		{
			description = StringHelper.unescapeHTML(m.group(1)) + " ";
			return true;
		}

		return false;
	}

	@Override
	protected View getContentView()
	{
		TextView desc = new TextView(context);
		loader = new NoelshackSpansLoader(context, desc);

		desc.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		desc.setText(JvcTextSpanner.getSpannableTextFromCdvDescription(loader, description, JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS), JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS)));
		loader.startLoading();
		desc.setTextSize(12);
		desc.setTypeface(Typeface.SANS_SERIF);

		desc.setMovementMethod(LinkMovementMethod.getInstance());
		desc.setTextColor(textPrimaryColor);

		return desc;
	}
}
