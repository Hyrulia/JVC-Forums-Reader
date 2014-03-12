package com.forum.jvcreader.graphics.jvc.cdv;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcUtils;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.StringHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CdvPortletRang extends CdvPortlet
{
	private static final Pattern patternExtractRankInfo = Pattern.compile("<td id=\"td_rang\"><strong>(.+?)</strong></td>\n<td id=\"td_niveau\"><p><strong>(.+?)</strong></p><p id=\"nbniv\"><img width=\"([0-9]+)\".*/></p></td>\n<td id=\"td_pts\"><strong>(.+?)</strong> <span>points?</span></td>\n</tr></tbody></table>\n(?:<p id=\"pniv\">Encore <strong>(.+?) points?</strong> avant le niveau <strong>\"(.+?)\"</strong></p>)?(?:<p id=\"prang\">Encore <strong>(.+?) points?</strong> avant le rang <strong>\"(.+?)\"</strong>)?");

	private String currentRank;
	private String currentLevel;
	private int currentLevelWidth;
	private String currentPoints;
	private String neededPointsLevel;
	private String futureLevel;
	private String neededPointsRank;
	private String futureRank;

	@Override
	protected boolean parseContent()
	{
		Matcher m = patternExtractRankInfo.matcher(StringHelper.unescapeHTML(content));
		if(m.find())
		{
			currentRank = m.group(1);
			currentLevel = m.group(2);
			currentLevelWidth = Integer.parseInt(m.group(3));
			currentPoints = m.group(4);
			neededPointsLevel = m.group(5);
			futureLevel = m.group(6);
			neededPointsRank = m.group(7);
			futureRank = m.group(8);

			return true;
		}

		return false;
	}

	@Override
	protected View getContentView()
	{
		View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cdv_portlet_rang, null);

		Drawable drawable = CachedRawDrawables.getDrawable(JvcUtils.getRawIdFromName(currentRank.toLowerCase()));
		((ImageView) view.findViewById(R.id.cdvRangCurrentRankImageView)).setImageDrawable(drawable);
		((TextView) view.findViewById(R.id.cdvRangCurrentRankTextView)).setText(currentRank);
		((TextView) view.findViewById(R.id.cdvRangCurrentLevelTextView)).setText(currentLevel);
		((ProgressBar) view.findViewById(R.id.cdvRangCurrentLevelProgressBar)).setProgress(currentLevelWidth);
		((TextView) view.findViewById(R.id.cdvRangCurrentPointsTextView)).setText(currentPoints);

		if(neededPointsLevel != null && futureLevel != null)
		{
			view.findViewById(R.id.cdvRangNextLevelView).setVisibility(View.VISIBLE);
			((ImageView) view.findViewById(R.id.cdvRangNextLevelImageView)).setImageDrawable(drawable);
			((TextView) view.findViewById(R.id.cdvRangNeededPointsLevelTextView)).setText(neededPointsLevel);
			((TextView) view.findViewById(R.id.cdvRangFutureLevelTextView)).setText(futureLevel);
		}

		if(neededPointsRank != null && futureRank != null)
		{
			view.findViewById(R.id.cdvRangNextRankView).setVisibility(View.VISIBLE);
			drawable = CachedRawDrawables.getDrawable(JvcUtils.getRawIdFromName(futureRank.toLowerCase()));
			((ImageView) view.findViewById(R.id.cdvRangNextRankImageView)).setImageDrawable(drawable);
			((TextView) view.findViewById(R.id.cdvRangNeededPointsRankTextView)).setText(neededPointsRank);
			((TextView) view.findViewById(R.id.cdvRangFutureRankTextView)).setText(futureRank);
		}

		return view;
	}

}
