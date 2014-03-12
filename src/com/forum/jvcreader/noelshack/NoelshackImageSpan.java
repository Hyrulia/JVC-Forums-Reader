package com.forum.jvcreader.noelshack;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.DrawableSequence;

import java.net.MalformedURLException;
import java.net.URL;

public class NoelshackImageSpan extends DynamicDrawableSpan
{
	private DrawableSequence loadingDrawableSequence;
	private Drawable drawable = null;

	private URL thumbnailUrl;
	private URL destinationUrl;

	public NoelshackImageSpan(NoelshackSpansLoader loader, String year, String month, String imageName, String ext) throws MalformedURLException
	{
		this(loader, year, month, imageName, ext, ALIGN_BOTTOM);
	}

	public NoelshackImageSpan(final NoelshackSpansLoader loader, String year, String month, String imageName, String ext, int verticalAlignment) throws MalformedURLException
	{
		super(verticalAlignment);

		loadingDrawableSequence = CachedRawDrawables.getDrawableSequence(R.array.loadingThumbnailSequence);
		generateUrls(year, month, imageName, ext);

		loader.addSpan(this);
	}

	public void setDrawable(Drawable drawable)
	{
		this.drawable = drawable;
	}

	public URL getThumbnailUrl()
	{
		return thumbnailUrl;
	}

	public URL getDestinationUrl()
	{
		return destinationUrl;
	}

	private void generateUrls(String year, String month, String imageName, String ext) throws MalformedURLException
	{
		String thumbnailString = String.format("http://image.noelshack.com/minis/%s/%s/%s.png", year, month, imageName);
		thumbnailUrl = new URL(thumbnailString);
		String destinationString = String.format("http://image.noelshack.com/fichiers/%s/%s/%s%s", year, month, imageName, ext);
		destinationUrl = new URL(destinationString);
	}

	@Override
	public Drawable getDrawable()
	{
		if(drawable != null)
			return drawable;

		return loadingDrawableSequence.getCurrentDrawable();
	}

	@Override
	public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm)
	{
		Drawable d = getDrawable();
		Rect rect = d.getBounds();

		if(fm != null)
		{
			fm.ascent = -rect.bottom;
			fm.descent = 0;

			fm.top = fm.ascent;
			fm.bottom = 0;
		}

		return rect.right;
	}

	@Override
	public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint)
	{
		Drawable d = getDrawable();
		canvas.save();

		int transY = bottom - d.getBounds().bottom;
		if(mVerticalAlignment == ALIGN_BASELINE)
		{
			transY -= paint.getFontMetricsInt().descent;
		}

		canvas.translate(x, transY);
		d.draw(canvas);
		canvas.restore();
	}
}