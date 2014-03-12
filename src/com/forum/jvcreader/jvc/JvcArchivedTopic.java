package com.forum.jvcreader.jvc;

import com.forum.jvcreader.utils.InternalStorageHelper;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class JvcArchivedTopic extends JvcTopic implements Serializable
{
	public static final int MAX_POSTS_PER_FILE = 200;
	private static final long serialVersionUID = 1L;

	transient private ArrayList<ArrayList<JvcPost>> postsPageList;
	transient private int currentFile = -1;
	transient private int currentPageStart;
	transient private int currentPageEnd;
	transient private boolean authorPostsOnly;

	private int archivedPageCountPerFile;
	private int archivedPostsPerPage;
	private int archivedPageCount;
	private int archivedFileCount;

	public JvcArchivedTopic(JvcTopic topic, int postsPerPage, int pageStart, int pageEnd, boolean authorPostsOnly)
	{
		super(topic.getForum(), topic.getTopicId());

		currentPageStart = Math.max(pageStart, 1);
		currentPageEnd = Math.min(pageEnd, 5000);
		this.authorPostsOnly = authorPostsOnly;

		archivedPostsPerPage = Math.min(Math.max(postsPerPage, 1), MAX_POSTS_PER_FILE);
		archivedPageCountPerFile = (int) Math.floor((double) MAX_POSTS_PER_FILE / postsPerPage);
		archivedPageCount = 0;
		archivedFileCount = 0;
	}

	public void deleteTopicContent() throws IOException
	{
		for(int i = 0; i < archivedFileCount; i++)
		{
			InternalStorageHelper.deleteFile(getForum().getContext(), getFileName(i));
		}
	}

	public boolean archivePages(OnPageArchivedListener listener) throws IOException, JvcErrorException
	{
		if(JvcUserData.isTopicInArchivedTopics(this))
			throw new IllegalArgumentException("Topic already in archived topics");

		ArrayList<ArrayList<JvcPost>> tempPageList = new ArrayList<ArrayList<JvcPost>>();
		ArrayList<JvcPost> tempPostList = new ArrayList<JvcPost>();
		String authorName = null;
		int fileCounter = 0;

		if(authorPostsOnly && currentPageStart > 1)
		{
			listener.onUpdateState(OnPageArchivedListener.FETCHING_AUTHOR_NAME, 0);
			if(requestPosts(JvcTopic.SHOW_POSTS_FROM_PAGE, 1))
			{
				throw new JvcErrorException(getRequestError());
			}
			else
			{
				authorName = getRequestResult().get(0).getPostPseudo().toLowerCase();
			}
		}

		for(int i = currentPageStart; i <= currentPageEnd; i++)
		{
			listener.onUpdateState(OnPageArchivedListener.FETCHING_PAGE, i);
			if(requestPosts(JvcTopic.SHOW_POSTS_FROM_PAGE, i))
			{
				throw new JvcErrorException(getRequestError());
			}
			else
			{
				ArrayList<JvcPost> postList = getRequestResult();
				if(i == 1 && authorName == null)
					authorName = postList.get(0).getPostPseudo().toLowerCase();

				for(JvcPost post : postList)
				{
					if(authorPostsOnly && !post.getPostPseudo().toLowerCase().equals(authorName))
						continue;

					tempPostList.add(post);
					if(tempPostList.size() == archivedPostsPerPage)
					{
						tempPageList.add(tempPostList);
						archivedPageCount++;
						tempPostList = new ArrayList<JvcPost>();
						if(tempPageList.size() == archivedPageCountPerFile)
						{
							InternalStorageHelper.writeSerializableObject(getForum().getContext(), getFileName(fileCounter), tempPageList);
							fileCounter++;
							archivedFileCount = fileCounter; /* Always update this in case of abort */
							tempPageList.clear();
						}
					}
				}
			}
		}

		if(tempPostList.size() > 0)
		{
			tempPageList.add(tempPostList); /* Append last page */
			archivedPageCount++;
		}

		if(tempPageList.size() > 0) /* Append last file */
		{
			InternalStorageHelper.writeSerializableObject(getForum().getContext(), getFileName(fileCounter), tempPageList);
			fileCounter++;
			archivedFileCount = fileCounter; /* Always update this in case of abort */
		}

		if(fileCounter > 0)
		{
			listener.onUpdateState(OnPageArchivedListener.SAVING_TOPIC, 0);
			JvcUserData.addToArchivedTopics(this);

			return true;
		}
		else
		{
			return false;
		}
	}

	public int getPostCountPerPage()
	{
		return archivedPostsPerPage;
	}

	public int getArchivedPageCount()
	{
		return archivedPageCount;
	}

	public int getInitialPageEnd()
	{
		return currentPageEnd;
	}

	private String getFileName(int fileId)
	{
		return "ArchivedTopic_" + getTopicId() + "_" + fileId + ".jvc";
	}

	public ArrayList<JvcPost> getPostsFromPage(int page) throws ClassNotFoundException, IOException
	{
		if(page >= 1 && page <= getArchivedPageCount())
		{
			final int requestedPageFile = (int) Math.floor((page - 1) / archivedPageCountPerFile);
			if(postsPageList == null || currentFile != requestedPageFile)
			{
				postsPageList = (ArrayList<ArrayList<JvcPost>>) InternalStorageHelper.readSerializableObject(getForum().getContext(), getFileName(requestedPageFile));
				currentFile = requestedPageFile;
			}
			return postsPageList.get((page - 1) - currentFile * archivedPageCountPerFile);
		}
		else
		{
			throw new IllegalArgumentException("Page number requested invalid");
		}
	}

	public interface OnPageArchivedListener
	{
		public static final int FETCHING_AUTHOR_NAME = 1;
		public static final int FETCHING_PAGE = 2;
		public static final int SAVING_TOPIC = 3;

		public void onUpdateState(int state, int arg1);
	}
}
