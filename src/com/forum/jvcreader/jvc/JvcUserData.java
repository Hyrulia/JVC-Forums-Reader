package com.forum.jvcreader.jvc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.DisplayMetrics;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.Base64Coder;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.widgets.SmartTimePicker;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class JvcUserData
{
	private static SharedPreferences preferences;
	private static SharedPreferences.Editor currentEditor;
	public static final String PREFS_FILE = "JvcForumsReaderPreferences";
	private static final String PREF_SETUP_DONE = "BooleanSetupDone";

	/* Login preferences */
	public static final String PREF_LOGIN = "StringLogin";
	public static final String PREF_PASSWORD = "StringPassword";
	public static final String PREF_REMEMBER_LOGIN = "BoolRememberLogin";
	public static final String PREF_REMEMBER_PASSWORD = "BoolRememberPassword";
	public static final String PREF_AUTO_CONNECT = "BoolAutoConnect";

	/* UPDATED TOPICS */
	public static final String PREF_RECEIVE_NOTIFICATION_ON_TOPIC_UPDATED = "BooleanReceiveNotificationOnTopicUpdated";
	public static final boolean DEFAULT_RECEIVE_NOTIFICATION_ON_TOPIC_UPDATED = true;
	public static final String PREF_CHECK_UPDATED_TOPICS = "BooleanCheckUpdatedTopics";
	public static final boolean DEFAULT_CHECK_UPDATED_TOPICS = true;
	public static final String PREF_UPDATED_TOPICS_CHECK_DELAY = "LongUpdatedTopicsCheckDelay";
	public static final long DEFAULT_UPDATED_TOPICS_CHECK_DELAY = SmartTimePicker.OPTION3_TIME; /* 1 hour */

	/* PRIVATE MESSAGES */
	public static final String PREF_RECEIVE_NOTIFICATION_ON_PM = "BooleanReceiveNotificationOnPrivateMessage";
	public static final boolean DEFAULT_RECEIVE_NOTIFICATION_ON_PM = true;
	public static final String PREF_CHECK_PM = "BooleanCheckPrivateMessages";
	public static final boolean DEFAULT_CHECK_PM = true;
	public static final String PREF_PM_CHECK_DELAY = "LongPrivateMessagesCheckDelay";
	public static final long DEFAULT_PM_CHECK_DELAY = SmartTimePicker.OPTION4_TIME; /* 12 hours */

	/* FORUMS */
	public static final String PREF_SMILEYS_IN_TOPIC_TITLES = "BooleanSmileysInTopicTitles";
	public static final boolean DEFAULT_SMILEYS_IN_TOPIC_TITLES = true;
	public static final String PREF_CHECK_POSTS_DURING_AUTOMATIC_BROWSING = "BooleanCheckPostsDuringAutomaticBrowsing";
	public static final boolean DEFAULT_CHECK_POSTS_DURING_AUTOMATIC_BROWSING = true;
	public static final String PREF_AUTOMATIC_BROWSING_CHECK_DELAY = "LongAutomaticBrowsingCheckDelay";
	public static final long DEFAULT_AUTOMATIC_BROWSING_CHECK_DELAY = SmartTimePicker.OPTION1_TIME; /* 1 minute */

	/* PUBLISHING TOPICS & POSTS */
	public static final String PREF_POST_AS_MOBILE_BY_DEFAULT = "BooleanPostAsMobileByDefault";
	public static final boolean DEFAULT_POST_AS_MOBILE_BY_DEFAULT = false;
	public static final String PREF_SHOW_CHARACTER_COUNTER = "BooleanShowCharacterCounter";
	public static final boolean DEFAULT_SHOW_CHARACTER_COUNTER = false;

	/* SIGNATURE INTEGRATION */
	public static final String PREF_USE_SIGNATURE = "BooleanUseSignature";
	public static final boolean DEFAULT_USE_SIGNATURE = false;
	public static final String PREF_SIGNATURE = "StringSignature";
	public static final String DEFAULT_SIGNATURE = "";
	public static final String PREF_SIGNATURE_AT_START = "BooleanSignatureAtStart";
	public static final boolean DEFAULT_SIGNATURE_AT_START = false;
	public static final String PREF_INCLUDE_SIGNATURE_BY_DEFAULT = "BooleanIncludeSignatureByDefault";
	public static final boolean DEFAULT_INCLUDE_SIGNATURE_BY_DEFAULT = true;

	/* APPEARANCE */
	public static final String PREF_ANIMATE_SMILEYS = "BooleanAnimateSmileys";
	public static final boolean DEFAULT_ANIMATE_SMILEYS = true;
	public static final String PREF_SHOW_NOELSHACK_THUMBNAILS = "BooleanShowNoelshackThumbnails";
	public static final boolean DEFAULT_SHOW_NOELSHACK_THUMBNAILS = true;
	public static final String PREF_KEEP_JVC_STYLE_TOPICS = "BooleanKeepJvcStyleTopics";
	public static final boolean DEFAULT_KEEP_JVC_STYLE_TOPICS = false;
	public static final String PREF_KEEP_JVC_STYLE_POSTS = "BooleanKeepJvcStylePosts";
	public static final boolean DEFAULT_KEEP_JVC_STYLE_POSTS = false;
	public static final String PREF_POSTS_TEXT_SIZE = "IntPostsTextSize";
	public static final int DEFAULT_POSTS_TEXT_SIZE = 1;
	public static final String PREF_SMILEYS_SIZE = "IntSmileysSize";
	public static final int DEFAULT_SMILEYS_SIZE = 2;
	public static final String PREF_HIDE_TITLE_BAR = "BooleanHideTitleBar";
	public static final boolean DEFAULT_HIDE_TITLE_BAR = false;

	/* Custom data */
	private static final String _PREF_FAVORITE_FORUMS = "ArrayListFavoriteForums";
	private static final String _PREF_FAVORITE_TOPICS = "ArrayListFavoriteTopics";
	private static final String _PREF_UPDATED_TOPICS = "ArrayListUpdatedTopics"; /* Should have been LinkedHashMapUpdatedTopics */
	private static final String _PREF_ARCHIVED_TOPICS = "LinkedHashMapArchivedTopics";

	private static float smileyScaleFactor = 0.0f;
	private static DisplayMetrics metrics = null;

	private static ArrayList<JvcForum> favoriteForums;
	private static ArrayList<JvcTopic> favoriteTopics;
	private static LinkedHashMap<Long, JvcTopic> updatedTopics;
	private static LinkedHashMap<Long, JvcArchivedTopic> archivedTopics;

	private static final String _PREF_PSEUDO_BLACKLIST = "HashMapPseudoBlacklist";
	private static HashMap<String, Boolean> pseudoBlacklist;

	private static final String _PREF_ACCOUNTS = "HashMapAccounts"; /* Should have been "ArrayListAccounts" */
	private static ArrayList<JvcAccount> accounts;

	public static void initialize(Context context) throws ClassNotFoundException, IOException
	{
		preferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		currentEditor = null;

		if(!preferences.getBoolean(PREF_SETUP_DONE, false))
		{
			setupPreferences();
			startEditing();
			currentEditor.putBoolean(PREF_SETUP_DONE, true);
			stopEditing();
			GlobalData.set("firstRun", true);

			MainApplication app = (MainApplication) context.getApplicationContext();
			app.setUnreadPmCount(0);

			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			{
				app.setJvcTheme(R.style.LightTheme);
			}
			else
			{
				app.setJvcTheme(R.style.HoloLightTheme);
			}
		}

		metrics = context.getResources().getDisplayMetrics(); /* Get metrics */
		computeSmileyScaleFactor();

		favoriteForums = (ArrayList<JvcForum>) getSerializableObject(_PREF_FAVORITE_FORUMS);
		favoriteTopics = (ArrayList<JvcTopic>) getSerializableObject(_PREF_FAVORITE_TOPICS);
		archivedTopics = (LinkedHashMap<Long, JvcArchivedTopic>) getSerializableObject(_PREF_ARCHIVED_TOPICS);
		pseudoBlacklist = (HashMap<String, Boolean>) getSerializableObject(_PREF_PSEUDO_BLACKLIST);
		accounts = (ArrayList<JvcAccount>) getSerializableObject(_PREF_ACCOUNTS);

		try
		{
			updatedTopics = (LinkedHashMap<Long, JvcTopic>) getSerializableObject(_PREF_UPDATED_TOPICS);
		}
		catch(InvalidClassException e)
		{
			autoClearEntry(_PREF_UPDATED_TOPICS);
			updatedTopics = null;
		}
	}

	public static void setupPreferences()
	{
		startEditing();
		currentEditor.clear();
		currentEditor.putBoolean(PREF_REMEMBER_LOGIN, false);
		currentEditor.putBoolean(PREF_REMEMBER_PASSWORD, false);
		currentEditor.putBoolean(PREF_AUTO_CONNECT, false);
		stopEditing();

		resetPreferences();
	}

	public static void resetPreferences()
	{
		startEditing();
		currentEditor.clear();
		stopEditing();
	}

	public static void startEditing()
	{
		if(currentEditor == null)
		{
			currentEditor = preferences.edit();
		}
	}

	public static void stopEditing()
	{
		if(currentEditor != null)
		{
			currentEditor.commit();
			//currentEditor.apply(); /* Because of lower APIs */
			currentEditor = null;
		}
	}
	
	/* Implementation of regular functions */

	public static String getString(String prefName, String defValue)
	{
		return preferences.getString(prefName, defValue);
	}

	public static void setString(String prefName, String value)
	{
		if(currentEditor != null)
			currentEditor.putString(prefName, value);
	}

	public static String getSecureString(String prefName, String defValue)
	{
		if(preferences.contains(prefName))
		{
			return Base64Coder.decodeString(preferences.getString(prefName, defValue));
		}
		else
		{
			return defValue;
		}
	}

	public static void setSecureString(String prefName, String value)
	{
		if(currentEditor != null)
			currentEditor.putString(prefName, Base64Coder.encodeString(value));
	}

	public static int getInt(String prefName, int defValue)
	{
		return preferences.getInt(prefName, defValue);
	}

	public static void setInt(String prefName, int value)
	{
		if(currentEditor != null)
			currentEditor.putInt(prefName, value);
	}

	public static float getFloat(String prefName, float defValue)
	{
		return preferences.getFloat(prefName, defValue);
	}

	public static void setFloat(String prefName, float value)
	{
		if(currentEditor != null)
			currentEditor.putFloat(prefName, value);
	}

	public static boolean getBoolean(String prefName, boolean defValue)
	{
		return preferences.getBoolean(prefName, defValue);
	}

	public static void setBoolean(String prefName, boolean value)
	{
		if(currentEditor != null)
			currentEditor.putBoolean(prefName, value);
	}

	public static long getLong(String prefName, long defValue)
	{
		return preferences.getLong(prefName, defValue);
	}

	public static void setLong(String prefName, long value)
	{
		if(currentEditor != null)
			currentEditor.putLong(prefName, value);
	}
	
	/* Implementation of serialized objects */

	public static Object getSerializableObject(String prefName) throws IOException, ClassNotFoundException
	{
		if(!preferences.contains(prefName))
			return null;

		Object deserializedObject;

		byte[] bytes = Base64Coder.decode(preferences.getString(prefName, ""));
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
		deserializedObject = in.readObject();
		in.close();

		return deserializedObject;
	}

	public static void setSerializableObject(String prefName, Object object) throws IOException
	{
		if(currentEditor != null)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(object);
			out.close();

			currentEditor.putString(prefName, String.valueOf(Base64Coder.encode(bos.toByteArray())));
		}
	}

	public static void remove(String prefName)
	{
		if(currentEditor != null)
		{
			currentEditor.remove(prefName);
		}
	}

	public static void autoClearEntry(String prefName)
	{
		startEditing();
		currentEditor.remove(prefName);
		stopEditing();
	}

	/* Smileys functions */

	public static float getSmileyScaleFactor()
	{
		return smileyScaleFactor;
	}

	public static void updateSmileyProperties()
	{
		CachedRawDrawables.unloadAllDrawables();
		computeSmileyScaleFactor();
	}

	private static void computeSmileyScaleFactor()
	{
		int smileySizeType = getInt(JvcUserData.PREF_SMILEYS_SIZE, JvcUserData.DEFAULT_SMILEYS_SIZE);

		switch(smileySizeType)
		{
			case 0: /* Original */
				smileyScaleFactor = 1.0f;
				break;
			case 1: /* Small */
				smileyScaleFactor = 0.8f;
				break;
			case 2: /* Medium */
				smileyScaleFactor = 1.0f;
				break;
			case 3: /* Big */
				smileyScaleFactor = 1.2f;
				break;
		}

		if(smileySizeType > 0 && metrics != null) /* then we want to fit smiley's density to screen density */
		{
			smileyScaleFactor *= metrics.density;
		}
	}

	/* Favorite forums */

	public static void saveFavoriteForums() throws IOException
	{
		if(favoriteForums != null)
		{
			startEditing();
			setSerializableObject(_PREF_FAVORITE_FORUMS, favoriteForums);
			stopEditing();
		}
		else
		{
			autoClearEntry(_PREF_FAVORITE_FORUMS);
		}
	}

	public static boolean isForumInFavorites(JvcForum forum)
	{
		int forumId = forum.getForumId();

		if(favoriteForums == null)
			return false;

		for(JvcForum f : favoriteForums)
		{
			if(f.getForumId() == forumId)
			{
				return true;
			}
		}

		return false;
	}

	public static ArrayList<JvcForum> getFavoriteForums()
	{
		return favoriteForums;
	}

	public static void addToFavoriteForums(JvcForum forum) throws Exception
	{
		if(favoriteForums == null)
		{
			favoriteForums = new ArrayList<JvcForum>();
		}

		favoriteForums.add(forum);

		startEditing();
		setSerializableObject(_PREF_FAVORITE_FORUMS, favoriteForums);
		stopEditing();
	}

	public static void removeFromFavoriteForums(JvcForum forum) throws Exception
	{
		int forumId = forum.getForumId();
		JvcForum uniqueForum = null;

		for(JvcForum f : favoriteForums)
		{
			if(f.getForumId() == forumId)
			{
				uniqueForum = f;
			}
		}

		if(uniqueForum != null)
			favoriteForums.remove(uniqueForum);

		startEditing();
		if(favoriteForums.size() == 0)
		{
			currentEditor.remove(_PREF_FAVORITE_FORUMS);
			favoriteForums = null;
		}
		else
			setSerializableObject(_PREF_FAVORITE_FORUMS, favoriteForums);
		stopEditing();
	}
	
	/* Favorite topics */

	public static void saveFavoriteTopics() throws IOException
	{
		if(favoriteTopics != null)
		{
			startEditing();
			setSerializableObject(_PREF_FAVORITE_TOPICS, favoriteTopics);
			stopEditing();
		}
		else
		{
			autoClearEntry(_PREF_FAVORITE_TOPICS);
		}
	}

	public static boolean isTopicInFavorites(JvcTopic topic)
	{
		long topicId = topic.getTopicId();

		if(favoriteTopics == null)
			return false;

		for(JvcTopic t : favoriteTopics)
		{
			if(t.getTopicId() == topicId)
			{
				return true;
			}
		}

		return false;
	}

	public static ArrayList<JvcTopic> getFavoriteTopics()
	{
		return favoriteTopics;
	}

	public static void addToFavoriteTopics(JvcTopic topic) throws Exception
	{
		if(favoriteTopics == null)
		{
			favoriteTopics = new ArrayList<JvcTopic>();
		}

		favoriteTopics.add(topic);

		startEditing();
		setSerializableObject(_PREF_FAVORITE_TOPICS, favoriteTopics);
		stopEditing();
	}

	public static void removeFromFavoriteTopics(JvcTopic topic) throws Exception
	{
		long topicId = topic.getTopicId();
		JvcTopic uniqueTopic = null;

		for(JvcTopic t : favoriteTopics)
		{
			if(t.getTopicId() == topicId)
			{
				uniqueTopic = t;
			}
		}

		if(uniqueTopic != null)
			favoriteTopics.remove(uniqueTopic);

		startEditing();
		if(favoriteTopics.size() == 0)
		{
			currentEditor.remove(_PREF_FAVORITE_TOPICS);
			favoriteTopics = null;
		}
		else
			setSerializableObject(_PREF_FAVORITE_TOPICS, favoriteTopics);
		stopEditing();
	}
	
	/* Updated topics */

	public static boolean isTopicInUpdatedTopics(JvcTopic topic)
	{
		if(updatedTopics == null)
			return false;

		return updatedTopics.containsKey(topic.getTopicId());

	}

	public static void updateUpdatedTopic(JvcTopic topic) throws IOException
	{
		if(updatedTopics == null)
			return;

		updatedTopics.put(topic.getTopicId(), topic);
		startEditing();
		setSerializableObject(_PREF_UPDATED_TOPICS, updatedTopics);
		stopEditing();
	}

	public static ArrayList<JvcTopic> getUpdatedTopics()
	{
		if(updatedTopics == null)
			return null;
		return new ArrayList<JvcTopic>(updatedTopics.values());
	}

	public static void addToUpdatedTopics(JvcTopic topic) throws Exception
	{
		if(updatedTopics == null)
		{
			updatedTopics = new LinkedHashMap<Long, JvcTopic>();
		}

		updatedTopics.put(topic.getTopicId(), topic);

		startEditing();
		setSerializableObject(_PREF_UPDATED_TOPICS, updatedTopics);
		stopEditing();
	}

	public static void removeFromUpdatedTopics(JvcTopic topic) throws Exception
	{
		if(updatedTopics == null)
			return;

		updatedTopics.remove(topic.getTopicId());

		startEditing();
		if(updatedTopics.size() == 0)
		{
			currentEditor.remove(_PREF_UPDATED_TOPICS);
			updatedTopics = null;
		}
		else
			setSerializableObject(_PREF_UPDATED_TOPICS, updatedTopics);
		stopEditing();
	}

	/* Archived topics */

	public static void saveArchivedTopics(ArrayList<JvcArchivedTopic> newList) throws IOException
	{
		if(newList != null && newList.size() > 0)
		{
			LinkedHashMap<Long, JvcArchivedTopic> newHashMap = new LinkedHashMap<Long, JvcArchivedTopic>();

			for(JvcArchivedTopic t : newList)
			{
				newHashMap.put(t.getTopicId(), t);
			}

			archivedTopics = newHashMap;
			startEditing();
			setSerializableObject(_PREF_ARCHIVED_TOPICS, archivedTopics);
			stopEditing();
		}
		else
		{
			autoClearEntry(_PREF_ARCHIVED_TOPICS);
		}
	}

	public static boolean isTopicInArchivedTopics(JvcTopic topic)
	{
		if(archivedTopics == null)
			return false;

		return archivedTopics.containsKey(topic.getTopicId());

	}

	public static ArrayList<JvcArchivedTopic> getArchivedTopics()
	{
		if(archivedTopics == null)
			return null;
		return new ArrayList<JvcArchivedTopic>(archivedTopics.values());
	}

	public static void addToArchivedTopics(JvcArchivedTopic topic) throws IOException
	{
		if(archivedTopics == null)
		{
			archivedTopics = new LinkedHashMap<Long, JvcArchivedTopic>();
		}

		archivedTopics.put(topic.getTopicId(), topic);

		startEditing();
		setSerializableObject(_PREF_ARCHIVED_TOPICS, archivedTopics);
		stopEditing();
	}

	public static void removeFromArchivedTopics(JvcArchivedTopic topic) throws IOException
	{
		if(archivedTopics == null)
			return;

		topic.deleteTopicContent();
		archivedTopics.remove(topic.getTopicId());

		startEditing();
		if(archivedTopics.size() == 0)
		{
			currentEditor.remove(_PREF_ARCHIVED_TOPICS);
			archivedTopics = null;
		}
		else
			setSerializableObject(_PREF_ARCHIVED_TOPICS, archivedTopics);
		stopEditing();
	}
	
	/* Pseudo blacklist */

	public static boolean isPseudoInBlacklist(String pseudo)
	{
		if(pseudoBlacklist == null)
			return false;

		Boolean b = pseudoBlacklist.get(pseudo.toLowerCase());
		if(b != null)
			return b;

		return false;
	}

	public static void setPseudoInBlacklist(String pseudo) throws IOException
	{
		if(pseudoBlacklist == null)
		{
			pseudoBlacklist = new HashMap<String, Boolean>();
		}

		pseudoBlacklist.put(pseudo.toLowerCase(), true);

		startEditing();
		setSerializableObject(_PREF_PSEUDO_BLACKLIST, pseudoBlacklist);
		stopEditing();
	}

	public static void removePseudoFromBlacklist(String pseudo) throws IOException
	{
		if(pseudoBlacklist == null)
			return;

		pseudoBlacklist.remove(pseudo.toLowerCase());

		startEditing();
		setSerializableObject(_PREF_PSEUDO_BLACKLIST, pseudoBlacklist);
		stopEditing();
	}

	public static HashMap<String, Boolean> getPseudoBlacklist()
	{
		return pseudoBlacklist;
	}

	public static void clearPseudoBlacklist() throws IOException
	{
		pseudoBlacklist.clear();

		startEditing();
		setSerializableObject(_PREF_PSEUDO_BLACKLIST, pseudoBlacklist);
		stopEditing();
	}
	
	/* Accounts */

	public static ArrayList<JvcAccount> getAccounts()
	{
		return accounts;
	}

	public static boolean isAccountInAccounts(JvcAccount newAccount)
	{
		if(accounts == null || accounts.size() == 0)
			return false;

		final String newPseudo = newAccount.getPseudo().toLowerCase();

		for(JvcAccount acc : accounts)
		{
			if(acc.getPseudo().toLowerCase().equals(newPseudo))
				return true;
		}

		return false;
	}

	public static void addToAccounts(JvcAccount account) throws Exception
	{
		if(accounts == null)
		{
			accounts = new ArrayList<JvcAccount>();
		}

		accounts.add(account);

		startEditing();
		setSerializableObject(_PREF_ACCOUNTS, accounts);
		stopEditing();
	}

	public static void removeFromAccounts(JvcAccount account) throws Exception
	{
		if(accounts == null)
			return;

		accounts.remove(account);

		startEditing();
		if(accounts.size() == 0)
		{
			currentEditor.remove(_PREF_ACCOUNTS);
			accounts = null;
		}
		else
			setSerializableObject(_PREF_ACCOUNTS, accounts);
		stopEditing();
	}
}
