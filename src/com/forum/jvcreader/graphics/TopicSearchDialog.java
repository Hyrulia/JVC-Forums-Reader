package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcForum;

public class TopicSearchDialog extends Dialog
{
	private EditText editText;
	private RadioButton radioSubject;
	private RadioButton radioAuthor;
	private RadioButton radioMessage;
	private Button buttonSearchMyTopics;
	private Button buttonSearch;

	private int listType;

	public TopicSearchDialog(Context context)
	{
		super(context);

		setContentView(R.layout.topic_search_dialog);
		setTitle(R.string.searchTopics);

		editText = (EditText) findViewById(R.id.topicSearchEditText);
		radioSubject = (RadioButton) findViewById(R.id.topicSearchSubjectRadioButton);
		radioAuthor = (RadioButton) findViewById(R.id.topicSearchAuthorRadioButton);
		radioMessage = (RadioButton) findViewById(R.id.topicSearchMessageRadioButton);
		buttonSearchMyTopics = (Button) findViewById(R.id.topicSearchMyTopicsButton);
		buttonSearch = (Button) findViewById(R.id.topicSearchSubmitButton);

		radioSubject.setOnClickListener(radioListener);
		radioAuthor.setOnClickListener(radioListener);
		radioMessage.setOnClickListener(radioListener);
		radioSubject.setChecked(true);
		listType = JvcForum.SEARCH_TOPICS;
	}

	public void setSearchMyTopicsButtonListener(View.OnClickListener listener)
	{
		buttonSearchMyTopics.setOnClickListener(listener);
	}

	public void setSearchButtonListener(View.OnClickListener listener)
	{
		buttonSearch.setOnClickListener(listener);
	}

	public int getListType()
	{
		return listType;
	}

	public String getSearchString()
	{
		return editText.getText().toString();
	}

	private final View.OnClickListener radioListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			if(view == radioSubject)
				listType = JvcForum.SEARCH_TOPICS;
			else if(view == radioAuthor)
				listType = JvcForum.SEARCH_PSEUDOS;
			else if(view == radioMessage)
				listType = JvcForum.SEARCH_TOPIC_POSTS;
		}
	};
}