package com.example.comsys.learn;

import com.example.comsys.datagenerator.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;


public class Learn {
	static String dir = "../learning_data/";

    static private JsonObject obj;
    static private String userID;
    static private String data;
    static private boolean skipped;
	
    public static void main(String[] args) {
        
        // Read from standard input
        if(args.length < 1) {

            try{
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String input, userString;

                // Input has form {'userid' : <userid>} {'header': {'relation' : <relation>, 'attributes' : <attributes>, 'data' <data>}
                // Split userID and rest of input
                while((input=br.readLine())!=null) {

                    if (input.equals("generate")) {
                        // for quick testing
                        input = DataGenerator.generateLearnData(100, false);
                    }

                    parseJSON(input);
                    runLearningPhase(userID, data, skipped);
                }

            }catch(Exception io){
                io.printStackTrace();
            }

        } else {
            // We got arguments from command line
            parseJSON(args[0]);

            try {
                runLearningPhase(userID, data, skipped);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void runLearningPhase(String userID, String data, Boolean skipped) throws Exception{
        // Build model (and store it for future use)
        //File f = new File(dir + userID + ".json");
        //f.delete();

        // Can decide to keep file with instances and add new ones (for large data sets because generation takes time)
        /*if(Boolean.valueOf(args[0])) { System.out.print(0);*///f.delete(); //}

    	if(!skipped) {
    		ModelBuilder mb = new ModelBuilder("../learning_data/" + userID, dir + "model/" + userID + ".model", data);
    		mb.buildModel();
    		//mb.evaluate(true);
    	}

        System.out.println("{ \"message\" : \"ok\"}");
    }
    
    /** Gets all the necessary information from the JSON input string */
    private static void parseJSON(String input){
    	obj = new JsonParser().parse(input).getAsJsonObject();
        userID = obj.getAsJsonObject("user").get("userid").toString().replace("\"","");
        data = obj.getAsJsonObject("toLearn").toString();
        skipped = !Boolean.parseBoolean(obj.get("feedback").toString());
    }
}
