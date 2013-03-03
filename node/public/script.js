
$(document).ready(function() {
	var socket = io.connect('http://192.168.1.102:3000');	
	socket.on('whoIs', function(data) {
		socket.emit('iAm', 'viewer');
		$('#output').text($('#output').text() + "\nwhoIs? iAm.");
	});

	$('body').append('<textarea id="output"></textarea>');
	$('#output').text('Test.');
});
