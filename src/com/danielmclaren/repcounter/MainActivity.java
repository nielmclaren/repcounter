package com.danielmclaren.repcounter;

import io.socket.*;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity implements SensorEventListener {
	private static final String TAG = "MainActivity";
	
	private static final long maxReadingAge = 500;
	private static final String baseUrl = "http://192.168.1.101/2012/dmclaren/repcounter/wwwdocs";

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private ArrayList<AccelerometerReading> readings;
	
	private boolean threshed = false;
	
	private SoundPool soundPool;
	private AudioManager audioManager;
	private SparseIntArray soundPoolMap;
	
	private StringBuffer accel;
	private StringBuffer reps;
	private StringBuffer detections;
	
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
		
		accel = new StringBuffer("t\tx\ty\tz\tadx\tady\tadz\n");
		reps = new StringBuffer("t\n");
		detections = new StringBuffer("t\n");
		
		////
		
		SocketIO socket;
		try {
			socket = new SocketIO("http://192.168.1.102:3000");
	        socket.connect(new IOCallback() {
	            @Override
	            public void onMessage(JSONObject json, IOAcknowledge ack) {
	                try {
	                    System.out.println("Server said:" + json.toString(2));
	                } catch (JSONException e) {
	                    e.printStackTrace();
	                }
	            }
	
	            @Override
	            public void onMessage(String data, IOAcknowledge ack) {
	                System.out.println("Server said: " + data);
	            }
	
	            @Override
	            public void onError(SocketIOException socketIOException) {
	                System.out.println("an Error occured");
	                socketIOException.printStackTrace();
	            }
	
	            @Override
	            public void onDisconnect() {
	                System.out.println("Connection terminated.");
	            }
	
	            @Override
	            public void onConnect() {
	                System.out.println("Connection established");
	            }
	
	            @Override
	            public void on(String event, IOAcknowledge ack, Object... args) {
	                System.out.println("Server triggered event '" + event + "'");
	            }
	        });
	
	        // This line is cached until the connection is establisched.
	        socket.send("Hello Server!");
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

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
			detections.append(System.currentTimeMillis());
			detections.append("\n");

			playSound(getApplicationContext(), R.raw.btn080);

			threshed = false;
		}
		if (!threshed && Math.abs(currReading.adx) > 0.2) {
			threshed = true;
		}
		
		accel.append(System.currentTimeMillis());
		accel.append("\t");
		accel.append(event.values[0]);
		accel.append("\t");
		accel.append(event.values[1]);
		accel.append("\t");
		accel.append(event.values[2]);
		accel.append("\t");
		accel.append(currReading.adx);
		accel.append("\t");
		accel.append(currReading.ady);
		accel.append("\t");
		accel.append(currReading.adz);
		accel.append("\n");
	}
	
	public void recordRep(View view) {
		reps.append(System.currentTimeMillis());
		reps.append("\n");
	}
	
	public void sendData(View view) {
		new SendDataTask().execute(accel.toString(), reps.toString(), detections.toString(), null, null);
		accel = new StringBuffer("t\tx\ty\tz\tadx\tady\tadz\n");
		reps = new StringBuffer("t\n");
		detections = new StringBuffer("t\n");
	}

	private class SendDataTask extends AsyncTask<String, Void, Void> {
		protected Void doInBackground(String... data) {
			try {
				URL url = new URL(baseUrl + "/save_accel.php");
				URLConnection conn = url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				
				DataOutputStream output = new DataOutputStream(conn.getOutputStream());
				output.writeBytes(data[0]);
				output.flush();
				output.close();
				
				DataInputStream in = new DataInputStream(conn.getInputStream());
				BufferedReader input = new BufferedReader(new InputStreamReader(in));
				String str, result = "";
				while ((str = input.readLine()) != null) {
					result = result + str + "\n";
				}
				input.close();
			} catch (MalformedURLException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}

			try {
				URL url = new URL(baseUrl + "/save_reps.php");
				URLConnection conn = url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				
				DataOutputStream output = new DataOutputStream(conn.getOutputStream());
				output.writeBytes(data[1]);
				output.flush();
				output.close();
				
				DataInputStream in = new DataInputStream(conn.getInputStream());
				BufferedReader input = new BufferedReader(new InputStreamReader(in));
				String str, result = "";
				while ((str = input.readLine()) != null) {
					result = result + str + "\n";
				}
				input.close();
			} catch (MalformedURLException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}

			try {
				URL url = new URL(baseUrl + "/save_detections.php");
				URLConnection conn = url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				
				DataOutputStream output = new DataOutputStream(conn.getOutputStream());
				output.writeBytes(data[2]);
				output.flush();
				output.close();
				
				DataInputStream in = new DataInputStream(conn.getInputStream());
				BufferedReader input = new BufferedReader(new InputStreamReader(in));
				String str, result = "";
				while ((str = input.readLine()) != null) {
					result = result + str + "\n";
				}
				input.close();
			} catch (MalformedURLException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private void playSound(final Context context, final int resourceId) {
		float streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		streamVolume = streamVolume / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		soundPool.play(soundPoolMap.get(resourceId), streamVolume, streamVolume, 1, 0, 1f);
	}
}
