package com.muddyfox.rgbrpg;

import com.muddyfox.rgbrpg.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MainMenu extends Activity
{
	private View highScoresButton;
	private View timeChallengeButton;
	private View enduranceModeButton;
	//private View optionsButton;
	private View mBackground;
	private View mTitle;

	private Animation mFadeOutAnimation;
	private Animation mButtonFlickerAnimation;
	private Animation mAlternateFadeOutAnimation;

	private View.OnClickListener timeChallengeButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			Intent myIntent = new Intent(getBaseContext(), TimeChallenge.class);

			v.startAnimation(mButtonFlickerAnimation);
			mFadeOutAnimation
					.setAnimationListener(new StartActivityAfterAnimation(
							myIntent));
			mBackground.startAnimation(mFadeOutAnimation);
			//optionsButton.startAnimation(mAlternateFadeOutAnimation);
			highScoresButton.startAnimation(mAlternateFadeOutAnimation);
			mTitle.startAnimation(mAlternateFadeOutAnimation);
		}
	};
	
	private View.OnClickListener enduranceModeButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			Intent myIntent = new Intent(getBaseContext(), EnduranceGame.class);

			v.startAnimation(mButtonFlickerAnimation);
			mFadeOutAnimation
					.setAnimationListener(new StartActivityAfterAnimation(
							myIntent));
			mBackground.startAnimation(mFadeOutAnimation);
			//optionsButton.startAnimation(mAlternateFadeOutAnimation);
			timeChallengeButton.startAnimation(mAlternateFadeOutAnimation);
			highScoresButton.startAnimation(mAlternateFadeOutAnimation);
			mTitle.startAnimation(mAlternateFadeOutAnimation);
		}
	};

	private View.OnClickListener highScoresButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			Intent myIntent = new Intent(getBaseContext(), HighScore.class);

			v.startAnimation(mButtonFlickerAnimation);
			mFadeOutAnimation
					.setAnimationListener(new StartActivityAfterAnimation(
							myIntent));
			mBackground.startAnimation(mFadeOutAnimation);
			//optionsButton.startAnimation(mAlternateFadeOutAnimation);
			timeChallengeButton.startAnimation(mAlternateFadeOutAnimation);
			mTitle.startAnimation(mAlternateFadeOutAnimation);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mBackground = findViewById(R.id.mainMenuBackground);
		highScoresButton = findViewById(R.id.highScoresButton);
		timeChallengeButton = findViewById(R.id.timeChallengeStartButton);
		enduranceModeButton = findViewById(R.id.enduranceModeStartButton);
		//optionsButton = findViewById(R.id.optionsButton);
		mTitle = findViewById(R.id.title);

		enduranceModeButton.setOnClickListener(enduranceModeButtonListener);
		highScoresButton.setOnClickListener(highScoresButtonListener);
		
		
		timeChallengeButton.setOnClickListener(timeChallengeButtonListener);

		mButtonFlickerAnimation = AnimationUtils.loadAnimation(this,
				R.anim.button_flicker);
		mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
		mAlternateFadeOutAnimation = AnimationUtils.loadAnimation(this,
				R.anim.fade_out);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (highScoresButton != null)
		{
			highScoresButton.setVisibility(View.VISIBLE);
			highScoresButton.clearAnimation();
		}

		if (timeChallengeButton != null)
		{
			timeChallengeButton.setVisibility(View.VISIBLE);
			timeChallengeButton.clearAnimation();
		}

		//if (optionsButton != null)
		//{
		//	optionsButton.setVisibility(View.VISIBLE);
		//	optionsButton.clearAnimation();
		//}
		
		if (enduranceModeButton != null)
		{
			enduranceModeButton.setVisibility(View.VISIBLE);
			enduranceModeButton.clearAnimation();
		}

		if (mTitle != null)
		{
			mTitle.setVisibility(View.VISIBLE);
			mTitle.clearAnimation();
		}

		if (mBackground != null)
		{
			mBackground.clearAnimation();
		}

	}

	protected class StartActivityAfterAnimation implements
			Animation.AnimationListener
	{
		private Intent mIntent;

		StartActivityAfterAnimation(Intent intent)
		{
			mIntent = intent;
		}

		public void onAnimationEnd(Animation animation)
		{
			highScoresButton.setVisibility(View.INVISIBLE);
			highScoresButton.clearAnimation();
			timeChallengeButton.setVisibility(View.INVISIBLE);
			timeChallengeButton.clearAnimation();
			enduranceModeButton.setVisibility(View.INVISIBLE);
			enduranceModeButton.clearAnimation();
			//optionsButton.setVisibility(View.INVISIBLE);
			//optionsButton.clearAnimation();
			mTitle.setVisibility(View.INVISIBLE);
			mTitle.clearAnimation();
			startActivity(mIntent);
		}

		public void onAnimationRepeat(Animation animation)
		{
			// TODO Auto-generated method stub

		}

		public void onAnimationStart(Animation animation)
		{
			// TODO Auto-generated method stub

		}

	}

}
