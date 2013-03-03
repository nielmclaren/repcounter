package com.danielmclaren.accelerometerreader;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity implements SensorEventListener {
	private static final String TAG = "MainActivity";;
	
	private static final long maxReadingAge = 250;

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private ArrayList<AccelerometerReading> readings;
	
	private boolean threshed = false;
	
	private SoundPool soundPool;
	private AudioManager audioManager;
	private SparseIntArray soundPoolMap;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		readings = new ArrayList<AccelerometerReading>();
		
		Context context = getApplicationContext();
		soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		soundPoolMap = new SparseIntArray();
		soundPoolMap.put(R.raw.beepbeep, soundPool.load(context, R.raw.beepbeep, 1));
		soundPoolMap.put(R.raw.btn080, soundPool.load(context, R.raw.btn080, 1));
		soundPoolMap.put(R.raw.btn402, soundPool.load(context, R.raw.btn402, 1));
		soundPoolMap.put(R.raw.censore, soundPool.load(context, R.raw.censore, 1));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
	}

	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		long t = System.currentTimeMillis();
		
		readings.add(new AccelerometerReading(
				t,
				event.values[0],
				event.values[1],
				event.values[2]));
		
		// Forget any readings that are too old.
		while (readings.size() > 0 && t - readings.get(0).t > maxReadingAge) {
			readings.remove(0);
		}
		
		// Calculate the moving average of the derivative.
		Iterator<AccelerometerReading> iter = readings.iterator();
		AccelerometerReading prevReading, reading = null;
		if (iter.hasNext()) {
			prevReading = iter.next();
			while (iter.hasNext()) {
				reading = iter.next();
				reading.dxAvg += reading.x - prevReading.x;
				reading.dyAvg += reading.y - prevReading.y;
				reading.dzAvg += reading.z - prevReading.z;
				prevReading = reading;
			}
			if (reading != null) {
				reading.dxAvg /= readings.size() - 1;
				reading.dyAvg /= readings.size() - 1;
				reading.dzAvg /= readings.size() - 1;
	
				if (threshed && reading.dxAvg / Math.abs(reading.dxAvg) != prevReading.dxAvg / Math.abs(prevReading.dxAvg)) {
					// Crossed over zero.
					threshed = false;
					
					// Rep!
					playSound(getApplicationContext(), R.raw.btn080);
				}
				if (!threshed && Math.abs(reading.dxAvg) > 0.05) {
					threshed = true;
				}
			}
		}
	}
	
	public void test(View view) {
		playSound(getApplicationContext(), R.raw.btn080);
	}
	
	private void playSound(final Context context, final int resourceId) {
		float streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		streamVolume = streamVolume / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		soundPool.play(soundPoolMap.get(resourceId), streamVolume, streamVolume, 1, 0, 1f);
	}
}
