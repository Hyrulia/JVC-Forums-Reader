package com.forum.jvcreader.noelshack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

public class NoelshackClickableSpan extends ClickableSpan
{
	private Context context;
	private NoelshackImageSpan imageSpan;

	public NoelshackClickableSpan(Context context, NoelshackImageSpan imageSpan)
	{
		this.context = context;
		this.imageSpan = imageSpan;
	}

	@Override
	public void onClick(View widget)
	{
		if(context instanceof Activity)
		{
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageSpan.getDestinationUrl().toString()));
			context.startActivity(browserIntent);
		}
	}
}
