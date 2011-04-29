package com.muddyfox.rgbrpg;

public interface GameTimerListener
{
    public void onStart();
    
    public void onTick(long millisUntilFinished);
    
    public void onFinish();
    
}
