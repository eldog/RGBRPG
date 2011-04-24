package com.muddyfox.rgbrpg.game;

public class Creature
{
    private final int mPaintColour;
    private final int mReferenceDrawableId;
    private final int mColourDrawableId;

    public Creature(final int colourDrawableId, final int referenceDrawableId,
            final int paintColour)
    {
        mColourDrawableId = colourDrawableId;
        mReferenceDrawableId = referenceDrawableId;
        mPaintColour = paintColour;
    }
    
    public int getColour()
    {
        return mPaintColour;
    }
    
    public int getReferenceDrawableId()
    {
        return mReferenceDrawableId;
    }
    
    public int getColourDrawableId()
    {
        return mColourDrawableId;
    }

}
