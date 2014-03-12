package com.forum.jvcreader.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;

public class DrawableSequenceSpan extends DynamicDrawableSpan
{
	private DrawableSequence sequence;

	public DrawableSequenceSpan(DrawableSequence sequence, int align)
	{
		super(align);
		this.sequence = sequence;
	}

	@Override
	public Drawable getDrawable()
	{
		return sequence.getCurrentDrawable();
	}

	/* Avoid DynamicDrawableSpan's drawable caching */

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
