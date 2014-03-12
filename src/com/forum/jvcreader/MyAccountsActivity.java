package com.forum.jvcreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.forum.jvcreader.graphics.AddAccountDialog;
import com.forum.jvcreader.jvc.JvcAccount;
import com.forum.jvcreader.jvc.JvcUserData;
import com.forum.jvcreader.utils.GlobalData;

import java.util.ArrayList;

public class MyAccountsActivity extends JvcActivity
{
	private ListView accountListView;
	private String[] stringList;
	private ArrayList<JvcAccount> accounts = null;

	private JvcAccount currentContextMenuAccount = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_accounts);

		accountListView = (ListView) findViewById(R.id.myAccountsListView);
		accountListView.setOnItemClickListener(itemListener);
		registerForContextMenu(accountListView);

		loadAccountList();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) /* Disable home button here too ! */
	{
		return true;
	}

	private void loadAccountList()
	{
		accounts = JvcUserData.getAccounts();
		if(accounts == null)
		{
			accountListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1));
		}
		else
		{
			stringList = new String[accounts.size()];
			int i = 0;
			for(JvcAccount acc : accounts)
			{
				stringList[i] = acc.getPseudo();
				i++;
			}
			accountListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, stringList));
		}
	}

	private final OnItemClickListener itemListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			int accId = (int) id;
			if(accounts != null && accId >= 0 && accId < accounts.size())
			{
				loginAccount(accounts.get(accId));
			}
		}
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		if(v.getId() == R.id.myAccountsListView)
		{
			getMenuInflater().inflate(R.menu.account_context_menu, menu);
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle(stringList[info.position]);
			currentContextMenuAccount = accounts.get(info.position);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		if(currentContextMenuAccount != null)
		{
			switch(item.getItemId())
			{
				case R.id.optionsMenuConnectToAccount:
					loginAccount(currentContextMenuAccount);
					break;

				case R.id.optionsMenuDeleteAccount:
					try
					{
						JvcUserData.removeFromAccounts(currentContextMenuAccount);
						loadAccountList();
					}
					catch(Exception e)
					{
						Toast.makeText(this, R.string.errorWhileLoadingUserPreferences, Toast.LENGTH_SHORT).show();
					}
					break;
			}

			return true;
		}

		return super.onContextItemSelected(item);
	}

	public void myAccountsAddAccountButtonClick(View view)
	{
		AddAccountDialog dialog = new AddAccountDialog(this);
		dialog.setOnAccountSubmitListener(new AddAccountDialog.OnAccountSubmitListener()
		{
			@Override
			public void onAccountSubmit(JvcAccount account)
			{
				if(JvcUserData.isAccountInAccounts(account))
				{
					Toast.makeText(MyAccountsActivity.this, R.string.accountAlreadyExisting, Toast.LENGTH_SHORT).show();
				}
				else
				{
					try
					{
						JvcUserData.addToAccounts(account);
						loadAccountList();
					}
					catch(Exception e)
					{
						Toast.makeText(MyAccountsActivity.this, R.string.errorWhileLoadingUserPreferences, Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		dialog.show();
	}

	private void loginAccount(JvcAccount account)
	{
		String key = "chosen_account_" + System.currentTimeMillis();

		Intent intent = new Intent();
		intent.putExtra("com.forum.jvcreader.ChosenAccountKey", key);
		GlobalData.set(key, account);
		setResult(RESULT_OK, intent);
		finish();
	}
}

