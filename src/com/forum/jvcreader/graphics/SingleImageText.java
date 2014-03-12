package com.forum.jvcreader.graphics;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import com.forum.jvcreader.utils.CachedRawDrawables;

public class SingleImageText
{
	private ImageSpan imageSpan;
	private Spannable spanText;

	public SingleImageText(String text, int drawableId)
	{
		spanText = new SpannableStringBuilder(text);
		imageSpan = new ImageSpan(CachedRawDrawables.getDrawable(drawableId), ImageSpan.ALIGN_BASELINE);
		int index = text.indexOf("{*}");
		spanText.setSpan(imageSpan, index, index + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	public CharSequence getText()
	{
		return spanText;
	}

	public Drawable getDrawable()
	{
		return imageSpan.getDrawable();
	}

	public void setImageAlpha(int alpha)
	{
		imageSpan.getDrawable().setAlpha(alpha);
	}
}
