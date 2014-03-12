package com.forum.jvcreader.graphics.jvc.cdv;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.blundell.tut.LoaderImageView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.utils.StringHelper;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletContribsrec extends CdvPortlet
{
	private static final Pattern patternFetchNextContrib = Pattern.compile("<div class=\"contrib c([12])\">\n.+\n<li class=\"img\"><a.+?><img.*?src=\"(.+?)\".*?/></a></li>\n<li class=\"type\">(.+?)</li>\n<li class=\"titre\"><a.*?href=\"(.+?)\">(.+?)</a></li>\n<li class=\"texte\"><strong class=\"date\">(.+?)</strong> ([^\u0007]*?)</li>\n(?:<li class=\"lien\"><a.*?href=\"(.+?)\">(.+?)</a></li>\n)?</ul>\n</div>");

	private final ArrayList<Contrib> contribList = new ArrayList<Contrib>();

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternFetchNextContrib.matcher(StringHelper.unescapeHTML(content));
		while(m.find())
		{
			Contrib c = new Contrib();

			c.colorSwitch = Integer.parseInt(m.group(1));
			c.imageUrl = m.group(2);
			c.type = m.group(3);
			c.titleUrl = m.group(4);
			c.title = m.group(5);
			c.date = m.group(6);
			c.text = m.group(7);
			c.suiteUrl = m.group(8);
			c.suite = m.group(9);

			contribList.add(c);
		}

		return true;
	}

	@Override
	protected View getContentView()
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);

		for(Contrib c : contribList)
		{
			View v = inflater.inflate(R.layout.contrib, null);
			((LoaderImageView) v.findViewById(R.id.contribLoaderImageView)).setImageDrawable(c.imageUrl);
			((TextView) v.findViewById(R.id.contribTypeTextView)).setText(c.type);

			TextView title = (TextView) v.findViewById(R.id.contribTitleTextView);
			title.setMovementMethod(LinkMovementMethod.getInstance());
			title.setText(JvcLinkIntent.makeLink(context, c.title, c.titleUrl, true));

			SpannableStringBuilder builder = new SpannableStringBuilder(c.date);
			builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			builder.append(" ").append(c.text);
			((TextView) v.findViewById(R.id.contribTextView)).setText(builder);

			final TextView suite = (TextView) v.findViewById(R.id.contribSuiteTextView);
			if(c.suiteUrl != null)
			{
				suite.setMovementMethod(LinkMovementMethod.getInstance());
				suite.setText(JvcLinkIntent.makeLink(context, c.suite, c.suiteUrl, true));
			}
			else
			{
				suite.setVisibility(View.GONE);
			}

			layout.addView(v);
		}

		return layout;
	}

	public class Contrib
	{
		public int colorSwitch;
		public String imageUrl;
		public String type;
		public String titleUrl;
		public String title;
		public String date;
		public String text;
		public String suiteUrl;
		public String suite;
	}
}
