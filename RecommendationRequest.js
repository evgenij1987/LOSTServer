/**
 * Created by evgenijavstein on 02/08/15.
 */
// Constructor
function RecommendationRequest(user,contextFeatures, audioFeaturesDummy) {
    // always initialize all instance properties



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