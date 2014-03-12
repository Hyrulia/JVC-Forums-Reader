package com.forum.jvcreader.utils;

import android.graphics.drawable.Drawable;

import java.util.List;

public class DrawableSequence
{
	private List<Drawable> sequence;
	private int sequenceSize;
	private int currentIndex = 0;

	public DrawableSequence(List<Drawable> drawableList)
	{
		if(drawableList == null || drawableList.size() == 0)
			throw new RuntimeException("Drawable list is null or empty");
		sequence = drawableList;
		sequenceSize = sequence.size();
	}

	public void nextFrame()
	{
		currentIndex = (currentIndex + 1) % sequenceSize;
	}

	public Drawable getCurrentDrawable()
	{
		return sequence.get(currentIndex);
	}
}
