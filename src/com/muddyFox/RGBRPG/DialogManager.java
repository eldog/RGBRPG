package com.muddyFox.RGBRPG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public class DialogManager
{
    private TimeChallenge mGame;
    private Context c;

    public DialogManager(TimeChallenge game, Context c)
    {
        this.mGame = game;
        this.c = c;
    }

    public void startDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage(
                "Colour in each picture as fast as you can...\nReady?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mGame.startGame();
                    }
                }).setNegativeButton("No",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void replayDialog(int score, boolean highscore)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        String highscoreMessage;
        if (highscore)
        {
            highscoreMessage = " NEW HIGHSCORE!";
        }
        else
        {
            highscoreMessage = " Keep trying!";
        }
        builder.setMessage(
                "You scored " + score + highscoreMessage + "\nPlay Again?")
                .setCancelable(false).setPositiveButton("Yes",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                mGame.newGame();
                                mGame.startGame();
                            }
                        }).setNegativeButton("No",
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                dialog.cancel();
                                finishDialog();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void finishDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage("Are you sure you want to exit?").setCancelable(
                false).setPositiveButton("Yes",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        mGame.stopAudio();
                        mGame.finish();
                    }
                }).setNegativeButton("No",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void highScoreNameEntryDialog()
    {

        // final View textEntryView =
        // factory.inflate(R.layout.high_score_text_entry, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        final EditText userName = new EditText(c);
        builder.setView(userName).setMessage("Enter Your Name")
                .setPositiveButton("Done",
                        new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        mGame.postHighscore(userName.getText().toString());

                        /* User clicked OK so do some stuff */
                    }
                }).setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {

                        /* User clicked cancel so do some stuff */
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}
