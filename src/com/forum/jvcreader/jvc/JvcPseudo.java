package com.forum.jvcreader.jvc;

public class JvcPseudo
{
	private String pseudo;
	private boolean isAdmin;
	private boolean exists;

	public JvcPseudo(String pseudo, boolean isAdmin, boolean exists)
	{
		this.pseudo = pseudo;
		this.isAdmin = isAdmin;
		this.exists = exists;
	}

	public String getPseudo()
	{
		return pseudo;
	}

	public boolean isAdmin()
	{
		return isAdmin;
	}

	public boolean exists()
	{
		return exists;
	}
}