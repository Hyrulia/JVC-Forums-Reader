package com.forum.jvcreader.jvc;

import android.content.Context;
import android.os.AsyncTask;
import com.forum.jvcreader.R;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class UpdatedTopicsManager
{
	private static UpdatedTopicsManager instance = null;

	public static synchronized UpdatedTopicsManager getInstance()
	{
		if(instance == null)
		{
			instance = new UpdatedTopicsManager();
		}

		return instance;
	}

	public UpdateResult updateTopics(Context context, ArrayList<JvcTopic> topics) throws IOException
	{
		return updateTopics(context, topics, null, null);
	}

	public UpdateResult updateTopics(Context context, ArrayList<JvcTopic> topics, Runnable runnable, AsyncTask task) throws IOException
	{
		int totalUnreadPostCount = 0, totalUnreadTopicCount = 0, newUnreadPostCount = 0;

		for(JvcTopic topic : topics)
		{
			topic.getForum().setContext(context);
			if(runnable != null)
				runnable.run();

			final UpdatedTopicData data = topic.getUpdatedTopicData();
			if(task != null && task.isCancelled())
				return null;
			boolean error = topic.requestPosts(JvcTopic.SHOW_POSTS_FROM_PAGE, data.computeLastPostPage());
			ArrayList<JvcPost> result = topic.getRequestResult();

			if(error) /* Lock */
			{
				if(topic.requestIsTopicEmpty()) /* Page does not exist (because of deleted post(s)) */
				{
					if(task != null && task.isCancelled())
						return null;
					error = topic.requestPosts(JvcTopic.SHOW_POSTS_FROM_PAGE, 1);
					if(error)
					{
						data.setLastError(topic.getRequestError());
					}
					else
					{
						data.setNewData(0, topic.getRequestPageCount() * JvcUtils.POSTS_PER_PAGE, -1);
						data.setAsRead();
						data.setLastError(context.getString(R.string.lastPageEmptyPleaseRetryError));
					}
				}
				else
				{
					data.setLastError(topic.getRequestError());
				}

				data.setAccessLocked(true);
			}
			else
			{
				data.setAccessLocked(false); /* Access to locked topics */

				if(topic.isTopicLocked())
				{
					data.setLastError(context.getString(R.string.topicLocked));
					continue;
				}
				else
				{
					data.setLastError(null);
				}

				int newPostCount;
				long newPostTotalCount = 0, postId = -1;
				final long lastPostTotalCount = data.getLastPostTotalCount();
				final int diff = topic.getRequestPageCount() - data.computeLastPostPage();

				if(diff == 0) /* No new page */
				{
					newPostTotalCount = (topic.getRequestPageCount() - 1) * 20 + result.size();
					postId = result.get(result.size() - 1).getPostId();
				}
				else if(diff > 0) /* New page(s) */
				{
					if(task != null && task.isCancelled())
						return null;
					error = topic.requestPosts(JvcTopic.SHOW_POSTS_FROM_PAGE, topic.getRequestPageCount());
					if(!error)
					{
						ArrayList<JvcPost> requestResult = topic.getRequestResult();
						int size = requestResult.size();
						newPostTotalCount = (topic.getRequestPageCount() - 1) * 20 + size;
						postId = requestResult.get(size - 1).getPostId();
					}
					else
					{
						data.setLastError(context.getString(R.string.couldNotLoadLastPage));
						data.setAccessLocked(true);
					}
				}
				else
				{
					data.setLastError(context.getString(R.string.lastPageEmptyPleaseRetryError));
					data.setAccessLocked(true);
					continue;
				}

				int oldPostCount = data.getNewPostCount();
				newPostCount = (int) Math.max(newPostTotalCount - lastPostTotalCount, 0);
				data.setNewData(newPostCount, newPostTotalCount, postId);
				totalUnreadPostCount += newPostCount;
				if(newPostCount > 0)
					totalUnreadTopicCount++;

				newUnreadPostCount += Math.max(newPostCount - oldPostCount, 0);
			}

			if(task != null && task.isCancelled())
				return null;

			JvcUserData.updateUpdatedTopic(topic);
		}

		if(runnable != null)
			runnable.run();

		UpdateResult result = new UpdateResult(totalUnreadPostCount, totalUnreadTopicCount, newUnreadPostCount);
		return result;
	}

	public UpdateResult getUpdateResultFromLastUpdate(Context context, ArrayList<JvcTopic> topics)
	{
		if(topics == null || topics.size() == 0)
			return new UpdateResult(0, 0, 0);

		UpdatedTopicData data;
		int totalUnreadPostCount = 0, totalUnreadTopicCount = 0;

		for(JvcTopic topic : topics)
		{
			data = topic.getUpdatedTopicData();
			totalUnreadPostCount += data.getNewPostCount();
			if(data.getNewPostCount() > 0)
				totalUnreadTopicCount++;
		}

		return new UpdateResult(totalUnreadPostCount, totalUnreadTopicCount, 0);
	}

	public static class UpdateResult implements Serializable
	{
		private static final long serialVersionUID = 1L;
		private int totalUnreadPostCount;
		private int totalUnreadTopicCount;
		private int newUnreadPostCount;

		public UpdateResult(int totalUnreadPostCount, int totalUnreadTopicCount, int newUnreadPostCount)
		{
			this.totalUnreadPostCount = totalUnreadPostCount;
			this.totalUnreadTopicCount = totalUnreadTopicCount;
			this.newUnreadPostCount = newUnreadPostCount;
		}

		public int getTotalUnreadPostCount()
		{
			return totalUnreadPostCount;
		}

		public int getTotalUnreadTopicCount()
		{
			return totalUnreadTopicCount;
		}

		public int getNewUnreadPostCount()
		{
			return newUnreadPostCount;
		}
	}
}
