package com.forum.jvcreader.utils;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.Serializable;
import java.util.Date;

public class SerializableCookie implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String name;
	private String domain;
	private Date expiryDate;
	private String path;
	private boolean secure;
	private String value;
	private int version;

	transient private Cookie cookie = null;

	public SerializableCookie(Cookie cookie)
	{
		this.cookie = cookie;
		name = cookie.getName();
		domain = cookie.getDomain();
		expiryDate = cookie.getExpiryDate();
		path = cookie.getPath();
		secure = cookie.isSecure();
		value = cookie.getValue();
		version = cookie.getVersion();
	}

	public SerializableCookie(Cookie cookie, String domain)
	{
		name = cookie.getName();
		this.domain = domain;
		expiryDate = cookie.getExpiryDate();
		path = cookie.getPath();
		secure = cookie.isSecure();
		value = cookie.getValue();
		version = cookie.getVersion();
	}

	public Cookie getCookie()
	{
		if(cookie == null)
		{
			BasicClientCookie basicCookie = new BasicClientCookie(name, value);
			basicCookie.setDomain(domain);
			basicCookie.setExpiryDate(expiryDate);
			basicCookie.setPath(path);
			basicCookie.setSecure(secure);
			basicCookie.setVersion(version);
			cookie = basicCookie;
		}

		return cookie;
	}
}
