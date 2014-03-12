package com.forum.jvcreader.graphics.jvc.cdv;

import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import com.blundell.tut.LoaderImageView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.CachedRawDrawables;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletAvatar extends CdvPortlet
{
	private static final Pattern patternExtractDescriptionMessage = Pattern.compile("<table.+?>\n.+src=\"(.+?)\"");

	private String url = null;

	@Override
	protected boolean parseContent()
	{
		if(content.contains("http://image.jeuxvideo.com/avatars/default.jpg"))
			return true;

		Matcher m = patternExtractDescriptionMessage.matcher(content);
		if(m.find())
		{
			url = m.group(1);
			if(content.contains("onClick=\"aff_image_grande"))
				url = url.replace("-s.", "-b.");
			return true;
		}

		return false;
	}

	@Override
	protected View getContentView()
	{
		if(url == null)
		{
			ImageView imageView = new ImageView(context);
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.CENTER_HORIZONTAL;
			imageView.setLayoutParams(params);
			imageView.setImageDrawable(CachedRawDrawables.getDrawable(R.raw.avatar_default));
			return imageView;
		}
		else
		{
			LoaderImageView imageView = new LoaderImageView(context, url);
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.CENTER_HORIZONTAL;
			imageView.setLayoutParams(params);
			return imageView;
		}
	}
}