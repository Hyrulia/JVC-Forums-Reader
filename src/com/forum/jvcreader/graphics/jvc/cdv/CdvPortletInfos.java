package com.forum.jvcreader.graphics.jvc.cdv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.StringHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletInfos extends CdvPortlet
{
	private String age, country, memberSince, lastSeen;

	private static final Pattern patternExtractUserInfo = Pattern.compile("summary=\"infos pseudo\">\n<tr><th scope=\"row\">Age</th><td>(.+?)</td></tr>\n<tr><th scope=\"row\">Pays</th><td>(.+?)</td></tr>\n<tr><th scope=\"row\">Membre depuis</th>\n<td>(.+?)</td></tr>\n(?:<tr><th scope=\"row\">Dernier passage</th><td>(.+?)</td></tr>)?</table>");

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternExtractUserInfo.matcher(StringHelper.unescapeHTML(content));
		if(m.find())
		{
			age = m.group(1);
			country = m.group(2);
			memberSince = m.group(3);
			lastSeen = m.group(4);

			return true;
		}

		return false;
	}

	@Override
	protected View getContentView()
	{
		View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cdv_portlet_infos, null);

		((TextView) view.findViewById(R.id.cdvInfosAgeTextView)).setText(age);
		((TextView) view.findViewById(R.id.cdvInfosCountryTextView)).setText(country);
		((TextView) view.findViewById(R.id.cdvInfosMemberSinceTextView)).setText(memberSince);
		((TextView) view.findViewById(R.id.cdvInfosLastSeenTextView)).setText(lastSeen);

		return view;
	}
}