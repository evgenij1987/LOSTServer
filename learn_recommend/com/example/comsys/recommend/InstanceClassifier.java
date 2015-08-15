package com.example.comsys.recommend;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import weka.classifiers.Classifier;
import weka.core.Instance;

/**
 * Created by Alexandra Wörner on 13.07.15.
 */
public class InstanceClassifier {

    private Classifier classifier;
    private int topN = 5;

    public InstanceClassifier(String filepath){

        try {
            classifier = (Classifier) weka.core.SerializationHelper.read(filepath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getInstanceClass(Instance instance, TreeMap<String,Integer> t) throws Exception {
        // Get probabilities and sort classes according to them
        double[] probabilities = classifier.distributionForInstance(instance);
        TreeMap<Double,String> tmap = new TreeMap<Double, String>(Collections.reverseOrder());
        for(int j = 0; j < probabilities.length; j++) {
            tmap.put(probabilities[j], instance.classAttribute().value(j));
        }

        int v = t.get(tmap.firstEntry().getValue()) != null ? t.get(tmap.firstEntry().getValue()) : 0;
        t.put(tmap.firstEntry().getValue(), v + 1);

        // Print first N classes
        int j = 0;
        for(Map.Entry<Double,String> entry : tmap.entrySet()) {
            if(j >= topN) break;
            //System.out.print(entry + ", ");
            j++;
        }

        //System.out.println("");

        return recommendationToJSON(tmap);
    }

    private String recommendationToJSON(TreeMap<Double,String> recommendation) {
        String s = "{\"songs\" : [";

        int j = 0;
        for(Map.Entry<Double,String> entry : recommendation.entrySet()) {
            if(j >= topN) break;
            s += "{\"index\":" + j + ", \"fileindex\":\""+ entry.getValue() + "\"},";
            j++;
        }
        s = s.substring(0, s.length()-1);
        s += "]}";

        return s;
    }

}
