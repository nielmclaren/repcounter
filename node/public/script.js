	
var data = [], chart;
var x, y, w = 20, h = 80;
var ago = 5000; // ms.

$(document).ready(function() {
	var socket = io.connect('http://192.168.1.102:3000');	
	socket.on('whoIs', function(data) {
		socket.emit('iAm', 'viewer');
		$('#output').text($('#output').text() + "\nwhoIs? iAm.");
	});

	socket.on('sensorChanged', function(time, x, y, z, adx, ady, adz) {
		data.push({time: time, x: x, y: y, z: z});
	});

	socket.on('repDetected', function(time) {
		console.log(time);
	});

	setInterval(redraw, 100);

	var now = (new Date()).getTime();
	chart = d3.select("body").append("svg")
			.attr("class", "chart")
			.attr("width", 800)
			.attr("height", 1600);
});

function redraw() {
	var before = (new Date()).getTime() - ago;
	data = data.filter(function(d) { return d.time > before; });

	var now = (new Date()).getTime();
	x = d3.scale.linear()
			.domain([now - ago, now])
			.range([0, 800]);
	
	y = d3.scale.linear()
			.domain([-20, 20])
			.rangeRound([-h, h]);
		
	redrawProperty("x", 200);
	redrawProperty("y", 400);
	redrawProperty("z", 600);
}

function redrawProperty(propertyName, yOffset) {
	var line = chart.selectAll("line." + propertyName)
			.data(data, function(d) { return d.time; });

	line.enter().insert("line")
			.attr("class", propertyName)
			.attr("x1", function(d) { return x(d.time); })
			.attr("y1", function(d) { return yOffset; })
			.attr("x2", function(d) { return x(d.time); })
			.attr("y2", function(d) { return yOffset + y(d[propertyName]); });

	line
			.attr("x1", function(d, i) { return x(d.time); })
			.attr("x2", function(d, i) { return x(d.time); });
	
	line.exit()
			.remove();
}

