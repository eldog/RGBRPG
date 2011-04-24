package com.muddyfox.rgbrpg.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.muddyfox.rgbrpg.R;

public class BattleView extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = BattleView.class.getSimpleName();

    /** The Thread that actually draws the colouring in action */
    private BattleViewThread mThread;

    public BattleView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create mThread only; it's started in surfaceCreated()
        mThread = new BattleViewThread(holder, context);

        Log.d(TAG, "mThread created");

        // make sure we get key events
        setFocusable(true);
    }

    private class BattleViewThread extends Thread
    {
        private static final int COLOUR_BRUSH_THICKNESS = 3;
        private static final int PIXEL_OUTSIDE = 666;
        private static final int PIXEL_LINE = 999;
        private static final int PIXEL_INSIDE = 333;

        /** Fuel Bar constants */
        private static final int UI_BAR = 100; // width of the bar(s)
        private static final int UI_BAR_HEIGHT = 10; // height of the bar(s)
        private static final int FILL_MAX = 100;

        private static final float TOUCH_TOLERANCE = 1.0f;

        /**
         * Handle to the application context, used to fetch Drawables and cute
         * animals.
         */
        private Context mContext;

        private SurfaceHolder mSurfaceHolder;
        private int mCanvasHeight = 1;
        private int mCanvasWidth = 1;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mTheadRun = false;

        private Paint mColouringInPaint = new Paint();
        private RectF mScratchRectangle = new RectF();
        private Paint mProgressBarPaint = new Paint();
        private Path mColouringPath = new Path();
        private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        {
            mColouringInPaint.setAntiAlias(true);
            mColouringInPaint.setDither(true);
            mColouringInPaint.setStyle(Paint.Style.STROKE);
            mColouringInPaint.setStrokeJoin(Paint.Join.ROUND);
            mColouringInPaint.setStrokeCap(Paint.Cap.ROUND);
            mColouringInPaint.setStrokeWidth(9);
            mColouringInPaint.setColor(Color.RED);
            mProgressBarPaint.setAntiAlias(true);
        }

        private Bitmap mLinesBitmap;
        private Bitmap mBackgroundBitmap;
        private Bitmap mCheckRegionBitmap;
        private Bitmap mCompletedColourBitmap;

        /**
         * An array which is used to tell which bits of the bit map we've
         * coloured already.
         */
        private int[][] mVisitedPixels;
        private int[] mCheckRegionPixels;
        private int mInsidePixelsColouredInSoFar;
        private int mOutsidePixelsColouredInSoFar;
        private int mInsideCount;
        private int mOutsideCount;

        private float mX, mY;

        private double mFillPercent;

        /** Decides whether we can colour in or not */
        private boolean mAllowedToColourIn = false;
        /** Whether or not the colour has been removed from the image */
        private boolean mLineBitmapDeColourfied = false;
        /** Can we draw the finished (colourful) Bitmap */
        private boolean mShowCompletedColourImage = false;

        public BattleViewThread(SurfaceHolder surfaceHolder, Context context)
        {
            super();
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            // Set the background bitmap
            Bitmap tempBitmap = BitmapFactory.decodeResource(mContext
                    .getResources(), R.drawable.page2);
            // Get mutable copy so we can paint
            mBackgroundBitmap = tempBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        @Override
        public void run()
        {
            Log.d(TAG, "Thread Run called");
            while (mTheadRun)
            {
                Canvas c = null;
                try
                {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder)
                    {
                        doDraw(c);
                    }
                }
                finally
                {
                    // do this in a finally so that if an exception is
                    // thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null)
                    {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }

            }
            Log.d(TAG, "Done Running");
        }

        private void doDraw(Canvas canvas)
        {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            if (!mShowCompletedColourImage && mLineBitmapDeColourfied)
            {
                canvas.drawBitmap(mLinesBitmap, 0, 0, mBitmapPaint);
                canvas.drawPath(mColouringPath, mColouringInPaint);
            }
            else if (mShowCompletedColourImage)
            {
                canvas.drawBitmap(mCompletedColourBitmap, 0, 0, mBitmapPaint);
            }

            mScratchRectangle.set(4, 4, 4 + UI_BAR, 4 + UI_BAR_HEIGHT);
            mProgressBarPaint.setColor(Color.RED);
            canvas.drawRect(mScratchRectangle, mProgressBarPaint);
            // draw the bar which shows how much we've "filled in"
            int fillWidth = (int) (UI_BAR * (mFillPercent / FILL_MAX));
            mScratchRectangle.set(4, 4, 4 + fillWidth, 4 + UI_BAR_HEIGHT);
            mProgressBarPaint.setColor(Color.GREEN);
            canvas.drawRect(mScratchRectangle, mProgressBarPaint);

            canvas.save();
            canvas.restore();
        }

        /**
         * Used to signal the mThread whether it should be running or not.
         * Passing true allows the mThread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         * 
         * @param b
         *            true to run, false to shut down
         */
        private void setRunning(boolean b)
        {
            synchronized (mSurfaceHolder)
            {
                mTheadRun = b;
            }
        }

        /**
         * Sets the picture that will be displayed
         * 
         * @param checkResID
         *            is the res ID of the "check picture", check pictures have
         *            the parts that are to be coloured in transparent, and the
         *            outside regions red.
         * @param colourResID
         *            is the res ID of the "colour picture", this is the picture
         *            that can be displayed when the image is coloured in
         *            enough. You probably want it to look somewhat like the
         *            check picture, otherwise it'll look pants.
         */
        private void setPicture(Context c, int checkResID, int colourResID)
        {
            synchronized (mSurfaceHolder)
            {
                // Create the check bitmap bitmap from the checkResID.
                mCheckRegionBitmap = BitmapFactory.decodeResource(c
                        .getResources(), checkResID);
                // Copy it to the bitmap we'll be colouring to, and make it
                // mutable
                mLinesBitmap = mCheckRegionBitmap.copy(Bitmap.Config.ARGB_8888,
                        true);
                // Create the colour bitmap from the colourRedID
                mCompletedColourBitmap = BitmapFactory.decodeResource(c
                        .getResources(), colourResID);

            }
        }

        /**
         * Sets the colour of the "crayon"
         */
        private void setCrayonColour(int colour)
        {
            synchronized (mSurfaceHolder)
            {
                mColouringInPaint.setColor(colour);
            }
        }

        /**
         * Resets or initialise the counters
         */
        private void reset()
        {
            synchronized (mSurfaceHolder)
            {
                mFillPercent = 0;
                mInsidePixelsColouredInSoFar = 0;
                mOutsidePixelsColouredInSoFar = 0;
                mLineBitmapDeColourfied = false;
                mShowCompletedColourImage = false;
                mColouringPath.reset();
            }
        }

        private void setSurfaceSize(int width, int height)
        {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder)
            {
                mCanvasWidth = width;
                mCanvasHeight = height;
                // Scale the bitmaps to fit the new surface
                // resize the background image and the colour image
                if (mBackgroundBitmap != null)
                {
                    mBackgroundBitmap = Bitmap.createScaledBitmap(
                            mBackgroundBitmap, mCanvasWidth, mCanvasHeight,
                            true);
                }
                if (mCompletedColourBitmap != null)
                {
                    mCompletedColourBitmap = Bitmap.createScaledBitmap(
                            mCompletedColourBitmap, mCanvasWidth,
                            mCanvasHeight, true);
                }
                if (mLinesBitmap != null)
                {
                    mLinesBitmap = Bitmap.createScaledBitmap(mLinesBitmap,
                            mCanvasWidth, mCanvasHeight, true);

                    if (mCheckRegionBitmap != null)
                    {
                        mCheckRegionBitmap = Bitmap.createScaledBitmap(
                                mCheckRegionBitmap, mCanvasWidth,
                                mCanvasHeight, true);
                        // resize the visited pixels
                        mVisitedPixels = new int[mCheckRegionBitmap.getWidth()][mCheckRegionBitmap
                                .getHeight()];
                        // adjust check pixels to new surface
                        mCheckRegionPixels = bitmapGetPixels(mCheckRegionBitmap);
                        // Create the outside regions, and remove the red.
                        bitmapSetPixels(mLinesBitmap,
                                createOutsideRegions(mCheckRegionPixels));

                        mLineBitmapDeColourfied = true;
                    }
                    Log.d(TAG, "Draw Image = " + mLinesBitmap.getWidth()
                            + " x " + mLinesBitmap.getHeight());
                    Log.d(TAG, "BG Image = " + mBackgroundBitmap.getWidth()
                            + " x " + mBackgroundBitmap.getHeight());
                }
            }
        }

        private int[] createOutsideRegions(int[] pixels)
        {
            synchronized (mSurfaceHolder)
            {

                int length = pixels.length;
                // Let's create the new pixels
                int newPixels[] = new int[length];
                mOutsideCount = 0;
                mInsideCount = 0;
                int lineCount = 0;
                for (int pixelIndex = 0; pixelIndex < length; pixelIndex++)
                {
                    // if the input pixel is RED then it's an outside area so
                    // mark
                    // it as that
                    int pixelValue = pixels[pixelIndex];
                    if (pixelValue == Color.RED)
                    {
                        pixels[pixelIndex] = PIXEL_OUTSIDE;
                        newPixels[pixelIndex] = Color.TRANSPARENT;
                        mOutsideCount++;
                    }
                    else if (pixelValue == Color.TRANSPARENT)
                    {
                        pixels[pixelIndex] = PIXEL_INSIDE;
                        newPixels[pixelIndex] = pixelValue;
                        mInsideCount++;
                    }
                    // Let's say it is a line then...
                    else
                    {
                        // Now we need to get rid of all those unwanted red
                        // bits...
                        pixels[pixelIndex] = PIXEL_LINE;
                        lineCount++;

                        // trim the red out
                        int red = Color.red(pixelValue);
                        if (red < 128)
                        {
                            newPixels[pixelIndex] = pixelValue;
                        }
                        else

                        {
                            pixels[pixelIndex] = Color.BLACK;
                        }
                    }
                }

                Log.v(TAG, "Number of outside pixels in check: "
                        + mOutsideCount);
                Log.v(TAG, "Number of inside pixels in check: " + mInsideCount);
                Log.v(TAG, "Number of line pixels in check: " + lineCount);

                return newPixels;
            }
        }

        private int[] bitmapGetPixels(Bitmap bitmap)
        {
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            int pixels[] = new int[height * width];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            return pixels;
        }

        private void bitmapSetPixels(Bitmap bitmap, int[] newPixels)
        {

            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            if (newPixels.length != (height * width))
            {
                Log.e(TAG, "New pixels" + newPixels.length + " bitmap pixels "
                        + (height * width));
            }
            else
            {
                bitmap.setPixels(newPixels, 0, width, 0, 0, width, height);
            }
        }

        /*
         * We go through a double loop, creating a square, we assume transparency
         * means that we haven't coloured that pixel yet.
         */
        private void updateCurrentFillPercentFromTouch(int startX, int startY)
        {
            int x = startX;
            int y = startY;
            for (int i = x - COLOUR_BRUSH_THICKNESS; i <= (int) x
                    + COLOUR_BRUSH_THICKNESS; i++)
            {
                for (int j = y - COLOUR_BRUSH_THICKNESS; j <= y
                        + COLOUR_BRUSH_THICKNESS; j++)
                {
                    if (i >= 0 && j >= 0 && i < mCheckRegionBitmap.getWidth()
                            && j < mCheckRegionBitmap.getHeight())
                    {

                        if (mVisitedPixels[i][j] != 1)
                        {
                            // if it is at the moment not coloured.
                            if (mLinesBitmap.getPixel(i, j) == Color.TRANSPARENT)
                            {
                                int checkColour = mCheckRegionBitmap.getPixel(
                                        i, j);

                                // if the check is clear, then we're in
                                if (checkColour != Color.RED
                                        && checkColour != Color.BLACK)
                                {
                                    mInsidePixelsColouredInSoFar++;
                                }
                                // if it's red we're out.
                                else if (checkColour == Color.RED)
                                {
                                    mOutsidePixelsColouredInSoFar++;
                                }

                                mVisitedPixels[i][j] = 1;
                            }
                        }
                    }
                }
            }

            mFillPercent = (mInsidePixelsColouredInSoFar / (double) mInsideCount) * 100;
        }

        // Draws a line between two points recursively
        private void drawPathBetweenRecurs(int x1, int y1, int x2, int y2)
        {
            int middleX = (x1 + x2) / 2;
            int middleY = (y1 + y2) / 2;

            updateCurrentFillPercentFromTouch(middleX, middleY);
            if ((middleX != x1 || middleY != y1)
                    && (middleX != x2 || middleY != y2))
            {
                drawPathBetweenRecurs(x1, y1, middleX, middleY);
                drawPathBetweenRecurs(x2, y2, middleX, middleY);
            }
        }

        // When the user starts pressing on the screen
        private void touchStart(float x, float y, float rX, float rY)
        {
            mColouringPath.moveTo(x, y);
            mX = x;
            mY = y;

            updateCurrentFillPercentFromTouch((int) x, (int) y);
        }

        // As the user moves their finger across the screen
        private void touchMove(float x, float y, float rX, float rY)
        {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
            {
                mColouringPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                updateCurrentFillPercentFromTouch((int) x, (int) y);
                drawPathBetweenRecurs((int) mX, (int) mY, (int) x, (int) y);
                mX = x;
                mY = y;
            }
        }

        // When the user lifts their finger off the screen
        private void touch_up()
        {
            mColouringPath.lineTo(mX, mY);
        }

        private boolean doTouchEvent(MotionEvent event)
        {
            synchronized (mSurfaceHolder)
            {
                if (mAllowedToColourIn)
                {
                    float x = event.getX();
                    float y = event.getY();
                    float rX = event.getRawX();
                    float rY = event.getRawY();

                    switch (event.getAction())
                    {
                        case MotionEvent.ACTION_DOWN:
                            touchStart(x, y, rX, rY);
                            invalidate();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            touchMove(x, y, rX, rY);
                            invalidate();
                            break;
                        case MotionEvent.ACTION_UP:
                            touch_up();
                            invalidate();
                            break;
                    }
                }
                return true;
            }
        }

        private void setAllowedToColourIn(boolean allowedToColourIn)
        {
            synchronized (mSurfaceHolder)
            {
                this.mAllowedToColourIn = allowedToColourIn;
            }
        }

    }

    public void setAllowedToColourIn(boolean allowedToColourIn)
    {
        mThread.setAllowedToColourIn(allowedToColourIn);
    }

    public void reset()
    {
        this.mThread.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return mThread.doTouchEvent(event);
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height)
    {
        Log.d(TAG, "Surface Changed");
        mThread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        // start the mThread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        Log.d(TAG, "Surface Created");
        if (mThread.getState() == Thread.State.TERMINATED)
        {
            Log.d(TAG, "Old Thread terminated Creating new one");
            mThread = new BattleViewThread(getHolder(), getContext());
            reset();
            setPicture(R.drawable.baby_otter_check,
                    R.drawable.baby_otter_colour_final);
            setCrayonColour(Color.argb(200, 206, 255, 150));
            setAllowedToColourIn(true);
        }

        if (mThread.getState() == Thread.State.NEW)
        {
            mThread.start();
            mThread.setRunning(true);
        }

    }

    public void setPicture(int checkResID, int colourResID)
    {
        mThread.setPicture(this.getContext(), checkResID, colourResID);
    }

    public void setCrayonColour(int colour)
    {
        mThread.setCrayonColour(colour);
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // we have to tell mThread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        mThread.setRunning(false);
        while (retry)
        {
            try
            {
                Log.d(TAG, "Joining Thread");
                mThread.join();
                retry = false;
            }
            catch (InterruptedException e)
            {
                Log.e(TAG, "Trying to destroy surface", e);
            }
        }
    }
}
