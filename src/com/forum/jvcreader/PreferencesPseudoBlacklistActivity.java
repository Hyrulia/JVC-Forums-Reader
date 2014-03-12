package com.forum.jvcreader;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.forum.jvcreader.graphics.NoticeDialog;
import com.forum.jvcreader.graphics.PseudoPromptDialog;
import com.forum.jvcreader.graphics.PseudoPromptDialog.OnPseudoChosenListener;
import com.forum.jvcreader.jvc.JvcPseudo;
import com.forum.jvcreader.jvc.JvcUserData;

import java.io.IOException;
import java.util.HashMap;

public class PreferencesPseudoBlacklistActivity extends JvcActivity
{
	private HashMap<String, Boolean> blacklist;
	private String chosenPseudo;
	private String[] stringList;

	private PseudoPromptDialog promptDialog;
	private ListView listView;
	private Button removeAllButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pseudo_blacklist);

		listView = (ListView) findViewById(R.id.pseudoBlacklistListView);
		listView.setOnItemClickListener(itemListener);
		removeAllButton = (Button) findViewById(R.id.pseudoBlacklistRemoveAllButton);

		updateBlacklist();
	}

	private final OnItemClickListener itemListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			chosenPseudo = stringList[(int) id];
			String message = String.format(getString(R.string.blacklistDialogConfirmPseudo), chosenPseudo);
			NoticeDialog.showYesNo(PreferencesPseudoBlacklistActivity.this, message, new DialogInterface.OnClickListener() /* YES */
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							try
							{
								JvcUserData.removePseudoFromBlacklist(chosenPseudo);
								updateBlacklist();
							}
							catch(IOException e)
							{
								NoticeDialog.show(PreferencesPseudoBlacklistActivity.this, e.toString());
							}
							dialog.dismiss();
						}
					}, new DialogInterface.OnClickListener() /* NO */
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
						}
					}
			);
		}
	};

	private void updateBlacklist()
	{
		blacklist = JvcUserData.getPseudoBlacklist();
		if(blacklist == null || blacklist.size() == 0)
		{
			listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1));
			removeAllButton.setEnabled(false);
		}
		else
		{
			removeAllButton.setEnabled(true);
			stringList = new String[blacklist.size()];
			int i = 0;
			for(String s : blacklist.keySet())
			{
				stringList[i] = s;
				i++;
			}
			listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, stringList));
		}
	}

	public void pseudoBlacklistRemoveAllButtonClick(View view)
	{
		NoticeDialog.showYesNo(PreferencesPseudoBlacklistActivity.this, getString(R.string.blacklistDialogConfirmAll), new DialogInterface.OnClickListener() /* YES */
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						try
						{
							JvcUserData.clearPseudoBlacklist();
							updateBlacklist();
						}
						catch(IOException e)
						{
							NoticeDialog.show(PreferencesPseudoBlacklistActivity.this, e.toString());
						}

						dialog.dismiss();
					}
				}, new DialogInterface.OnClickListener() /* NO */
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				}
		);
	}

	public void pseudoBlacklistAddPseudoButtonClick(View view)
	{
		promptDialog = new PseudoPromptDialog(this, new OnPseudoChosenListener()
		{
			@Override
			public void onPseudoChosen(JvcPseudo pseudo)
			{
				try
				{
					JvcUserData.setPseudoInBlacklist(pseudo.getPseudo());
					updateBlacklist();
				}
				catch(IOException e)
				{
					NoticeDialog.show(PreferencesPseudoBlacklistActivity.this, e.toString());
				}

				promptDialog.dismiss();
				promptDialog = null;
			}
		}, true);

		promptDialog.show();
	}
}
