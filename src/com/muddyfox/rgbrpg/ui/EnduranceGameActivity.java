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
        mBattleView.setAllowedToColourIn(true);
        Log.d(TAG, "End of starting game");
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
        startGame();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
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
    
 // These are the images that get coloured, in this order.. For now
    // TODO TURN THIS INTO A MAP
    private static final int[] sBitmapList =
        { Color.argb(99, 255, 156, 68), R.drawable.baby_otter_check,
                R.drawable.baby_otter_colour_final,
                Color.argb(99, 255, 156, 68), R.drawable.otter_check,
                R.drawable.otter_colour_final, Color.argb(99, 68, 156, 255),
                R.drawable.baby_wolf_check, R.drawable.baby_wolf_colour_final,
                Color.argb(99, 68, 156, 255), R.drawable.wolf_check,
                R.drawable.wolf_colour_final, Color.argb(99, 206, 255, 150),
                R.drawable.owl_baby_check, R.drawable.owl_baby_colour_final,
                Color.argb(99, 206, 224, 150), R.drawable.owl_check,
                R.drawable.owl_final };
}
