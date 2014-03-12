package com.forum.jvcreader.utils;

import java.util.HashMap;

public class GlobalData
{
	private static HashMap<String, Object> hashMap;

	public static void initialize()
	{
		hashMap = new HashMap<String, Object>();
	}

	public static void set(String key, Object value)
	{
		hashMap.put(key, value);
	}

	public static Object get(String key)
	{
		return hashMap.get(key);
	}

	public static Object getOnce(String key)
	{
		Object o = hashMap.get(key);
		hashMap.remove(key);
		return o;
	}

	public static void remove(String key)
	{
		hashMap.remove(key);
	}
}
