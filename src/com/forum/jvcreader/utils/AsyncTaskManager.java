package com.forum.jvcreader.utils;

import android.app.Activity;
import android.os.AsyncTask;

import java.util.HashMap;

public class AsyncTaskManager
{
	private static HashMap<Activity, HashMap<Class<? extends AsyncTask>, AsyncTask>> activityHashMap;

	public static void initialize()
	{
		activityHashMap = new HashMap<Activity, HashMap<Class<? extends AsyncTask>, AsyncTask>>();
	}

	public static void registerActivity(Activity activity)
	{
		if(!activityHashMap.containsKey(activity))
		{
			activityHashMap.put(activity, new HashMap<Class<? extends AsyncTask>, AsyncTask>());
		}
	}

	public static void unregisterActivity(Activity activity)
	{
		if(activityHashMap.containsKey(activity))
		{
			activityHashMap.remove(activity);
		}
	}

	public static void addTask(Activity activity, AsyncTask task)
	{
		if(!activityHashMap.containsKey(activity))
			throw new RuntimeException("activity not registered");
		HashMap<Class<? extends AsyncTask>, AsyncTask> taskHashMap = activityHashMap.get(activity);

		if(taskHashMap.containsKey(task.getClass()))
		{
			AsyncTask oldTask = taskHashMap.get(task.getClass());
			if(oldTask != null && oldTask.getStatus() != AsyncTask.Status.FINISHED && !oldTask.isCancelled())
			{
				oldTask.cancel(true);
			}
		}

		taskHashMap.put(task.getClass(), task);
	}

	public static void killAllTasks(Activity activity)
	{
		if(activityHashMap.containsKey(activity))
		{
			HashMap<Class<? extends AsyncTask>, AsyncTask> taskHashMap = activityHashMap.get(activity);
			if(taskHashMap.size() > 0)
			{
				for(AsyncTask task : taskHashMap.values())
				{
					if(task != null && task.getStatus() != AsyncTask.Status.FINISHED && !task.isCancelled())
					{
						task.cancel(true);
					}
				}

				taskHashMap.clear();
			}
		}
		else
		{
			//throw new RuntimeException("activity not registered");
		}
	}

	public static AsyncTask getCurrentTask(Activity activity, Class<? extends AsyncTask> taskClass)
	{
		HashMap<Class<? extends AsyncTask>, AsyncTask> taskHashMap = activityHashMap.get(activity);
		if(taskHashMap == null)
			throw new RuntimeException("activity not registered");

		return taskHashMap.get(taskClass);
	}

	public static boolean isActivityRegistered(Activity activity)
	{
		return activityHashMap.containsKey(activity);
	}
}
