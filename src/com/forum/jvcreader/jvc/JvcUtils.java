package com.forum.jvcreader.jvc;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.UnderlineSpan;
import com.forum.jvcreader.CdvActivity;
import com.forum.jvcreader.ForumActivity;
import com.forum.jvcreader.R;
import com.forum.jvcreader.TopicActivity;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.PatternCollection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;

public class JvcUtils
{
	public static final String HTTP_TIMEOUT_RESULT = "_timeout";

	private static HashMap<String, Integer> iconIds = new HashMap<String, Integer>();
	private static HashMap<String, Integer> animatedIconIds = new HashMap<String, Integer>();

	public static final int NOTIFICATION_ID_CHECK_PRIVATE_MESSAGES = 1;
	public static final int NOTIFICATION_ID_CHECK_UPDATED_TOPICS = 2;
	public static final int NOTIFICATION_ID_DOWNLOADING_TOPIC = 3;
	public static final int NOTIFICATION_ID_DOWNLOADED_TOPIC = 4;

	public static final String INTENT_ACTION_UPDATE_PM = "com.forum.jvcreader.action.UPDATE_PM";
	public static final String INTENT_ACTION_UPDATE_UPDATED_TOPICS = "com.forum.jvcreader.action.UPDATE_UPDATED_TOPICS";

	public static final long[] PM_NOTIFICATION_VIBRATOR_PATTERN = {0, 1000, 100, 250, 100, 250};
	public static final long[] UPDATED_TOPICS_SINGLE_NOTIFICATION_VIBRATOR_PATTERN = {0, 250, 100, 1000, 100, 250};
	public static final long[] UPDATED_TOPICS_MULTI_NOTIFICATION_VIBRATOR_PATTERN = {0, 250, 100, 250, 100, 1000};

	public static final String QUOTE_DELIMITER = "-------------------------------------------------";
	public static final int POSTS_PER_PAGE = 20;

	public static int getRawIdFromName(String name)
	{
		Integer integer = iconIds.get(name);
		if(integer == null)
			return -1;
		return integer;
	}

	public static int getSequenceIdFromName(String name)
	{
		Integer integer = animatedIconIds.get(name);
		if(integer == null)
			return -1;
		return integer;
	}

	public static boolean isPseudoCorrect(String pseudo)
	{
		return PatternCollection.matchLoginFormatting.matcher(pseudo).matches();
	}

	public static boolean isPasswordCorrect(String pwd)
	{
		return PatternCollection.matchPasswordFormatting.matcher(pwd).matches();
	}

