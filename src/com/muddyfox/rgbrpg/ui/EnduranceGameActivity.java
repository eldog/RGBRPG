package com.muddyfox.rgbrpg.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.muddyfox.rgbrpg.R;

public class EnduranceGameActivity extends Activity
{
    private static final String TAG = EnduranceGameActivity.class.getSimpleName();
    private BattleView mBattleView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.battle_layout);
        mBattleView = (BattleView) findViewById(R.id.rgbrpgview);
    }

    public void startGame()
    {
        Log.d(TAG, "Start Game called");
        mBattleView.reset();
        mBattleView.setPicture(R.drawable.baby_otter_check,
                R.drawable.baby_otter_colour_final);
        mBattleView.setCrayonColour(Color.argb(200, 206, 255, 150));
        mBattleView.setIsInGame(true);
        Log.d(TAG, "End of starting game");
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        startGame();
    }

    public void showStartDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                "Colour in each picture as fast as you can...\nReady?")
                .setCancelable(false).setPositiveButton("Yes",
                        new DialogInterface.OnClickListener()
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
