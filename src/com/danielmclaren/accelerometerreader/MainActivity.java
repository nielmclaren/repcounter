package com.danielmclaren.accelerometerreader;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity implements SensorEventListener {
	private static final String TAG = "MainActivity";
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private ArrayList<AccelerometerReading> readings;
	
	private StringBuffer accel;
	private StringBuffer reps;
	
	private static final String baseUrl = "http://192.168.1.121/2012/dmclaren/repcounter/wwwdocs";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		readings = new ArrayList<AccelerometerReading>();
		
		accel = new StringBuffer("t\tx\ty\tz\n");
		reps = new StringBuffer("t\n");
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
		readings.add(new AccelerometerReading(
				System.currentTimeMillis(),
				event.values[0],
				event.values[1],
				event.values[2]));
		
		accel.append(System.currentTimeMillis());
		accel.append("\t");
		accel.append(event.values[0]);
		accel.append("\t");
		accel.append(event.values[1]);
		accel.append("\t");
		accel.append(event.values[2]);
		accel.append("\n");
	}
	
	public void recordRep(View view) {
		reps.append(System.currentTimeMillis());
		reps.append("\n");
	}
	
	public void sendData(View view) {
		Log.d(TAG, accel.toString());
		new SendDataTask().execute(accel.toString(), reps.toString(), null, null);
		accel = new StringBuffer("t\tx\ty\tz\n");
		reps = new StringBuffer("t\n");
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
			return null;
		}

		protected void onProgressUpdate(Void... progress) {
		}

		protected void onPostExecute(String result) {
		}
	}

}
