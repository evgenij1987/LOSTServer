/**
 * Created by evgenijavstein on 31/07/15.
 */
/**
 * Created by evgenijavstein on 14/07/15.
 */
/**
 * Created by evgenijavstein on 01/06/15.
 */

// Constructor
function LearnObject(user, audioFeatures, contextFeatures) {
    // always initialize all instance properties




    var concatenatedAttributes= audioFeatures.header.attributes.concat(contextFeatures.header.attributes);
    var concatenatedData=audioFeatures.data;
    concatenatedData[0].values=concatenatedData[0].values.concat(contextFeatures.data[0].values)


    this.user=user;
    this.toLearn={
        header:{
            relation: 'instance',
            attributes:concatenatedAttributes
        },
        data:concatenatedData
    };


}


// export the class
module.exports = LearnObject;
