package com.blundell.tut;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.forum.jvcreader.R;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Free for anyone to use, just say thanks and share :-)
 *
 * @author Blundell
 */
public class LoaderImageView extends LinearLayout
{

	private static final int COMPLETE = 0;
	private static final int FAILED = 1;

	private Context mContext;
	private Drawable mDrawable;
	private ImageView mImage;
	private TextView mTv;
	private boolean mLoadingImage;

	/**
	 * This is used when creating the view in XML
	 * To have an image load in XML use the tag 'image="http://developer.android.com/images/dialog_buttons.png"'
	 * Replacing the url with your desired image
	 * Once you have instantiated the XML view you can call
	 * setImageDrawable(url) to change the image
	 *
	 * @param context
	 * @param attrSet
	 */
	public LoaderImageView(final Context context, final AttributeSet attrSet)
	{
		super(context, attrSet);

		final String url = attrSet.getAttributeValue(null, "image");
		if(url != null)
		{
			instantiate(context, url);
		}
		else
		{
			instantiate(context, null);
		}
	}

	/**
	 * This is used when creating the view programmatically
	 * Once you have instantiated the view you can call
	 * setImageDrawable(url) to change the image
	 *
	 * @param context  the Activity context
	 * @param imageUrl the Image URL you wish to load
	 */
	public LoaderImageView(final Context context, final String imageUrl)
	{
		super(context);
		instantiate(context, imageUrl);
	}

	/**
	 * First time loading of the LoaderImageView
	 * Sets up the LayoutParams of the view, you can change these to
	 * get the required effects you want
	 */
	private void instantiate(final Context context, final String imageUrl)
	{
		mContext = context;

		mImage = new ImageView(mContext);
		mImage.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mImage.setScaleType(ScaleType.CENTER_CROP);
		mImage.setVisibility(View.GONE);

		mTv = new TextView(mContext);
		mTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mTv.setText(R.string.loaderImageViewLoading);
		mTv.setTextColor(Color.WHITE);
		mTv.setGravity(Gravity.CENTER);

		addView(mTv);
		addView(mImage);

		if(imageUrl != null)
		{
			setImageDrawable(imageUrl);
		}
	}

	/**
	 * Set's the view's drawable, this uses the internet to retrieve the image
	 * don't forget to add the correct permissions to your manifest
	 *
	 * @param imageUrl the url of the image you wish to load
	 */
	public void setImageDrawable(final String imageUrl)
	{
		if(!mLoadingImage)
		{
			mLoadingImage = true;
			mDrawable = null;
			mImage.setVisibility(View.GONE);
			mTv.setVisibility(View.VISIBLE);
			mTv.setText(R.string.loaderImageViewLoading);
			mTv.setTextColor(Color.WHITE);
			new Thread()
			{
				public void run()
				{
					try
					{
						mDrawable = getDrawableFromUrl(imageUrl);
						if(mDrawable == null)
						{
							imageLoadedHandler.sendEmptyMessage(FAILED);
						}
						else
						{
							imageLoadedHandler.sendEmptyMessage(COMPLETE);
						}
					}
					catch(MalformedURLException e)
					{
						imageLoadedHandler.sendEmptyMessage(FAILED);
					}
					catch(IOException e)
					{
						imageLoadedHandler.sendEmptyMessage(FAILED);
					}
				}
			}.start();
		}
	}

	/**
	 * Callback that is received once the image has been downloaded
	 */
	private final Handler imageLoadedHandler = new Handler(new Callback()
	{
		@Override
		public boolean handleMessage(Message msg)
		{
			mLoadingImage = false;

			switch(msg.what)
			{
				case COMPLETE:
					mImage.setImageDrawable(mDrawable);
					mImage.setVisibility(View.VISIBLE);
					mTv.setVisibility(View.GONE);
					break;
				case FAILED:
				default:
					mTv.setText(R.string.loaderImageViewError);
					mTv.setTextColor(Color.RED);
					break;
			}
			return true;
		}
	});

	/**
	 * Pass in an image url to get a drawable object
	 *
	 * @return a drawable object
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static Drawable getDrawableFromUrl(final String url) throws IOException
	{
		return Drawable.createFromStream(((java.io.InputStream) new java.net.URL(url).getContent()), "name");
	}

}
