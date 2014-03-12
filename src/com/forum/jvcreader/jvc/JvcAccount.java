package com.forum.jvcreader.jvc;

import com.forum.jvcreader.utils.Base64Coder;

import java.io.Serializable;

public class JvcAccount implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String pseudo;
	private String encodedPassword;

	public JvcAccount(String pseudo, String password)
	{
		this.pseudo = pseudo;
		encodedPassword = Base64Coder.encodeString(password);
	}

	public String getPseudo()
	{
		return pseudo;
	}

	public String getPassword()
	{
		return Base64Coder.decodeString(encodedPassword);
	}
}
