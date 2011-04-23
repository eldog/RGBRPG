package com.muddyfox.rgbrpg;

import java.util.List;

import com.muddyfox.rgbrpg.ui.TimeChallengeActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelerometerListener implements SensorEventListener
{

	private SensorManager sensorManager;
	private List<Sensor> sensors;
	private Sensor sensor;
	private long lastUpdate = -1;
	private long currentTime = -1;
	private TimeChallengeActivity parent;

	private float last_x, last_y, last_z;
	private float current_x, current_y, current_z, currenForce;
	private static final int FORCE_THRESHOLD = 700;
	private final int DATA_X = SensorManager.DATA_X;
	private final int DATA_Y = SensorManager.DATA_Y;
	private final int DATA_Z = SensorManager.DATA_Z;

	public AccelerometerListener(TimeChallengeActivity parent)
	{

		// this.subscriber = subscriber;
		this.parent = parent;
		Context context = parent.getApplicationContext();
		sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		this.sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0)
		{
			sensor = sensors.get(0);
		}
	}

	public void start()
	{
		if (sensor != null)
		{
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_GAME);
		}
	}

	public void stop()
	{
		sensorManager.unregisterListener(this);
	}

	public void onAccuracyChanged(Sensor s, int valu)
	{

	}

	public void onSensorChanged(SensorEvent event)
	{

		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER
				|| event.values.length < 3)
			return;

		currentTime = System.currentTimeMillis();

		if ((currentTime - lastUpdate) > 100)
		{
			long diffTime = (currentTime - lastUpdate);
			lastUpdate = currentTime;

			current_x = event.values[DATA_X];
			current_y = event.values[DATA_Y];
			current_z = event.values[DATA_Z];

			currenForce = Math.abs(current_x + current_y + current_z - last_x
					- last_y - last_z)
					/ diffTime * 10000;

			if (currenForce > FORCE_THRESHOLD)
			{

				// Device has been shaken now go on and do something
				// you could now inform the parent activity ...

				if (!TimeChallengeActivity.isInGame)
				{
					this.stop();
					parent.checkScore();

				}

			}
			last_x = current_x;
			last_y = current_y;
			last_z = current_z;

		}
	}

}
