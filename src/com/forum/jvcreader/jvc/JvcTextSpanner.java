package com.forum.jvcreader.jvc;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import com.forum.jvcreader.jvc.JvcUtils.JvcLinkIntent;
import com.forum.jvcreader.noelshack.NoelshackClickableSpan;
import com.forum.jvcreader.noelshack.NoelshackImageSpan;
import com.forum.jvcreader.noelshack.NoelshackSpansLoader;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.DrawableSequence;
import com.forum.jvcreader.utils.DrawableSequenceSpan;
import com.forum.jvcreader.utils.PatternCollection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;

public class JvcTextSpanner
{
	public static int JVC_SMILEY_LIMIT = 20;

	public static Spannable getSpannableTextFromSmileyNames(CharSequence text, boolean animateSmileys)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder(text).append(' ');

		Matcher m = PatternCollection.fetchNextTextualSmiley.matcher(builder);
		while(m.find())
		{
			int sequenceId = JvcUtils.getSequenceIdFromName(m.group(1));
			if(!animateSmileys || sequenceId == -1)
			{
				Drawable drawable = CachedRawDrawables.getSmileyDrawable(JvcUtils.getRawIdFromName(m.group(1)));
				builder.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			else
			{
				DrawableSequence sequence = CachedRawDrawables.getSmileyDrawableSequence(sequenceId);
				builder.setSpan(new DrawableSequenceSpan(sequence, ImageSpan.ALIGN_BASELINE), m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		return builder;
	}

	public static Spannable getColoredSpannableText(String text, int color)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder();

		Matcher m = PatternCollection.findColorTagOnce.matcher(text);
		if(m.find())
		{
			builder.append(m.group(1));
			builder.append(m.group(2));
			builder.setSpan(new ForegroundColorSpan(color), m.group(1).length(), m.group(1).length() + m.group(2).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			builder.append(m.group(3));
		}

		return builder;
	}

	public static Spannable getSpannableTextFromTopicPost(NoelshackSpansLoader loader, CharSequence text, boolean showThumbnails, boolean animateSmileys)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder(text.toString().replaceAll("<(br /|BR)>", "\n"));

		Matcher m = PatternCollection.fetchNextSmiley.matcher(builder);
		while(m.find())
		{
			int sequenceId = JvcUtils.getSequenceIdFromName(m.group(2));
			if(!animateSmileys || sequenceId == -1)
			{
				final int drawableId = JvcUtils.getRawIdFromName(m.group(2));
				if(drawableId != -1)
				{
					Drawable cachedDrawable = CachedRawDrawables.getSmileyDrawable(drawableId);
					builder.setSpan(new ImageSpan(cachedDrawable, ImageSpan.ALIGN_BASELINE), m.start(), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				else
				{
					Log.w("JvcForumsReader", "JvcTextSpanner : smiley " + m.group(2) + " not found !");
				}
			}
			else
			{
				DrawableSequence sequence = CachedRawDrawables.getSmileyDrawableSequence(sequenceId);
				builder.setSpan(new DrawableSequenceSpan(sequence, ImageSpan.ALIGN_BASELINE), m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		m = PatternCollection.fetchNextHtmlLink.matcher(builder);
		while(m.find())
		{
			try
			{
				String urlString = m.group(1);
				URL url = new URL(urlString);
				Matcher m2 = PatternCollection.fetchNoelshackImageUrlData.matcher(urlString);
				int start = m.start();

				if(showThumbnails && m2.find())
				{
					builder = builder.replace(start, m.end(), "i");
					final NoelshackImageSpan imageSpan = new NoelshackImageSpan(loader, m2.group(1), m2.group(2), m2.group(3), m2.group(4));
					builder.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					builder.setSpan(new NoelshackClickableSpan(loader.getContext(), imageSpan), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				else
				{
					builder = builder.replace(m.start(), m.end(), urlString);

					int end = start + urlString.length();
					UnderlineSpan underSpan = new UnderlineSpan();
					builder.setSpan(underSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					builder.setSpan(new JvcClickableSpan(loader.getContext(), url, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

				m = PatternCollection.fetchNextHtmlLink.matcher(builder);
			}
			catch(MalformedURLException e)
			{
				/* TODO */
			}
		}

		return builder;
	}

	public static Spannable getSpannableTextFromCdvDescription(NoelshackSpansLoader loader, CharSequence text, boolean showThumbnails, boolean animateSmileys)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder(text.toString().replace("<br />", ""));

		Matcher m = PatternCollection.fetchNextSmiley.matcher(builder);
		while(m.find())
		{
			int sequenceId = JvcUtils.getSequenceIdFromName(m.group(2));
			if(!animateSmileys || sequenceId == -1)
			{
				final int drawableId = JvcUtils.getRawIdFromName(m.group(2));
				if(drawableId != -1)
				{
					Drawable cachedDrawable = CachedRawDrawables.getSmileyDrawable(drawableId);
					builder.setSpan(new ImageSpan(cachedDrawable, ImageSpan.ALIGN_BASELINE), m.start() + 1, m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				else
				{
					Log.w("JvcForumsReader", "JvcTextSpanner : smiley " + m.group(2) + " not found !");
				}
			}
			else
			{
				DrawableSequence sequence = CachedRawDrawables.getSmileyDrawableSequence(sequenceId);
				builder.setSpan(new DrawableSequenceSpan(sequence, ImageSpan.ALIGN_BASELINE), m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		m = PatternCollection.fetchNextHtmlLink.matcher(builder);
		while(m.find())
		{
			try
			{
				String urlString = m.group(1);
				URL url = new URL(urlString);
				Matcher m2 = PatternCollection.fetchNoelshackImageUrlData.matcher(urlString);
				int start = m.start();

				if(showThumbnails && m2.find())
				{
					builder = builder.replace(start, m.end(), "i");
					final NoelshackImageSpan imageSpan = new NoelshackImageSpan(loader, m2.group(1), m2.group(2), m2.group(3), m2.group(4));
					builder.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					builder.setSpan(new NoelshackClickableSpan(loader.getContext(), imageSpan), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				else
				{
					builder = builder.replace(m.start(), m.end(), urlString);

					int end = start + urlString.length();
					UnderlineSpan underSpan = new UnderlineSpan();
					builder.setSpan(underSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					builder.setSpan(new JvcClickableSpan(loader.getContext(), url, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

				m = PatternCollection.fetchNextHtmlLink.matcher(builder);
			}
			catch(MalformedURLException e)
			{
				/* TODO */
			}
		}

		return builder;
	}

	public static Spannable getSpannableTextFromTopicTextualPost(NoelshackSpansLoader loader, CharSequence text, boolean showThumbnails, boolean animateSmileys)
	{
		int limit = 0;
		SpannableStringBuilder builder = new SpannableStringBuilder(text);

		Matcher m = PatternCollection.fetchNextTextualSmiley.matcher(builder);
		while(m.find())
		{
			if(limit >= JVC_SMILEY_LIMIT)
				break;

			int sequenceId = JvcUtils.getSequenceIdFromName(m.group(1));
			if(!animateSmileys || sequenceId == -1)
			{
				Drawable drawable = CachedRawDrawables.getSmileyDrawable(JvcUtils.getRawIdFromName(m.group(1)));
				builder.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			else
			{
				DrawableSequence sequence = CachedRawDrawables.getSmileyDrawableSequence(sequenceId);
				builder.setSpan(new DrawableSequenceSpan(sequence, ImageSpan.ALIGN_BASELINE), m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}

			limit++;
		}

		m = PatternCollection.fetchNextLink.matcher(builder);
		while(m.find())
		{
			String url = m.group();
			Matcher m2 = PatternCollection.fetchNoelshackImageUrlData.matcher(url);
			int start = m.start();

			if(showThumbnails && m2.find())
			{
				try
				{
					builder = builder.replace(start, m.end(), "i");
					NoelshackImageSpan imageSpan = new NoelshackImageSpan(loader, m2.group(1), m2.group(2), m2.group(3), m2.group(4));
					builder.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				catch(IndexOutOfBoundsException e)
				{
					Log.w("JvcForumReaders", "JvcTextSpanner : " + e.toString());
				}
				catch(MalformedURLException e)
				{
					Log.w("JvcForumReaders", "JvcTextSpanner : " + e.toString());
				}
			}
			else
			{
				UnderlineSpan underSpan = new UnderlineSpan();
				builder.setSpan(underSpan, start, m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				try
				{
					builder.setSpan(new JvcClickableSpan(loader.getContext(), new URL(url), false), start, m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				catch(MalformedURLException e)
				{
					builder.removeSpan(underSpan);
				}
			}
		}

		return builder;
	}

	public static class JvcClickableSpan extends ClickableSpan
	{
		private boolean enabled;

		private JvcLinkIntent jvcIntent;

		public JvcClickableSpan(Context context, URL url, boolean enabled)
		{
			this.enabled = enabled;

			if(enabled)
			{
				jvcIntent = new JvcLinkIntent(context, url);
			}
		}

		public void onClick(View view)
		{
			if(enabled)
			{
				jvcIntent.startIntent();
			}
		}
	}
}
