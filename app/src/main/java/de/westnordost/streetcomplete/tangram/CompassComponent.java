package de.westnordost.streetcomplete.tangram;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Timer;
import java.util.TimerTask;

/** Component that gets the sensor data from accelerometer and magnetic field, smoothens it out and
 *  makes callbacks to report a simple rotation to its parent.
 */
public class CompassComponent implements SensorEventListener
{
	private SensorManager sensorManager;
	private Sensor accelerometer, magnetometer;
	private Timer compassTimer;
	private CompassAnimator compassAnimator;
	private float[] gravity, geomagnetic;
	/** time the compass needle needs in order to rotate into a new direction (from sensor data).
	 *  The sensor data is a bit erratic, so this smoothens it out. */
	private static final int DURATION = 200;
	// the compass doesn't move that fast, this is more than enough
	private static final int RotationUpdateFPS = 30;

	private Listener listener;
	public interface Listener
	{
		void onRotationChanged(float rotation, float tilt);
	}

	public void setListener(Listener listener)
	{
		this.listener = listener;
	}

	public void onCreate(SensorManager sensorManager)
	{
		this.sensorManager = sensorManager;
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	public void onResume()
	{
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}

	public void onPause()
	{
		if(compassTimer != null) compassTimer.cancel();
		sensorManager.unregisterListener(this);
	}

	@Override public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

	}

	private void initializeCompassAnimator()
	{
		compassTimer = new Timer();
		compassAnimator = new CompassAnimator();
		compassTimer.scheduleAtFixedRate(compassAnimator, 0, 1000/RotationUpdateFPS);
	}

	@Override public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			geomagnetic = event.values;
		}
		else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			gravity = event.values;
		}

		if (gravity != null && geomagnetic != null)
		{
			if(compassTimer == null) initializeCompassAnimator();

			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				float azimut = orientation[0]; // orientation contains: azimut, pitch and roll
				float pitch = orientation[1];
				compassAnimator.targetRotation = azimut;
				compassAnimator.targetTilt = pitch;
			}
		}
	}

	/** dampens the erratic-ness of the sensors by <b>animating towards</b> the calculated rotation
	 *  and not directly setting it */
	private class CompassAnimator extends TimerTask
	{
		private float INITIAL = -9999;
		private float currentRotation = INITIAL;
		private float currentTilt = INITIAL;
		private long lastTime = System.currentTimeMillis();

		volatile float targetRotation = INITIAL;
		volatile float targetTilt = INITIAL;

		@Override public void run()
		{
			currentRotation = animate(currentRotation, targetRotation);
			currentTilt = animate(currentTilt, targetTilt);

			if(listener != null) listener.onRotationChanged(currentRotation, currentTilt);
			lastTime = System.currentTimeMillis();
		}

		private float animate(float current, float target)
		{
			if(target == INITIAL) return current;
			if(current == INITIAL || current == target) return target;

			long deltaTime = System.currentTimeMillis() - lastTime;
			if(deltaTime > DURATION) return target;

			float deltaRotation = target - current;
			while (deltaRotation > +Math.PI) deltaRotation -= 2*Math.PI;
			while (deltaRotation < -Math.PI) deltaRotation += 2*Math.PI;

			return current + deltaRotation * deltaTime / DURATION;
		}
	}
}
