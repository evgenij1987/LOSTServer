package com.example.comsys.recommend;

import com.example.comsys.datagenerator.*;
import com.example.comsys.learn.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.JSONLoader;

/**
 * Created by Alexandra Wörner on 19.07.15.
 */
public class Recommend {

	static String dir = "../learning_data/";
	
    // Load instances to be classified
    public static void main(String[] args) {

        JsonObject obj;
        String userID;
        String data;

        // Read from standard input
        if(args.length < 1) {

            try{
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String input, userString;

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
        InstanceClassifier ic = new InstanceClassifier(dir + "model/" + userID + ".model");
        Instances instances;


        // Load instances
        instances = ModelBuilder.loadNewInstances(data);
        instances = Weka_ManageInstances.attributeSelection(instances, "1");	// remove song name from classification

        // convert string to nonimal attributes because classifier (Random Forest) can only handle numeric and nominal data
        //instances = Weka_ManageInstances.convertStringAttributes(instances, "27");

        // scale and discretize numeric values
        Weka_ManageInstances.scaleNumericAttributes(instances);
        //Weka_ManageInstances.normalizeNumericAttributes(instances);
        //instances = Weka_ManageInstances.discretizeNumericAttributes(instances, "2-26,28-33,36-" + Integer.toString(instances.numAttributes()));

        // Set class attribute
        instances.setClassIndex(0);

        /*Classify instance */

        // For comparison if label was chosen correct (first correct/one of top 5 correct)
        TreeMap<String,Integer> t = new TreeMap<String, Integer>(Collections.reverseOrder());

       // int correctlyClassified = 0;
       // int correctlyClassified5 = 0;
       // String oldClass;


        // Reclassify all instances
        for(Instance i : instances) {
            //oldClass = instances.classAttribute().value((int) i.classValue());
            
            // Remove class label and music features
            // TODO remove later, instances come with missing values
            /*for(int j = 1; j < 20; j++) {
                i.setMissing(j);
            }*/

            // Alter some values
            /*for (int j = 1; j < instances.numAttributes(); j++) {
                i.setValue(j, i.value(j) * new Random().nextFloat() );
            }*/

            s = ic.getInstanceClass(i, t);
            System.out.println(s+"\n");

            /********** Debugging/Testing ****************/
            // Check if one of top 5 labels is correct
        /*    for(int j = 0; j < 5; j++) {
            	JsonObject obj = new JsonParser().parse(s).getAsJsonObject();
                JsonArray probs = obj.getAsJsonArray("songs");
                String newClass =  probs.get(j).getAsJsonObject().get("fileindex").toString();

                correctlyClassified5 += oldClass.equals(newClass.replace("\"", "")) ? 1 : 0;
                if(j == 0) {correctlyClassified += oldClass.equals(newClass.replace("\"", "")) ? 1 : 0;}
            }*/
        }
        //System.out.println(correctlyClassified);
        //System.out.println(correctlyClassified5);

        // All labels with frequency
        /*String st = "";
        for(Map.Entry<String, Integer> entry : t.entrySet()) {
            System.out.print(entry + ", ");
           // st += entry + ", ";
        }*/
        //DataGenerator.saveToFile(st + "\n","res/log.txt", true);

        //DataGenerator.saveToFile(Integer.toString(correctlyClassified) + "\n", "res/stats.txt", true);
        //DataGenerator.saveToFile(Integer.toString(correctlyClassified5)+ "\n","res/stats5.txt", true);


        //return s;
    }

}
