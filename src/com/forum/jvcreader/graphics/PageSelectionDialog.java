package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.forum.jvcreader.R;

public class PageSelectionDialog extends Dialog
{
	private Context context;

	private EditText editText;
	private Button buttonGo;

	private View.OnClickListener buttonGoListener;

	private int pageNumber;
	private int pageCount;

	public PageSelectionDialog(Context context, int pageCount)
	{
		super(context);

		setContentView(R.layout.page_selection_dialog);
		setTitle(R.string.topicGoToPage);
		setCancelable(true);

		editText = (EditText) findViewById(R.id.pageSelectionEditText);
		buttonGo = (Button) findViewById(R.id.pageSelectionGoButton);
		buttonGo.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				if(editText.length() > 0)
				{
					boolean exceptionThrown = false;
					try
					{
						pageNumber = Integer.parseInt(editText.getText().toString());
					}
					catch(NumberFormatException e)
					{
						exceptionThrown = true;
					}

					if(pageNumber < 1 || pageNumber > PageSelectionDialog.this.pageCount || exceptionThrown)
					{
						String text = String.format(PageSelectionDialog.this.context.getString(R.string.badPageSelection), 1, PageSelectionDialog.this.pageCount);
						Toast.makeText(PageSelectionDialog.this.context, text, Toast.LENGTH_LONG).show();
						editText.setText("");
					}
					else
					{
						if(buttonGoListener != null)
							buttonGoListener.onClick(view);
					}
				}
			}
		});

		this.context = context;
		this.pageCount = pageCount;
	}

	public int getSelectedPageNumber()
	{
		return pageNumber;
	}

	public void setButtonOnClickListener(View.OnClickListener l)
	{
		buttonGoListener = l;
	}
}
