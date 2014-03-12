package com.forum.jvcreader.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseArray;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.forum.jvcreader.jvc.JvcUserData;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;

public class CachedRawDrawables
{
	public static final int MAX_CACHED_NOELSHACK_DRAWABLES = 50;

	private static Resources resources;
	private static SparseArray<BitmapDrawable> bitmapSparseArray;
	private static SparseArray<DrawableSequence> drawableSequenceSparseArray;
	private static ConcurrentSkipListMap<String, BitmapDrawable> noelshackDrawableMap;

	public static void initialize(Context ctx)
	{
		resources = ctx.getResources();
		bitmapSparseArray = new SparseArray<BitmapDrawable>();
		drawableSequenceSparseArray = new SparseArray<DrawableSequence>();
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO)
		{
			noelshackDrawableMap = new ConcurrentSkipListMap<String, BitmapDrawable>();
		}
	}

	public static void unloadAllDrawables()
	{
		bitmapSparseArray.clear();
		drawableSequenceSparseArray.clear();
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO)
			noelshackDrawableMap.clear();
	}

	private static BitmapDrawable getCachedRawDrawable(int id, float sizeMultiplier)
	{
		int keyIndex = bitmapSparseArray.indexOfKey(id);

		if(keyIndex < 0)
		{
			BitmapDrawable drawable = (BitmapDrawable) BitmapDrawable.createFromStream(resources.openRawResource(id), null);
			Bitmap bitmap = drawable.getBitmap();

			int scaledDensity = (int) (bitmap.getDensity() * sizeMultiplier);
			drawable.setTargetDensity(scaledDensity);
			drawable.setBounds(0, 0, bitmap.getScaledWidth(scaledDensity), bitmap.getScaledHeight(scaledDensity));
			bitmapSparseArray.put(id, drawable);

			return drawable;
		}
		else
		{
			return bitmapSparseArray.valueAt(keyIndex);
		}
	}

	public static BitmapDrawable getDrawable(int id)
	{
		return getCachedRawDrawable(id, 1.0f);
	}

	public static BitmapDrawable getSmileyDrawable(int id)
	{
		return getCachedRawDrawable(id, JvcUserData.getSmileyScaleFactor());
	}

	private static DrawableSequence getCachedRawDrawableSequence(int id, float sizeMultiplier)
	{
		int keyIndex = drawableSequenceSparseArray.indexOfKey(id);

		if(keyIndex < 0)
		{
			ArrayList<Drawable> drawableList = new ArrayList<Drawable>();
			TypedArray sequenceArray = resources.obtainTypedArray(id);
			
			/* Retrieve drawables */
			for(int i = 0; i < sequenceArray.length(); i += 2)
			{
				Drawable drawable = getCachedRawDrawable(sequenceArray.getResourceId(i, 0), sizeMultiplier);
				for(int j = 0; j < sequenceArray.getInt(i + 1, 0); j++)
				{
					drawableList.add(drawable);
				}
			}

			sequenceArray.recycle();
			DrawableSequence sequence = new DrawableSequence(drawableList);
			drawableSequenceSparseArray.put(id, sequence);
			return sequence;
		}
		else
		{
			return drawableSequenceSparseArray.valueAt(keyIndex);
		}
	}

	public static DrawableSequence getDrawableSequence(int id)
	{
		return getCachedRawDrawableSequence(id, 1.0f);
	}

	public static DrawableSequence getSmileyDrawableSequence(int id)
	{
		return getCachedRawDrawableSequence(id, JvcUserData.getSmileyScaleFactor());
	}

	public static void nextFrameForAllDrawableSequences()
	{
		for(int i = 0; i < drawableSequenceSparseArray.size(); i++)
		{
			drawableSequenceSparseArray.valueAt(i).nextFrame();
		}
	}

	public static void pushNoelshackDrawable(String url, BitmapDrawable drawable)
	{
		if(url == null || drawable == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
			return;

		if(noelshackDrawableMap.size() == MAX_CACHED_NOELSHACK_DRAWABLES)
		{
			noelshackDrawableMap.remove(noelshackDrawableMap.firstKey()); // First in, first out
		}

		noelshackDrawableMap.putIfAbsent(url, drawable);
	}

	public static BitmapDrawable getNoelshackDrawable(String url)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
			return null;

		return noelshackDrawableMap.get(url);
	}

	public static LayoutParams getDrawableLinearLayoutParams(int id)
	{
		BitmapDrawable drawable = bitmapSparseArray.get(id);
		if(drawable == null)
			return new LinearLayout.LayoutParams(0, 0);

		Bitmap bitmap = drawable.getBitmap();

		return new LinearLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight());
	}
}
