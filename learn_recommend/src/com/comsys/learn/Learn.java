package com.comsys.learn;

import com.comsys.datagenerator.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
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
                String input;

                // Input has form {'userid' : <userid>} {'header': {'relation' : <relation>, 'attributes' : <attributes>, 'data' <data>}
                // Split userID, feedback and rest of input
                while((input=br.readLine())!=null) {

                    if (input.equals("generate")) {
                        // for quick testing
                        input = DataGenerator.generateLearnData(100, false);
                    }

                    parseJSON(input);
                    runLearningPhase();
                }

            }catch(Exception io){
                io.printStackTrace();
            }

        } else {
            // We got arguments from command line
            
            try {
            	parseJSON(args[0]);
                runLearningPhase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Lets the model learn from the context sent by the user
     * @throws Exception
     */
    private static void runLearningPhase() throws Exception{
        // Build model (and store it for future use)
        //File f = new File(dir + userID + ".json");
        //f.delete();

    	if(!skipped) {
    		ModelBuilder mb = new ModelBuilder("../learning_data/" + userID, dir + "model/" + userID + ".model", data);
    		mb.buildModel();
    		//mb.evaluate(true);
    	}

        System.out.println("{ \"message\" : \"ok\"}");
    }
    
    /**
     * Gets all the necessary information (user id, context, skipped song) from the JSON input string
     * @param input	String in JSON format
     */
    private static void parseJSON(String input){
    	obj = new JsonParser().parse(input).getAsJsonObject();
        userID = obj.getAsJsonObject("user").get("userid").toString().replace("\"","");
        data = obj.getAsJsonObject("toLearn").toString();
        skipped = !Boolean.parseBoolean(obj.get("feedback").toString());
    }
}