	static
	{
		/* Cdv rank icons */
		iconIds.put("carton", R.raw.rang_carton);
		iconIds.put("bronze", R.raw.rang_bronze);
		iconIds.put("argent", R.raw.rang_argent);
		iconIds.put("or", R.raw.rang_or);
		iconIds.put("rubis", R.raw.rang_rubis);
		iconIds.put("saphir", R.raw.rang_saphir);
		iconIds.put("emeraude", R.raw.rang_emeraude);
		iconIds.put("diamant", R.raw.rang_diamant);
		
		/* Cdv icons */
		iconIds.put("ico_myspace.png", R.raw.ico_myspace);
		iconIds.put("ico_twitter.png", R.raw.ico_twitter);
		iconIds.put("ico_windows.png", R.raw.ico_windows);
		iconIds.put("ico_lastfm.png", R.raw.ico_lastfm);
		iconIds.put("ico_youtube.png", R.raw.ico_youtube);
		
		/* Misc. icons */
		iconIds.put("topic_cadenas.gif", R.raw.topic_cadenas);
		iconIds.put("topic_dossier1.gif", R.raw.topic_dossier1);
		iconIds.put("topic_dossier2.gif", R.raw.topic_dossier2);
		iconIds.put("topic_marque_off.gif", R.raw.topic_marque_off);
		iconIds.put("topic_marque_on.gif", R.raw.topic_marque_on);
		iconIds.put("for_fleche.png", R.raw.for_fleche);
		iconIds.put("page_debut.gif", R.raw.page_debut);
		iconIds.put("page_prec.gif", R.raw.page_prec);
		iconIds.put("page_suiv.gif", R.raw.page_suiv);
		
		/* Devices */
		iconIds.put("mc/3ds.gif", R.raw.r_3ds);
		iconIds.put("mc/32.gif", R.raw.r_32);
		iconIds.put("mc/amiga.gif", R.raw.amiga);
		iconIds.put("mc/android.gif", R.raw.android);
		iconIds.put("mc/apple2.gif", R.raw.apple2);
		iconIds.put("mc/c6.gif", R.raw.c6);
		iconIds.put("mc/cd.gif", R.raw.cd);
		iconIds.put("mc/cp.gif", R.raw.cp);
		iconIds.put("mc/do.gif", R.raw.r_do);
		iconIds.put("mc/dreamcast.gif", R.raw.dreamcast);
		iconIds.put("mc/gamecube.gif", R.raw.gamecube);
		iconIds.put("mc/gba.gif", R.raw.gba);
		iconIds.put("mc/gboy.gif", R.raw.gboy);
		iconIds.put("mc/gg.gif", R.raw.gg);
		iconIds.put("mc/gizmondo.gif", R.raw.gizmondo);
		iconIds.put("mc/gp.gif", R.raw.gp);
		iconIds.put("mc/ipad.gif", R.raw.ipad);
		iconIds.put("mc/iphone.gif", R.raw.iphone);
		iconIds.put("mc/ja.gif", R.raw.ja);
		iconIds.put("mc/ly.gif", R.raw.ly);
		iconIds.put("mc/mac.gif", R.raw.mac);
		iconIds.put("mc/master.gif", R.raw.master);
		iconIds.put("mc/megadrive.gif", R.raw.megadrive);
		iconIds.put("mc/n64.gif", R.raw.n64);
		iconIds.put("mc/nds.gif", R.raw.nds);
		iconIds.put("mc/neogeo.gif", R.raw.neogeo);
		iconIds.put("mc/neogeopocket.gif", R.raw.neogeopocket);
		iconIds.put("mc/nes.gif", R.raw.nes);
		iconIds.put("mc/ngage.gif", R.raw.ngage);
		iconIds.put("mc/p3.gif", R.raw.p3);
		iconIds.put("mc/pc.gif", R.raw.pc);
		iconIds.put("mc/ps2.gif", R.raw.ps2);
		iconIds.put("mc/psp.gif", R.raw.psp);
		iconIds.put("mc/psx.gif", R.raw.psx);
		iconIds.put("mc/saturn.gif", R.raw.saturn);
		iconIds.put("mc/snes.gif", R.raw.snes);
		iconIds.put("mc/st.gif", R.raw.st);
		iconIds.put("mc/tu.gif", R.raw.tu);
		iconIds.put("mc/vbo.gif", R.raw.vbo);
		iconIds.put("mc/vita.gif", R.raw.vita);
		iconIds.put("mc/wb.gif", R.raw.wb);
		iconIds.put("mc/wii.gif", R.raw.wii);
		iconIds.put("mc/wiiu.gif", R.raw.wiiu);
		iconIds.put("mc/x3.gif", R.raw.x3);
		iconIds.put("mc/xbox.gif", R.raw.xbox);
		iconIds.put("mc/xboxone.gif", R.raw.xboxone);
		iconIds.put("mc/ps4.gif", R.raw.ps4);
		
		/* Smileys */
		iconIds.put("fish.png", R.raw.fish);
		iconIds.put("1.gif", R.raw.s1);
		iconIds.put(":)", R.raw.s1);
		iconIds.put("2.gif", R.raw.s2);
		iconIds.put(":question:", R.raw.s2);
		iconIds.put("3.gif", R.raw.s3);
		iconIds.put(":g)", R.raw.s3);
		iconIds.put("4.gif", R.raw.s4);
		iconIds.put(":d)", R.raw.s4);
		iconIds.put("5.gif", R.raw.s5);
		iconIds.put(":cd:", R.raw.s5);
		iconIds.put("6.gif", R.raw.s6);
		iconIds.put(":globe:", R.raw.s6);
		iconIds.put("7.gif", R.raw.s7);
		iconIds.put(":p)", R.raw.s7);
		iconIds.put("8.gif", R.raw.s8);
		iconIds.put(":malade:", R.raw.s8);
		iconIds.put("9.gif", R.raw.s9);
		iconIds.put(":pacg:", R.raw.s9);
		iconIds.put("10.gif", R.raw.s10);
		iconIds.put(":pacd:", R.raw.s10);
		iconIds.put("11.gif", R.raw.s11);
		iconIds.put(":noel:", R.raw.s11);
		iconIds.put("12.gif", R.raw.s12);
		iconIds.put(":o))", R.raw.s12);
		iconIds.put("13.gif", R.raw.s13);
		iconIds.put(":snif2:", R.raw.s13);
		iconIds.put("14.gif", R.raw.s14);
		iconIds.put(":-(", R.raw.s14);
		iconIds.put("15.gif", R.raw.s15);
		iconIds.put(":-((", R.raw.s15);
		iconIds.put("16.gif", R.raw.s16);
		iconIds.put(":mac:", R.raw.s16);
		iconIds.put("17.gif", R.raw.s17);
		iconIds.put(":gba:", R.raw.s17);
		iconIds.put("18.gif", R.raw.s18);
		iconIds.put(":hap:", R.raw.s18);
		iconIds.put("19.gif", R.raw.s19);
		iconIds.put(":nah:", R.raw.s19);
		iconIds.put("20.gif", R.raw.s20);
		iconIds.put(":snif:", R.raw.s20);
		iconIds.put("21.gif", R.raw.s21);
		iconIds.put(":mort:", R.raw.s21);
		iconIds.put("22.gif", R.raw.s22_23);
		iconIds.put(":ouch:", R.raw.s22_23);
		iconIds.put("23.gif", R.raw.s23_1);
		iconIds.put(":-)))", R.raw.s23_1);
		iconIds.put("24.gif", R.raw.s24_4);
		iconIds.put(":content:", R.raw.s24_4);
		iconIds.put("25.gif", R.raw.s25_1);
		iconIds.put(":nonnon:", R.raw.s25_1);
		iconIds.put("26.gif", R.raw.s26_1);
		iconIds.put(":cool:", R.raw.s26_1);
		iconIds.put("27.gif", R.raw.s27_6);
		iconIds.put(":sleep:", R.raw.s27_6);
		iconIds.put("28.gif", R.raw.s28_2);
		iconIds.put(":doute:", R.raw.s28_2);
		iconIds.put("29.gif", R.raw.s29);
		iconIds.put(":hello:", R.raw.s29);
		iconIds.put("30.gif", R.raw.s30_1);
		iconIds.put(":honte:", R.raw.s30_1);
		iconIds.put("31.gif", R.raw.s31_5);
		iconIds.put(":-p", R.raw.s31_5);
		iconIds.put("32.gif", R.raw.s32_1);
		iconIds.put(":lol:", R.raw.s32_1);
		iconIds.put("33.gif", R.raw.s33_1);
		iconIds.put(":non2:", R.raw.s33_1);
		iconIds.put("34.gif", R.raw.s34_4);
		iconIds.put(":monoeil:", R.raw.s34_4);
		iconIds.put("35.gif", R.raw.s35_1);
		iconIds.put(":non:", R.raw.s35_1);
		iconIds.put("36.gif", R.raw.s36);
		iconIds.put(":ok:", R.raw.s36);
		iconIds.put("37.gif", R.raw.s37_2);
		iconIds.put(":oui:", R.raw.s37_2);
		iconIds.put("38.gif", R.raw.s38);
		iconIds.put(":rechercher:", R.raw.s38);
		iconIds.put("39.gif", R.raw.s39_1);
		iconIds.put(":rire:", R.raw.s39_1);
		iconIds.put("40.gif", R.raw.s40);
		iconIds.put(":-D", R.raw.s40);
		iconIds.put("41.gif", R.raw.s41_1);
		iconIds.put(":rire2:", R.raw.s41_1);
		iconIds.put("42.gif", R.raw.s42);
		iconIds.put(":salut:", R.raw.s42);
		iconIds.put("43.gif", R.raw.s43_1);
		iconIds.put(":sarcastic:", R.raw.s43_1);
		iconIds.put("44.gif", R.raw.s44);
		iconIds.put(":up:", R.raw.s44);
		iconIds.put("45.gif", R.raw.s45);
		iconIds.put(":(", R.raw.s45);
		iconIds.put("46.gif", R.raw.s46);
		iconIds.put(":-)", R.raw.s46);
		iconIds.put("47.gif", R.raw.s47);
		iconIds.put(":peur:", R.raw.s47);
		iconIds.put("48.gif", R.raw.s48);
		iconIds.put(":bye:", R.raw.s48);
		iconIds.put("49.gif", R.raw.s49);
		iconIds.put(":dpdr:", R.raw.s49);
		iconIds.put("50.gif", R.raw.s50);
		iconIds.put(":fou:", R.raw.s50);
		iconIds.put("51.gif", R.raw.s51);
		iconIds.put(":gne:", R.raw.s51);
		iconIds.put("52.gif", R.raw.s52);
		iconIds.put(":dehors:", R.raw.s52);
		iconIds.put("53.gif", R.raw.s53);
		iconIds.put(":fier:", R.raw.s53);
		iconIds.put("54.gif", R.raw.s54);
		iconIds.put(":coeur:", R.raw.s54);
		iconIds.put("55.gif", R.raw.s55);
		iconIds.put(":rouge:", R.raw.s55);
		iconIds.put("56.gif", R.raw.s56);
		iconIds.put(":sors:", R.raw.s56);
		iconIds.put("57.gif", R.raw.s57);
		iconIds.put(":ouch2:", R.raw.s57);
		iconIds.put("58.gif", R.raw.s58);
		iconIds.put(":merci:", R.raw.s58);
		iconIds.put("59.gif", R.raw.s59);
		iconIds.put(":svp:", R.raw.s59);
		iconIds.put("60.gif", R.raw.s60);
		iconIds.put(":ange:", R.raw.s60);
		iconIds.put("61.gif", R.raw.s61);
		iconIds.put(":diable:", R.raw.s61);
		iconIds.put("62.gif", R.raw.s62);
		iconIds.put(":gni:", R.raw.s62);
		iconIds.put("63.gif", R.raw.s63);
		iconIds.put(":spoiler:", R.raw.s63);
		iconIds.put("64.gif", R.raw.s64);
		iconIds.put(":hs:", R.raw.s64);
		iconIds.put("65.gif", R.raw.s65);
		iconIds.put(":desole:", R.raw.s65);
		iconIds.put("66.gif", R.raw.s66_3);
		iconIds.put(":fete:", R.raw.s66_3);
		iconIds.put("67.gif", R.raw.s67);
		iconIds.put(":sournois:", R.raw.s67);
		iconIds.put("68.gif", R.raw.s68);
		iconIds.put(":hum:", R.raw.s68);
		iconIds.put("69.gif", R.raw.s69_1);
		iconIds.put(":bravo:", R.raw.s69_1);
		iconIds.put("70.gif", R.raw.s70_1);
		iconIds.put(":banzai:", R.raw.s70_1);
		iconIds.put("71.gif", R.raw.s71_4);
		iconIds.put(":bave:", R.raw.s71_4);
		iconIds.put("nyu.gif", R.raw.nyu_1);
		iconIds.put(":cute:", R.raw.nyu_1);
		iconIds.put("loveyou.gif", R.raw.loveyou_1);
		iconIds.put(":loveyou:", R.raw.loveyou_1);
		iconIds.put("hapoelparty.gif", R.raw.hapoelparty_1);
		iconIds.put(":hapoelparty:", R.raw.hapoelparty_1);
		iconIds.put("play.gif", R.raw.play_1);
		iconIds.put(":play:", R.raw.play_1);
		iconIds.put("pf.gif", R.raw.pf);
		iconIds.put(":pf:", R.raw.pf);
		
		/* Animated smileys */
		animatedIconIds.put("22.gif", R.array.s22Sequence);
		animatedIconIds.put(":ouch:", R.array.s22Sequence);
		animatedIconIds.put("23.gif", R.array.s23Sequence);
		animatedIconIds.put(":-)))", R.array.s23Sequence);
		animatedIconIds.put("24.gif", R.array.s24Sequence);
		animatedIconIds.put(":content:", R.array.s24Sequence);
		animatedIconIds.put("25.gif", R.array.s25Sequence);
		animatedIconIds.put(":nonnon:", R.array.s25Sequence);
		animatedIconIds.put("26.gif", R.array.s26Sequence);
		animatedIconIds.put(":cool:", R.array.s26Sequence);
		animatedIconIds.put("27.gif", R.array.s27Sequence);
		animatedIconIds.put(":sleep:", R.array.s27Sequence);
		animatedIconIds.put("28.gif", R.array.s28Sequence);
		animatedIconIds.put(":doute:", R.array.s28Sequence);
		animatedIconIds.put("30.gif", R.array.s30Sequence);
		animatedIconIds.put(":honte:", R.array.s30Sequence);
		animatedIconIds.put("31.gif", R.array.s31Sequence);
		animatedIconIds.put(":-p", R.array.s31Sequence);
		animatedIconIds.put("32.gif", R.array.s32Sequence);
		animatedIconIds.put(":lol:", R.array.s32Sequence);
		animatedIconIds.put("33.gif", R.array.s33Sequence);
		animatedIconIds.put(":non2:", R.array.s33Sequence);
		animatedIconIds.put("34.gif", R.array.s34Sequence);
		animatedIconIds.put(":monoeil:", R.array.s34Sequence);
		animatedIconIds.put("35.gif", R.array.s35Sequence);
		animatedIconIds.put(":non:", R.array.s35Sequence);
		animatedIconIds.put("37.gif", R.array.s37Sequence);
		animatedIconIds.put(":oui:", R.array.s37Sequence);
		animatedIconIds.put("39.gif", R.array.s39Sequence);
		animatedIconIds.put(":rire:", R.array.s39Sequence);
		animatedIconIds.put("41.gif", R.array.s41Sequence);
		animatedIconIds.put(":rire2:", R.array.s41Sequence);
		animatedIconIds.put("43.gif", R.array.s43Sequence);
		animatedIconIds.put(":sarcastic:", R.array.s43Sequence);
		animatedIconIds.put("66.gif", R.array.s66Sequence);
		animatedIconIds.put(":fete:", R.array.s66Sequence);
		animatedIconIds.put("69.gif", R.array.s69Sequence);
		animatedIconIds.put(":bravo:", R.array.s69Sequence);
		animatedIconIds.put("70.gif", R.array.s70Sequence);
		animatedIconIds.put(":banzai:", R.array.s70Sequence);
		animatedIconIds.put("71.gif", R.array.s71Sequence);
		animatedIconIds.put(":bave:", R.array.s71Sequence);
		animatedIconIds.put("nyu.gif", R.array.nyuSequence);
		animatedIconIds.put(":cute:", R.array.nyuSequence);
		animatedIconIds.put("loveyou.gif", R.array.loveyouSequence);
		animatedIconIds.put(":loveyou:", R.array.loveyouSequence);
		animatedIconIds.put("hapoelparty.gif", R.array.hapoelSequence);
		animatedIconIds.put(":hapoelparty:", R.array.hapoelSequence);
		animatedIconIds.put("play.gif", R.array.playSequence);
		animatedIconIds.put(":play:", R.array.playSequence);
	}

