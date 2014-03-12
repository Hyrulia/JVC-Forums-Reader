package com.forum.jvcreader.utils;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.Serializable;
import java.util.ArrayList;

public class SerializableCookieStore implements Serializable
{
	private static final long serialVersionUID = 1L;

	private ArrayList<SerializableCookie> cookies;

	transient private CookieStore store = null;

	public SerializableCookieStore(CookieStore store)
	{
		this.store = store;
		cookies = new ArrayList<SerializableCookie>();
		for(Cookie cookie : store.getCookies())
		{
			cookies.add(new SerializableCookie(cookie));
		}
	}

	public SerializableCookieStore(CookieStore store, String domain)
	{
		cookies = new ArrayList<SerializableCookie>();
		for(Cookie cookie : store.getCookies())
		{
			cookies.add(new SerializableCookie(cookie, domain));
		}
	}

	public CookieStore getCookieStore()
	{
		if(store == null)
		{
			store = new BasicCookieStore();
			for(SerializableCookie serializedCookie : cookies)
			{
				store.addCookie(serializedCookie.getCookie());
			}
		}

		return store;
	}
}
