/**
 * Created by evgenijavstein on 13/07/15.
 */
var express = require('express'),
    bodyParser = require('body-parser');




var PORT = 3000;
var app = express();
var mainController=require('./MainController');
mainController.init();

// parse application/x-www-form-urlencoded
app.use(bodyParser.urlencoded({extended: false}))

// parse application/json
app.use(bodyParser.json())


app.get('/api/list/', mainController.listAudioTracks);


//API to stream an audio file via http
app.get('/api/play/:id', mainController.playAudioTrack);


//API to provide feedback about context & music choice
app.post('/api/learn',mainController.learnFromSongAndContext);

//API to stream audio file selected by ML lib
app.post('/api/list/next',mainController.listRecommendedAudioTracks);


var server = app.listen(PORT, function () {
    var host = server.address().address;
    var port = server.address().port;

    console.log('Media server app listening at http://%s:%s', host, port);
});

