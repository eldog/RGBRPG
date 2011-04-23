package com.muddyfox.rgbrpg.ui;

import android.app.Activity;
import android.os.Bundle;

import com.muddyfox.rgbrpg.R;

public class BattleGameActivity extends Activity
{
	BattleView mBattleView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.battle_layout);
		mBattleView = (BattleView) findViewById(R.id.rgbrpgview);
		
	}

}
