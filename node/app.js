var express = require('express');

var app = express.createServer(express.logger()); 

var net = require('net');

app.use(express.static(__dirname + '/public'));

var io = require('socket.io').listen(app);

var viewer;
var reader;

io.sockets.on('connection', function (socket) {
	// Find out who connecting client is.
	socket.emit('whoIs', "");

	socket.on('iAm', function(data) {
		console.log("the connecting client is: " + data);
		switch (data) {
			case 'viewer':
				viewer = socket;
				break;

			case 'reader':
				reader = socket;
				socket.on('sensorChanged', function(socket, x, y, z, adx, ady, adz) {
					if (viewer) {
						viewer.emit('sensorChanged', x, y, z, adx, ady, adz);
					}
				});
				break;

			default:
				console.log('Got a client of unknown type. type="' + data + '"');
		}
	});
});

if (!Array.prototype.remove) {
	Array.prototype.remove = function(from, to) {
		var rest = this.slice((to || from) + 1 || this.length);
		this.length = from < 0 ? this.length + from : from;
		return this.push.apply(this, rest);
	};
}

if (!Array.prototype.forEach) {
	Array.prototype.forEach = function(fun /*, thisp*/) {
		var len = this.length;
		if (typeof fun != "function")
			throw new TypeError();

		var thisp = arguments[1];
		for (var i = 0; i < len; i++) {
			if (i in this)
				fun.call(thisp, this[i], i, this);
		}
	};
}

var port = process.env.PORT || 3000;

app.listen(port, function() {
	console.log("Listening on " + port);
});

