package com.forum.jvcreader.graphics.jvc.cdv;

import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.utils.StringHelper;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletArticlespref extends CdvPortlet
{
	private static final Pattern patternFetchNextElement = Pattern.compile("<tr id=\".+?\"><td class=\"date\">(.+?)</td>\n<td class=\"type\">(.+?)</td>\n<td class=\"titre\"><p><a.*?href=\"(.+?)\".*?>(.+?)</a>.*?<strong>(.+?)</strong></p></td></tr>");

	private final ArrayList<JvcPrefElement> elementList = new ArrayList<JvcPrefElement>();

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternFetchNextElement.matcher(StringHelper.unescapeHTML(content));
		while(m.find())
		{
			elementList.add(new JvcPrefElement(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)));
		}

		return elementList.size() > 0;
	}

	@Override
	protected View getContentView()
	{
		LinearLayout layout = new LinearLayout(context);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		layout.setLayoutParams(params);
		layout.setOrientation(LinearLayout.VERTICAL);

		boolean first = false;
		for(JvcPrefElement e : elementList)
		{
			CdvPrefItem item = new CdvPrefItem(context);

			if(!first)
			{
				item.setSeparatorVisibility(View.GONE);
				first = true;
			}

			SpannableStringBuilder builder = JvcLinkIntent.makeLink(context, e.getName(), e.getLink(), true);
			builder.append(" ").append(e.getSubname());
			item.setData(e.getDate(), e.getType(), builder);
			layout.addView(item.getView());
		}

		return layout;
	}

	private class JvcPrefElement
	{
		private String date;
		private String type;
		private String link;
		private String name;
		private String subname;

		public JvcPrefElement(String date, String type, String link, String name, String subname)
		{
			this.date = date;
			this.type = type;
			this.link = link;
			this.name = name;
			this.subname = subname;
		}

		public String getDate()
		{
			return date;
		}

		public String getType()
		{
			return type;
		}

		public String getLink()
		{
			return link;
		}

		public String getName()
		{
			return name;
		}

		public String getSubname()
		{
			return subname;
		}
	}
}
