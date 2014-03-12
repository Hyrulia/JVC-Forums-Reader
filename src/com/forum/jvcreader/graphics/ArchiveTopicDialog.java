package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcArchivedTopic;
import com.forum.jvcreader.jvc.JvcTopic;
import com.forum.jvcreader.jvc.JvcUtils;

public class ArchiveTopicDialog extends Dialog
{
	private Context context;
	private JvcTopic topic;
	private OnArchivedTopicSubmitListener onArchivedTopicListener;

	private EditText postCountPerPageEditText;
	private EditText fromPageEditText;
	private EditText toPageEditText;
	private CheckBox authorPostsOnlyCheckBox;

	public ArchiveTopicDialog(Context context, JvcTopic topic)
	{
		super(context);
		this.context = context;
		this.topic = topic;

		setContentView(R.layout.archive_topic_dialog);
		setTitle(R.string.archiveTopicDialogTitle);

		int pageCount = topic.getRequestPageCount();
		String title = String.format("\u00AB %s \u00BB\n%d ", topic.getExtraTopicName(), pageCount);
		if(pageCount > 1)
		{
			title += context.getString(R.string.pages);
		}
		else
		{
			title += context.getString(R.string.page);
		}
		((TextView) findViewById(R.id.archiveTopicTextView)).setText(title);

		postCountPerPageEditText = (EditText) findViewById(R.id.archiveTopicPostCountEditText);
		postCountPerPageEditText.setText(String.valueOf(JvcUtils.POSTS_PER_PAGE));
		fromPageEditText = (EditText) findViewById(R.id.archiveTopicFromPageEditText);
		fromPageEditText.setText("1");
		toPageEditText = (EditText) findViewById(R.id.archiveTopicToPageEditText);
		toPageEditText.setText(String.valueOf(pageCount));
		authorPostsOnlyCheckBox = (CheckBox) findViewById(R.id.archiveTopicAuthorPostsCheckBox);
		findViewById(R.id.archiveTopicSubmitButton).setOnClickListener(submitListener);
	}

	private final View.OnClickListener submitListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			int postCountPerPage, pageStart, pageEnd;

			if(postCountPerPageEditText.length() == 0)
				return;
			postCountPerPage = Integer.parseInt(postCountPerPageEditText.getText().toString());
			if(postCountPerPage < 1 || postCountPerPage > JvcArchivedTopic.MAX_POSTS_PER_FILE)
			{
				Toast.makeText(context, R.string.incorrectPostCountPerPage, Toast.LENGTH_SHORT).show();
				return;
			}

			if(fromPageEditText.length() == 0)
				return;
			pageStart = Integer.parseInt(fromPageEditText.getText().toString());
			if(pageStart < 1 || pageStart > topic.getRequestPageCount())
			{
				String notice = String.format(context.getString(R.string.incorrectStartingPage), topic.getRequestPageCount());
				Toast.makeText(context, notice, Toast.LENGTH_SHORT).show();
				return;
			}

			if(toPageEditText.length() == 0)
				return;
			pageEnd = Integer.parseInt(toPageEditText.getText().toString());
			if(pageEnd < pageStart || pageEnd > topic.getRequestPageCount())
			{
				String notice = String.format(context.getString(R.string.incorrectEndingPage), pageStart, topic.getRequestPageCount());
				Toast.makeText(context, notice, Toast.LENGTH_SHORT).show();
				return;
			}

			JvcArchivedTopic archivedTopic = new JvcArchivedTopic(topic, postCountPerPage, pageStart, pageEnd, authorPostsOnlyCheckBox.isChecked());
			onArchivedTopicListener.onArchivedTopicSubmit(archivedTopic);
			dismiss();
		}
	};

	public void setOnArchivedTopicSubmitListener(OnArchivedTopicSubmitListener listener)
	{
		onArchivedTopicListener = listener;
	}

	public interface OnArchivedTopicSubmitListener
	{
		public void onArchivedTopicSubmit(JvcArchivedTopic topic);
	}
}
