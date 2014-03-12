package com.forum.jvcreader;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.SparseArray;
import android.view.View;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.jvc.UpdatedTopicsManager;
import com.forum.jvcreader.jvc.UpdatedTopicsManager.UpdateResult;
import com.forum.jvcreader.utils.AsyncTaskManager;
import com.forum.jvcreader.utils.CachedRawDrawables;
import com.forum.jvcreader.utils.GlobalData;
import com.forum.jvcreader.utils.SerializableCookieStore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Date;

public class MainApplication extends Application
{
	public static final int JVC_SESSION = 1;
	public static final int JVC_MOBILE_SESSION = 2;
	public static final int JVFORUM_SESSION = 3;

	private static final String PREF_JVC_COOKIE_STORE = "_jvcCookieStore";
	private static final String PREF_JVC_PSEUDO = "_jvcPseudo";
	private static final String PREF_UNREAD_PM_COUNT = "_unreadPmCount";
	private static final String PREF_JVC_THEME = "_jvcTheme";

	private CookieStore jvcCookieStore;
	private CookieStore jvcMobileCookieStore;
	private CookieStore jvforumCookieStore;
	private SparseArray<HttpClient> httpClientSparseArray;
	private String userAgent = null;

	private String jvcPseudo = null;

	private final Handler animatedDrawablesHandler = new Handler();
	private boolean isAnimatingDrawables = false;
	private View animationView;

	public static final String[] jvcThemeNames = {"Black", "Light", "Holo Dark (Honeycomb)", "Holo Light (Honeycomb)"};
	public static final int[] jvcThemeResids = {R.style.BlackTheme, R.style.LightTheme, R.style.HoloDarkTheme, R.style.HoloLightTheme};

	public void onCreate()
	{
		super.onCreate();

		/* Initialize network stuff */
		httpClientSparseArray = new SparseArray<HttpClient>();

		/* Clean ongoing notifications ? */
		//NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		//manager.cancel(JvcUtils.NOTIFICATION_ID_DOWNLOADING_TOPIC);

		/* Initialize global data */
		GlobalData.initialize();

		/* Initialise asynchronous task manager */
		AsyncTaskManager.initialize();
		
		/* Initialize unique raw drawables */
		CachedRawDrawables.initialize(this);

        /* Initialise user preferences */
		try
		{
			JvcUserData.initialize(this);
			SerializableCookieStore serializedStore = (SerializableCookieStore) JvcUserData.getSerializableObject(PREF_JVC_COOKIE_STORE);

			jvcMobileCookieStore = null;
			jvforumCookieStore = null;

			if(serializedStore == null)
			{
				jvcCookieStore = null;
			}
			else
			{
				jvcCookieStore = serializedStore.getCookieStore();
				if(!jvcCookieStore.clearExpired(new Date()))
				{
					jvcMobileCookieStore = new SerializableCookieStore(jvcCookieStore, "m.jeuxvideo.com").getCookieStore();
					jvforumCookieStore = new SerializableCookieStore(jvcCookieStore, "forumjv.com").getCookieStore();
				}
				else
				{
					jvcCookieStore = null;
				}
			}

			jvcPseudo = JvcUserData.getString(PREF_JVC_PSEUDO, null);
		}
		catch(IOException e)
		{
			//JvcUserData.setupPreferences(); May have caused problems
			GlobalData.set("exitMainActivity", true);
		}
		catch(ClassNotFoundException e)
		{
			GlobalData.set("exitMainActivity", true);
		}
	}

	public HttpClient getHttpClient(int auth_type)
	{
		return getHttpClient(auth_type, true);
	}

	public HttpClient getHttpClient(int auth_type, boolean useCached)
	{
		HttpClient cachedClient = httpClientSparseArray.get(auth_type);

		if(cachedClient == null || !useCached)
		{
			HttpParams params = new BasicHttpParams();

			if(userAgent != null && userAgent.length() > 0)
				HttpProtocolParams.setUserAgent(params, userAgent);
			HttpProtocolParams.setUseExpectContinue(params, false); // SFR fix?
			HttpConnectionParams.setConnectionTimeout(params, 20000); // Connection timeout
			HttpConnectionParams.setSoTimeout(params, 25000); // Socket timeout
			ConnManagerParams.setMaxTotalConnections(params, 5); // Total connections
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1); // HTTP Version
			SchemeRegistry schemeRegistry = new SchemeRegistry(); // HTTP(S) Scheme registration
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
			ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry); // Thread-safe manager
			DefaultHttpClient client = new DefaultHttpClient(cm, params);

			switch(auth_type)
			{
				case JVC_SESSION:
					client.setCookieStore(jvcCookieStore);
					break;
				case JVC_MOBILE_SESSION:
					client.setCookieStore(jvcMobileCookieStore);
					break;
				case JVFORUM_SESSION:
					client.setCookieStore(jvforumCookieStore);
					break;
			}

