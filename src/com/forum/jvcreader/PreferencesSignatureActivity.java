package com.forum.jvcreader;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.forum.jvcreader.graphics.PostItem;
import com.forum.jvcreader.jvc.JvcPost;
import com.forum.jvcreader.jvc.JvcTextSpanner;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.GlobalData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PreferencesSignatureActivity extends JvcActivity
{
	private CheckBox useSignatureCheckBox;
	private EditText editText;
	private Button previewButton;
	private RadioButton appendStartRadioButton;
	private RadioButton appendEndRadioButton;
	private CheckBox includeByDefaultCheckBox;

	private boolean showNoelshackThumbnails;
	private boolean animateSmileys;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signature);

		showNoelshackThumbnails = JvcUserData.getBoolean(JvcUserData.PREF_SHOW_NOELSHACK_THUMBNAILS, JvcUserData.DEFAULT_SHOW_NOELSHACK_THUMBNAILS);
		animateSmileys = JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS);

		useSignatureCheckBox = (CheckBox) findViewById(R.id.signatureUseSignatureCheckBox);
		useSignatureCheckBox.setOnClickListener(useSignatureListener);
		editText = (EditText) findViewById(R.id.signatureEditText);
		previewButton = (Button) findViewById(R.id.signaturePreviewButton);
		previewButton.setOnClickListener(previewListener);
		appendStartRadioButton = (RadioButton) findViewById(R.id.signatureAppendAtStartRadioButton);
		appendStartRadioButton.setOnClickListener(radioButtonListener);
		appendEndRadioButton = (RadioButton) findViewById(R.id.signatureAppendAtEndRadioButton);
		appendEndRadioButton.setOnClickListener(radioButtonListener);
		includeByDefaultCheckBox = (CheckBox) findViewById(R.id.signatureIncludeByDefaultCheckBox);
		includeByDefaultCheckBox.setOnClickListener(includeByDefaultListener);

		useSignatureCheckBox.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_USE_SIGNATURE, JvcUserData.DEFAULT_USE_SIGNATURE));
		editText.setText(JvcUserData.getString(JvcUserData.PREF_SIGNATURE, JvcUserData.DEFAULT_SIGNATURE));
		includeByDefaultCheckBox.setChecked(JvcUserData.getBoolean(JvcUserData.PREF_INCLUDE_SIGNATURE_BY_DEFAULT, JvcUserData.DEFAULT_INCLUDE_SIGNATURE_BY_DEFAULT));

		updateItems();
	}

	@Override
	public void onPause()
	{
		JvcUserData.startEditing();
		JvcUserData.setString(JvcUserData.PREF_SIGNATURE, editText.getText().toString());
		if(editText.length() == 0)
		{
			useSignatureCheckBox.setChecked(false);
			JvcUserData.setBoolean(JvcUserData.PREF_USE_SIGNATURE, false);
		}
		JvcUserData.stopEditing();
		super.onPause();
	}

	private final OnClickListener useSignatureListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			JvcUserData.startEditing();
			JvcUserData.setBoolean(JvcUserData.PREF_USE_SIGNATURE, useSignatureCheckBox.isChecked());
			JvcUserData.stopEditing();
			GlobalData.set("editedPrefs", true);
			updateItems();
		}
	};

	private final OnClickListener previewListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			if(editText.length() > 0)
			{
				MainApplication appContext = (MainApplication) getApplicationContext();
				PostItem item = new PostItem(PreferencesSignatureActivity.this, true, showNoelshackThumbnails, animateSmileys);
				String date = new SimpleDateFormat("d MMMM y \u00E0 HH:mm:ss", Locale.FRANCE).format(new Date());
				JvcPost post = new JvcPost(null, 0, "", appContext.getJvcPseudo(), false, false, 1, date, null, null);

				item.updateDataFromPost(post, true);
				if(appendStartRadioButton.isChecked())
				{
					SpannableStringBuilder builder = new SpannableStringBuilder(JvcTextSpanner.getSpannableTextFromTopicTextualPost(item.getLoader(), editText.getText().toString() + "\n\n", showNoelshackThumbnails, animateSmileys));
					final String s = getString(R.string.firstLineOfPost);
					builder.append(s).append("\n");
					builder.setSpan(new ForegroundColorSpan(Color.GRAY), builder.length() - (s.length() + 1), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					item.getPostTextView().setText(builder);
				}
				else if(appendEndRadioButton.isChecked())
				{
					SpannableStringBuilder builder = new SpannableStringBuilder(getString(R.string.lastLineOfPost));
					builder.setSpan(new ForegroundColorSpan(Color.GRAY), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					builder.append("\n\n");
					builder.append(JvcTextSpanner.getSpannableTextFromTopicTextualPost(item.getLoader(), editText.getText().toString() + "\n", showNoelshackThumbnails, animateSmileys));
					item.getPostTextView().setText(builder);
				}

				final Dialog previewDialog = new Dialog(PreferencesSignatureActivity.this, R.style.FullScreenNoTitleDialogTheme);
				previewDialog.setContentView(item.getView());
				startAnimatingDrawables(item.getView());
				previewDialog.setCancelable(true);
				previewDialog.setOnDismissListener(new OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						stopAnimatingDrawables();
					}
				});
				previewDialog.show();
				item.getLoader().startLoading();
			}
			else
			{
				Toast.makeText(PreferencesSignatureActivity.this, R.string.signatureEmpty, Toast.LENGTH_LONG).show();
			}
		}
	};

	private final OnClickListener radioButtonListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			JvcUserData.startEditing();

			if(view.equals(appendStartRadioButton))
			{
				JvcUserData.setBoolean(JvcUserData.PREF_SIGNATURE_AT_START, true);
				appendEndRadioButton.setChecked(false);
			}
			else if(view.equals(appendEndRadioButton))
			{
				JvcUserData.setBoolean(JvcUserData.PREF_SIGNATURE_AT_START, false);
				appendStartRadioButton.setChecked(false);
			}

			JvcUserData.stopEditing();
			GlobalData.set("editedPrefs", true);
		}
	};

	private final OnClickListener includeByDefaultListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			JvcUserData.startEditing();
			JvcUserData.setBoolean(JvcUserData.PREF_INCLUDE_SIGNATURE_BY_DEFAULT, includeByDefaultCheckBox.isChecked());
			JvcUserData.stopEditing();
			GlobalData.set("editedPrefs", true);
		}
	};

	private void updateItems()
	{
		if(useSignatureCheckBox.isChecked())
		{
			editText.setEnabled(true);
			previewButton.setEnabled(true);
			appendStartRadioButton.setEnabled(true);
			appendEndRadioButton.setEnabled(true);
			includeByDefaultCheckBox.setEnabled(true);

			if(JvcUserData.getBoolean(JvcUserData.PREF_SIGNATURE_AT_START, JvcUserData.DEFAULT_SIGNATURE_AT_START))
			{
				appendEndRadioButton.setChecked(false);
				appendStartRadioButton.setChecked(true);
			}
			else
			{
				appendStartRadioButton.setChecked(false);
				appendEndRadioButton.setChecked(true);
			}
		}
		else
		{
			appendStartRadioButton.setChecked(false);
			appendEndRadioButton.setChecked(false);

			editText.setEnabled(false);
			previewButton.setEnabled(false);
			appendStartRadioButton.setEnabled(false);
			appendEndRadioButton.setEnabled(false);
			includeByDefaultCheckBox.setEnabled(false);
		}
	}
}
