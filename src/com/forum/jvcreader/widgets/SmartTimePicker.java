package com.forum.jvcreader.widgets;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.forum.jvcreader.R;

public class SmartTimePicker extends LinearLayout
{
	public static final long OPTION1_TIME = 60000L; /* 1 minute */
	public static final long OPTION2_TIME = 900000L; /* 15 minutes */
	public static final long OPTION3_TIME = 3600000L; /* 1 hour */
	public static final long OPTION4_TIME = 43200000L; /* 12 hours */

	private Context context;
	private OnClickListener timeChangeListener;

	private RadioButton timeOption1;
	private RadioButton timeOption2;
	private RadioButton timeOption3;
	private RadioButton timeOption4;
	private RadioButton timeOptionCustom;

	private LinearLayout customOptionLayout;
	private EditText hourEdit;
	private EditText minEdit;
	private EditText secEdit;

	private long selectedTime = OPTION1_TIME;

	public SmartTimePicker(Context context)
	{
		super(context);
		this.context = context;
		initializeTimePickerLayout();
	}

	public SmartTimePicker(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
		initializeTimePickerLayout();
	}

	private void initializeTimePickerLayout()
	{
		setOrientation(VERTICAL);

		RadioGroup group = new RadioGroup(context);

		timeOption1 = new RadioButton(context);
		timeOption1.setText(R.string.timeEveryMinute);
		timeOption1.setTextAppearance(context, android.R.style.TextAppearance_Medium);
		timeOption1.setOnClickListener(checkboxListener);
		group.addView(timeOption1);

		timeOption2 = new RadioButton(context);
		timeOption2.setText(R.string.timeEvery15Minutes);
		timeOption2.setTextAppearance(context, android.R.style.TextAppearance_Medium);
		timeOption2.setOnClickListener(checkboxListener);
		group.addView(timeOption2);

		timeOption3 = new RadioButton(context);
		timeOption3.setText(R.string.timeEveryHour);
		timeOption3.setTextAppearance(context, android.R.style.TextAppearance_Medium);
		timeOption3.setOnClickListener(checkboxListener);
		group.addView(timeOption3);

		timeOption4 = new RadioButton(context);
		timeOption4.setText(R.string.timeEvery12Hours);
		timeOption4.setTextAppearance(context, android.R.style.TextAppearance_Medium);
		timeOption4.setOnClickListener(checkboxListener);
		group.addView(timeOption4);

		timeOptionCustom = new RadioButton(context);
		timeOptionCustom.setText(R.string.timeCustom);
		timeOptionCustom.setTextAppearance(context, android.R.style.TextAppearance_Medium);
		timeOptionCustom.setOnClickListener(checkboxListener);
		group.addView(timeOptionCustom);

		this.addView(group);

		customOptionLayout = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.custom_time_pick_form, null);
		customOptionLayout.setVisibility(View.GONE);
		this.addView(customOptionLayout);

		hourEdit = (EditText) customOptionLayout.findViewById(R.id.customTimePickFormHourEditText);
		minEdit = (EditText) customOptionLayout.findViewById(R.id.customTimePickFormMinEditText);
		secEdit = (EditText) customOptionLayout.findViewById(R.id.customTimePickFormSecEditText);
	}

	public void updateSelectionFromTime(long time)
	{
		if(time == OPTION1_TIME)
			timeOption1.setChecked(true); /* No switch statement available for long */
		else if(time == OPTION2_TIME)
			timeOption2.setChecked(true);
		else if(time == OPTION3_TIME)
			timeOption3.setChecked(true);
		else if(time == OPTION4_TIME)
			timeOption4.setChecked(true);
		else
		{
			timeOptionCustom.setChecked(true);
			time /= 1000;
			hourEdit.setText(String.valueOf((int) (time / 3600)));
			minEdit.setText(String.valueOf((int) ((time % 3600) / 60)));
			secEdit.setText(String.valueOf((int) (time % 60)));
			customOptionLayout.setVisibility(View.VISIBLE);
		}

		hourEdit.addTextChangedListener(onTextChangedWatcher);
		minEdit.addTextChangedListener(onTextChangedWatcher);
		secEdit.addTextChangedListener(onTextChangedWatcher);
	}

	public long getSelectedTime()
	{
		return selectedTime;
	}

	public void setTimeChangeOnClickListener(OnClickListener l)
	{
		timeChangeListener = l;
	}

	private final OnClickListener checkboxListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			if(view.equals(timeOptionCustom))
			{
				hourEdit.setText("");
				minEdit.setText("");
				secEdit.setText("");
				customOptionLayout.setVisibility(View.VISIBLE);
				hourEdit.requestFocus();
			}
			else
			{
				customOptionLayout.setVisibility(View.GONE);

				if(view.equals(timeOption1))
					selectedTime = OPTION1_TIME;
				else if(view.equals(timeOption2))
					selectedTime = OPTION2_TIME;
				else if(view.equals(timeOption3))
					selectedTime = OPTION3_TIME;
				else if(view.equals(timeOption4))
					selectedTime = OPTION4_TIME;

				if(timeChangeListener != null)
					timeChangeListener.onClick(SmartTimePicker.this);
			}
		}
	};

	private final TextWatcher onTextChangedWatcher = new TextWatcher()
	{

		@Override
		public void afterTextChanged(Editable text)
		{
			String s = text.toString();
			if(s.length() > 0)
			{
				try
				{
					int time = Integer.parseInt(text.toString());
					if(time < 0)
					{
						text.clear();
						text.append("0");
					}
					else if(time > 59)
					{
						text.clear();
						text.append("59");
					}
				}
				catch(NumberFormatException e)
				{
					text.clear();
				}
			}

			String hour = hourEdit.getText().toString(), min = minEdit.getText().toString(), sec = secEdit.getText().toString();
			int hourInt, minInt, secInt;
			if(hour.length() > 0)
				hourInt = Integer.parseInt(hour);
			else
				hourInt = 0;
			if(min.length() > 0)
				minInt = Integer.parseInt(min);
			else
				minInt = 0;
			if(sec.length() > 0)
				secInt = Integer.parseInt(sec);
			else
				secInt = 0;

			long time = hourInt * 3600000L + minInt * 60000L + secInt * 1000L;
			if(time > 0)
			{
				selectedTime = time;
				if(timeChangeListener != null)
					timeChangeListener.onClick(SmartTimePicker.this);
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}
	};
}
