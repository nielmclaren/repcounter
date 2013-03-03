var express = require('express');

var app = express.createServer(express.logger()); 
var appUrl = 'http://192.168.1.139:3000';

var net = require('net');

app.use(express.static(__dirname + '/public'));

app.get('/js/common.js', function(request, response) {
    response.writeHead(200, {'Content-Type': 'text/javascript'});
    response.write("var appUrl = '" + appUrl + "';\n"
        + "var socket = io.connect(appUrl);\n");
    response.end();
});

app.get('/trebek', function(request, response) {
    response.sendfile('public/trebek.html');
});

app.get('/screen', function(request, response) {
    response.sendfile('public/screen.html');
});

app.get('/player', function(request, response) {
    response.sendfile('public/player.html');
});

var io = require('socket.io').listen(app);
var trebek = null;
var screens = [];
var players = [];
var clients = {};
var acceptingResponses = false;

io.sockets.on('connection', function (socket) {
    // Find out who connecting client is.
    socket.emit('whoIs', "");

    socket.on('iAm', function(data) {
        console.log("the connecting client is a: " + data);
        switch (data) {
            case 'trebek':
                if (trebek) {
                    console.log('Alex is already here.');
                }
                else {
                    trebek = {id: socket.id};
                    clients[socket.id] = socket;
                }
                break;

            case 'screen':
                screens.push({id: socket.id});
                clients[socket.id] = socket;
                socket.emit('playerList', players);
                break;

            case 'player':
                // Wait for join before adding the player.
                break;

            default:
                console.log('Got a client of unknown type.');
        }
    });

    socket.on('join', function(data) {
        console.log('join');
        // FIXME: Make sure player names are unique.

        data.id = socket.id;
        clients[socket.id] = socket;
        players.push(data);

        // A player has joined so notify all screens.
        for (var i = 0; i < screens.length; i++) {
            clients[screens[i].id].emit('playerJoin', data);
        }

        socket.emit('joined', data);
    });

    socket.on('clue', function(data) {
        console.log('clue');
        screens.forEach(function(screen, i) {
            console.log('forEach', i, screen);
            clients[screen.id].emit('clue');
        });
        players.forEach(function(player, i) {
            clients[player.id].emit('clue');
        });
    });

    // Trebek is ready to accept responses.
    socket.on('response', function(data) {
        console.log('response');
        acceptingResponses = true;
        screens.forEach(function(screen, i) {
            clients[screen.id].emit('response');
        });
        players.forEach(function(player, i) {
            clients[player.id].emit('response');
        });
    });

    // Got a response from one of the players.
    socket.on('respond', function(player) {
        console.log('respond');
        acceptingResponses = false;
        clients[trebek.id].emit('respond', player);
        screens.forEach(function(screen, i) {
            clients[screen.id].emit('respond', player);
        });
        players.forEach(function(player, i) {
            clients[player.id].emit('respond', player);
        });
    });

    socket.on('disconnect', function (data) {
        console.log('disconnect');
        if (trebek && socket.id === trebek.id) {
            trebek = null;
        }

        for (var i = 0; i < screens.length; i++) {
            if (socket.id === screens[i].id) {
                screens.remove(i);
                delete clients[socket.id];
                break;
            }
        }

        for (var i = 0; i < players.length; i++) {
            if (socket.id === players[i].id) {
                // A player has left so notify all screens.
                for (var j = 0; j < screens.length; j++) {
                    clients[screens[j].id].emit('playerLeave', players[i]);
                }

                delete clients[socket.id];
                players.remove(i);
                break;
            }
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