	public static class JvcLinkIntent
	{
		public static final int GENERIC_URL = -1;
		public static final int FORUM_URL = 0;
		public static final int TOPIC_URL = 1;
		public static final int CDV_URL = 2;

		private Context context;
		private URL url;

		private int urlType;

		private int forumId;
		private boolean isForumJV;
		private String forumJVSubdomain;
		private long topicId;
		private int pageNumber;
		private long postId;

		private int forumPageNumber;
		private int forumSearchType;
		private String forumSearchValue;

		private String pseudo = null;

		public static SpannableStringBuilder makeLink(Context context, String name, String url, boolean enabled)
		{
			SpannableStringBuilder builder = new SpannableStringBuilder(name);

			UnderlineSpan span = new UnderlineSpan();
			builder.setSpan(span, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			try
			{
				builder.setSpan(new JvcTextSpanner.JvcClickableSpan(context, new URL(url), enabled), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			catch(MalformedURLException e)
			{
				builder.removeSpan(span);
			}

			return builder;
		}

		@SuppressWarnings("deprecation")
		public JvcLinkIntent(Context context, URL url)
		{
			this.context = context;
			this.url = url;

			urlType = JvcLinkIntent.GENERIC_URL;

			String host = url.getHost();
			if(host.contains(".jeuxvideo.com") || host.contains(".forumjv.com"))
			{
				String path = url.getPath();

				Matcher m = PatternCollection.extractCdvUrl.matcher(path);
				if(m.find())
				{
					urlType = JvcLinkIntent.CDV_URL;
					pseudo = m.group(1);
				}
				else
				{
					if(url.getRef() != null)
						path += url.getRef();
					m = PatternCollection.extractForumUrl.matcher(path);
					if(m.find())
					{
						urlType = Integer.parseInt(m.group(1));
						urlType = urlType == 3 ? TOPIC_URL : urlType;
						forumId = Integer.parseInt(m.group(2));
						topicId = Long.parseLong(m.group(3));
						pageNumber = Integer.parseInt(m.group(4));
						forumPageNumber = (int) Math.floor((Long.parseLong(m.group(5)) - 1) / 20) + 1;
						forumSearchType = Integer.parseInt(m.group(6));
						forumSearchValue = URLDecoder.decode(m.group(7));
						if(m.group(8) != null)
							postId = Long.parseLong(m.group(8));
						else
							postId = -1;
					}

					if(host.contains(".forumjv.com"))
					{
						isForumJV = true;
						final String domain = url.getAuthority();
						forumJVSubdomain = domain.substring(0, domain.indexOf('.'));
					}
					else
					{
						isForumJV = false;
					}
				}
			}
		}

		public void startIntent()
		{
			JvcForum forum;
			if(isForumJV)
				forum = new JvcForum(context, forumId, forumJVSubdomain);
			else
				forum = new JvcForum(context, forumId);

			switch(urlType)
			{
				case JvcLinkIntent.GENERIC_URL:
					context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())));
					break;

				case JvcLinkIntent.FORUM_URL:
					GlobalData.set("forumFromPreviousActivity", forum);
					Intent intent = new Intent(context, ForumActivity.class);
					intent.putExtra("com.forum.jvcreader.PageNumber", forumPageNumber);
					intent.putExtra("com.forum.jvcreader.SearchType", forumSearchType);
					intent.putExtra("com.forum.jvcreader.SearchValue", forumSearchValue);
					context.startActivity(intent);
					break;

				case JvcLinkIntent.TOPIC_URL:
					Intent topicIntent = new Intent(context, TopicActivity.class);
					JvcTopic topic;

					if(postId != -1)
					{
						topic = new JvcTopic(forum, topicId, pageNumber, postId);
					}
					else
					{
						topic = new JvcTopic(forum, topicId);
						topicIntent.putExtra("com.forum.jvcreader.PageNumber", pageNumber);
					}

					GlobalData.set("topicFromPreviousActivity", topic);
					context.startActivity(topicIntent);
					break;

				case JvcLinkIntent.CDV_URL:
					GlobalData.set("cdvPseudoFromLastActivity", pseudo);
					context.startActivity(new Intent(context, CdvActivity.class));
					break;
			}
		}

