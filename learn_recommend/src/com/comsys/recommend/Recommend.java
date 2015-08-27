package com.comsys.recommend;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import weka.core.Instance;
import weka.core.Instances;

import com.comsys.datagenerator.DataGenerator;
import com.comsys.learn.ModelBuilder;
import com.comsys.learn.Weka_ManageInstances;
import com.comsys.recommend.InstanceClassifier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Created by Alexandra Wörner on 19.07.15.
 */
public class Recommend {

	static String modelPath = "../learning_data/";
	static String audioFeaturesPath = "../audio_features/json/";
	
    // Load instances to be classified
    public static void main(String[] args) {

    	JsonObject obj;
        String userID;
        String context;

        // Read from standard input
        if(args.length < 1) {

            try{
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String input;

                // Input has form {'userid' : <userid>} {'header': {'relation' : <relation>, 'attributes' : <attributes>, 'data' <data>}
                // Split userID and rest of input
                while((input=br.readLine())!=null){
	
                    if(input.equals("generate")) {
                        // for quick testing
                    	input = DataGenerator.generateRecommendationData(1, false);
                    }

                    obj = new JsonParser().parse(input).getAsJsonObject();
                    userID = obj.getAsJsonObject("user").get("userid").toString().replace("\"", "");;
                    context = obj.getAsJsonObject("toRecommend").toString();
                    
                    runRecommendationPhase(userID, context);
                }

            }catch(Exception io){
                io.printStackTrace();
            }

        } else {
            // We got arguments from command line
        	obj = new JsonParser().parse(args[0]).getAsJsonObject();
            userID = obj.getAsJsonObject("user").get("userid").toString().replace("\"", "");;
            context = obj.getAsJsonObject("toRecommend").toString();

            try {
                runRecommendationPhase(userID, context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Gets the user's context and computes a list of matching songs which can be played next
     * @param userID	The id of the user to retrieve the right learning model
     * @param context	The user's context
     * @throws Exception
     */
    private static void runRecommendationPhase(String userID, String context) throws Exception{
        String s = "";
        InstanceClassifier ic = new InstanceClassifier(modelPath + "model/" + userID + ".model", audioFeaturesPath);
        Instances instances;

        // Load instances
        instances = ModelBuilder.loadNewInstances(context);
        instances = Weka_ManageInstances.attributeSelection(instances, "1,3-21");	// remove song name and song features for classification
        
        // Set class attribute
        instances.setClassIndex(0);

        // Classify all instances
        for(Instance i : instances) {

            s = ic.getSongRecommendation(i);
            System.out.println(s+"\n");

        }
       
    }
}
