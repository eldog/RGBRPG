package com.muddyfox.rgbrpg.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.muddyfox.rgbrpg.AccelerometerListener;
import com.muddyfox.rgbrpg.DialogManager;
import com.muddyfox.rgbrpg.GameTimerController;
import com.muddyfox.rgbrpg.R;

/** Creates a new Time Challenge Game
 **/
public class TimeChallengeActivity extends Activity
{
    private static final int HIGH_SCORE_RESET_ID = Menu.FIRST;
    private static final int NEW_GAME_ID = HIGH_SCORE_RESET_ID + 2;
    private static final int QUIT_ID = HIGH_SCORE_RESET_ID + 3;
    private static final int SHAKE_ID = HIGH_SCORE_RESET_ID + 4;
    private static final int SOUND_ID = HIGH_SCORE_RESET_ID + 5;

    private static final String PREFERENCE_ENABLE_SHAKE = "enableShake";
    private static final String PREFERENCE_ENABLE_MUSIC = "enableMusic";
    private static final String PREFERENCE_ENABLE_VIBRATE = "enableVibrate";
    private static final String PREFERENCE_HIGH_SCORE = "highScore";

    private static final String PREFERENCE_NAME = "RGBRPGPrefs";
    ProgressDialog mPostDialog;

    private AccelerometerListener al;
    public DialogManager dialogManager;
    private static boolean shakeEnabled;
    private static boolean vibrateEnabled;
    public MediaPlayer mp;
    private static boolean musicEnabled;
    private boolean isRunning;
    private static SharedPreferences.Editor mPrefsEditor;
    ProgressDialog mUploadDialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFERENCE_NAME,
                MODE_PRIVATE);

        timer = new GameTimerController();

        highScore = 0;
        musicEnabled = true;
        shakeEnabled = true;
        mPrefsEditor = prefs.edit();
        highScore = prefs.getInt(PREFERENCE_HIGH_SCORE, 0);
        musicEnabled = prefs.getBoolean(PREFERENCE_ENABLE_MUSIC, true);
        shakeEnabled = prefs.getBoolean(PREFERENCE_ENABLE_SHAKE, true);
        vibrateEnabled = prefs.getBoolean(PREFERENCE_ENABLE_VIBRATE, true);

        al = new AccelerometerListener(this);
        isRunning = true;

        newGame();
        mp = MediaPlayer.create(this, R.raw.gbbattle);
        mp.setLooping(true);
        if (musicEnabled)
        {
            mp.start();
        }
        dialogManager = new DialogManager(this, this);
        dialogManager.startDialog();
    }

    @Override
    protected void onStop()
    {
        // TODO Auto-generated method stub
        super.onStop();
        this.isRunning = false;
        this.finish();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        this.isRunning = false;
        this.timer.pauseTimer();

        if (mp.isPlaying())
        {
            mp.pause();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        this.isRunning = true;
        this.timer.startTimer();
        if (musicEnabled && !mp.isPlaying())
        {
            mp.start();
        }
    }

    private TextView timeDisplay;
    private TextView currentScoreDisplay;
    private TextView highScoreDisplay;
    private TextView fillPercentageDisplay;

    private GameTimerController timer;
    static int highScore;
    static int currentScore;

    public void newGame()
    {
        setContentView(R.layout.time_challenge_layout);

        currentScore = 0;
        myView = (BattleView) findViewById(R.id.timeChallengeView);

        timeDisplay = (TextView) findViewById(R.id.timeChallengeTimer);
        currentScoreDisplay = (TextView) findViewById(R.id.timeChallengeScore);
        highScoreDisplay = (TextView) findViewById(R.id.timeChallengeHighScore);
        fillPercentageDisplay = (TextView) findViewById(R.id.timeChallengeFillPercentage);

        Typeface mTypeface = Typeface.createFromAsset(getAssets(),
                "fonts/Garfield.ttf");
        timeDisplay.setTypeface(mTypeface);
        currentScoreDisplay.setTypeface(mTypeface);
        highScoreDisplay.setTypeface(mTypeface);
        fillPercentageDisplay.setTypeface(mTypeface);

        timeDisplay
                .setText("Press the Menu Key and New Game to Get Colouring!");
        currentScoreDisplay.setText("SCORE: " + currentScore + " ");
        highScoreDisplay.setText("HIGHSCORE: " + highScore);

    }

    public void startGame()
    {
        myView.setIsInGame(true);

        timer.initTimer(timeDisplay, this, 5000, 50);
        timer.startTimer();
        isInGame = true;
    }

    public void updatePercentage()
    {
        // NULL OP 
        //this.fillPercentageDisplay.setText(String.format("%.1f", myView.fillPercent) + "% fill");

    }

    private void calculateGameScore(int insideCount, int totalInCount,
            int outsideCount, int totalOutCount)
    {
        double fillPercent = (insideCount / (double) totalInCount) * 100.0;

        double score = ((insideCount - outsideCount) / (double) totalInCount) * 1000;

        Log.d("INSIDE", "Inside is " + insideCount + " out of " + totalInCount);
        Log.d("OUTSIDE", "You got " + outsideCount + " out of " + totalOutCount
                + " outside the lines!");
        Log.d("FILL", "Colouring in complete = " + Double.toString(fillPercent));
        Log.d("SCORE", "Score = " + Double.toString(score));

        currentScore += Integer.parseInt(String.format("%.0f", score));
        currentScoreDisplay.setText("SCORE: " + currentScore + " ");

        checkGameOver(score);

    }

    private void checkGameOver(double score)
    {
        // NULL OP
//        // If not we're still colouring
//        if (!isInGame)
//        {
//            // If we are at the end of colouring tasks
//            if (myView.isAtEndOfSequence())
//            {
//                boolean isHighscore;
//                if (currentScore > highScore)
//                {
//                    isHighscore = true;
//                    highScore = currentScore;
//                    // announce it
//                    highScoreDisplay.setText("HIGHSCORE: " + highScore);
//                    saveGame();
//
//                    Log.d("NOW", "Here I am at the BEGINNING");
//                    dialogManager.highScoreNameEntryDialog();
//
//                } else
//                {
//                    isHighscore = false;
//
//                    dialogManager.replayDialog(currentScore, isHighscore);
//                }
//            } else
//            {
//                isInGame = true;
//                // keep playing!
//                myView.reset();
//                startGame();
//            }
//            Log.d("HERE", "Here I am at the END");
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, NEW_GAME_ID, 0, "New Game");
        menu.add(0, SOUND_ID, 0, "Sound");
        menu.add(0, QUIT_ID, 0, "Quit");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case SHAKE_ID:
            shakeEnabled = !shakeEnabled;
            Toast shakeToast = Toast.makeText(this,
                    (shakeEnabled ? "Shake enabled" : "Shake disabled"),
                    Toast.LENGTH_SHORT);
            shakeToast.show();
            saveGame();
            return true;

        case SOUND_ID:
            musicEnabled = !musicEnabled;
            if (musicEnabled && !mp.isPlaying())
            {
                mp.start();
            } else
            {
                mp.pause();
            }
            Toast soundToast = Toast.makeText(this,
                    (musicEnabled ? "Sound enabled" : "Sound disabled"),
                    Toast.LENGTH_SHORT);

            soundToast.show();
            saveGame();
            return true;

        case HIGH_SCORE_RESET_ID:
            highScore = 0;
            saveGame();
            return true;

        case NEW_GAME_ID:
            newGame();
            startGame();
            return true;

        case QUIT_ID:
            dialogManager.finishDialog();
            return true;
        }
        return false;
    }
    
    public void stopAudio()
    {
        
    }

    protected static void saveGame()
    {
        if (mPrefsEditor != null)
        {
            mPrefsEditor.putBoolean(PREFERENCE_ENABLE_SHAKE, shakeEnabled);
            mPrefsEditor.putBoolean(PREFERENCE_ENABLE_MUSIC, musicEnabled);
            mPrefsEditor.putBoolean(PREFERENCE_ENABLE_VIBRATE, vibrateEnabled);
            mPrefsEditor.putInt(PREFERENCE_HIGH_SCORE, highScore);

            Log.d("SAVE SHAKE", (shakeEnabled ? "Shake Enabled"
                    : "Shake Disabled"));
            Log.d("SAVE GAME", (mPrefsEditor.commit() ? "Succesful game save"
                    : "Failed to save"));
        }
    }

    public void promptShake()
    {
//        if (isRunning)
//        {
//            myView.mShowCompletedColourImage = true;
//            myView.invalidate();
//            Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//            vibe.vibrate(100);
//            if (shakeEnabled)
//            {
//                al.start();
//                Toast shakeItToast = Toast.makeText(this, "SHAKE IT!",
//                        Toast.LENGTH_SHORT);
//                shakeItToast.show();
//            } else
//            {
//                checkScore();
//            }
//        }
    }

    public void checkScore()
    {
        int[] scoredata = myView.getCanvasDetails();
        calculateGameScore(scoredata[0], scoredata[1], scoredata[2],
                scoredata[3]);
    }

    private BattleView myView;

    public static boolean isInGame = false;

    @Override
    public void finish()
    {
        
        isRunning = false;
        super.finish();

    }

    public void uploadCompleted(boolean b)
    {
        // TODO Auto-generated method stub
        if (mUploadDialog.isShowing())
        {
            mUploadDialog.dismiss();
            dialogManager.replayDialog(currentScore, true);

        }

    }

    private String userName;

    public void postHighscore(String userName)
    {
        this.userName = userName;

        Log.i("USERNAME", "User name is" + userName);
        // start upload
        new HighScorePoster().execute();
    }

    public class HighScorePoster extends AsyncTask<URL, Integer, Void>
    {
        private HttpClient mClient;
        private HttpPost mPost;
        int highScoreToPost;
        private HttpResponse response;

        private final ProgressDialog dialog = new ProgressDialog(
                TimeChallengeActivity.this);

        protected void onPreExecute()
        {

            this.dialog.setMessage("Uploading new highscore...");

            this.dialog.show();

        }

        @Override
        protected Void doInBackground(URL... params)
        {
            // TODO Auto-generated method stub
            mClient = new DefaultHttpClient();
            mPost = new HttpPost("http://rgbrpg.appspot.com/sign");
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("content", userName));
            pairs.add(new BasicNameValuePair("score", "" + highScore));
            try
            {
                mPost.setEntity(new UrlEncodedFormEntity(pairs));
            } catch (UnsupportedEncodingException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();

            }
            try
            {
                response = mClient.execute(mPost);
               
            } catch (ClientProtocolException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();

            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();

            }

            return null;
        }

        protected void onPostExecute(final Void unused)
        {

            if (this.dialog.isShowing())
            {

                this.dialog.dismiss();
                

            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            {
            	Toast error = Toast.makeText(TimeChallengeActivity.this, "Error Whilst Posting: " + response.getStatusLine().getReasonPhrase(), Toast.LENGTH_LONG);
            	error.show();
            }
            dialogManager.replayDialog(currentScore, true);

        }
    }
}