			if(useCached)
				httpClientSparseArray.put(auth_type, client);
			return client;
		}

		return cachedClient;
	}

	public void invalidateHttpClients()
	{
		httpClientSparseArray.clear();
	}

	public void setUserAgent(String str)
	{
		userAgent = str;
	}

	public String getUserAgent()
	{
		return userAgent;
	}

	public static String getEntityContent(HttpEntity entity) throws IOException
	{
		return EntityUtils.toString(entity);
	}

	public static String getEntityContent(HttpEntity entity, String charset) throws IOException
	{
		return EntityUtils.toString(entity, charset);
	}

	public void setJvcCookieStore(CookieStore store) throws Exception
	{
		JvcUserData.startEditing();
		JvcUserData.setSerializableObject(PREF_JVC_COOKIE_STORE, new SerializableCookieStore(store));
		JvcUserData.stopEditing();
		jvcCookieStore = store;
		jvcMobileCookieStore = new SerializableCookieStore(store, "m.jeuxvideo.com").getCookieStore();
		jvforumCookieStore = new SerializableCookieStore(store, "forumjv.com").getCookieStore();
	}

	public boolean isJvcSessionValid()
	{
		return (jvcCookieStore != null && jvcMobileCookieStore != null && jvforumCookieStore != null);
	}

	public void invalidateJvcSession() throws Exception
	{
		jvcCookieStore = jvcMobileCookieStore = jvforumCookieStore = null;
		JvcUserData.startEditing();
		JvcUserData.remove(PREF_JVC_COOKIE_STORE);
		JvcUserData.setInt(PREF_UNREAD_PM_COUNT, 0);
		JvcUserData.stopEditing();
	}

	public String getJvcPseudo()
	{
		if(jvcPseudo != null && jvcPseudo.length() > 0)
		{
			return jvcPseudo;
		}
		else
		{
			return "(Pseudo)";
		}
	}

	public void setJvcPseudo(String pseudo)
	{
		jvcPseudo = pseudo;
		JvcUserData.startEditing();

		if(pseudo == null)
		{
			JvcUserData.remove(PREF_JVC_PSEUDO);
		}
		else
		{
			JvcUserData.setString(PREF_JVC_PSEUDO, jvcPseudo);
		}

		JvcUserData.stopEditing();
	}

	public int getUnreadPmCount()
	{
		return JvcUserData.getInt(PREF_UNREAD_PM_COUNT, 0);
	}

	public void setUnreadPmCount(int count)
	{
		JvcUserData.startEditing();
		JvcUserData.setInt(PREF_UNREAD_PM_COUNT, count);
		JvcUserData.stopEditing();
	}

	public int getJvcTheme()
	{
		return jvcThemeResids[JvcUserData.getInt(PREF_JVC_THEME, 0)];
	}

	public int getJvcThemeId()
	{
		return JvcUserData.getInt(PREF_JVC_THEME, 0);
	}

	public void setJvcThemeId(int id)
	{
		JvcUserData.startEditing();
		JvcUserData.setInt(PREF_JVC_THEME, id);
		JvcUserData.stopEditing();
	}

	public void setJvcTheme(int res)
	{
		for(int i = 0; i < jvcThemeResids.length; i++)
		{
			if(jvcThemeResids[i] == res)
			{
				setJvcThemeId(i);
				break;
			}
		}
	}

	public static String handleHttpTimeout(Context context) /* Must be called on UI thread */
	{
		String message;
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		if(info != null && info.isAvailable() && info.isConnected())
		{
			message = context.getString(R.string.timeoutWhileConnectedNotice);
		}
		else
		{
			message = context.getString(R.string.noConnectionNotice);
		}

		if(context instanceof Activity && ((Activity) context).hasWindowFocus())
		{
			NoticeDialog.show(context, message);
		}

		MainApplication app = (MainApplication) context.getApplicationContext();
		app.invalidateHttpClients();

		return message;
	}

	public boolean isConnectedToInternet()
	{
		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		return info != null && info.isAvailable() && info.isConnected();
	}

	public boolean isActiveConnectionRoaming()
	{
		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		return info != null && info.isRoaming();
	}

	public UpdateResult getUpdatedTopicsUpdateResult()
	{
		UpdatedTopicsManager manager = UpdatedTopicsManager.getInstance();
		return manager.getUpdateResultFromLastUpdate(this, JvcUserData.getUpdatedTopics());
	}

	public void requestDrawableAnimation(View view)
	{
		if(JvcUserData.getBoolean(JvcUserData.PREF_ANIMATE_SMILEYS, JvcUserData.DEFAULT_ANIMATE_SMILEYS))
		{
			animationView = view;
			if(!isAnimatingDrawables)
			{
				animatedDrawablesHandler.post(animatedDrawablesRunnable);
				isAnimatingDrawables = true;
			}
		}
	}

	public void finishDrawableAnimation()
	{
		animatedDrawablesHandler.removeCallbacks(animatedDrawablesRunnable);
		isAnimatingDrawables = false;
	}

	public void setAnimationView(View view)
	{
		animationView = view;
	}

	private final Runnable animatedDrawablesRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			CachedRawDrawables.nextFrameForAllDrawableSequences();
			if(animationView != null)
				animationView.postInvalidate();
			animatedDrawablesHandler.postDelayed(this, 60);
		}
	};
}
