/**
 * Created by evgenijavstein on 14/07/15.
 */
AudioTrack = require('./AudioTrack'),
    fs = require('fs'),
    path = require('path');

var wekaMLProcess;
var debugMode = true;

exports.init = function (mode) {

    debugMode = mode;
    wekaMLProcess = runMLProcess();

};

/**
 * Files from /mp3 folder are listed in a JSON reponse
 * @param req
 * @param res
 */
exports.listAudioTracks = function (req, res) {
    fs.readdir("./mp3", function (err, files) {

        var audioTracks = new Array();
        var audioTrack;
        for (var i = 0; i < files.length; i++) {

            audioTrack = new AudioTrack(files[i], i);
            audioTracks.push(audioTrack);
        }
        res.send(JSON.stringify(audioTracks));
    });
}
/**
 * AudioTrack is selected via filename or file index and played via piped via
 * http. Playback is possible as progressive download.
 * @param req
 * @param res
 */
exports.playAudioTrack = function (req, res) {
    var audioTrack = req.params.id;
    var filePath;
    if (isNaN(audioTrack)) {
        filePath = path.join(__dirname, 'mp3/' + audioTrack);
        if (filePath)
            streamFile(filePath, res);

    } else {
        fs.readdir("./mp3", function (err, files) {

            var audioTrackName = files[audioTrack];
            if (audioTrackName) {
                filePath = path.join(__dirname, 'mp3/' + audioTrackName);

                streamFile(filePath, res);


            } else {
                res.status(400).send({message: "no such file"});
            }

        });
    }

}

/**
 * Methods accepts context information coupled with audio track name
 * @param req
 * @param res
 */
exports.learnFromSongAndContext = function (req, res) {

    /* */
    var learnCommand = "Learn this!";
    writeToWekaMLProcess(learnCommand,
        function (lernresponse) {

            res.sendStatus(200);

        }, function (error) {

            console.log('err data: ' + error);

        }
    );
}
/**
 * Recommended songs are retrieved from ML lib processed
 * and send back same way as in listAudioTracks() method
 * @param req
 * @param res
 */
exports.listRecommendedAudioTracks = function (req, res) {

    /* JUST FOR TESTING */
    var audioTrack;
    var audioTracks = new Array();
    for (var i = 0; i < 10; i++) {
        audioTrack = new AudioTrack("name" + i, i);
        audioTracks.push(audioTrack);
    }
    var getRecommendedItemsCommand = JSON.stringify(audioTracks);


    if (!debugMode) {//PUT REAL COMMAND TO LEARNING LIB IN THIS STRING
        getRecommendedItemsCommand = "real commmand comes here!";
    }

    writeToWekaMLProcess(getRecommendedItemsCommand,
        function (data) {
            handleRecommendation(data, res)
        },

        function (error) {
            console.log('err data: ' + error);
        });
}

/**
 * Process here response from ML lib and send back as simple list as in listAudioTracks()
 * @param data
 */
function handleRecommendation(data, res) {
    var recommendation = JSON.parse(data);

    //PROCESS RECOMMENDATION HERE
    res.send(recommendation);

}

/**
 * Method passes string via stdin to lerning lib process
 * @param command
 * @param stdoutcallback reponse from lerning lib
 * @param stderrcallback error occured
 */
function writeToWekaMLProcess(command, stdoutcallback, stderrcallback) {
    wekaMLProcess.stdout.removeAllListeners('data');
    wekaMLProcess.stderr.removeAllListeners('data');
    wekaMLProcess.stdout.on('data', stdoutcallback);
    wekaMLProcess.stderr.on('data', stderrcallback);
    wekaMLProcess.stdin.write(command);

}


/**
 * Starts  new ML process, we will communication with it via writeToWekaMLProcess() method
 * @returns {*}
 */
function runMLProcess() {

    var spawn = require('child_process').spawn;

    var command = "cat";

    if (!debugMode) {
        //run binary command
        command = "./mlmodule/mlmodule.jar";
    }

    //process is started only once here and used via pipe again and again
    var child = spawn(command, []);
    child.stdout.on('data',
        function (buffer) {
            console.log(buffer.toString("utf-8"))
        }
    );
    child.stderr.on('data',
        function (data) {
            console.log('err data: ' + data);
        }
    );
    return child;
}

/**
 * Streams a file via http, progressive download/playback
 * @param filePath
 * @param res
 */
function streamFile(filePath, res) {

    fs.exists(filePath, function (exists) {
        if (exists) {
            var stat = fs.statSync(filePath);

            res.writeHead(200, {
                'Content-Type': 'audio/mpeg',
                'Content-Length': stat.size
            });

            var readStream = fs.createReadStream(filePath);
            readStream.pipe(res);
        } else {
            res.status(400).send({message: "no such file"});
        }
    });

}