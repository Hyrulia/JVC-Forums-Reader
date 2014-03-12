package com.forum.jvcreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.widgets.MenuCollapsibleItem;
import com.forum.jvcreader.widgets.SmartTimePicker;

public class PreferencesActivity extends JvcActivity
{
	MainApplication app;

	/* UPDATED TOPICS */
	private ToggleButton receiveNotificationOnTopicUpdatedToggle;
	private ToggleButton checkUpdatedTopicsToggle;
	private MenuCollapsibleItem updatedTopicsCheckDelayItem;
	private SmartTimePicker updatedTopicsCheckDelayPicker;

	/* PRIVATE MESSAGES */
	private ToggleButton receiveNotificationOnPrivateMessageToggle;
	private ToggleButton checkPrivateMessagesToggle;
	private MenuCollapsibleItem privateMessagesCheckDelayItem;
	private SmartTimePicker privateMessagesCheckDelayPicker;

	/* FORUMS NAVIGATION */
	private ToggleButton showSmileysInTopicTitlesToggle;
	private ToggleButton checkPostsDuringAutomaticBrowsingToggle;
	private MenuCollapsibleItem automaticBrowsingCheckDelayItem;
	private SmartTimePicker automaticBrowsingCheckDelayPicker;

	/* PUBLISHING TOPICS & POSTS */
	private ToggleButton postAsMobileByDefaultToggle;
	private ToggleButton showCharacterCounterToggle;

