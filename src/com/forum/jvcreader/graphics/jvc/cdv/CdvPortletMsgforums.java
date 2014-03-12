package com.forum.jvcreader.graphics.jvc.cdv;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.forum.jvcreader.TopicActivity;
import com.forum.jvcreader.graphics.MenuTopicItem;
import com.forum.jvcreader.jvc.JvcForum;
import com.forum.jvcreader.jvc.JvcTopic;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.PatternCollection;
import com.forum.jvcreader.utils.StringHelper;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletMsgforums extends CdvPortlet
{
	private static final Pattern fetchNextPost = Pattern.compile("<li class=\"msg([12])\"><a.*?href=\"(.+?)\".*?>(.+?)</a> <span>(.+?)</span></li>");

	private ArrayList<JvcTopic> topicList = new ArrayList<JvcTopic>();

	@Override
	protected boolean parseContent()
	{
		Matcher m = fetchNextPost.matcher(content), m2 = null;
		while(m.find())
		{
			JvcForum forum;
			JvcTopic topic;

			int colorSwitch = Integer.parseInt(m.group(1));
			String url = StringHelper.unescapeHTML(m.group(2));
			String name = StringHelper.unescapeHTML(m.group(3));
			String date = m.group(4);

			if(m2 == null)
				m2 = PatternCollection.extractForumUrl.matcher(url);
			else
				m2.reset(url);

			if(!m2.find())
				return false;
			int forumId = Integer.parseInt(m2.group(2));
			long topicId = Long.parseLong(m2.group(3));
			int pageNumber = Integer.parseInt(m2.group(4));
			long postId = -1;
			if(m2.group(8) != null)
				postId = Long.parseLong(m2.group(8));

			forum = new JvcForum(context, forumId);
			if(postId != -1)
				topic = new JvcTopic(forum, topicId, pageNumber, postId);
			else
				topic = new JvcTopic(forum, topicId);

			topic.setExtras(name, colorSwitch, null, null, false, 0, date);
			topicList.add(topic);
		}

		return topicList.size() > 0;
	}

	@Override
	protected View getContentView()
	{
		LinearLayout mainLayout = new LinearLayout(context);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mainLayout.setLayoutParams(params);

		boolean enableSmileys = JvcUserData.getBoolean(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES, JvcUserData.DEFAULT_SMILEYS_IN_TOPIC_TITLES);
		boolean animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);
		boolean jvcLike = JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_TOPICS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_TOPICS);
		for(JvcTopic topic : topicList)
		{
			MenuTopicItem item = new MenuTopicItem(context, enableSmileys, animateSmileys, jvcLike);
			item.updateDataFromCdvTopic(topic);
			item.setItemOnClickListener(new TopicItemOnClickListener(topic));
			mainLayout.addView(item.getView());
		}

		return mainLayout;
	}

	private class TopicItemOnClickListener implements OnClickListener
	{
		JvcTopic topic;

		public TopicItemOnClickListener(JvcTopic topic)
		{
			this.topic = topic;
		}

		public void onClick(View view)
		{
			GlobalData.set("topicFromPreviousActivity", topic);
			context.startActivity(new Intent(context, TopicActivity.class));
		}
	}
}
