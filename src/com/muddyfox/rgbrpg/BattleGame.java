package com.muddyfox.rgbrpg;

import com.muddyfox.rgbrpg.R;
import com.muddyfox.rgbrpg.RGBRPGview;

import android.app.Activity;
import android.os.Bundle;

public class BattleGame extends Activity
{
	RGBRPGview mBattleView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.battle_layout);
		mBattleView = (RGBRPGview) findViewById(R.id.rgbrpgview);
		
	}

}
