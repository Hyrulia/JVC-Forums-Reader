package com.forum.jvcreader;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.forum.jvcreader.jvc.JvcUtils;

public class InfoActivity extends JvcActivity
{
	private TextView creditsTextView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);

		creditsTextView = (TextView) findViewById(R.id.infoCreditsTextView);
		creditsTextView.setMovementMethod(LinkMovementMethod.getInstance());

		/* Dirty */
		SpannableStringBuilder creditsText = new SpannableStringBuilder();
		creditsText.append(getString(R.string.infoAppAuthor) + " ");
		creditsText.append(makeCdvLink("Mat000"));
		creditsText.append("\n\n" + getString(R.string.infoThanksIcon) + " ");
		creditsText.append(makeCdvLink("AlexeiVolkoff"));
		creditsText.append("\n\n" + getString(R.string.infoThanksVideo) + " ");
		creditsText.append(makeCdvLink("Serdaigle"));
		creditsText.append("  ");
		creditsText.append(JvcUtils.JvcLinkIntent.makeLink(this, getString(R.string.infoVideo), "http://www.youtube.com/watch?v=wv3CNCf_SaY", true));
		creditsText.append("\n\n" + getString(R.string.infoThanksDonate) + " mathieu.denuit@gmail.com");
		creditsText.append("\n\n" + getString(R.string.infoThanksCreditedPseudos));

		String[] creditedPseudos = getResources().getStringArray(R.array.creditedPseudos);
		for(int i = 0; i < creditedPseudos.length; i++)
		{
			creditsText.append("\n");
			creditsText.append(makeCdvLink(creditedPseudos[i]));
		}
		creditsText.append("\n" + getString(R.string.infoAndAllOthers));

		creditsTextView.setText(creditsText);
	}

	public void faqButtonClick(View view)
	{
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		ScrollView scrollView = new ScrollView(this);
		scrollView.setLayoutParams(params);

		TextView tv = new TextView(this);
		tv.setLayoutParams(params);
		final int eightDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
		tv.setPadding(eightDp, eightDp, eightDp, eightDp);
		tv.setTextAppearance(this, android.R.style.TextAppearance_Medium);
		tv.setTextColor(Color.WHITE);

		SpannableStringBuilder faqText = new SpannableStringBuilder();
		String[] faqQuestions = getResources().getStringArray(R.array.faqQuestions), faqAnswers = getResources().getStringArray(R.array.faqAnswers);
		if(faqQuestions.length != faqAnswers.length)
			throw new RuntimeException("faqDialog : Not same number of questions/answers");
		for(int i = 0; i < faqQuestions.length; i++)
		{
			SpannableStringBuilder questionSpan = new SpannableStringBuilder(String.format("%s %s\n", getString(R.string.faqQuestionFormatting), faqQuestions[i]));
			questionSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, questionSpan.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			faqText.append(questionSpan);
			faqText.append(String.format("%s %s\n\n", getString(R.string.faqAnswerFormatting), faqAnswers[i]));
		}
		tv.setText(faqText.subSequence(0, faqText.length() - 2));
		scrollView.addView(tv);

		Dialog faqDialog = new Dialog(this, R.style.FullScreenDialogTheme);
		faqDialog.setTitle(R.string.faq);
		faqDialog.setContentView(scrollView);
		faqDialog.setCancelable(true);
		faqDialog.show();
	}

	private CharSequence makeCdvLink(String pseudo)
	{
		return JvcUtils.JvcLinkIntent.makeLink(this, pseudo, "http://www.jeuxvideo.com/profil/" + pseudo.toLowerCase() + ".html", true);
	}
}
