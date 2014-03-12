package com.forum.jvcreader.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.CachedRawDrawables;

public class CancelableLabel extends LinearLayout
{
	private Context context;

	private TextView labelTv;
	private ImageView cancelView;
	private OnClickListener cancelListener;

	public CancelableLabel(Context context)
	{
		super(context);
		this.context = context;
		instantiate();
	}

	public CancelableLabel(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
		instantiate();
	}

	private void instantiate()
	{
		LayoutParams params;
		final int fiveDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics());
		setOrientation(LinearLayout.HORIZONTAL);
		setBackgroundResource(R.drawable.cancelable_label_background);
		setPadding(fiveDp, fiveDp * 2, fiveDp, fiveDp * 2);

		params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.rightMargin = fiveDp;
		params.gravity = Gravity.CENTER;
		labelTv = new TextView(context);
		labelTv.setLayoutParams(params);
		labelTv.setTextColor(Color.WHITE);
		labelTv.setTextSize(18);
		addView(labelTv);

		Drawable cancelDrawable = CachedRawDrawables.getDrawable(R.raw.cancel_icon);
		params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER;
		cancelView = new ImageView(context);
		cancelView.setLayoutParams(params);
		cancelView.setImageDrawable(cancelDrawable);
		cancelView.setClickable(true);
		addView(cancelView);
	}

	public void setText(CharSequence text)
	{
		labelTv.setText(text);
	}

	public void setTextColor(int color)
	{
		labelTv.setTextColor(color);
	}

	public CharSequence getText()
	{
		return labelTv.getText();
	}

	public void setCancelOnClickListener(OnClickListener l)
	{
		cancelListener = l;
		cancelView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				cancelListener.onClick(CancelableLabel.this);
			}
		});
	}
}
