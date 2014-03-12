package com.forum.jvcreader.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.forum.jvcreader.R;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class MenuCollapsibleItem extends LinearLayout
{
	private static final String ARROW_RIGHT = "\u25B6";
	private static final String ARROW_DOWN = "\u25BC";

	private Context context;

	private TextView arrowTextView;
	private TextView itemTextView;

	private LinearLayout collapsibleLayout;

	private ColorStateList enabledColorList, disabledColorList;

	private boolean isCollapsed = false;

	public MenuCollapsibleItem(Context context, AttributeSet attrs) throws IOException, NotFoundException, XmlPullParserException
	{
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MenuCollapsibleItem);
		enabledColorList = a.getColorStateList(R.styleable.MenuCollapsibleItem_itemTextColor);
		disabledColorList = a.getColorStateList(R.styleable.MenuCollapsibleItem_itemTextColorDisabled);
		
		/* Main layout attributes */
		this.setOrientation(VERTICAL); /* Override orientation to vertical */
		this.setClickable(true);
		this.setOnClickListener(itemClickListener);
		
		/* Item layout attributes */
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(HORIZONTAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		int tenDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
		layout.setPadding(0, tenDp, 0, tenDp);
		
		/* Arrow */
		arrowTextView = new TextView(context);
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER;
		arrowTextView.setLayoutParams(params);
		arrowTextView.setTextSize(30);
		if(enabledColorList != null)
			arrowTextView.setTextColor(enabledColorList);
		arrowTextView.setText(ARROW_RIGHT);
		
		/* Item text */
		itemTextView = new TextView(context);
		params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
		params.weight = 1;
		params.gravity = Gravity.CENTER;
		params.leftMargin = tenDp;
		itemTextView.setLayoutParams(params);
		itemTextView.setTextSize(18);
		if(enabledColorList != null)
			itemTextView.setTextColor(enabledColorList);
		itemTextView.setText(a.getString(R.styleable.MenuCollapsibleItem_itemText));

		layout.addView(arrowTextView);
		layout.addView(itemTextView);
		this.addView(layout);
		
		/* Collapsible layout */
		collapsibleLayout = new LinearLayout(context);
		collapsibleLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		collapsibleLayout.setPadding(tenDp, 0, tenDp, tenDp);
		collapsibleLayout.setOrientation(VERTICAL);
		collapsibleLayout.setVisibility(View.GONE);
		collapsibleLayout.setClickable(true);
		
		/* Line separator */
		View view = new View(context);
		params = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		params.bottomMargin = tenDp;
		view.setLayoutParams(params);
		view.setBackgroundResource(R.drawable.gradient_black_transparent);
		collapsibleLayout.addView(view);
		this.addView(collapsibleLayout);

		this.context = context;
		a.recycle();
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		if(enabled)
		{
			setClickable(true);
			arrowTextView.setTextColor(enabledColorList);
			itemTextView.setTextColor(enabledColorList);
		}
		else
		{
			if(isCollapsed)
				toggleCollapsibleLayout();
			setClickable(false);
			arrowTextView.setTextColor(disabledColorList);
			itemTextView.setTextColor(disabledColorList);
		}
	}

	private final OnClickListener itemClickListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			toggleCollapsibleLayout();
		}
	};

	public void addViewInCollapsibleLayout(View view)
	{
		collapsibleLayout.addView(view);
	}

	public void toggleCollapsibleLayout()
	{
		if(!isCollapsed)
		{
			arrowTextView.setText(ARROW_DOWN);
			collapsibleLayout.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_slide_in));
			collapsibleLayout.setVisibility(View.VISIBLE);
			isCollapsed = true;
		}
		else
		{
			arrowTextView.setText(ARROW_RIGHT);

			collapsibleLayout.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_slide_out));
			collapsibleLayout.startLayoutAnimation();
			collapsibleLayout.postDelayed(new Runnable()
			{
				public void run()
				{
					collapsibleLayout.setVisibility(View.GONE);
				}
			}, 200);

			isCollapsed = false;
		}
	}
}
