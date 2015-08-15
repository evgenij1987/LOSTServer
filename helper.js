/**
 * Created by evgenijavstein on 14/08/15.
 */
exports.insertFileIndex=function (audioFeatures, playedFileIndex) {
    audioFeatures.header.attributes.unshift({
        "weight": 1,
        "name": "index",
        "class": false,
        "type": "string"
    });
    if((playedFileIndex))
        audioFeatures.data[0].values.unshift(playedFileIndex+"");
    else
        audioFeatures.data[0].values.unshift("?");
}

exports.insertLables=function(audioFeatures,files){

    var attributes=audioFeatures.header.attributes;
    for(var i= 0; i<attributes.length;i++){
        if(attributes[i].name=="filename"){
            attributes[i].type="nominal";
            attributes[i].labels=files;

        }

    }

}

