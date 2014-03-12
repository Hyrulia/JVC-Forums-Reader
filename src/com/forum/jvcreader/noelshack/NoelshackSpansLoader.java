package com.forum.jvcreader.noelshack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.CachedRawDrawables;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class NoelshackSpansLoader
{
	public static final int NOELSHACK_THUMBNAIL_WIDTH = 136;
	public static final int NOELSHACK_THUMBNAIL_HEIGHT = 102;

	private Context context;
	private View view;
	private ArrayList<NoelshackImageSpan> spans = new ArrayList<NoelshackImageSpan>();
	private Thread thread = null;

	public NoelshackSpansLoader(Context context, View view)
	{
		this.context = context;
		this.view = view;
	}

	public Context getContext()
	{
		return context;
	}

	public View getView()
	{
		return view;
	}

	public void addSpan(NoelshackImageSpan span)
	{
		spans.add(span);
	}

	public void startLoading()
	{
		if(spans.size() > 0 && (thread == null || !thread.isAlive()))
		{
			thread = new Thread()
			{
				public void run()
				{
					try
					{
						for(Iterator<NoelshackImageSpan> it = spans.iterator(); it.hasNext(); )
						{
							if(thread.isInterrupted())
								break;
							NoelshackImageSpan span = it.next();
							String url = span.getThumbnailUrl().toString();
							BitmapDrawable drawable = CachedRawDrawables.getNoelshackDrawable(url);

							if(drawable == null)
							{
								drawable = getNoelshackThumbnailFromURL(span.getThumbnailUrl());
								if(drawable == null)
								{
									drawable = CachedRawDrawables.getDrawable(R.raw.avatar_default);
									Bitmap bitmap = drawable.getBitmap();
									if(bitmap != null)
									{
										drawable.setTargetDensity(bitmap.getDensity());
										drawable.setBounds(0, 0, NOELSHACK_THUMBNAIL_WIDTH, NOELSHACK_THUMBNAIL_HEIGHT);
									}
								}
								else
								{
									CachedRawDrawables.pushNoelshackDrawable(url, drawable);
								}
							}
							span.setDrawable(drawable);
							view.postInvalidate();
						}
						spans.clear();
					}
					catch(IOException e)
					{
						Log.w("JvcForumsReader", "NoelshackSpansLoader : " + e.toString());
					}
				}
			};

			thread.start();
		}
	}

	public void clearAndStopLoading()
	{
		if(thread != null && thread.isAlive())
		{
			thread.interrupt();
		}
	}

	private BitmapDrawable getNoelshackThumbnailFromURL(final URL url) throws IOException
	{
		BitmapDrawable drawable = (BitmapDrawable) BitmapDrawable.createFromStream(((java.io.InputStream) url.getContent()), "name");
		if(drawable == null)
			return null;
		Bitmap bitmap = drawable.getBitmap();
		if(bitmap != null)
		{
			drawable.setTargetDensity(bitmap.getDensity());
			drawable.setBounds(0, 0, NOELSHACK_THUMBNAIL_WIDTH, NOELSHACK_THUMBNAIL_HEIGHT);
		}

		return drawable;
	}
}
