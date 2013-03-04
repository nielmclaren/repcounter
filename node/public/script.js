	
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

	y = d3.scale.linear()
			.domain([-5, 5])
			.rangeRound([0, h]);
		
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
	
	redrawRect("x", 0);
	redrawRect("y", 400);
	redrawRect("z", 800);
}

function redrawRect(propertyName, yOffset) {
	var rect = chart.selectAll("rect." + propertyName)
			.data(data, function(d) { return d.time; });

	rect.enter().insert("rect", "line")
			.attr("class", propertyName)
			.attr("x", function(d, i) { return x(d.time); })
			.attr("y", function(d) { return h - y(d[propertyName]) - 0.5 + yOffset; })
			.attr("width", 2)
			.attr("height", function(d) { return y(d[propertyName]); });

	rect
			.attr("x", function(d, i) { return x(d.time); });
	
	rect.exit()
			.remove();
}

