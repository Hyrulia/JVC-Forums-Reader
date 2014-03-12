package com.forum.jvcreader.jvc;

import java.io.Serializable;

public class UpdatedTopicData implements Serializable
{
	private static final long serialVersionUID = 2L;

	private boolean isNew;
	private int newPostCount;
	private String lastError = null;
	private boolean lockAccess = false;

	private long lastPostId;
	private long lastPostTotalCount;
	private long newPostId;
	private long newPostTotalCount;

	public UpdatedTopicData(long lastPostTotalCount, long lastPostId)
	{
		isNew = false;
		newPostCount = 0;
		this.lastPostTotalCount = lastPostTotalCount;
		this.lastPostId = lastPostId;
	}

	public void setNewData(int newPostCount, long newPostTotalCount, long newPostId)
	{
		this.newPostCount = newPostCount;
		isNew = newPostCount > 0;

		this.newPostTotalCount = newPostTotalCount;
		this.newPostId = newPostId;
	}

	public String getLastError()
	{
		return lastError;
	}

	public void setLastError(String s)
	{
		lastError = s;
	}

	public boolean isNew()
	{
		return isNew;
	}

	public void setAsRead()
	{
		newPostCount = 0;
		isNew = false;

		lastPostTotalCount = newPostTotalCount;
		lastPostId = newPostId;
	}

	public int getNewPostCount()
	{
		return newPostCount;
	}

	public long getLastPostTotalCount()
	{
		return lastPostTotalCount;
	}

	public long getLastPostId()
	{
		return lastPostId;
	}

	public int computeFirstNewPostPos()
	{
		return (int) (lastPostTotalCount % JvcUtils.POSTS_PER_PAGE);
	}

	public int computeFirstNewPostPage()
	{
		return (int) Math.ceil((double) (lastPostTotalCount + 1) / (double) JvcUtils.POSTS_PER_PAGE);
	}

	public int computeLastPostPage()
	{
		return (int) Math.ceil((double) lastPostTotalCount / (double) JvcUtils.POSTS_PER_PAGE);
	}

	public void addAlreadyReadPosts(int count, long postId)
	{
		newPostTotalCount += count;
		newPostId = postId;
		isNew = true;
	}

	public void setAccessLocked(boolean locked)
	{
		lockAccess = locked;
	}

	public boolean isAccessLocked()
	{
		return lockAccess;
	}
}
