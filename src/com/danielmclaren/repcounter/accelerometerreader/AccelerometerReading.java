package com.danielmclaren.accelerometerreader;

public class AccelerometerReading {
	public long t;
	public float x;
	public float y;
	public float z;
	
	public float dxAvg;
	public float dyAvg;
	public float dzAvg;
	
	public AccelerometerReading(long timestamp, float xReading, float yReading, float zReading) {
		t = timestamp;
		x = xReading;
		y = yReading;
		z = zReading;
	}
	
	public String toString() {
		return Long.toString(t) + ": " + Float.toString(x) + "," + Float.toString(y) + "," + Float.toString(z);
	}
}
