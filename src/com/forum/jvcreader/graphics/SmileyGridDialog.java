package com.forum.jvcreader.graphics;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.CachedRawDrawables;

public class SmileyGridDialog
{
	private Dialog dialog;
	private OnClickListener listener;
	private int currentId;

	private static final int[] drawableList = {R.raw.s1, R.raw.s20, R.raw.s17, R.raw.s3, R.raw.s46, R.raw.s13, R.raw.s69_1, R.raw.s4, R.raw.s18, R.raw.s22_23, R.raw.s9, R.raw.s5, R.raw.s23_1, R.raw.s57, R.raw.s10, R.raw.nyu_1, R.raw.s24_4, R.raw.s7, R.raw.s31_5, R.raw.s11, R.raw.s37_2, R.raw.s45, R.raw.s47, R.raw.s2, R.raw.s26_1, R.raw.s14, R.raw.s54, R.raw.s21, R.raw.s39_1, R.raw.s15, R.raw.s50, R.raw.s27_6, R.raw.s40, R.raw.s25_1, R.raw.s53, R.raw.s30_1, R.raw.s41_1, R.raw.s33_1, R.raw.s43_1, R.raw.s34_4, R.raw.s12, R.raw.s19, R.raw.s28_2, R.raw.s55, R.raw.s36, R.raw.s35_1, R.raw.s8, R.raw.s66_3, R.raw.s67, R.raw.s68, R.raw.s60, R.raw.s61, R.raw.s62, R.raw.play_1, R.raw.s65, R.raw.s63, R.raw.s58, R.raw.s59, R.raw.s56, R.raw.s42, R.raw.s38, R.raw.s29, R.raw.s44, R.raw.s48, R.raw.s51, R.raw.s32_1, R.raw.s49, R.raw.s52, R.raw.s64, R.raw.s70_1, R.raw.s71_4, R.raw.pf};

	private static final String[] smileyNameList = {":)", ":snif:", ":gba:", ":g)", ":-)", ":snif2:", ":bravo:", ":d)", ":hap:", ":ouch:", ":pacg:", ":cd:", ":-)))", ":ouch2:", ":pacd:", ":cute:", ":content:", ":p)", ":-p", ":noel:", ":oui:", ":(", ":peur:", ":question:", ":cool:", ":-(", ":coeur:", ":mort:", ":rire:", ":-((", ":fou:", ":sleep:", ":-D", ":nonnon:", ":fier:", ":honte:", ":rire2:", ":non2:", ":sarcastic:", ":monoeil:", ":o))", ":nah:", ":doute:", ":rouge:", ":ok:", ":non:", ":malade:", ":fete:", ":sournois:", ":hum:", ":ange:", ":diable:", ":gni:", ":play:", ":desole:", ":spoiler:", ":merci:", ":svp:", ":sors:", ":salut:", ":rechercher:", ":hello:", ":up:", ":bye:", ":gne:", ":lol:", ":dpdr:", ":dehors:", ":hs:", ":banzai:", ":bave:", ":pf:"};

	public SmileyGridDialog(Context context, OnClickListener listener)
	{
		ScrollView mainView = (ScrollView) ((Activity) context).getLayoutInflater().inflate(R.layout.smiley_grid_dialog, null);
		LinearLayout mainLayout = (LinearLayout) mainView.getChildAt(0);

		for(int i = 0; i < 18; i++)
		{
			LinearLayout layout = (LinearLayout) mainLayout.getChildAt(i);
			for(int j = 0; j < 4; j++)
			{
				int id = i * 4 + j;
				if(id == drawableList.length)
					break;

				FrameLayout frame = (FrameLayout) layout.getChildAt(j);
				frame.setOnClickListener(new SmileyOnClickListener(id));
				ImageView imageView = (ImageView) frame.getChildAt(0);
				imageView.setImageDrawable(CachedRawDrawables.getSmileyDrawable(drawableList[id]));
			}
		}

		dialog = new Dialog(context, R.style.FullScreenNoTitleDialogTheme);
		dialog.setContentView(mainView);
		dialog.setCancelable(true);

		this.listener = listener;
	}

	public void show()
	{
		dialog.show();
	}

	public void dismiss()
	{
		dialog.dismiss();
	}

	public Dialog getDialog()
	{
		return dialog;
	}

	public String getSelectedSmiley()
	{
		return smileyNameList[currentId];
	}

	private class SmileyOnClickListener implements OnClickListener
	{
		private int id;

		public SmileyOnClickListener(int id)
		{
			this.id = id;
		}

		@Override
		public void onClick(View view)
		{
			currentId = id;
			listener.onClick(view);
		}
	}
}
