package com.forum.jvcreader.graphics;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import com.forum.jvcreader.R;

public class NoticeDialog
{
	public static void show(Context context, CharSequence message)
	{
		if(context == null)
			return;
		new AlertDialog.Builder(context).setMessage(message).setNeutralButton("Ok", new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		}).setCancelable(false).create().show();
	}

	public static void showYesNo(Context context, CharSequence message, DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListener)
	{
		if(context == null)
			return;
		new AlertDialog.Builder(context).setMessage(message).setCancelable(false).setPositiveButton(R.string.yes, yesListener).setNegativeButton(R.string.no, noListener).create().show();
	}
}