	/* APPEARANCE */
	private Spinner themeSelectionSpinner;
	private ToggleButton animateSmileysToggle;
	private ToggleButton showNoelshackThumbnailsToggle;
	private ToggleButton keepJvcStyleTopicsToggle;
	private ToggleButton keepJvcStylePostsToggle;
	private Spinner postsTextSizeSpinner;
	private Spinner smileysSizeSpinner;
	private ToggleButton hideTitleBarToggle;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences);

		app = (MainApplication) getApplicationContext();

	    /* UPDATED TOPICS */
		receiveNotificationOnTopicUpdatedToggle = (ToggleButton) findViewById(R.id.prefsReceiveNotificationOnTopicUpdatedToggleButton);
		receiveNotificationOnTopicUpdatedToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_RECEIVE_NOTIFICATION_ON_TOPIC_UPDATED));

		updatedTopicsCheckDelayPicker = new SmartTimePicker(this);
		updatedTopicsCheckDelayPicker.setTimeChangeOnClickListener(new TimeChangeListener(JvcUserData.PREF_UPDATED_TOPICS_CHECK_DELAY, JvcUserData.DEFAULT_UPDATED_TOPICS_CHECK_DELAY, true));

		updatedTopicsCheckDelayItem = (MenuCollapsibleItem) findViewById(R.id.prefsUpdatedTopicsCheckDelayCollapsibleItem);
		updatedTopicsCheckDelayItem.addViewInCollapsibleLayout(updatedTopicsCheckDelayPicker);

		checkUpdatedTopicsToggle = (ToggleButton) findViewById(R.id.prefsCheckUpdatedTopicsToggleButton);
		checkUpdatedTopicsToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_CHECK_UPDATED_TOPICS, true, updatedTopicsCheckDelayItem));

	    /* PRIVATE MESSAGES */
		receiveNotificationOnPrivateMessageToggle = (ToggleButton) findViewById(R.id.prefsReceiveNotificationOnPrivateMessageToggleButton);
		receiveNotificationOnPrivateMessageToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_RECEIVE_NOTIFICATION_ON_PM));

		privateMessagesCheckDelayPicker = new SmartTimePicker(this);
		privateMessagesCheckDelayPicker.setTimeChangeOnClickListener(new TimeChangeListener(JvcUserData.PREF_PM_CHECK_DELAY, JvcUserData.DEFAULT_PM_CHECK_DELAY, true));

		privateMessagesCheckDelayItem = (MenuCollapsibleItem) findViewById(R.id.prefsPrivateMessagesCheckDelayCollapsibleItem);
		privateMessagesCheckDelayItem.addViewInCollapsibleLayout(privateMessagesCheckDelayPicker);

		checkPrivateMessagesToggle = (ToggleButton) findViewById(R.id.prefsCheckPrivateMessagesToggleButton);
		checkPrivateMessagesToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_CHECK_PM, true, privateMessagesCheckDelayItem));

	    /* FORUMS NAVIGATION */
		showSmileysInTopicTitlesToggle = (ToggleButton) findViewById(R.id.prefsShowSmileysInTopicTitlesToggleButton);
		showSmileysInTopicTitlesToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES));

		automaticBrowsingCheckDelayPicker = new SmartTimePicker(this);
		automaticBrowsingCheckDelayPicker.setTimeChangeOnClickListener(new TimeChangeListener(JvcUserData.PREF_AUTOMATIC_BROWSING_CHECK_DELAY, JvcUserData.DEFAULT_AUTOMATIC_BROWSING_CHECK_DELAY));

		automaticBrowsingCheckDelayItem = (MenuCollapsibleItem) findViewById(R.id.prefsAutomaticBrowsingCheckDelayCollapsibleItem);
		automaticBrowsingCheckDelayItem.addViewInCollapsibleLayout(automaticBrowsingCheckDelayPicker);

		checkPostsDuringAutomaticBrowsingToggle = (ToggleButton) findViewById(R.id.prefsCheckPostsDuringAutomaticBrowsingToggleButton);
		checkPostsDuringAutomaticBrowsingToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_CHECK_POSTS_DURING_AUTOMATIC_BROWSING, false, automaticBrowsingCheckDelayItem));
	    
	    /* PUBLISHING TOPICS & POSTS */
		postAsMobileByDefaultToggle = (ToggleButton) findViewById(R.id.prefsPostAsMobileByDefaultToggleButton);
		postAsMobileByDefaultToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_POST_AS_MOBILE_BY_DEFAULT));

		showCharacterCounterToggle = (ToggleButton) findViewById(R.id.prefsShowCharacterCounterToggleButton);
		showCharacterCounterToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_SHOW_CHARACTER_COUNTER));
		
		/* APPEARANCE */
		themeSelectionSpinner = (Spinner) findViewById(R.id.prefsThemeSelectionSpinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, MainApplication.jvcThemeNames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		themeSelectionSpinner.setAdapter(adapter);
		themeSelectionSpinner.setOnItemSelectedListener(themeSelectionListener);

		animateSmileysToggle = (ToggleButton) findViewById(R.id.prefsAnimateSmileysToggleButton);
		animateSmileysToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_ANIMATE_SMILEYS));

		showNoelshackThumbnailsToggle = (ToggleButton) findViewById(R.id.prefsShowNoelshackThumbnailsToggleButton);
		showNoelshackThumbnailsToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS));

		keepJvcStyleTopicsToggle = (ToggleButton) findViewById(R.id.prefsKeepJvcStyleTopicsToggleButton);
		keepJvcStyleTopicsToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_KEEP_JVC_STYLE_TOPICS));

		keepJvcStylePostsToggle = (ToggleButton) findViewById(R.id.prefsKeepJvcStylePostsToggleButton);
		keepJvcStylePostsToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_KEEP_JVC_STYLE_POSTS));

		postsTextSizeSpinner = (Spinner) findViewById(R.id.prefsPostsTextSizeSpinner);
		postsTextSizeSpinner.setAdapter(newPrefPickerAdapter(R.array.prefsTextSizes));
		postsTextSizeSpinner.setOnItemSelectedListener(new PrefPickerListener(JvcUserData.PREF_POSTS_TEXT_SIZE, JvcUserData.DEFAULT_POSTS_TEXT_SIZE));

		smileysSizeSpinner = (Spinner) findViewById(R.id.prefsSmileysSizeSpinner);
		smileysSizeSpinner.setAdapter(newPrefPickerAdapter(R.array.prefsSmileysSizes));
		smileysSizeSpinner.setOnItemSelectedListener(new PrefPickerListener(JvcUserData.PREF_SMILEYS_SIZE, JvcUserData.DEFAULT_SMILEYS_SIZE));

		hideTitleBarToggle = (ToggleButton) findViewById(R.id.prefsHideTitleBarToggleButton);
		hideTitleBarToggle.setOnClickListener(new ToggleButtonPrefListener(JvcUserData.PREF_HIDE_TITLE_BAR));

		updateUiFromUserData();
	}

	private void updateUiFromUserData()
	{
		boolean b;
		
		/* UPDATED TOPICS */
		receiveNotificationOnTopicUpdatedToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_RECEIVE_NOTIFICATION_ON_TOPIC_UPDATED, JvcUserData.DEFAULT_RECEIVE_NOTIFICATION_ON_TOPIC_UPDATED));
		b = JvcUserData.getBoolean(JvcUserData.PREF_CHECK_UPDATED_TOPICS, JvcUserData.DEFAULT_CHECK_UPDATED_TOPICS);
		checkUpdatedTopicsToggle.setChecked(b);
		updatedTopicsCheckDelayItem.setEnabled(b);
		updatedTopicsCheckDelayPicker.updateSelectionFromTime(JvcUserData.getLong(JvcUserData.PREF_UPDATED_TOPICS_CHECK_DELAY, JvcUserData.DEFAULT_UPDATED_TOPICS_CHECK_DELAY));
		
		/* PRIVATE MESSAGES */
		receiveNotificationOnPrivateMessageToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_RECEIVE_NOTIFICATION_ON_PM, JvcUserData.DEFAULT_RECEIVE_NOTIFICATION_ON_PM));
		b = JvcUserData.getBoolean(JvcUserData.PREF_CHECK_PM, JvcUserData.DEFAULT_CHECK_PM);
		checkPrivateMessagesToggle.setChecked(b);
		privateMessagesCheckDelayItem.setEnabled(b);
		privateMessagesCheckDelayPicker.updateSelectionFromTime(JvcUserData.getLong(JvcUserData.PREF_PM_CHECK_DELAY, JvcUserData.DEFAULT_PM_CHECK_DELAY));
		
		/* FORUMS NAVIGATION */
		showSmileysInTopicTitlesToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_SMILEYS_IN_TOPIC_TITLES, JvcUserData.DEFAULT_SMILEYS_IN_TOPIC_TITLES));
		b = JvcUserData.getBoolean(JvcUserData.PREF_CHECK_POSTS_DURING_AUTOMATIC_BROWSING, JvcUserData.DEFAULT_CHECK_POSTS_DURING_AUTOMATIC_BROWSING);
		checkPostsDuringAutomaticBrowsingToggle.setChecked(b);
		automaticBrowsingCheckDelayItem.setEnabled(b);
		automaticBrowsingCheckDelayPicker.updateSelectionFromTime(JvcUserData.getLong(JvcUserData.PREF_AUTOMATIC_BROWSING_CHECK_DELAY, JvcUserData.DEFAULT_AUTOMATIC_BROWSING_CHECK_DELAY));
	    
	    /* PUBLISHING TOPICS & POSTS */
		postAsMobileByDefaultToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_POST_AS_MOBILE_BY_DEFAULT, JvcUserData.DEFAULT_POST_AS_MOBILE_BY_DEFAULT));
		showCharacterCounterToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_SHOW_CHARACTER_COUNTER, JvcUserData.DEFAULT_SHOW_CHARACTER_COUNTER));
	    
	    /* APPEARANCE */
		themeSelectionSpinner.setSelection(app.getJvcThemeId());
		animateSmileysToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS));
		showNoelshackThumbnailsToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS));
		keepJvcStyleTopicsToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_TOPICS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_TOPICS));
		keepJvcStylePostsToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_KEEP_JVC_STYLE_POSTS, JvcUserData.DEFAULT_KEEP_JVC_STYLE_POSTS));
		postsTextSizeSpinner.setSelection(JvcUserData.getInt(JvcUserData.PREF_POSTS_TEXT_SIZE, JvcUserData.DEFAULT_POSTS_TEXT_SIZE));
		smileysSizeSpinner.setSelection(JvcUserData.getInt(JvcUserData.PREF_SMILEYS_SIZE, JvcUserData.DEFAULT_SMILEYS_SIZE));
		hideTitleBarToggle.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_HIDE_TITLE_BAR, JvcUserData.DEFAULT_HIDE_TITLE_BAR));
		hideTitleBarToggle.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked)
			{
				startActivity(getIntent());
				finish();
			}
		});
	}

	public void prefsPseudoBlacklistItemClick(View view)
	{
		startActivity(new Intent(this, PreferencesPseudoBlacklistActivity.class));
	}

	public void prefsSignatureItemClick(View view)
	{
		startActivity(new Intent(this, PreferencesSignatureActivity.class));
	}

	private final OnItemSelectedListener themeSelectionListener = new OnItemSelectedListener()
	{
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
		{
			app.setJvcThemeId((int) id);
			checkTheme();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
			app.setJvcThemeId(0);
			checkTheme();
		}
	};

	private class PrefPickerListener implements OnItemSelectedListener
	{
		private String prefName;
		private int prefDefault;

		public PrefPickerListener(String prefName, int prefDefault)
		{
			this.prefName = prefName;
			this.prefDefault = prefDefault;
		}

		public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
		{
			int id_int = (int) id;
			if(id_int != JvcUserData.getInt(prefName, prefDefault))
			{
				JvcUserData.startEditing();
				JvcUserData.setInt(prefName, id_int);
				JvcUserData.stopEditing();
				GlobalData.set("editedPrefs", true);
			}
		}

		public void onNothingSelected(AdapterView<?> adapterView)
		{
		}
	}

	private class ToggleButtonPrefListener implements OnClickListener
	{
		private String prefName;
		private boolean trackChanges;
		private View parentedItem = null;

		public ToggleButtonPrefListener(String prefName)
		{
			this(prefName, false, null);
		}

		public ToggleButtonPrefListener(String prefName, boolean trackChanges)
		{
			this(prefName, trackChanges, null);
		}

		public ToggleButtonPrefListener(String prefName, boolean trackChanges, MenuCollapsibleItem parentedItem)
		{
			this.prefName = prefName;
			this.trackChanges = trackChanges;
			this.parentedItem = parentedItem;
		}

		public void onClick(View view)
		{
			boolean newValue = ((ToggleButton) view).isChecked();
			JvcUserData.startEditing();
			JvcUserData.setBoolean(prefName, newValue);
			JvcUserData.stopEditing();
			GlobalData.set("editedPrefs", true);
			if(trackChanges)
				GlobalData.set("editedPref" + prefName, true);
			if(parentedItem != null)
				parentedItem.setEnabled(newValue);
		}
	}

	private class TimeChangeListener implements OnClickListener
	{
		private String prefName;
		private Long prefDefault;
		private boolean trackChanges;

		public TimeChangeListener(String prefName, Long prefDefault, boolean trackChanges)
		{
			this.prefName = prefName;
			this.prefDefault = prefDefault;
			this.trackChanges = trackChanges;
		}

		public TimeChangeListener(String prefName, Long prefDefault)
		{
			this(prefName, prefDefault, false);
		}

		public void onClick(View view)
		{
			long newValue = ((SmartTimePicker) view).getSelectedTime();
			if(newValue != JvcUserData.getLong(prefName, prefDefault))
			{
				if(trackChanges)
					GlobalData.set("editedPref" + prefName, true);
				JvcUserData.startEditing();
				JvcUserData.setLong(prefName, newValue);
				JvcUserData.stopEditing();
				GlobalData.set("editedPrefs", true);
			}
		}
	}

	private ArrayAdapter<CharSequence> newPrefPickerAdapter(int resId)
	{
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, resId, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return adapter;
	}
}
