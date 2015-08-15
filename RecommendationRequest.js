/**
 * Created by evgenijavstein on 02/08/15.
 */
// Constructor

var helper=require("./helper");

function RecommendationRequest(user,contextFeatures, audioFeaturesDummy,files) {
    // always initialize all instance properties

    //just to be conform the data type expected by ml module
    helper.insertFileIndex(audioFeaturesDummy);
    helper.insertLables(audioFeaturesDummy,files);


    //merge audioFeatures & contextFeatures
    var concatenatedAttributes= audioFeaturesDummy.header.attributes.concat(contextFeatures.header.attributes);
    var concatenatedData=audioFeaturesDummy.data;
    concatenatedData[0].values=concatenatedData[0].values.concat(contextFeatures.data[0].values)


    this.user=user;

    this.toRecommend={
        header:{
            relation: 'instance',
            attributes:concatenatedAttributes
        },
        data:concatenatedData
    };


}

// export the class
module.exports = RecommendationRequest;