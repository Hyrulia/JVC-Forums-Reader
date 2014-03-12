package com.forum.jvcreader.graphics;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.forum.jvcreader.MainApplication;
import com.forum.jvcreader.R;
import com.forum.jvcreader.jvc.JvcPseudo;
import com.forum.jvcreader.jvc.JvcUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class PseudoPromptDialog
{
	private Context context;

	private Dialog dialog;
	private EditText editText;
	private ProgressBar progressBar;
	private ListView listView;

	private boolean unofficialPseudos;
	private OnPseudoChosenListener pseudoListener;
	private LoadPseudosTask currentTask = null;

	public PseudoPromptDialog(Context context, OnPseudoChosenListener onPseudoChosenListener, boolean allowUnofficialPseudos)
	{
		this.context = context;
		pseudoListener = onPseudoChosenListener;
		unofficialPseudos = allowUnofficialPseudos;

		dialog = new Dialog(context, R.style.FullScreenDialogTheme);
		dialog.setTitle(R.string.pseudoPromptDialogTitle);
		dialog.setContentView(R.layout.pseudo_prompt_dialog);
		dialog.setCancelable(true);

		editText = (EditText) dialog.findViewById(R.id.pseudoPromptDialogEditText);
		editText.addTextChangedListener(textWatcher);
		progressBar = (ProgressBar) dialog.findViewById(R.id.pseudoPromptDialogProgressBar);
		listView = (ListView) dialog.findViewById(R.id.pseudoPromptDialogListView);
		listView.setOnItemClickListener(itemListener);
	}

	public void show()
	{
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(dialog.getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		dialog.show();
		dialog.getWindow().setAttributes(lp);
	}

	public void dismiss()
	{
		dialog.dismiss();
	}

	public void setCancelable(boolean b)
	{
		dialog.setCancelable(b);
	}

	private final TextWatcher textWatcher = new TextWatcher()
	{
		@Override
		public void afterTextChanged(Editable s)
		{
			if(s.length() > 0)
			{
				if(currentTask != null)
				{
					currentTask.cancel(true);
				}

				currentTask = new LoadPseudosTask(s.toString());
				currentTask.execute();
			}
			else
			{
				listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1));
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}

		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}
	};

	private final OnItemClickListener itemListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			JvcPseudo pseudo = (JvcPseudo) parent.getItemAtPosition(position);

			if(!JvcUtils.isPseudoCorrect(pseudo.getPseudo()))
			{
				Toast.makeText(context, R.string.incorrectPseudoFormatting, Toast.LENGTH_LONG).show();
			}
			else if(!unofficialPseudos && !pseudo.exists())
			{
				Toast.makeText(context, R.string.unofficialPseudosNotAllowed, Toast.LENGTH_LONG).show();
			}
			else
			{
				pseudoListener.onPseudoChosen(pseudo);
			}
		}
	};

	private class LoadPseudosTask extends AsyncTask<Void, Void, Boolean>
	{
		private String pseudo;

		private String requestError;
		private final ArrayList<JvcPseudo> requestPseudoList = new ArrayList<JvcPseudo>();

		public LoadPseudosTask(String pseudo)
		{
			super();
			this.pseudo = pseudo;
		}

		protected Boolean doInBackground(Void... voids)
		{
			try
			{
				MainApplication app = (MainApplication) context.getApplicationContext();
				HttpClient client = app.getHttpClient(MainApplication.JVC_SESSION);
				HttpGet httpGet = new HttpGet("http://www.jeuxvideo.com/messages-prives/ajax_pseudo_mp.php?to_search=" + JvcUtils.encodeUrlParam(pseudo));
				String content = MainApplication.getEntityContent(client.execute(httpGet).getEntity());

				if(content.equals("[]"))
				{
					requestPseudoList.add(new JvcPseudo(pseudo, false, false));
					return true;
				}

				JSONObject json = new JSONObject(content);
				JSONArray jsonPseudo = json.getJSONArray("pseudo");
				JSONArray jsonAdmin = json.getJSONArray("admin");

				final int length = jsonPseudo.length();
				for(int i = 0; i < length; i++)
				{
					requestPseudoList.add(new JvcPseudo(jsonPseudo.getString(i), jsonAdmin.getInt(i) > 0, true));
				}

				return true;
			}
			catch(UnknownHostException e)
			{
				requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(HttpHostConnectException e)
			{
				requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(ConnectTimeoutException e)
			{
				requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(SocketTimeoutException e)
			{
				requestError = JvcUtils.HTTP_TIMEOUT_RESULT;
			}
			catch(IOException e)
			{
				requestError = e.toString();
			}
			catch(JSONException e)
			{
				requestError = e.toString();
			}

			return false;
		}

		protected void onPreExecute()
		{
			progressBar.setVisibility(View.VISIBLE);
		}

		protected void onPostExecute(Boolean result)
		{
			progressBar.setVisibility(View.INVISIBLE);

			if(result)
			{
				listView.setAdapter(new JvcPseudoAdapter(context, requestPseudoList));
			}
			else
			{
				if(requestError.equals(JvcUtils.HTTP_TIMEOUT_RESULT))
				{
					MainApplication.handleHttpTimeout(context);
				}
				else
				{
					NoticeDialog.show(context, context.getString(R.string.errorWhileLoading) + " : " + requestError);
				}
			}

			currentTask = null;
		}
	}

	private class JvcPseudoAdapter extends ArrayAdapter<JvcPseudo>
	{
		private LayoutInflater inflater;
		private int adminColor;

		public JvcPseudoAdapter(Context context, List<JvcPseudo> pseudos)
		{
			super(context, android.R.layout.simple_list_item_1, pseudos);

			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			adminColor = context.getResources().getColor(R.color.jvcAdminPseudo);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			final JvcPseudo pseudo = getItem(position);
			TextView view;

			if(convertView == null)
			{
				view = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			}
			else
			{
				view = (TextView) convertView;
			}

			view.setText(pseudo.getPseudo());
			if(!pseudo.exists())
				view.append(" (?)");

			if(pseudo.isAdmin())
				view.setTextColor(adminColor);
			else
				view.setTextColor(Color.WHITE);

			return view;
		}
	}

	public interface OnPseudoChosenListener
	{
		public abstract void onPseudoChosen(JvcPseudo pseudo);
	}
}
