package com.muddyfox.rgbrpg;

import com.muddyfox.rgbrpg.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class RGBRPGview extends SurfaceView implements SurfaceHolder.Callback
{
	private int[] checkPixels;
	private Canvas mCanvas;
	private Path mPath;
	private Paint mBitmapPaint;
	public boolean showColourImage;
	private Paint colourPaint;

	private int dynamicInCount;
	private int dynamicOutCount;
	private int insideCount;
	private int outsideCount;

	public double fillPercent;

	private Bitmap mDrawImage;
	private Bitmap mBackgroundImage;
	private Bitmap checkBitmap;
	private Bitmap colourBitmap;

	private final static int BRUSH_THICKNESS = 3;

	/** Handle to the application context, used to e.g. fetch Drawables. */
	private Context mContext;

	private static final int OUTSIDE = 666;
	private static final int LINES = 999;
	private static final int INSIDE = 333;

	/** Pointer to the text view to display "Paused.." etc. */
	private TextView mStatusText;

	/** The thread that actually draws the animation */
	private RGBRPGThread thread;

	public RGBRPGview(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		thread = new RGBRPGThread(holder, context, new Handler()
		{
			@Override
			public void handleMessage(Message m)
			{
				mStatusText.setVisibility(m.getData().getInt("viz"));
				mStatusText.setText(m.getData().getString("text"));
			}
		});

		setFocusable(true); // make sure we get key events
	}

	class RGBRPGThread extends Thread
	{
		// These are the images that get coloured, in this order.. For now
		// TODO Add constants for all these
		private int[] bitmapList =
		{ Color.argb(99, 255, 156, 68), R.drawable.baby_otter_check,
				R.drawable.baby_otter_colour_final,
				Color.argb(99, 255, 156, 68), R.drawable.otter_check,
				R.drawable.otter_colour_final, Color.argb(99, 68, 156, 255),
				R.drawable.baby_wolf_check, R.drawable.baby_wolf_colour_final,
				Color.argb(99, 68, 156, 255), R.drawable.wolf_check,
				R.drawable.wolf_colour_final, Color.argb(99, 206, 255, 150),
				R.drawable.owl_baby_check, R.drawable.owl_baby_colour_final,
				Color.argb(99, 206, 224, 150), R.drawable.owl_check,
				R.drawable.owl_final };

		private int bitmapListIndex;

		// An array which is used to tell which bits of the bit map we've
		// coloured already.
		private int visitedPixels[][];

		/*
		 * UI constants (i.e. the speed & fuel bars)
		 */
		public static final int UI_BAR = 100; // width of the bar(s)
		public static final int UI_BAR_HEIGHT = 10; // height of the bar(s)
		public static final int FILL_MAX = 100;

		private RectF mScratchRectangle;
		private Paint progressBarPaint;

		private boolean isInGame = false;

		public static final int STATE_LOSE = 1;
		public static final int STATE_PAUSE = 2;
		public static final int STATE_READY = 3;
		public static final int STATE_RUNNING = 4;
		public static final int STATE_WIN = 5;

		private SurfaceHolder mSurfaceHolder;

		private int mCanvasHeight = 1;
		private int mCanvasWidth = 1;

		/** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
		private int mMode;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;
		private boolean mDrawImageProcessed;
		// This holds all the drawing together, without it everything goes nuts
		private boolean isVirgin;

		public RGBRPGThread(SurfaceHolder surfaceHolder, Context context,
				Handler handler)
		{
			mSurfaceHolder = surfaceHolder;
			mContext = context;
			mScratchRectangle = new RectF();

			colourPaint = new Paint();
			colourPaint.setAntiAlias(true);
			colourPaint.setDither(true);
			colourPaint.setStyle(Paint.Style.STROKE);
			colourPaint.setStrokeJoin(Paint.Join.ROUND);
			colourPaint.setStrokeCap(Paint.Cap.ROUND);
			colourPaint.setStrokeWidth(9);
			// set default colour
			colourPaint.setColor(Color.RED);
			progressBarPaint = new Paint();
			progressBarPaint.setAntiAlias(true);
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);

			bitmapListIndex = 0;
			mPath = new Path();
			// Set the background bitmap
			Bitmap temp = BitmapFactory.decodeResource(mContext.getResources(),
					R.drawable.page2);
			mBackgroundImage = temp.copy(Bitmap.Config.ARGB_8888, true);
		}

		@Override
		public void run()
		{
			while (mRun)
			{
				Canvas c = null;
				try
				{
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder)
					{
						if (mMode == STATE_RUNNING)
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
		}

		private void doDraw(Canvas canvas)
		{
			// Draw the background image. Operations on the Canvas accumulate
			// so this is like clearing the screen.
			canvas.drawBitmap(mBackgroundImage, 0, 0, null);
			if (!showColourImage && mDrawImageProcessed)
			{
				canvas.drawBitmap(mDrawImage, 0, 0, mBitmapPaint);
				canvas.drawPath(mPath, colourPaint);
			} else if (showColourImage)
			{
				canvas.drawBitmap(colourBitmap, 0, 0, mBitmapPaint);
			}

			mScratchRectangle.set(4, 4, 4 + UI_BAR, 4 + UI_BAR_HEIGHT);
			progressBarPaint.setColor(Color.RED);
			canvas.drawRect(mScratchRectangle, progressBarPaint);
			// draw the bar which shows how much we've "filled in"
			int fillWidth = (int) (UI_BAR * (fillPercent / FILL_MAX));
			mScratchRectangle.set(4, 4, 4 + fillWidth, 4 + UI_BAR_HEIGHT);
			progressBarPaint.setColor(Color.GREEN);
			canvas.drawRect(mScratchRectangle, progressBarPaint);

			canvas.save();
			canvas.restore();
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 * 
		 * @param b
		 *            true to run, false to shut down
		 */
		public void setRunning(boolean b)
		{
			mRun = b;
			mMode = STATE_RUNNING;
		}

		public boolean isAtEndOfSequence()
		{
			return (bitmapListIndex > bitmapList.length - 1);
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
				checkBitmap = BitmapFactory.decodeResource(c.getResources(),
						checkResID);
				// Copy it to the bitmap we'll be colouring to, and make it
				// mutable
				mDrawImage = checkBitmap.copy(Bitmap.Config.ARGB_8888, true);
				// Create the colour bitmap from the colourRedID
				colourBitmap = BitmapFactory.decodeResource(c.getResources(),
						colourResID);
				// Scale the bitmaps to fit the screen
				scaleBitmaps();
			}

		}

		/**
		 * Sets the colour of the "crayon"
		 */
		private void setCrayonColour(int colour)
		{
			synchronized (mSurfaceHolder)
			{
				colourPaint.setColor(colour);
			}
		}

		/**
		 * Resets or initialise the counters
		 */
		protected void setup()
		{
			fillPercent = 0;
			dynamicInCount = 0;
			dynamicOutCount = 0;
			mDrawImageProcessed = false;
			showColourImage = false;
			isVirgin = true;
			mPath.reset();
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
			if (colourBitmap != null)
			{
				colourBitmap = Bitmap.createScaledBitmap(colourBitmap,
						mCanvasWidth, mCanvasHeight, true);
			}
			if (mDrawImage != null)
			{
				mDrawImage = Bitmap.createScaledBitmap(mDrawImage,
						mCanvasWidth, mCanvasHeight, true);

				if (checkBitmap != null)
				{
					checkBitmap = Bitmap.createScaledBitmap(checkBitmap,
							mCanvasWidth, mCanvasHeight, true);
					// resize the visited pixels
					visitedPixels = new int[checkBitmap.getWidth()][checkBitmap
							.getHeight()];
					// adjust check pixels to new surface
					checkPixels = bitmapGetPixels(checkBitmap);
					// Create the outside regions, and remove the red.
					bitmapSetPixels(mDrawImage,
							createOutsideRegions(checkPixels));
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

		// Process the check image and mark the pixels as OUTSIDE, INSIDE or
		// LINES
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
						pixels[pixelIndex] = OUTSIDE;
						newPixels[pixelIndex] = Color.TRANSPARENT;
						outsideCount++;
					} else if (pixelValue == Color.TRANSPARENT)
					{
						pixels[pixelIndex] = INSIDE;
						newPixels[pixelIndex] = pixelValue;
						insideCount++;
					}
					// Let's say it is a line then...
					else
					{
						// Now we need to get rid of all those unwanted red
						// bits...
						pixels[pixelIndex] = LINES;
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
			mPath.reset();
			mPath.moveTo(x, y);
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
			for (int i = x - BRUSH_THICKNESS; i <= (int) x + BRUSH_THICKNESS; i++)
			{
				for (int j = y - BRUSH_THICKNESS; j <= y + BRUSH_THICKNESS; j++)
				{
					if (i >= 0 && j >= 0 && i < checkBitmap.getWidth()
							&& j < checkBitmap.getHeight())
					{

						if (visitedPixels[i][j] != 1)
						{
							// if it is at the moment not coloured.
							if (mDrawImage.getPixel(i, j) == Color.TRANSPARENT)
							{
								int checkColour = checkBitmap.getPixel(i, j);

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

								visitedPixels[i][j] = 1;
							}
						}
					}

				}
			}

			fillPercent = (dynamicInCount / (double) insideCount) * 100;
		}

		// As the user moves their finger across the screen
		private void touch_move(float x, float y, float rX, float rY)
		{
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
			{
				// PATH DRAW
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
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

			mPath.lineTo(mX, mY);
			// PATH DRAW
			mCanvas.drawPath(mPath, colourPaint);
			// kill this so we don't double draw
			mPath.reset();
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

	}

	public int[] getCanvasDetails()
	{
		return thread.getColouringSoFar();
	}

	public void setIsInGame(boolean isInGame)
	{
		thread.isInGame = isInGame;
	}

	public boolean isAtEndOfSequence()
	{
		return thread.isAtEndOfSequence();
	}

	public void setup()
	{
		this.thread.setup();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return thread.doTouchEvent(event);
	}

	/* Callback invoked when the surface dimensions change. */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height)
	{
		thread.setSurfaceSize(width, height);
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.setRunning(true);
		thread.start();

	}

	public void setPicture(int checkResID, int colourResID)
	{
		thread.setPicture(this.getContext(), checkResID, colourResID);
	}

	public void setCrayonColour(int colour)
	{
		thread.setCrayonColour(colour);
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		boolean retry = true;
		thread.setRunning(false);
		while (retry)
		{
			try
			{
				thread.join();
				retry = false;
			} catch (InterruptedException e)
			{
			}
		}
	}
}
