package com.comsys.learn;

import com.comsys.datagenerator.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.JSONLoader;
import weka.core.converters.JSONSaver;


public class ModelBuilder
{
	//RandomForest classifier;
	NaiveBayes classifier;
	String filepath;
	String writepath;
	Instances newInstances;
	Instances instances;
	Instances trainingInstances;
	Instances testInstances;
	Evaluation eval;

	/**
	 * Constructor
	 * @param filepath 		Path to contexts on which the model is trained
	 * @param writepath		Path to the model
	 * @param newInstances	New context information
	 */
	public ModelBuilder(String filepath, String writepath, String newInstances) {
		this.filepath = filepath;
		this.writepath = writepath;

		try {
			this.newInstances = loadNewInstances(newInstances);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the model 
	 * @throws Exception
	 */	
	protected final void buildModel() throws Exception
	{
		loadData();
		saveData();			// Save data into file on disk for next iteration

		if(instances.numInstances() < 3) { return; }		// Too few instances to build a model
		
		// Remove all music features
		instances = Weka_ManageInstances.attributeSelection(instances, "1,3-21");

		// scale and discretize numeric values
		//Weka_ManageInstances.scaleNumericAttributes(instances);
		//Weka_ManageInstances.normalizeNumericAttributes(instances);
		//instances = Weka_ManageInstances.discretizeNumericAttributes(instances, "2-26,28-33,36-" + Integer.toString(instances.numAttributes()));

		// thought numeric attributes had to be converted too (older weka version)
		//instances = Weka_ManageInstances.convertNumericAttributes(instances, "2-26,28-33,36-" + Integer.toString(instances.numAttributes()));

		// Set class attribute
		instances.setClassIndex(0);

		// Build training and test instances
		//double percent = 1.0;
		//String attributes_filter = "1,3-21";			// Remove all music features

		//testInstances	  = buildInstancesP(instances, attributes_filter, true, percent, 1.0);
		//trainingInstances = buildInstancesP(instances, attributes_filter, true, 0, percent);
		trainingInstances = instances;

		// Build model
		trainModel();

		// Serialize/store model
		File dir = new File(writepath.substring(0, writepath.lastIndexOf("/")));
		if (!dir.exists()) { dir.mkdir(); }
		weka.core.SerializationHelper.write(writepath, classifier);
	}
	
	/**
	 * Used to build an instance from some percents of an other one.
	 * It's possible to add a filter on the attributes and choose if the new Instances got or not
	 * only different following rows.
	 * 
	 * @param data
	 * 				The instances from which part will be extracted
	 * @param attributes_filter
	 * 				String which represents the attributes to remove.
	 * 				<i>"1-4"</i>  | <i>"28"</i>  | <i>"1-70,45,68-72"</i> | <i>""</i> | <i>...</i>
	 * @param differentNext
	 * 				Only different following rows or not?
	 * @param percent_start
	 * 				Percent indicating the first line of the selection.
	 * @param percent_end
	 * 				Percent indicating the last line of the selection.
	 * 				
	 * @return The new extracted and filtered Instances.
	 * 
	 * @throws Exception
	 */
	/*private static Instances buildInstancesP (Instances data, String attributes_filter, boolean differentNext, double percent_start, double percent_end) throws Exception
	{		
	 	// Security (on percent_start and percent_end)
		percent_end 	= Math.max(percent_end, 0);
		percent_start 	= Math.max(percent_start, 0);
		percent_end 	= Math.min(percent_end, 1);
		percent_start	= Math.min(percent_start, 1);
		if (percent_end < percent_start){
			double temp = percent_start;
			percent_start = percent_end;
			percent_end = temp;
		}
		// Row selection
		Instances instances = Weka_ManageInstances.percentSelection(data, percent_start, percent_end);

			// Attribute filter
		if (attributes_filter.length()>0) instances = Weka_ManageInstances.attributeSelection(instances, attributes_filter);

			// Delete rows whose next is same.
		if(differentNext) instances = Weka_ManageInstances.differentNextSelection(instances);

		return(instances);
	}*/

	/**
	 * Used to train a Naive Bayes classifier
	 * 
	 * @throws Exception
	 */
	private void trainModel() throws Exception
	{
		classifier = new NaiveBayes();
		classifier.setUseSupervisedDiscretization(true);
		classifier.buildClassifier(trainingInstances);
	}
	
	/**
	 * Used to evaluate test instances and display it.
	 * 
	 * @param displayResults
	 * 		  Print results to console
	 * 
	 * @throws Exception
	 */
	protected void evaluate (boolean displayResults) throws Exception
	{
		/*for(Instance i : testInstances) {
			// Alter values
			/*for (int j = 1; j < 55; j++) {
				//int n = (int) Math.floor(new Random().nextFloat() * (instances.numAttributes()- 1)) + 1;
				i.setValue(j, i.value(j) * new Random().nextFloat() );
            }
		}*/

		eval = new Evaluation(trainingInstances);
		eval.evaluateModel(classifier, testInstances);
		//eval.crossValidateModel(classifier, testInstances, 10, new Random(1));

		if(displayResults) { System.out.println(eval.toSummaryString(true)); }
	}

	/**
	 * Loads the old context data and combines it with the new context
	 * @throws Exception
	 */
	private void loadData() throws Exception{
		File f = new File(filepath + ".json");
		if(f.exists()) {
			// Load from JSON file
			JSONLoader loader = new JSONLoader();
			loader.setSource(f);
			instances = loader.getDataSet();
		} else {
			// First use of program for the user
			// Load header/attributes
			String s = DataGenerator.generateData(0, false, false);
			
			// Replace single quotation marks (syntax error)
			s = s.replace("\'", "\"");

			JSONLoader loader = new JSONLoader();
			loader.setSource(new ByteArrayInputStream(s.getBytes()));
			instances = loader.getDataSet();
		}


		// Merge old and new elements
		for(Instance i : newInstances) {

			double[] instanceValue = new double[instances.numAttributes()];
			for(int j = 0; j < i.numAttributes(); j++) {
				// Nominal and numeric attributes
				if(j != 0)
					instanceValue[j] = i.value(j);
				// String attributes
				else
					instanceValue[j] = instances.attribute(j).addStringValue(i.stringValue(j));

			}
			instances.add(new DenseInstance(1.0, instanceValue));
		}

	}

	/**
	 * Saves the context data in a JSON file for future use
	 * @throws Exception
	 */
	private void saveData() throws Exception {
		JSONSaver saver = new JSONSaver();
		saver.setInstances(instances);

		try {
			saver.setFile(new File(filepath + ".json"));
			saver.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the new context data sent by the user
	 * @param newInstances 	The new context data
	 * @return				Context data in format used by the library
	 * @throws IOException
	 */
	public static Instances loadNewInstances(String newInstances) throws IOException{
		// Replace single quotation marks (syntax error)
		newInstances = newInstances.replace("\'", "\"");

		// Load instances from string
		JSONLoader loader = new JSONLoader();
		loader.setSource(new ByteArrayInputStream(newInstances.getBytes()));
		return loader.getDataSet();

	}
}