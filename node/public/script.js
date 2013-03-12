	
var readings = [], reps = [], chart;
var w = 20, h = 40, xScale, yScale, mins = {}, maxes = {};
var ago = 5000; // ms.
var intervalId;

var xLowThreshed = false;
var xHighThreshed = false;
var zLowThreshed = false;
var zHighThreshed = false;
var repped = false;

var repCount = 0;

$(document).ready(function() {
	var socket = io.connect('http://localhost:3000');	
	socket.on('whoIs', function(data) {
		socket.emit('iAm', 'viewer');
		$('#output').text($('#output').text() + "\nwhoIs? iAm.");
	});

	socket.on('sensorChanged', function(time, x, y, z) {
		var p = readings[readings.length - 1] || {x: 0, y: 0, z: 0};
		var r = {
			time: time,
			x: x, y: y, z: z,
			dx: x - p.x,
			dy: y - p.y,
			dz: z - p.z,
			adx: getAverageReading(50, 'dx'),
			ady: getAverageReading(50, 'dy'),
			adz: getAverageReading(50, 'dz')
		};

		if (Math.abs(getAverageReading(50, 'y')) > 5) {
			// If y-axis is in the wrong orientation, reset.
			xLowThreshed = false;
			xHighThreshed = false;
			zLowThreshed = false;
			zHighThreshed = false;
		}
		else {
			// Reps are two-part cycles based on x- and z-axes. The half-rep
			// (resting position) is detected when x is low and z is high. Then
			// the rep is detected when x is high and z is low.
			if (!xLowThreshed && x < 0) {
				xLowThreshed = true;
				xHighThreshed = false;

				if (repped && zHighThreshed) {
					repped = false;
				}
			}
			if (!xHighThreshed && x > 5) {
				xLowThreshed = false;
				xHighThreshed = true;

				if (!repped && zLowThreshed) {
					reps.push({time: time, count: repCount++});
					repped = true;
				}
			}
			if (!zLowThreshed && z < 0) {
				zLowThreshed = true;
				zHighThreshed = false;

				if (!repped && xHighThreshed) {
					reps.push({time: time, count: repCount++});
					repped = true;
				}
			}
			if (!zHighThreshed && z > 5) {
				zLowThreshed = false;
				zHighThreshed = true;

				if (repped && xLowThreshed) {
					repped = false;
				}
			}
		}
	
		readings.push(r);
	});

	socket.on('repDetected', function(time) {
		reps.push({time: time});
	});

	intervalId = setInterval(step, 100);

	var now = (new Date()).getTime();
	chart = d3.select("body").append("svg")
			.attr("class", "chart")
			.attr("width", 1200)
			.attr("height", 1600);
	
	$('body').click(function() {
		if (intervalId) {
			clearInterval(intervalId);
			intervalId = null;
		}
		else {
			intervalId = setInterval(step, 100);
		}
	});
});

function getAverageReading(numReadings, propertyName) {
	return readings
		.slice(readings.length - numReadings - 1)
		.map(function(d) { return d[propertyName]; })
		.reduce(function(a,b) { return a + b; }, 0) / numReadings;
}

function step() {
	var now = (new Date()).getTime();
	var before = now - ago;
	readings = readings.filter(function(d) { return d.time > before; });
	reps = reps.filter(function(d) { return d.time > before; });

	xScale = d3.scale.linear()
			.domain([now - ago, now])
			.range([0, 1200]);
	
	yScale = d3.scale.linear()
			.domain([-20, 20])
			.rangeRound([-h, h]);
		
	redrawProperty("x", h);
	redrawProperty("y", 3*h);
	redrawProperty("z", 5*h);

	yScale = d3.scale.linear()
			.domain([-1, 1])
			.rangeRound([-h, h]);
		
	redrawProperty("dx", 7*h);
	redrawProperty("dy", 9*h);
	redrawProperty("dz", 11*h);

	redrawProperty("adx", 13*h);
	redrawProperty("ady", 15*h);
	redrawProperty("adz", 17*h);

	redrawReps();
}

function redrawProperty(propertyName, yOffset) {
	yOffset += 40;

	var line = chart.selectAll("line." + propertyName)
			.data(readings, function(d) { return d.time; });

	line.enter().insert("line")
			.attr("class", propertyName)
			.attr("x1", function(d) { return xScale(d.time); })
			.attr("y1", function(d) { return yOffset; })
			.attr("x2", function(d) { return xScale(d.time); })
			.attr("y2", function(d) { return yOffset + yScale(d[propertyName]); });

	line
			.attr("x1", function(d, i) { return xScale(d.time); })
			.attr("x2", function(d, i) { return xScale(d.time); });
	
	line.exit()
			.remove();
	
	mins[propertyName] = isNaN(mins[propertyName]) ? 0 : mins[propertyName];
	mins[propertyName] = Math.min(mins[propertyName], readings
		.map(function(d) { return d[propertyName];})
		.reduce(function(a,b) { return a < b ? a : b; }, 0));
	maxes[propertyName] = isNaN(maxes[propertyName]) ? 0 : maxes[propertyName];
	maxes[propertyName] = Math.max(maxes[propertyName], readings
		.map(function(d) { return d[propertyName]; })
		.reduce(function(a,b) { return a > b ? a : b; }, 0));

	if (chart.select('.' + propertyName + '-min').node() == null) {
		chart.append('text')
			.attr('class', propertyName + '-min');
	}
	if (chart.select('.' + propertyName + '-max').node() == null) {
		chart.append('text')
			.attr('class', propertyName + '-max');
	}

	chart.select('.' + propertyName + '-min')
		.attr('x', 806)
		.attr('y', yOffset)
		.text('Min: ' + Math.floor(mins[propertyName] * 100) / 100);
	chart.select('.' + propertyName + '-max')
		.attr('x', 800)
		.attr('y', yOffset + 24)
		.text('Max: ' + Math.floor(maxes[propertyName] * 100) / 100);
}

function redrawReps() {
	var line = chart.selectAll("line.rep")
			.data(reps, function(d) { return d.time; });
	
	line.enter().insert("line")
			.attr("class", "rep")
			.attr("x1", function(d) { return xScale(d.time); })
			.attr("y1", 0)
			.attr("x2", function(d) { return xScale(d.time); })
			.attr("y2", 1200);

	line
			.attr("x1", function(d, i) { return xScale(d.time); })
			.attr("x2", function(d, i) { return xScale(d.time); });
	
	line.exit()
			.remove();
	
	var label = chart.selectAll('text.rep')
			.data(reps, function(d) { return d.time; });

	label.enter().insert('text')
			.attr('class', 'rep')
			.attr("x", function(d) { return xScale(d.time) + 10; })
			.attr("y", 40)
			.text(function(d) { return d.count; });

	label	
			.attr("x", function(d, i) { return xScale(d.time) + 10; })
	
	label.exit()
			.remove();
}


