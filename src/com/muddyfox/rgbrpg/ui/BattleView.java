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
    private RGBRPGThread mThread;

    public BattleView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create mThread only; it's started in surfaceCreated()
        mThread = new RGBRPGThread(holder, context);

        Log.d(TAG, "mThread created");

        setFocusable(true); // make sure we get key events
    }

    class RGBRPGThread extends Thread
    {
        private static final int COLOUR_BRUSH_THICKNESS = 3;
        private static final int PIXEL_OUTSIDE = 666;
        private static final int PIXEL_LINE = 999;
        private static final int PIXEL_INSIDE = 333;

        /** Fuel Bar constants */
        private static final int UI_BAR = 100; // width of the bar(s)
        private static final int UI_BAR_HEIGHT = 10; // height of the bar(s)
        private static final int FILL_MAX = 100;

        /**
         * Handle to the application context, used to fetch Drawables and cute
         * animals.
         */
        private Context mContext;
        /** The Canvas we will be drawing to */
        private Canvas mCanvas;
        private SurfaceHolder mSurfaceHolder;
        private int mCanvasHeight = 1;
        private int mCanvasWidth = 1;
        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        private Paint mColouringInPaint = new Paint();
        private RectF mScratchRectangle = new RectF();
        private Paint progressBarPaint = new Paint();
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
            progressBarPaint.setAntiAlias(true);
        }
        
        private Bitmap mDrawImage;
        private Bitmap mBackgroundImage;
        private Bitmap mCheckRegionBitmap;
        private Bitmap mCompletedColoutBitmap;

        /**
         * An array which is used to tell which bits of the bit map we've
         * coloured already.
         */
        private int[][] mVisitedPixels;
        private int[] mCheckPixels;
        private int dynamicInCount;
        private int dynamicOutCount;
        private int insideCount;
        private int outsideCount;

        private double mFillPercent;

        private boolean isInGame = false;
        private boolean mDrawImageProcessed;
        // This holds all the drawing together, without it everything goes nuts
        private boolean isVirgin;
        private boolean mShowCompletedColourImage;
        
        
        public RGBRPGThread(SurfaceHolder surfaceHolder, Context context)
        {
            super();
            mSurfaceHolder = surfaceHolder;
            mContext = context;
            // Set the background bitmap
            Bitmap tempBitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.page2);
            // Get mutable copy so we can paint
            mBackgroundImage = tempBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        @Override
        public void run()
        {
            Log.d(TAG, "Running");
            while (mRun)
            {
                Canvas c = null;
                try
                {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder)
                    {
                        doDraw(c);
                    }
                } finally
                {
                    // do this in a finally so that if an exception is thrown
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
            canvas.drawBitmap(mBackgroundImage, 0, 0, null);
            if (!mShowCompletedColourImage && mDrawImageProcessed)
            {
                canvas.drawBitmap(mDrawImage, 0, 0, mBitmapPaint);
                canvas.drawPath(mColouringPath, mColouringInPaint);
            } else if (mShowCompletedColourImage)
            {
                canvas.drawBitmap(mCompletedColoutBitmap, 0, 0, mBitmapPaint);
            }

            mScratchRectangle.set(4, 4, 4 + UI_BAR, 4 + UI_BAR_HEIGHT);
            progressBarPaint.setColor(Color.RED);
            canvas.drawRect(mScratchRectangle, progressBarPaint);
            // draw the bar which shows how much we've "filled in"
            int fillWidth = (int) (UI_BAR * (mFillPercent / FILL_MAX));
            mScratchRectangle.set(4, 4, 4 + fillWidth, 4 + UI_BAR_HEIGHT);
            progressBarPaint.setColor(Color.GREEN);
            canvas.drawRect(mScratchRectangle, progressBarPaint);

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
        public void setRunning(boolean b)
        {
            synchronized (mSurfaceHolder)
            {
                mRun = b;
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
                mDrawImage = mCheckRegionBitmap.copy(Bitmap.Config.ARGB_8888,
                        true);
                // Create the colour bitmap from the colourRedID
                mCompletedColoutBitmap = BitmapFactory.decodeResource(c
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
        protected void reset()
        {
            synchronized (mSurfaceHolder)
            {
                mFillPercent = 0;
                dynamicInCount = 0;
                dynamicOutCount = 0;
                mDrawImageProcessed = false;
                mShowCompletedColourImage = false;
                isVirgin = true;
                mColouringPath.reset();
            }
        }

        /**
         * Scales the bitmaps to fit the canvas, and creates the outside
         * regions.
         */
        private void scaleBitmaps()
        {
            // resize the background image and the colour image
            if (mBackgroundImage != null)
            {
                mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage,
                        mCanvasWidth, mCanvasHeight, true);
            }
            if (mCompletedColoutBitmap != null)
            {
                mCompletedColoutBitmap = Bitmap.createScaledBitmap(
                        mCompletedColoutBitmap, mCanvasWidth, mCanvasHeight,
                        true);
            }
            if (mDrawImage != null)
            {
                mDrawImage = Bitmap.createScaledBitmap(mDrawImage,
                        mCanvasWidth, mCanvasHeight, true);

                if (mCheckRegionBitmap != null)
                {
                    mCheckRegionBitmap = Bitmap.createScaledBitmap(
                            mCheckRegionBitmap, mCanvasWidth, mCanvasHeight,
                            true);
                    // resize the visited pixels
                    mVisitedPixels = new int[mCheckRegionBitmap.getWidth()][mCheckRegionBitmap
                            .getHeight()];
                    // adjust check pixels to new surface
                    mCheckPixels = bitmapGetPixels(mCheckRegionBitmap);
                    // Create the outside regions, and remove the red.
                    bitmapSetPixels(mDrawImage,
                            createOutsideRegions(mCheckPixels));
                    mCanvas = new Canvas(mDrawImage);

                    mDrawImageProcessed = true;
                }
                Log.d("BITMAP SIZE", "Draw Image = " + mDrawImage.getWidth()
                        + " x " + mDrawImage.getHeight());
                Log.d("BITMAP SIZE", "BG Image = "
                        + mBackgroundImage.getWidth() + " x "
                        + mBackgroundImage.getHeight());
            }
        }

        public void setSurfaceSize(int width, int height)
        {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder)
            {
                mCanvasWidth = width;
                mCanvasHeight = height;
                // Scale the bitmaps to fit the new surface
                scaleBitmaps();
            }
        }

        // Process the check image and mark the pixels as OUTSIDE, PIXEL_INSIDE
        // or
        // PIXEL_LINE
        public int[] getColouringSoFar()
        {
            int[] canvasInfo =
                { 0, 0, 0, 0 };
            canvasInfo[0] = dynamicInCount;
            canvasInfo[1] = insideCount;
            canvasInfo[2] = dynamicOutCount;
            canvasInfo[3] = outsideCount;

            return canvasInfo;
        }

        private int[] createOutsideRegions(int[] pixels)
        {
            synchronized (mSurfaceHolder)
            {

                int length = pixels.length;
                // Let's create the new pixels
                int newPixels[] = new int[length];
                outsideCount = 0;
                insideCount = 0;
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
                        outsideCount++;
                    } else if (pixelValue == Color.TRANSPARENT)
                    {
                        pixels[pixelIndex] = PIXEL_INSIDE;
                        newPixels[pixelIndex] = pixelValue;
                        insideCount++;
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

                        } else

                        {
                            pixels[pixelIndex] = Color.BLACK;
                        }
                    }

                }

                Log.v("OUT", "Number of outside pixels in check: "
                        + outsideCount);
                Log.v("IN", "Number of inside pixels in check: " + insideCount);
                Log.v("LINE", "Number of line pixels in check: " + lineCount);

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
                Log.e("SET PIXEL", "New pixels" + newPixels.length
                        + " bitmap pixels " + (height * width));
            } else
            {
                bitmap.setPixels(newPixels, 0, width, 0, 0, width, height);
            }

        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 1;

        // When the user starts pressing on the screen
        private void touch_start(float x, float y, float rX, float rY)
        {
            mColouringPath.reset();
            mColouringPath.moveTo(x, y);
            mX = x;
            mY = y;

            drawRawPixels((int) x, (int) y);
        }

        /*
         * Draw colour to the drawBitmap pixels surrounding the touch coords We
         * go through a double loop, creating a square, we assume transparency
         * means that we haven't coloured that pixel yet.
         */
        private void drawRawPixels(int startX, int startY)
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
                            if (mDrawImage.getPixel(i, j) == Color.TRANSPARENT)
                            {
                                int checkColour = mCheckRegionBitmap.getPixel(
                                        i, j);

                                // if the check is clear, then we're in
                                if (checkColour != Color.RED
                                        && checkColour != Color.BLACK)
                                {
                                    // RAW DRAW
                                    // mDrawImage.setPixel(i, j,
                                    // Color.argb(99, 68, 156, 255));
                                    dynamicInCount++;

                                }
                                // if it's red we're out.
                                else if (checkColour == Color.RED)
                                {
                                    // RAW DRAW
                                    // mDrawImage.setPixel(i, j,
                                    // Color.argb(99, 255, 156, 68));
                                    dynamicOutCount++;
                                }

                                mVisitedPixels[i][j] = 1;
                            }
                        }
                    }

                }
            }

            mFillPercent = (dynamicInCount / (double) insideCount) * 100;
        }

        // As the user moves their finger across the screen
        private void touch_move(float x, float y, float rX, float rY)
        {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
            {
                // PATH DRAW
                mColouringPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                drawRawPixels((int) x, (int) y);
                drawPathBetweenRecurs((int) mX, (int) mY, (int) x, (int) y);
                mX = x;
                mY = y;
            }

        }

        // Draws a line between two points recursively
        private void drawPathBetweenRecurs(int x1, int y1, int x2, int y2)
        {
            int middleX = (x1 + x2) / 2;
            int middleY = (y1 + y2) / 2;

            drawRawPixels(middleX, middleY);
            if ((middleX != x1 || middleY != y1)
                    && (middleX != x2 || middleY != y2))
            {
                drawPathBetweenRecurs(x1, y1, middleX, middleY);
                drawPathBetweenRecurs(x2, y2, middleX, middleY);
            }
        }

        // When the user lifts their finger off the screen
        private void touch_up()
        {

            mColouringPath.lineTo(mX, mY);
            // PATH DRAW
            mCanvas.drawPath(mColouringPath, mColouringInPaint);
            // kill this so we don't double draw
            mColouringPath.reset();
        }

        public boolean doTouchEvent(MotionEvent event)
        {
            synchronized (mSurfaceHolder)
            {
                if (isInGame)
                {
                    float x = event.getX();
                    float y = event.getY();
                    float rX = event.getRawX();
                    float rY = event.getRawY();

                    switch (event.getAction())
                    {
                    case MotionEvent.ACTION_DOWN:
                        touch_start(x, y, rX, rY);
                        isVirgin = false;
                        invalidate();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isVirgin)
                        {
                            touch_start(x, y, rX, rY);
                            isVirgin = false;
                        } else
                        {
                            touch_move(x, y, rX, rY);
                        }
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

        protected void setInGame(boolean isInGame)
        {
            synchronized (mSurfaceHolder)
            {
                this.isInGame = isInGame;
            }
        }

    }

    public int[] getCanvasDetails()
    {
        return mThread.getColouringSoFar();
    }

    public void setIsInGame(boolean isInGame)
    {
        mThread.setInGame(isInGame);
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
        mThread.setRunning(true);
        mThread.start();

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
                mThread.join();
                retry = false;
            } catch (InterruptedException e)
            {
            }
        }
    }
}
