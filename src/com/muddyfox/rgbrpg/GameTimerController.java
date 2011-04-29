package com.muddyfox.rgbrpg;

import android.os.CountDownTimer;

public class GameTimerController
{
    private GameTimerListener mGameTimerListener;

    private long length;
    private long interval;

    private long startTime = 0;
    private long currentTime = 0;
    private long timeElapsed = 0;
    private long timeRemaining = 0;
    private long prevTimeRemaining = 0;
    private volatile boolean mIsRunning;

    private GameTimer mGameTimer;

    public GameTimerController(long millisInFuture, long countDownInterval,
            GameTimerListener gameTimerListener)
    {
        mGameTimerListener = gameTimerListener;
        length = millisInFuture;
        interval = countDownInterval;
        mGameTimer = new GameTimer(length, interval);
        mIsRunning = false;
    }

    public void startTimer()
    {
        if (!mIsRunning)
        {
            startTime = System.currentTimeMillis();
            mGameTimer.start();
            mIsRunning = true;
        }
        else
        {
            throw new IllegalStateException("Tried to start an already running timer");
        }
    }

    public void pauseTimer()
    {
        if (mIsRunning)
        {
            currentTime = System.currentTimeMillis();
            timeElapsed = currentTime - startTime;
            
            if (prevTimeRemaining == 0)
            {
                timeRemaining = length - timeElapsed;
            }
            else
            {
                timeRemaining = prevTimeRemaining - timeElapsed;
            }
            
            mGameTimer.cancel();

            prevTimeRemaining = timeRemaining;
            mGameTimer = new GameTimer(timeRemaining, interval);
            mIsRunning = false;
        }
        // Possible race condition...
//        else
//        {
//            throw new IllegalStateException("Tried to pause timer that was not running");
//        }
    }
    
    public void resetTimer()
    {
        if (mIsRunning)
        {
            // Possible race condition if it has finished?
            mGameTimer.cancel();
        }
        mGameTimer = new GameTimer(length, interval);
        mIsRunning = false;
        
    }

    private class GameTimer extends CountDownTimer
    {
        public GameTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
            mGameTimerListener.onStart();
        }

        @Override
        public void onTick(long millisUntilFinished)
        {
            mGameTimerListener.onTick(millisUntilFinished);
        }

        @Override
        public void onFinish()
        {
            mGameTimerListener.onFinish();
            mIsRunning = false;
        }
    }

    public static String formatTime(long millis)
    {
        long seconds = millis / 1000;
        long minutes = seconds / 60;

        millis = millis % 100;
        seconds = seconds % 60;
        minutes = minutes % 60;

        // Nine... to be on the safe side
        StringBuffer outputBuffer = new StringBuffer(9);


        if (minutes < 10)
        {
            outputBuffer.append('0');
        }
        outputBuffer.append(minutes);
        outputBuffer.append(':');
        if (seconds < 10)
        {
            outputBuffer.append('0');
        }
        outputBuffer.append(seconds);
        outputBuffer.append(':');
        if (millis < 10)
        {
            outputBuffer.append('0');
        }
        outputBuffer.append(millis);

        return outputBuffer.toString();
    }
}
