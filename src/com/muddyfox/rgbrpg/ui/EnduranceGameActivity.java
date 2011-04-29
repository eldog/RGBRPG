package com.muddyfox.rgbrpg.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.muddyfox.rgbrpg.BattleViewListener;
import com.muddyfox.rgbrpg.GameTimerController;
import com.muddyfox.rgbrpg.GameTimerListener;
import com.muddyfox.rgbrpg.R;
import com.muddyfox.rgbrpg.game.Creature;

public class EnduranceGameActivity extends Activity
{
    private static final String TAG = EnduranceGameActivity.class
            .getSimpleName();
    
    private BattleView mBattleView;
    private TextView mBattleTimerTextView;
    private TextView mBattleCurrentScoreTextView;
    private TextView mBattleTotalScoreTextView;
    private int currentScore = 0;
    private int totalScore = 0;

    private Iterator<Creature> creatureListIterator;
    
    private GameTimerController mGameTimerController;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        creatureListIterator = sCreatureList.iterator();
        mGameTimerController = new GameTimerController(5000, 10,  new GameTimerListener()
        {
            
            
            public void onTick(long millisUntilFinished)
            {
                mBattleTimerTextView.setText(GameTimerController.formatTime(millisUntilFinished));             
                mBattleCurrentScoreTextView.setText(String.valueOf(currentScore));
            }
            
            public void onFinish()
            {
                mBattleTimerTextView.setText("00:00:00");
            }

            public void onStart()
            {
            }
        });
        
        setContentView(R.layout.battle_layout);
        mBattleView = (BattleView) findViewById(R.id.rgbrpgview);
        mBattleTimerTextView = (TextView) findViewById(R.id.battle_timer_text_view);
        mBattleCurrentScoreTextView = (TextView) findViewById(R.id.battle_current_score_text_view);
        mBattleTotalScoreTextView = (TextView) findViewById(R.id.battle_total_score_text_view);
    } 

    public void startGame(Creature creature)
    {
        Log.d(TAG, "Start Game called");
        mBattleView.newGame(creature);
        mBattleView.setOnFilleCompleteListener(new BattleViewListener()
        {

            public void fillComplete(int fillIn, int fillOut)
            {
                mBattleView.setAllowedToColourIn(false);
                mBattleView.setShowColouredInBitmap(true);
                updateCurrentScore(fillIn, fillOut);
                totalScore += currentScore;
                mBattleTotalScoreTextView.setText(String.valueOf(totalScore));
                if (creatureListIterator.hasNext())
                {
                    currentScore = 0;
                    mGameTimerController.resetTimer();
                    mGameTimerController.startTimer();
                    startGame(creatureListIterator.next());
                }
                else
                {
                    // GameOver
                }
            }

            public void fillUpdate(int fillIn, int fillOut)
            {
                updateCurrentScore(fillIn, fillOut);
            }
        });
        mBattleView.setAllowedToColourIn(true);
        Log.d(TAG, "End of starting game");
    }
    
    private void updateCurrentScore(int fillIn, int fillOut)
    {
        currentScore = fillIn - fillOut;
        
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
        creatureListIterator = sCreatureList.iterator();
        startGame(creatureListIterator.next());
        mGameTimerController.startTimer();
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
                               // startGame();
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

    private static final List<Creature> sCreatureList = new ArrayList<Creature>();
    // These are the images that get coloured, in this order.. For now
    static
    {
        // Baby otter
        sCreatureList.add(new Creature(R.drawable.baby_otter_colour_final,
                R.drawable.baby_otter_check, Color.argb(99, 255, 156, 68)));
        // Daddy otter
        sCreatureList.add(new Creature(R.drawable.otter_colour_final,
                R.drawable.otter_check, Color.argb(99, 68, 156, 255)));
        // Baby wolf
        sCreatureList.add(new Creature(R.drawable.baby_wolf_colour_final,
                R.drawable.baby_wolf_check, Color.argb(99, 68, 156, 255)));
        // Mummy wolf
        sCreatureList.add(new Creature(R.drawable.wolf_colour_final,
                R.drawable.wolf_check, Color.argb(99, 206, 255, 150)));
        // Baby owl
        sCreatureList.add(new Creature(R.drawable.owl_baby_colour_final,
                R.drawable.owl_baby_check, Color.argb(99, 206, 224, 150)));
        // Grampa Owl
        sCreatureList.add(new Creature(R.drawable.owl_final,
                R.drawable.owl_check, Color.argb(99, 206, 224, 150)));
    }
}
