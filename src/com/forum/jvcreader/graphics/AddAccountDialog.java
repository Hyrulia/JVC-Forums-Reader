package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcAccount;
import com.forum.jvcreader.jvc.JvcUtils;

public class AddAccountDialog extends Dialog
{
	private Context context;
	private EditText pseudoEdit;
	private EditText passwordEdit;

	private OnAccountSubmitListener accountSubmitListener = null;

	public AddAccountDialog(Context context)
	{
		super(context);
		this.context = context;

		setContentView(R.layout.add_account_dialog);
		setTitle(R.string.addAccountDialogTitle);

		pseudoEdit = (EditText) findViewById(R.id.addAccountDialogPseudoEditText);
		passwordEdit = (EditText) findViewById(R.id.addAccountDialogPasswordEditText);
		findViewById(R.id.addAccountDialogSubmitButton).setOnClickListener(submitButtonListener);
	}

	public void setOnAccountSubmitListener(OnAccountSubmitListener listener)
	{
		accountSubmitListener = listener;
	}

	private final View.OnClickListener submitButtonListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			final String pseudo = pseudoEdit.getText().toString();
			final String password = passwordEdit.getText().toString();

			if(!JvcUtils.isPseudoCorrect(pseudo) || !JvcUtils.isPasswordCorrect(password))
			{
				Toast.makeText(context, R.string.incorrectLoginOrPasswordFormatting, Toast.LENGTH_LONG).show();
			}
			else
			{
				dismiss();
				if(accountSubmitListener != null)
					accountSubmitListener.onAccountSubmit(new JvcAccount(pseudo, password));
			}
		}
	};

	public interface OnAccountSubmitListener
	{
		public void onAccountSubmit(JvcAccount account);
	}
}
