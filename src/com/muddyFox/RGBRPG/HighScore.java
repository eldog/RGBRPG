package com.muddyFox.RGBRPG;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class HighScore extends Activity
{
	TableLayout highScoreTable;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.high_score_layout);
		highScoreTable = (TableLayout) findViewById(R.id.highScoreTable);
		new HighScoreDownload().execute();
	}

	public void addRow(TableRow tr, TextView tv)
	{

	}

	public class HighScoreDownload extends AsyncTask<URL, Integer, Void>
	{
		private InputStream is;
		private NodeList names;
		private NodeList scores;
		private NodeList ranks;

		private final ProgressDialog dialog = new ProgressDialog(HighScore.this);

		protected void onPreExecute()
		{

			this.dialog.setMessage("Downloading Leaderboard...");

			this.dialog.show();

		}

		@Override
		protected Void doInBackground(URL... params)
		{

			URL connectURL;
			try
			{
				connectURL = new URL("http://rgbrpg.appspot.com/highscore");

				try
				{
					URLConnection connection = connectURL.openConnection();
					is = connection.getInputStream();
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (MalformedURLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute(final Void unused)
		{
			if (is != null)
			{
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
						.newInstance();
				try
				{
					DocumentBuilder docBuillder = docBuilderFactory
							.newDocumentBuilder();
					Document doc = docBuillder.parse(is);
					ranks = doc.getElementsByTagName("rank");
					names = doc.getElementsByTagName("name");
					scores = doc.getElementsByTagName("score");
				} catch (ParserConfigurationException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Typeface tf = Typeface.createFromAsset(getAssets(),
						"fonts/Garfield.ttf");

				for (int i = 0; i < names.getLength(); i++)
				{

					TableRow tr = new TableRow(HighScore.this);
					highScoreTable.addView(tr);

					TextView rankText = new TextView(HighScore.this);
					rankText.setText(ranks.item(i).getTextContent());
					rankText.setTypeface(tf);
					rankText.setTextColor(Color.BLACK);
					tr.addView(rankText);

					TextView nameText = new TextView(HighScore.this);
					nameText.setText(names.item(i).getTextContent());
					nameText.setTypeface(tf);
					nameText.setTextColor(Color.BLACK);

					tr.addView(nameText);

					TextView scoreText = new TextView(HighScore.this);
					scoreText.setText(scores.item(i).getTextContent());
					scoreText.setTypeface(tf);
					scoreText.setTextColor(Color.BLACK);

					tr.addView(scoreText);

					Log.i("TABLE ROW", "Table row added");

				}

				dialog.dismiss();
			}

		}
	}

}
