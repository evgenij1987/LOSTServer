package com.comsys.recommend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import json.NearestNeighbourOnJSONResolver;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.JSONLoader;

/**
 * Created by Alexandra Wörner on 13.07.15.
 */
public class InstanceClassifier {

    private Classifier classifier;
    private String audioFeaturesPath;
    private int topN = 10;
    private String lastListenedSong;

    /**
     * Constructor
     * @param filepath				Path to the model stored for the user
     * @param audioFeaturesPath		Path to the JSON files containing the music features
     */
    public InstanceClassifier(String filepath, String audioFeaturesPath){

        try {
        	this.audioFeaturesPath = audioFeaturesPath;
            classifier = (Classifier) weka.core.SerializationHelper.read(filepath);
            
            // Load saved learning data from JSON file
            File f = new File(filepath.replace("model/", "").replace(".model",".json"));
         	JSONLoader loader = new JSONLoader();
         	loader.setSource(f);
         	Instances instances = loader.getDataSet();
         	instances.setClassIndex(1);
         	
         	// Song which is compared to the recommended ones to avoid to much of a rift from the previous song(s)
         	lastListenedSong = instances.lastInstance().classAttribute().value((int) instances.lastInstance().classValue());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get song recommendation based on the given context and on audio features
     * @param context	The context the user is currently in
     * @return A list of songs in order of their suitability to the current context and 
     * 		   to the audio features of the latest song played
     * @throws Exception
     */
    protected String getSongRecommendation(Instance context) throws Exception {
        // Get probabilities and sort classes according to them
        double[] probabilities = classifier.distributionForInstance(context);
        TreeMap<Double,String> tmap = new TreeMap<Double, String>(Collections.reverseOrder());
        for(int j = 0; j < probabilities.length; j++) {
            tmap.put(probabilities[j], context.classAttribute().value(j));
        }
       
        // Compare to nearest songs based on music features
        List<String> combinedTmap = 
        		combineWithNearestNeighbourSongs(lastListenedSong, new ArrayList<String>(tmap.values()));

        return recommendationToJSON(combinedTmap);
    }

    /**
     * Puts the recommendation list in a JSON format to send back to server
     * @param recommendation	The list containing the recommended songs in order of suitability
     * @return list of recommended songs as a JSON string
     */
    private String recommendationToJSON(List<String> recommendation) {
        String s = "{\"songs\" : [";

        for(int i = 0; i < topN && i < recommendation.size(); i++) {
            s += "{\"index\":" + i + ", \"fileindex\":\""+ recommendation.get(i) + "\"},";
        }
        s = s.substring(0, s.length()-1);
        s += "]}";

        return s;
    }
    
    /**
     * Compare the list of recommended songs based on the context to the song the user listened to last and resort recommendation list
     * @param lastListenedSong	The song the user listened to when the context was sent
     * @param songsFromContextList	Similarity of songs based only on the context
     * @throws IOException
     */
    private List<String> combineWithNearestNeighbourSongs(String lastListenedSong, List<String> songsFromContextList) throws IOException{

    	// Get most similar songs based on audio features
        List<String> similarSongsForAudio = NearestNeighbourOnJSONResolver.getSimilarJSONFilesForAudio(
        		new File(audioFeaturesPath + lastListenedSong.replace(".mp3", ".json")), audioFeaturesPath);
        
        TreeMap<Double,String> combinedTmap = new TreeMap<Double, String>();
        for(int i = 0; i < songsFromContextList.size(); i++) {
            //find song in neighbour list
        	int index = similarSongsForAudio.indexOf(songsFromContextList.get(i).replace(".mp3", ".json"));
        	
        	if(index != -1) {
        		// insert it at position (context + audio)/2
        		combinedTmap.put((index + i)/2.0, songsFromContextList.get(i));
        	} else {
        		// if not in the list of nearest song neighbours, put penalty on the song (number of songs)
        		combinedTmap.put((double) (i + songsFromContextList.size()), songsFromContextList.get(i));
        	} 	
        	
        }
        
        List <String> recommendationList = new ArrayList<String>(combinedTmap.values());
        
        // Remove the song which the user listened to most recently
        //recommendationList.remove(lastListenedSong);

        // Fill list with nearest songs in case there are not recommended enough songs from the context
        for(int i = 0; i < similarSongsForAudio.size() && recommendationList.size() < topN; i++) {
        	String nextSong = similarSongsForAudio.get(i).replace(".json", ".mp3");
        	if(recommendationList.indexOf(nextSong) == -1) {
        		recommendationList.add(nextSong);
        	}
        }
		return recommendationList;
		
	}

}
