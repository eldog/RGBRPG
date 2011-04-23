package com.muddyfox.rgbrpg;

import android.os.CountDownTimer;
import android.widget.TextView;

import com.muddyfox.rgbrpg.ui.TimeChallengeActivity;

public class GameTimerController
{
	private static final int STATE_NOT_RUNNING = 0;
	private static final int STATE_IS_RUNNING = 1;
	private static final int STATE_FINISHED = 2;
	private TextView timerView;
	private TimeChallengeActivity game;

	private GameTimer gameTimer;

	private long length = 0;
	private long interval = 0;

	private long startTime = 0;
	private long currentTime = 0;
	private long timeElapsed = 0;
	private long timeRemaining = 0;
	private long prevTimeRemaining = 0;
	private int mState;

	public GameTimerController()
	{
	}

	public void initTimer(TextView view, TimeChallengeActivity game, long millisInFuture, long countDownInterval)
	{
		this.game = game;
		timerView = view;
		length = millisInFuture;
		interval = countDownInterval;
		gameTimer = new GameTimer(millisInFuture, countDownInterval);
		mState = STATE_NOT_RUNNING;
	}

	public void startTimer()
	{
		if (gameTimer != null)
		{
			startTime = System.currentTimeMillis();
			gameTimer.start();
			mState = STATE_IS_RUNNING;
		}
	}

	public void pauseTimer()
	{
		if (gameTimer != null)
		{
			currentTime = System.currentTimeMillis();
			timeElapsed = currentTime - startTime;
			if (prevTimeRemaining == 0)
			{
				timeRemaining = length - timeElapsed;
			} else
			{
				timeRemaining = prevTimeRemaining - timeElapsed;
			}
			gameTimer.cancel();

			prevTimeRemaining = timeRemaining;
			gameTimer = new GameTimer(timeRemaining, interval);
			mState = STATE_NOT_RUNNING;
		}
	}

	public void stopTimer()
	{

	}

	public class GameTimer extends CountDownTimer
	{

		public GameTimer(long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);

			timerView.setText("00:00:00");
		}

		@Override
		public void onFinish()
		{
			timerView.setText("Done!");
			game.isInGame = false;
			game.promptShake();
		}

		@Override
		public void onTick(long millisUntilFinished)
		{
			timerView.setText(formatTime(millisUntilFinished));

			game.updatePercentage();

		}

		private String formatTime(long millis)
		{
			String output = "00:00:00";
			long seconds = millis;
			long minutes = seconds / 1000;
			long hours = minutes / 60;

			seconds = seconds % 100;
			minutes = minutes % 60;
			hours = hours % 60;

			String secondsD = String.valueOf(seconds);
			String minutesD = String.valueOf(minutes);
			String hoursD = String.valueOf(hours);

			if (seconds < 10)
				secondsD = "0" + seconds;
			if (minutes < 10)
				minutesD = "0" + minutes;
			if (hours < 10)
				hoursD = "0" + hours;

			output = hoursD + " : " + minutesD + " : " + secondsD;
			return output;
		}

	}
}
