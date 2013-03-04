	
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
		data.push({
			time: time,
			value: adx
		});
	});

	socket.on('repDetected', function(time) {
		console.log(time);
	});

	setInterval(redraw, 100);

	var now = (new Date()).getTime();

	y = d3.scale.linear()
			.domain([-1, 1])
			.rangeRound([0, h]);
		
	chart = d3.select("body").append("svg")
			.attr("class", "chart")
			.attr("width", 800)
			.attr("height", 100);
});

function redraw() {
	var before = (new Date()).getTime() - ago;
	data = data.filter(function(d) { return d.time > before; });

	var now = (new Date()).getTime();
	x = d3.scale.linear()
			.domain([now - ago, now])
			.range([0, 800]);

	var rect = chart.selectAll("rect")
			.data(data, function(d) { return d.time; });

  rect.enter().insert("rect", "line")
      .attr("x", function(d, i) { return x(d.time); })
      .attr("y", function(d) { return h - y(d.value) - 0.5; })
      .attr("width", 2)
      .attr("height", function(d) { return y(d.value); });

	rect
      .attr("x", function(d, i) { return x(d.time); });
	
	rect.exit()
			.remove();
}
