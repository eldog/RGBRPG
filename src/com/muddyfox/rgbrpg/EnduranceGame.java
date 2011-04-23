package com.muddyfox.rgbrpg;


import com.muddyfox.rgbrpg.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

public class EnduranceGame extends Activity
{
	RGBRPGview mEnduranceView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.battle_layout);
		mEnduranceView = (RGBRPGview) findViewById(R.id.rgbrpgview);
		startGame();
	}
	
	
	public void startGame()
	{
		mEnduranceView.setup();
		mEnduranceView.setPicture(R.drawable.baby_otter_check, R.drawable.baby_otter_colour_final);
		mEnduranceView.setCrayonColour(Color.argb(99, 206, 255, 150));
		mEnduranceView.setIsInGame(true);
	}
	
	public void showStartDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Colour in each picture as fast as you can...\nReady?").setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						startGame();
					}
				}).setNegativeButton("No",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								finish();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
