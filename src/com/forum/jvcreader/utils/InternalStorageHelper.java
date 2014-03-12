package com.forum.jvcreader.utils;

import android.content.Context;

import java.io.*;

public class InternalStorageHelper
{
	public static void writeSerializableObject(Context context, String fileName, Object object) throws IOException
	{
		FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
		ObjectOutputStream output = new ObjectOutputStream(fos);
		output.writeObject(object);
		fos.close();
	}

	public static Object readSerializableObject(Context context, String fileName) throws IOException, ClassNotFoundException
	{
		FileInputStream fis = context.openFileInput(fileName);
		ObjectInputStream input = new ObjectInputStream(fis);
		Object object = input.readObject();
		fis.close();

		return object;
	}

	public static boolean deleteFile(Context context, String fileName)
	{
		File file = new File(context.getFilesDir(), fileName);
		return file.delete();
	}
}
