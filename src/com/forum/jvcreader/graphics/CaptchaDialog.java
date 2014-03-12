package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.blundell.tut.LoaderImageView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.StringHelper;

public class CaptchaDialog extends Dialog
{
	private Context context;
	private String captchaUrl;

	private FrameLayout errorLayout;
	private TextView errorTextView;

	private TextView dialogTextView;
	private LoaderImageView imageView;
	private EditText editText;
	private Button reloadButton;
	private Button submitButton;
	private View.OnClickListener submitButtonListener;

	public CaptchaDialog(Context context, String url)
	{
		super(context);
		this.context = context;
		captchaUrl = StringHelper.unescapeHTML(url);

		setContentView(R.layout.captcha_dialog);
		setTitle("Captcha");

		dialogTextView = (TextView) findViewById(R.id.captchaDialogTextView);
		errorLayout = (FrameLayout) findViewById(R.id.captchaErrorLayout);
		errorLayout.setVisibility(View.GONE);
		errorTextView = (TextView) findViewById(R.id.captchaErrorTextView);

		imageView = (LoaderImageView) findViewById(R.id.captchaImageView);
		imageView.setImageDrawable(captchaUrl);
		editText = (EditText) findViewById(R.id.captchaEditText);

		reloadButton = (Button) findViewById(R.id.captchaReloadButton);
		reloadButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				imageView.setImageDrawable(captchaUrl);
			}
		});
		submitButton = (Button) findViewById(R.id.captchaSubmitButton);
		submitButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				if(submitButtonListener != null)
				{
					String captchaString = editText.getText().toString();
					if(captchaString.matches("^[0-9]{4}$"))
					{
						showErrorFrame(false, "");
						submitButtonListener.onClick(view);
					}
					else
					{
						showErrorFrame(true, CaptchaDialog.this.context.getString(R.string.badCaptchaFormatting));
					}
				}
			}
		});

		setCancelable(true);
	}

	public void setDialogTitle(CharSequence title)
	{
		dialogTextView.setText(title);
	}

	public String getCaptchaString()
	{
		return editText.getText().toString();
	}

	public void setSubmitButtonListener(View.OnClickListener l)
	{
		submitButtonListener = l;
	}

	public void showErrorFrame(boolean show, String text)
	{
		int visibility = show ? View.VISIBLE : View.GONE;
		errorLayout.setVisibility(visibility);
		errorTextView.setText(text);
	}
}