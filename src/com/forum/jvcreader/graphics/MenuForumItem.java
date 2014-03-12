package com.forum.jvcreader.graphics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.forum.jvcreader.R;

public class MenuForumItem
{
	private LinearLayout layout;
	private View separatorView;
	private TextView textView;

	public MenuForumItem(LayoutInflater inflater)
	{
		layout = (LinearLayout) inflater.inflate(R.layout.menu_forum_item, null);
		separatorView = layout.getChildAt(0);
		textView = (TextView) layout.getChildAt(1);
	}

	public void setItemText(String text)
	{
		textView.setText(text);
	}

	public void setItemText(int resource)
	{
		textView.setText(resource);
	}

	public void setItemOnClickListener(OnClickListener listener)
	{
		textView.setOnClickListener(listener);
	}

	public void setSeparatorVisibility(int visibility)
	{
		separatorView.setVisibility(visibility);
	}

	public View getView()
	{
		return layout;
	}
}
