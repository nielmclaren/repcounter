package com.danielmclaren.accelerometerreader;

public class AccelerometerReading {
	public long t;
	public float x;
	public float y;
	public float z;
	
	public AccelerometerReading(long timestamp, float xReading, float yReading, float zReading) {
		t = timestamp;
		x = xReading;
		y = yReading;
		z = zReading;
	}
}
