/**
 * Created by evgenijavstein on 14/07/15.
 */
AudioTrack = require('./AudioTrack'),
    fs = require('fs'),
    path = require('path'),
    LearnObject = require('./LearnObject'),
    RecommendationRequest = require('./RecommendationRequest');

var dummyAudioFeatures = require('./audio_analysis_module/dummyAudioFeatures.json');
var wekaMLProcess;
var debugMode = true;
//const ONES_WITHOUT_DECIMAL_DELIMITER_REG_EXP = /(:\s*)1(\s*}|,)/g;


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
    fs.readdir("./mp3/tracks", function (err, files) {

        var audioTracks = getAudioTracks(files, 0, files.length);
        res.send(JSON.stringify(audioTracks));
    });
}

/**
 * Reads an interval of files into AudioTrack objects
 * @param files
 * @param from
 * @param to
 * @returns {Array}
 */
function getAudioTracks(files, from, to) {
    var audioTracks = new Array();
    var audioTrack;
    for (var i = from; i < to; i++) {
        if (!isUnixHiddenPath(files[i])) {
            audioTrack = new AudioTrack(files[i], i);
            audioTracks.push(audioTrack);
        }

    }
    return audioTracks;
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
        filePath = path.join(__dirname, 'mp3/tracks/' + audioTrack);
        if (filePath)
            streamFile(filePath, res);

    } else {
        fs.readdir("./mp3/tracks", function (err, files) {

            var audioTrackName = files[audioTrack];
            if (audioTrackName) {
                filePath = path.join(__dirname, 'mp3/tracks/' + audioTrackName);

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

    var learnCommand = "Learn this!";

    var contextFeatures = req.body.context;
    var playedFileIndex = req.body.fileIndex;
    var user = req.body.user;
    var feedBack = req.body.feedBack;

    var jsonFeaturesFilePath;
    fs.readdir("./audio_features/json", function (err, files) {

        var jsonFileName = files[playedFileIndex];
        jsonFeaturesFilePath = path.join(__dirname, 'audio_features/json/' + jsonFileName);


        fs.readFile(jsonFeaturesFilePath, 'utf8', function (err, data) {

            if (err)
                res.status(500).send(err);

            var audioFeatures = JSON.parse(data);

            initAudioFeaturesIfEmpty(audioFeatures, playedFileIndex);

            learnCommand = JSON.stringify(new LearnObject(user, feedBack, audioFeatures, contextFeatures));
            //console.log(learnCommand+"\n \n \n");
            //Replace 1 by 1.0 as expected by learning module

            //learnCommand = learnCommand.replace(ONES_WITHOUT_DECIMAL_DELIMITER_REG_EXP, "$11.0$2");//1.0 in between variables $1 and $2
            writeToWekaMLProcess(learnCommand,
                function (lernresponse) {

                    console.log(lernresponse.toString("utf-8"));
                    res.sendStatus(200);

                }, function (error) {

                    console.log('err data: ' + error);

                }
            );

        })


    });


}


/**
 * Recommended songs are retrieved from ML lib processed
 * and send back same way as in listAudioTracks() method
 * @param req
 * @param res
 */
exports.listRecommendedAudioTracks = function (req, res) {

    var contextFeatures = req.body.context;
    var user = req.body.user;

    console.log(JSON.stringify(contextFeatures));
    var audioFeaturesDummy = clone(dummyAudioFeatures);//we don't want to change it

    //just to be conform the data type expected by ml module
    insertFileIndex(audioFeaturesDummy);
    var recommendationRequestString = JSON.stringify(new RecommendationRequest(user, contextFeatures, audioFeaturesDummy));
    recommendationRequestString=removeSlashes(recommendationRequestString);

    console.log(recommendationRequestString);
    if (!debugMode) {//PUT REAL COMMAND TO LEARNING LIB IN THIS STRING
        getRecommendedItemsCommand = "real commmand comes here!";
    }

    writeToWekaMLProcess(recommendationRequestString,
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
    //console.log(JSON.stringify(recommendation));
    //SEND RECOMMENDATIONS INSTEAD HERE
    fs.readdir("./mp3/tracks", function (err, files) {

        var audioTracks = getAudioTracks(files, 0, files.length);
        res.send(JSON.stringify(audioTracks));
    });

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

/**
 * For some mp3 files analysis is not possible, fill ? for instead of real numbers
 * as expected by learning module. If data is present obj is kept unchanged
 * @param audioFeatures
 */
function initAudioFeaturesIfEmpty(audioFeatures, playedFileIndex) {
    var audioFeaturesDummy = clone(dummyAudioFeatures);
    if (audioFeatures.data[0].values.length == 1) {

        audioFeatures.header.attributes = audioFeaturesDummy.header.attributes;
        //1 because 0 is always avaliable as file name
        for (var i = 1; i < audioFeatures.header.attributes.length; i++) {
            audioFeatures.data[0].values.push('?');
        }

    }

    //append fileIndex to audioFeatures too, so you dont need to qery file system, by allready getting this back from ML module
    insertFileIndex(audioFeatures, playedFileIndex);
}

/**
 * Checks whether a path starts with or contains a hidden file or a folder.
 * @param {string} source - The path of the file that needs to be validated.
 * returns {boolean} - `true` if the source is blacklisted and otherwise `false`.
 */
var isUnixHiddenPath = function (path) {
    return (/(^|.\/)\.+[^\/\.]/g).test(path);
};

function clone(a) {
    return JSON.parse(JSON.stringify(a));
}
/**
 * Helper method to add file index
 * When api/learn is called we receive the index
 * When apu/list/next is called we pass the index to ml module
 * @param audioFeatures
 * @param playedFileIndex
 */
function insertFileIndex(audioFeatures, playedFileIndex) {
    audioFeatures.header.attributes.unshift({
        "weight": 1,
        "name": "index",
        "class": false,
        "type": "string"
    });
    if (playedFileIndex)
        audioFeatures.data[0].values.unshift(playedFileIndex);
    else
        audioFeatures.data[0].values.unshift("?");
}


function removeSlashes(jsonString){
    var replaced=jsonString.replace(/"weight":1/g,"\"weight\":1.0");
    replaced=replaced.replace(/"\//g, "").replace(/\/"/g, "");
    return replaced;
}