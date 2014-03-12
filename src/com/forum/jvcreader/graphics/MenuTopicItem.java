package com.forum.jvcreader.graphics;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.*;
import com.forum.jvcreader.utils.CachedRawDrawables;

public class MenuTopicItem
{
	private Context context;
	private PmTopicItemOnClickListener checkBoxListener;

	private LinearLayout mainLayout;
	private View separatorView;
	private ImageView iconImageView;
	private TextView topicNameTextView;
	private TextView pseudoTextView;
	private TextView postCountTextView;
	private TextView dateTextView;
	private CheckBox checkBox;

	private int regularPseudoColor;
	private int adminPseudoColor;
	private boolean enableSmileys;
	private boolean animateSmileys;
	private boolean jvcLike;

	private Drawable originalBackground;
	private ColorStateList titleOriginalColors;

	public MenuTopicItem(Context context, boolean enableSmileys, boolean animateSmileys, boolean jvcLike)
	{
		if(jvcLike)
			mainLayout = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.menu_topic_item_jvc, null);
		else
			mainLayout = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.menu_topic_item_normal, null);
		originalBackground = mainLayout.getBackground();
		separatorView = mainLayout.findViewById(R.id.menuTopicItemSeparator);
		iconImageView = (ImageView) mainLayout.findViewById(R.id.menuTopicItemIconImageView);
		topicNameTextView = (TextView) mainLayout.findViewById(R.id.menuTopicItemTopicNameTextView);
		pseudoTextView = (TextView) mainLayout.findViewById(R.id.menuTopicItemPseudoTextView);
		postCountTextView = (TextView) mainLayout.findViewById(R.id.menuTopicItemPostCountTextView);
		dateTextView = (TextView) mainLayout.findViewById(R.id.menuTopicItemDateTextView);
		checkBox = (CheckBox) mainLayout.findViewById(R.id.menuTopicItemCheckBox);
		checkBox.setVisibility(View.GONE);

		titleOriginalColors = topicNameTextView.getTextColors();

		regularPseudoColor = context.getResources().getColor(R.color.jvcRegularPseudo);
		adminPseudoColor = context.getResources().getColor(R.color.jvcAdminPseudo);
		this.enableSmileys = enableSmileys;
		this.animateSmileys = animateSmileys;
		this.jvcLike = jvcLike;
		this.context = context;
	}

	public void updateDataFromTopic(JvcTopic topic)
	{
		checkBox.setVisibility(View.GONE);

		if(topic.isReplyTopic())
		{
			topicNameTextView.setTextSize(14);
			postCountTextView.setText("");
			dateTextView.setText("");
		}
		else
		{
			topicNameTextView.setTextSize(16);
			postCountTextView.setText(String.format("(%d)", topic.getExtraPostCount()));
			dateTextView.setText(topic.getExtraDate());
		}
		
		/* Topic name */
		if(JvcUserData.isPseudoInBlacklist(topic.getExtraPseudo()))
		{
			topicNameTextView.setText(R.string.topicTitleHidden);
			topicNameTextView.setTextColor(Color.GRAY);
		}
		else
		{
			CharSequence spannable = Html.fromHtml(topic.getExtraTopicName());
			if(enableSmileys)
			{
				spannable = JvcTextSpanner.getSpannableTextFromSmileyNames(spannable, animateSmileys);
			}
			topicNameTextView.setText(spannable);
			topicNameTextView.setTextColor(titleOriginalColors);
		}
		
		/* Color switch */
		if(jvcLike)
		{
			if(topic.getExtraColorSwitch() == 2)
				mainLayout.setBackgroundResource(R.drawable.menu_topic2_item);
			else
				mainLayout.setBackgroundResource(R.drawable.menu_topic1_item);
		}
		
		/* Pseudo */
		pseudoTextView.setText(topic.getExtraPseudo());
		if(topic.getExtraIsAdmin())
			pseudoTextView.setTextColor(adminPseudoColor);
		else
			pseudoTextView.setTextColor(regularPseudoColor);
		
		/* ImageView */
		iconImageView.setVisibility(View.VISIBLE);
		int rawId = JvcUtils.getRawIdFromName(topic.getExtraIconName());
		iconImageView.setImageDrawable(CachedRawDrawables.getDrawable(rawId));
		LayoutParams lParams = CachedRawDrawables.getDrawableLinearLayoutParams(rawId);
		lParams.gravity = Gravity.CENTER;
		lParams.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics());
		if(topic.isReplyTopic())
			lParams.leftMargin = 25;
		iconImageView.setLayoutParams(lParams);
	}

	public void updateDataFromCdvTopic(JvcTopic topic)
	{
		checkBox.setVisibility(View.GONE);
		iconImageView.setVisibility(View.GONE);

		topicNameTextView.setTextSize(16);
		if(enableSmileys)
			topicNameTextView.setText(JvcTextSpanner.getSpannableTextFromSmileyNames(topic.getExtraTopicName(), animateSmileys));
		else
			topicNameTextView.setText(topic.getExtraTopicName());

		pseudoTextView.setText(topic.getExtraDate());

		if(jvcLike)
		{
			if(topic.getExtraColorSwitch() == 2)
				mainLayout.setBackgroundResource(R.drawable.menu_topic2_item);
			else
				mainLayout.setBackgroundResource(R.drawable.menu_topic1_item);
		}
	}

	public void updateDataFromPmTopic(JvcPmTopic topic, boolean selected)
	{
		if(!selected)
			checkBox.setChecked(false);
		else
			checkBox.setChecked(true);
		checkBox.setVisibility(View.VISIBLE);
		iconImageView.setVisibility(View.GONE);

		topicNameTextView.setTextSize(16);
		if(enableSmileys)
			topicNameTextView.setText(JvcTextSpanner.getSpannableTextFromSmileyNames(topic.getSubject(), animateSmileys));
		else
			topicNameTextView.setText(topic.getSubject());
		if(!topic.isTopicRead())
		{
			topicNameTextView.setTypeface(null, Typeface.BOLD);
			postCountTextView.setText(context.getString(R.string.unreadPm));
		}
		else
		{
			topicNameTextView.setTypeface(Typeface.DEFAULT);
			postCountTextView.setText("");
		}

		pseudoTextView.setText(topic.getPseudo());
		if(topic.isPseudoAdmin())
			pseudoTextView.setTextColor(adminPseudoColor);
		else
			pseudoTextView.setTextColor(regularPseudoColor);
		dateTextView.setText(topic.getDate());

		if(!selected)
		{
			if(jvcLike)
			{
				if(topic.getColorSwitch() == 2)
					mainLayout.setBackgroundResource(R.drawable.menu_topic2_item);
				else
					mainLayout.setBackgroundResource(R.drawable.menu_topic1_item);
			}
			else
			{
				mainLayout.setBackgroundDrawable(originalBackground);
			}
		}
		else
		{
			mainLayout.setBackgroundResource(R.drawable.jvc_post_box_highlighted);
		}
	}

	public void updateFromUpdatedTopic(JvcTopic topic)
	{
		final UpdatedTopicData data = topic.getUpdatedTopicData();
		iconImageView.setVisibility(View.GONE);

		topicNameTextView.setTextSize(16);
		if(enableSmileys)
			topicNameTextView.setText(JvcTextSpanner.getSpannableTextFromSmileyNames(topic.getExtraTopicName(), animateSmileys));
		else
			topicNameTextView.setText(topic.getExtraTopicName());

		dateTextView.setVisibility(View.VISIBLE);
		dateTextView.setText(topic.getForum().getForumName());

		if(data.getLastError() != null)
		{
			pseudoTextView.setText(data.getLastError());
			pseudoTextView.setTextColor(Color.RED);
			pseudoTextView.setTypeface(null, Typeface.BOLD);
			dateTextView.setVisibility(View.GONE);
		}
		else if(data.isNew())
		{
			final int postCount = data.getNewPostCount();
			if(postCount > 1)
			{
				pseudoTextView.setText(String.format(context.getString(R.string.updatedTopicsNewPosts), data.getNewPostCount()));
			}
			else
			{
				pseudoTextView.setText(context.getString(R.string.updatedTopicsNewPost));
			}
			pseudoTextView.setTextColor(context.getResources().getColor(R.color.brightOrange));
			pseudoTextView.setTypeface(null, Typeface.BOLD);
		}
		else
		{
			pseudoTextView.setText(R.string.updatedTopicsNoNewPost);
			pseudoTextView.setTextColor(regularPseudoColor);
			pseudoTextView.setTypeface(Typeface.DEFAULT);
		}

		if(jvcLike)
			mainLayout.setBackgroundResource(R.drawable.menu_topic2_item);
		else
			mainLayout.setBackgroundDrawable(originalBackground);
	}

	public void emptyItem()
	{
		if(jvcLike)
			mainLayout.setBackgroundResource(R.color.jvcTopicBackground1);
		else
			mainLayout.setBackgroundDrawable(originalBackground);
		topicNameTextView.setText("");
		topicNameTextView.setTypeface(Typeface.DEFAULT);
		postCountTextView.setText("");
		pseudoTextView.setText("");
		dateTextView.setText("");
		iconImageView.setImageDrawable(null);
		checkBox.setVisibility(View.GONE);
	}

	public void setCheckboxVisible(boolean visible)
	{
		if(visible)
		{
			checkBox.setChecked(false);
			checkBox.setVisibility(View.VISIBLE);
		}
		else
		{
			checkBox.setVisibility(View.GONE);
		}
	}

	public void setCheckboxChecked(boolean checked)
	{
		checkBox.setChecked(checked);
	}

	public void setTopicName(String topicName)
	{
		topicNameTextView.setText(topicName);
	}

	public void setItemOnClickListener(OnClickListener listener)
	{
		mainLayout.setOnClickListener(listener);
	}

	public void setPmTopicItemOnClickListener(PmTopicItemOnClickListener listener)
	{
		checkBoxListener = listener;
		checkBox.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				checkBoxListener.onClick(((CheckBox) v).isChecked());
			}
		});
	}

	public void setSeparatorVisibility(int visibility)
	{
		separatorView.setVisibility(visibility);
	}

	public View getView()
	{
		return mainLayout;
	}

	public View getSeparator()
	{
		return separatorView;
	}

	public static abstract class PmTopicItemOnClickListener
	{
		protected int id;

		public PmTopicItemOnClickListener(int id)
		{
			this.id = id;
		}

		public abstract void onClick(boolean checked);
	}
}
