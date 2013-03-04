package com.danielmclaren.repcounter;

import io.socket.*;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.*;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity implements IOCallback, SensorEventListener {
	private static final String TAG = "MainActivity";
	
	private static final long maxReadingAge = 500;

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private ArrayList<AccelerometerReading> readings;
	
	private boolean threshed = false;
	
	private SoundPool soundPool;
	private AudioManager audioManager;
	private SparseIntArray soundPoolMap;
	
	SocketIO socket;
	private boolean emitEvents;
	
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
		
		try {
			socket = new SocketIO("http://192.168.1.102:3000");
	        socket.connect(this);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		emitEvents = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
		
		AccelerometerReading prevReading = readings.size() > 0 ? readings.get(readings.size() - 1) : null;
		AccelerometerReading currReading = new AccelerometerReading(t, event.values, prevReading);
		readings.add(currReading);
		
		// Forget any readings that are too old.
		while (readings.size() > 0 && t - readings.get(0).t > maxReadingAge) {
			readings.remove(0);
		}
		
		// Calculate the moving average of the derivative.
		Iterator<AccelerometerReading> iter = readings.iterator();
		while (iter.hasNext()) {
			AccelerometerReading reading = iter.next();
			currReading.adx += reading.dx;
			currReading.ady += reading.dy;
			currReading.adz += reading.dz;
		}
		currReading.adx /= readings.size();
		currReading.ady /= readings.size();
		currReading.adz /= readings.size();

		if (prevReading != null && threshed && currReading.adx / Math.abs(currReading.adx) != prevReading.adx / Math.abs(prevReading.adx)) {
			// Crossed over zero.
			if (emitEvents) {
				socket.emit("repDetected", System.currentTimeMillis());
			}
			
			playSound(getApplicationContext(), R.raw.btn080);

			threshed = false;
		}
		if (!threshed && Math.abs(currReading.adx) > 0.2) {
			threshed = true;
		}
		
		if (emitEvents) {
			socket.emit(
					"sensorChanged",
					System.currentTimeMillis(),
					event.values[0],
					event.values[1],
					event.values[2],
					currReading.adx,
					currReading.ady,
					currReading.adz);
		}
	}

    public void onMessage(JSONObject json, IOAcknowledge ack) {
        try {
            System.out.println("Server said:" + json.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onMessage(String data, IOAcknowledge ack) {
        System.out.println("Server said: " + data);
    }

    public void onError(SocketIOException socketIOException) {
        System.out.println("an Error occured");
        socketIOException.printStackTrace();
    }

    public void onDisconnect() {
        System.out.println("Connection terminated.");
    }

    public void onConnect() {
        System.out.println("Connection established");
    }

    public void on(String event, IOAcknowledge ack, Object... args) {
        System.out.println("Server triggered event '" + event + "'");
    	if (event.equals("whoIs")) {
    		socket.emit("iAm", "reader");
    	}
    }
    
    public void toggleEvents(View view) {
    	emitEvents = !emitEvents;
    }
	
	private void playSound(final Context context, final int resourceId) {
		float streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		streamVolume = streamVolume / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		soundPool.play(soundPoolMap.get(resourceId), streamVolume, streamVolume, 1, 0, 1f);
	}
}
