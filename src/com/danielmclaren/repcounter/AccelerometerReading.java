package com.danielmclaren.repcounter;

public class AccelerometerReading {
	public long t;
	public float x;
	public float y;
	public float z;
	public float dx;
	public float dy;
	public float dz;
	public float adx;
	public float ady;
	public float adz;
	
	public AccelerometerReading(long timestamp, float xReading, float yReading, float zReading) {
		t = timestamp;
		x = xReading;
		y = yReading;
		z = zReading;
		
		dx = 0;
		dy = 0;
		dz = 0;
		
		adx = 0;
		ady = 0;
		adz = 0;
	}
	
	public AccelerometerReading(long timestamp, float xReading, float yReading, float zReading, AccelerometerReading prevReading) {
		t = timestamp;
		x = xReading;
		y = yReading;
		z = zReading;
		
		dx = prevReading == null ? 0 : x - prevReading.x;
		dy = prevReading == null ? 0 : y - prevReading.y;
		dz = prevReading == null ? 0 : z - prevReading.z;
		
		adx = 0;
		ady = 0;
		adz = 0;
	}
	
	public AccelerometerReading(long timestamp, float[] values) {
		t = timestamp;
		x = values[0];
		y = values[1];
		z = values[2];
		
		dx = 0;
		dy = 0;
		dz = 0;
		
		adx = 0;
		ady = 0;
		adz = 0;
	}
	
	public AccelerometerReading(long timestamp, float[] values, AccelerometerReading prevReading) {
		t = timestamp;
		x = values[0];
		y = values[1];
		z = values[2];
		
		dx = prevReading == null ? 0 : x - prevReading.x;
		dy = prevReading == null ? 0 : y - prevReading.y;
		dz = prevReading == null ? 0 : z - prevReading.z;
		
		adx = 0;
		ady = 0;
		adz = 0;
	}
	
	public String toString() {
		return Long.toString(t) + ": " + Float.toString(x) + "," + Float.toString(y) + "," + Float.toString(z);
	}
}
