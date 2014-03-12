package com.forum.jvcreader.graphics.jvc.cdv;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.forum.jvcreader.R;
import com.forum.jvcreader.utils.StringHelper;

import java.util.HashMap;

public abstract class CdvPortlet
{

	private static HashMap<String, Class<? extends CdvPortlet>> cdvClassList;

	static
	{
		cdvClassList = new HashMap<String, Class<? extends CdvPortlet>>();
		
		/* CDV */
		cdvClassList.put("pt_avatar", CdvPortletAvatar.class);
		cdvClassList.put("pt_describ", CdvPortletDescrib.class);
		cdvClassList.put("pt_nbpost", CdvPortletNbpost.class);
		cdvClassList.put("pt_infos", CdvPortletInfos.class);
		cdvClassList.put("pt_machines", CdvPortletMachines.class);
		cdvClassList.put("pt_rang", CdvPortletRang.class);
		cdvClassList.put("pt_typejeux", CdvPortletTypejeux.class);
		cdvClassList.put("pt_msgforums", CdvPortletMsgforums.class);
		cdvClassList.put("pt_web", CdvPortletWeb.class);
		cdvClassList.put("pt_jeuonline", CdvPortletJeuonline.class);
		
		/* 2nd page */
		cdvClassList.put("pt_forumspref", CdvPortletGenericPrefs.class);
		cdvClassList.put("pt_jeuxpref", CdvPortletGenericPrefs.class);
		cdvClassList.put("pt_jeuxavoir", CdvPortletGenericPrefs.class);
		cdvClassList.put("pt_articlespref", CdvPortletArticlespref.class);
		
		/* 3rd page */
		cdvClassList.put("pt_contribs_rec", CdvPortletContribsrec.class);
		cdvClassList.put("pt_contribs_autres", CdvPortletContribsautres.class);
	}

	protected Cdv cdv;
	protected Context context;
	protected boolean hidden;
	protected String name;
	protected String content;
	protected int textPrimaryColor;

	private String id;
	private boolean parsedCorrectly = false;

	public static CdvPortlet getCdvPortlet(Cdv cdv, String id, boolean hidden, String name, String content) throws InstantiationException, IllegalAccessException
	{
		Class<? extends CdvPortlet> portletClass = cdvClassList.get(id);
		if(portletClass == null)
			portletClass = CdvPortletUnknown.class;

		CdvPortlet portlet = portletClass.newInstance();
		portlet.setData(cdv, id, hidden, name, content);

		return portlet;
	}

	private void setData(Cdv cdv, String id, boolean hidden, String name, String content)
	{
		this.cdv = cdv;
		this.context = cdv.getContext();
		this.hidden = hidden;
		this.name = name;
		this.content = content;

		TypedValue textColorValue = new TypedValue();
		context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, textColorValue, true);
		textPrimaryColor = context.getResources().getColor(textColorValue.resourceId);

		this.id = id;
		if(!hidden)
			parsedCorrectly = parseContent();
	}

	public String getId()
	{
		return id;
	}

	public boolean isHidden()
	{
		return hidden;
	}

	public View getView()
	{
		LinearLayout layout = new LinearLayout(context);
		final int tenDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
		layout.setPadding(tenDp, tenDp, tenDp, tenDp);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setBackgroundResource(R.drawable.cdv_box);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.topMargin = tenDp;
		layout.setLayoutParams(params);
		
		/* Header view */
		layout.addView(getHeaderView());
		
		/* Separator */
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.cdv_portlet_divider, null);
		params = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		params.topMargin = params.bottomMargin = tenDp;
		view.setLayoutParams(params);
		layout.addView(view);
			
		/* Content view */
		if(parsedCorrectly)
		{
			layout.addView(getContentView());
		}
		else
		{
			TextView tv = new TextView(context);
			tv.setGravity(Gravity.CENTER);
			tv.setText(R.string.errorWhileParsing);
			tv.setTextColor(Color.RED);
			tv.setTextSize(18);
			layout.addView(tv);
		}

		return layout;
	}

	private View getHeaderView()
	{
		TextView headerTextView = new TextView(context);

		headerTextView.setText(getHeaderTitle());
		headerTextView.setTextSize(18);
		headerTextView.setTextColor(context.getResources().getColor(R.color.jvcAdminPseudo));

		return headerTextView;
	}

	protected CharSequence getHeaderTitle()
	{
		return StringHelper.unescapeHTML(name);
	}

	protected abstract boolean parseContent();

	protected abstract View getContentView();
}