		public int getUrlType()
		{
			return urlType;
		}

		public int getForumId()
		{
			return forumId;
		}

		public boolean isForumJV()
		{
			return isForumJV;
		}

		public String getPseudo()
		{
			return pseudo;
		}
	}

	public static String applyFontCompatibility(String s) /* To complete ? */
	{
		String replaced = s.replace('\u0092', '\u2019'); /* Apostrophe */
		replaced = replaced.replace('\u009C', '\u0153'); /* &oelig; */
		replaced = replaced.replace('\u0080', '\u20AC'); /* Euro sign */
		replaced = replaced.replace('\u0095', '\u2022'); /* Bullet sign */
		replaced = replaced.replace('\u0085', '\u2026'); /* Horizontal ellipsis */
		replaced = replaced.replaceAll("[\u001C\u001D\u001E\u001F]", ""); /* Admin 'code' */

		return replaced;
	}

	public static String applyInverseFontCompatibility(String s) /* To complete ? */
	{
		String replaced = s.replace('\u2019', '\u0092'); /* Apostrophe */
		replaced = replaced.replace('\u0153', '\u009C'); /* &oelig; */
		replaced = replaced.replace('\u20AC', '\u0080'); /* Euro sign */
		replaced = replaced.replace('\u2022', '\u0095'); /* Bullet sign */
		replaced = replaced.replace('\u2026', '\u0085'); /* Horizontal ellipsis */

		return replaced;
	}

	public static String encodeUrlParam(String s)
	{
		try
		{
			return URLEncoder.encode(s.replace("/", "%2F"), "UTF-8").replace("+", "%20");
		}
		catch(UnsupportedEncodingException e)
		{
			return s;
		}
	}
}
