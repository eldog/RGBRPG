package com.muddyFox.RGBRPG;

import android.app.Activity;

public abstract class Game extends Activity
{
	
	public Game()
	{
		super();
	}
	
	public void newGame()
	{
		
	}
	
	public void startGame()
	{
		
	}
	
	public void pauseGame()
	{
		
	}
	
	public void stopGame()
	{
		
	}
	
	public void stopAudio()
	{
		
	}
	
	public void postHighscore(String userName)
	{
		
	}
	
	public void finish()
	{
		
	}
	
	@Override
    protected void onStop()
    {
        // TODO Auto-generated method stub
        super.onStop();
        this.finish();
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
        
    }

}
