package com.forum.jvcreader.graphics.jvc.cdv;

import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.StringHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletWeb extends CdvPortlet
{
	private static final String SOCIAL_NETWORKS_KEY = "R\u00E9seaux sociaux :";

	private static final Pattern patternFetchNextKeyValueLink = Pattern.compile("<li><strong>(.+?)</strong> (.+?)</li>");
	private static final Pattern patternFetchNextSocialNetwork = Pattern.compile("<a.*?href=\"(.+?)\".*?><img.*?src=\"http://image.jeuxvideo.com/pics/cdv/(.+?\\.png)\".*?/></a>");
	private static final Pattern patternFetchGenericLink = Pattern.compile("(?:<a.*?href=\"(.+?)\".*?>)?(.+?)(?:</a>)?$");

	private final LinkedHashMap<String, String> socialNetworks = new LinkedHashMap<String, String>();

	private final ArrayList<String> genericKeys = new ArrayList<String>();
	private final ArrayList<String> genericLinks = new ArrayList<String>();
	private final ArrayList<String> genericNames = new ArrayList<String>();

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternFetchNextKeyValueLink.matcher(content);
		while(m.find())
		{
			if(m.group(1).equals(SOCIAL_NETWORKS_KEY))
			{
				Matcher m2 = patternFetchNextSocialNetwork.matcher(m.group(2));
				while(m2.find())
				{
					socialNetworks.put(StringHelper.unescapeHTML(m2.group(2)), StringHelper.unescapeHTML(m2.group(1)));
				}
			}
			else
			{
				Matcher m2 = patternFetchGenericLink.matcher(m.group(2));
				if(!m2.find())
					return false;
				genericKeys.add(StringHelper.unescapeHTML(m.group(1)));
				genericLinks.add(StringHelper.unescapeHTML(m2.group(1)));
				genericNames.add(StringHelper.unescapeHTML(m2.group(2)));
			}
		}

		return true;
	}

	@Override
	protected View getContentView()
	{
		final int tenDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics());
		LinearLayout layout = new LinearLayout(context);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		layout.setLayoutParams(params);
		layout.setOrientation(LinearLayout.VERTICAL);

		if(socialNetworks.size() > 0)
		{
			LinearLayout socialLayout = new LinearLayout(context);
			socialLayout.setOrientation(LinearLayout.HORIZONTAL);

			TextView tv = new TextView(context);
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.CENTER;
			tv.setLayoutParams(params);
			tv.setText(SOCIAL_NETWORKS_KEY);
			tv.setTextColor(textPrimaryColor);
			tv.setTextSize(16);
			socialLayout.addView(tv);

			params = new LayoutParams(tenDp * 8, tenDp * 8);
			params.leftMargin = tenDp;

			for(String key : socialNetworks.keySet())
			{
				final int drawableId = JvcUtils.getRawIdFromName(key);
				if(drawableId != -1)
				{
					ImageButton button = new ImageButton(context);
					button.setLayoutParams(params);
					button.setImageDrawable(CachedRawDrawables.getDrawable(drawableId));
					try
					{
						button.setOnClickListener(new UrlOnClickListener(new URL(socialNetworks.get(key))));
					}
					catch(MalformedURLException e)
					{
						Log.w("JvcForumsReader", "CdvPortletWeb : malformed URL " + socialNetworks.get(key));
					}
					socialLayout.addView(button);
				}
				else
				{
					Log.w("JvcForumsReader", "CdvPortletWeb : social network icon " + key + " not found !");
				}
			}

			layout.addView(socialLayout);
		}

		final int count = genericKeys.size();
		if(count > 0)
		{
			for(int i = 0; i < count; i++)
			{
				TextView tv = new TextView(context);
				tv.setMovementMethod(LinkMovementMethod.getInstance());
				tv.setTextColor(textPrimaryColor);
				tv.setTextSize(16);
				SpannableStringBuilder builder = new SpannableStringBuilder(genericKeys.get(i) + " ");
				builder.append(JvcLinkIntent.makeLink(context, genericNames.get(i), genericLinks.get(i), true));
				tv.setText(builder);
				layout.addView(tv);
			}
		}

		return layout;
	}

	private class UrlOnClickListener implements OnClickListener
	{
		JvcLinkIntent jvcIntent;

		public UrlOnClickListener(URL url)
		{
			jvcIntent = new JvcLinkIntent(context, url);
		}

		public void onClick(View view)
		{
			jvcIntent.startIntent();
		}
	}
}
