package com.forum.jvcreader.graphics;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MenuSeparator
{
	private LinearLayout layout;
	private TextView textView;

	public MenuSeparator(int resource, LayoutInflater inflater)
	{
		layout = (LinearLayout) inflater.inflate(resource, null);
		textView = (TextView) layout.getChildAt(0);
	}

	public void setText(String text)
	{
		textView.setText(text);
	}

	public void setText(int resource)
	{
		textView.setText(resource);
	}

	public View getView()
	{
		return layout;
	}
}