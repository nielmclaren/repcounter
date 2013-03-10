package com.danielmclaren.repcounter;

import io.socket.*;

import java.net.MalformedURLException;

import org.json.*;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity implements IOCallback, SensorEventListener {
	private static final String TAG = "MainActivity";
	
	private SensorManager sensorManager;
	private Sensor accelerometer;
	
	SocketIO socket;
	private boolean emitEvents;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		try {
			socket = new SocketIO("http://192.168.0.104:3000");
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
		if (emitEvents) {
			socket.emit(
					"sensorChanged",
					System.currentTimeMillis(),
					event.values[0],
					event.values[1],
					event.values[2]);
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
    	if (emitEvents) {
    		System.out.println("Emitting events.");
    	}
    	else {
    		System.out.println("Stopped emitting events.");
    	}
    }
}
