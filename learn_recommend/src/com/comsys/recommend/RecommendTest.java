package com.comsys.recommend;

import com.comsys.datagenerator.*;
import com.comsys.learn.*;

import json.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by Alexandra Wörner on 19.07.15.
 */
public class RecommendTest {

	static String modelPath = "../learning_data/";
	static String audioFeaturesPath = "../audio_features/json";
	
    // Load instances to be classified
    public static void main(String[] args) {

        JsonObject obj;
        String userID;
        String data;

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
                    	input = DataGenerator.generateRecommendationData(100, false);
                    }

                    obj = new JsonParser().parse(input).getAsJsonObject();
                    userID = obj.getAsJsonObject("user").get("userid").toString().replace("\"","");;
                    data = obj.getAsJsonObject("toRecommend").toString();

                    runRecommendationPhase(userID, data);
                }

            }catch(Exception io){
                io.printStackTrace();
            }

        } else {
            // We got arguments from command line
            obj = new JsonParser().parse(args[0]).getAsJsonObject();
            userID = obj.getAsJsonObject("user").get("userid").toString().replace("\"", "");;
            data = obj.getAsJsonObject("toRecommend").toString();

            try {
                runRecommendationPhase(userID, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void runRecommendationPhase(String userID, String data) throws Exception{
        String s = "";
        InstanceClassifier ic = new InstanceClassifier(modelPath + "model/" + userID + ".model", audioFeaturesPath);
        Instances instances;

        // Load instances
        instances = ModelBuilder.loadNewInstances(data);
        instances = Weka_ManageInstances.attributeSelection(instances, "1");	// remove song name from classification

        // scale numeric values
        Weka_ManageInstances.scaleNumericAttributes(instances);
        
        // Set class attribute
        instances.setClassIndex(0);

        int correctlyClassified = 0;
        int correctlyClassified5 = 0;
        String oldClass;


        // Classify all instances
        for(Instance i : instances) {
            oldClass = instances.classAttribute().value((int) i.classValue());
            
            // Alter some values
            /*for (int j = 1; j < instances.numAttributes(); j++) {
                i.setValue(j, i.value(j) * new Random().nextFloat() );
            }*/

            s = ic.getSongRecommendation(i);
            System.out.println(s+"\n");

            // Check if one of top 5 labels is correct
            for(int j = 0; j < 5; j++) {
            	JsonObject obj = new JsonParser().parse(s).getAsJsonObject();
                JsonArray probs = obj.getAsJsonArray("songs");
                String newClass =  probs.get(j).getAsJsonObject().get("fileindex").toString();

                correctlyClassified5 += oldClass.equals(newClass.replace("\"", "")) ? 1 : 0;
                if(j == 0) {correctlyClassified += oldClass.equals(newClass.replace("\"", "")) ? 1 : 0;}
            }
            
            List<String> similarJSONFilesForAudio = NearestNeighbourOnJSONResolver.getSimilarJSONFilesForAudio(new File(
					"../../LOSTServer/audio_features/json/01. Avril Lavigne Losing Grip.json"), "../../LOSTServer/audio_features/json/");
			for (String file : similarJSONFilesForAudio) {
				System.out.println(file);
			}
        }
        System.out.println(correctlyClassified);
        System.out.println(correctlyClassified5);


        //DataGenerator.saveToFile(Integer.toString(correctlyClassified) + "\n", "res/stats.txt", true);
        //DataGenerator.saveToFile(Integer.toString(correctlyClassified5)+ "\n","res/stats5.txt", true);

    }

}
