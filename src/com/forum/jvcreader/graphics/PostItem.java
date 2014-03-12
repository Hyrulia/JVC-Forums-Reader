package com.forum.jvcreader.graphics;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcPost;
import com.forum.jvcreader.jvc.JvcTextSpanner;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.noelshack.NoelshackSpansLoader;

public class PostItem
{
	private Context context;
	private NoelshackSpansLoader loader;

	private LinearLayout mainLayout;
	private LinearLayout titleBarLayout;
	private View separatorView;
	private TextView pseudoTextView;
	private TextView dateTextView;
	private TextView postTextView;

	private int regularPseudoColor;
	private int adminPseudoColor;

	private boolean jvcLike;
	private boolean showNoelshackThumbnails;
	private boolean animateSmileys;

	private int textColorPrimary;
	private Drawable defaultBackground;

	public PostItem(Context context, boolean jvcLike, boolean showNoelshackThumbnails, boolean animateSmileys)
	{
		this.context = context;
		this.jvcLike = jvcLike;
		this.showNoelshackThumbnails = showNoelshackThumbnails;
		this.animateSmileys = animateSmileys;
		regularPseudoColor = context.getResources().getColor(R.color.jvcRegularPseudo);
		adminPseudoColor = context.getResources().getColor(R.color.jvcAdminPseudo);

		if(jvcLike)
			mainLayout = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.post_item_jvc, null);
		else
			mainLayout = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.post_item_normal, null);
		defaultBackground = mainLayout.getBackground();
		titleBarLayout = (LinearLayout) mainLayout.findViewById(R.id.postItemTitleBarLayout);
		separatorView = mainLayout.findViewById(R.id.postItemSeparator);
		pseudoTextView = (TextView) titleBarLayout.findViewById(R.id.postItemTitleBarPseudoTextView);
		dateTextView = (TextView) titleBarLayout.findViewById(R.id.postItemTitleBarDateTextView);

		postTextView = (TextView) mainLayout.findViewById(R.id.postItemPostTextView);
		postTextView.setMovementMethod(LinkMovementMethod.getInstance());
		loader = new NoelshackSpansLoader(context, postTextView);

		textColorPrimary = postTextView.getTextColors().getDefaultColor();

		int textSize = JvcUserData.getInt(JvcUserData.PREF_POSTS_TEXT_SIZE, JvcUserData.DEFAULT_POSTS_TEXT_SIZE);
		switch(textSize)
		{
			case 0:
				postTextView.setTextSize(12);
				break;

			case 1:
				postTextView.setTextSize(14);
				break;

			case 2:
				postTextView.setTextSize(16);
				break;
		}

		emptyItem();
	}

	public void updateDataFromPost(JvcPost post, boolean isTextual)
	{
		boolean inBlacklist = JvcUserData.isPseudoInBlacklist(post.getPostPseudo());

		if(post.getType() == JvcPost.TYPE_PREVIOUS_TOPIC)
		{
			titleBarLayout.setVisibility(View.GONE);
			separatorView.setVisibility(View.GONE);
		}
		else
		{
			titleBarLayout.setVisibility(View.VISIBLE);
			separatorView.setVisibility(View.VISIBLE);
			pseudoTextView.setText(post.getPostPseudo());

			CharSequence date = post.getPostDate();
			if(post.isMobilePost())
			{
				SingleImageText text = new SingleImageText("{*} " + date, R.raw.mobile_phone);
				date = text.getText();
			}
			dateTextView.setText(date);
		}

		if(inBlacklist)
		{
			mainLayout.setVisibility(View.GONE);
		}
		else
		{
			mainLayout.setVisibility(View.VISIBLE);

			if(post.isAdminPost())
				pseudoTextView.setTextColor(adminPseudoColor);
			else
			{
				if(jvcLike)
					pseudoTextView.setTextColor(regularPseudoColor);
				else
					pseudoTextView.setTextColor(textColorPrimary);
			}

			dateTextView.setTextColor(textColorPrimary);

			String postData = post.getPostData();
			if(post.getType() == JvcPost.TYPE_GENERIC)
			{
				postTextView.setTextColor(Color.GRAY);
				postTextView.setTypeface(Typeface.DEFAULT);
				postData += '\n';
			}
			else
			{
				postTextView.setTextColor(textColorPrimary);
				postTextView.setTypeface(Typeface.DEFAULT);
			}

			String topicNotice = post.getOtherTopicNotice();
			if(topicNotice != null)
			{
				SpannableStringBuilder postText;

				if(isTextual)
				{
					postText = new SpannableStringBuilder(JvcTextSpanner.getSpannableTextFromTopicTextualPost(loader, postData, showNoelshackThumbnails, animateSmileys));
				}
				else
				{
					postText = new SpannableStringBuilder(JvcTextSpanner.getSpannableTextFromTopicPost(loader, postData, showNoelshackThumbnails, animateSmileys));
				}

				postText.append("\n"); /* Only for regular posts ; not for first-page first posts */
				postText.append(topicNotice);
				int length = postText.length();
				postText.setSpan(new ForegroundColorSpan(Color.GRAY), length - topicNotice.length(), length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				postText.append(JvcUtils.JvcLinkIntent.makeLink(context, post.getOtherTopicName(), post.getOtherTopicUrl(), true));
				postText.append("\n");

				postTextView.setText(postText);
			}
			else
			{
				if(isTextual)
				{
					postTextView.setText(JvcTextSpanner.getSpannableTextFromTopicTextualPost(loader, postData, showNoelshackThumbnails, animateSmileys));
				}
				else
				{
					postTextView.setText(JvcTextSpanner.getSpannableTextFromTopicPost(loader, postData, showNoelshackThumbnails, animateSmileys));
				}
			}

			loader.startLoading();
		}

		if(jvcLike)
		{
			if(post.getPostColorSwitch() == 2)
				mainLayout.setBackgroundResource(R.drawable.jvc_post_box2);
			else
				mainLayout.setBackgroundResource(R.drawable.jvc_post_box1);
		}
		else
		{
			mainLayout.setBackgroundDrawable(defaultBackground);
		}
	}

	public void setHighlighted()
	{
		if(jvcLike)
		{
			mainLayout.setBackgroundResource(R.drawable.jvc_post_box_highlighted);
		}
		else
		{
			mainLayout.setBackgroundResource(R.drawable.orange_gradient_reflection);
		}
	}

	public void emptyItem()
	{
		mainLayout.setVisibility(View.GONE);
	}

	public View getView()
	{
		return mainLayout;
	}

	public NoelshackSpansLoader getLoader()
	{
		return loader;
	}

	public LinearLayout getTitleBarLayout()
	{
		return titleBarLayout;
	}

	public TextView getPostTextView()
	{
		return postTextView;
	}
}
