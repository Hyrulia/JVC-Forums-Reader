package com.forum.jvcreader.graphics.jvc.cdv;

import android.content.Context;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.forum.jvcreader.R;

public class CdvPrefItem
{
	View view, separator;
	TextView dateTextView, typeTextView, titleTextView;

	public CdvPrefItem(Context context)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.jvc_pref_item, null);
		separator = view.findViewById(R.id.jvcPrefItemSeparatorView);
		dateTextView = (TextView) view.findViewById(R.id.jvcPrefItemDateTextView);
		typeTextView = (TextView) view.findViewById(R.id.jvcPrefItemTypeTextView);
		titleTextView = (TextView) view.findViewById(R.id.jvcPrefItemTitleTextView);
		titleTextView.setMovementMethod(LinkMovementMethod.getInstance());
		titleTextView.setTextColor(Color.BLACK);
	}

	public void setData(CharSequence date, CharSequence type, CharSequence title)
	{
		dateTextView.setText(date);
		typeTextView.setText(type);
		titleTextView.setText(title);
	}

	public void setSeparatorVisibility(int v)
	{
		separator.setVisibility(v);
	}

	public View getView()
	{
		return view;
	}
}
