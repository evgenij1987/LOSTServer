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


/**
 * <b>Weka_Use is the class to use methods provide by the library Weka.</b>
 * <p>
 * Weka_Use uses different methods of Weka_ManageInstances to filter the extracted data.
 * </p>
 */
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
	 * 
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
		double percent = 1.0;
		String attributes_filter = "";

		//testInstances	  = buildInstancesP(instances, attributes_filter, true, percent, 1.0);
		trainingInstances = buildInstancesP(instances, attributes_filter, true, 0, percent);

		// Build model
		trainModel();

		// Serialize/store model
		File dir = new File(writepath.substring(0, writepath.lastIndexOf("/")));
		if (!dir.exists()) { dir.mkdir(); }
		weka.core.SerializationHelper.write(writepath, classifier);
	}
	
	/**
	 * Used to build an Instances from some Percents of an other one.
	 * It's possible to add a filter on the attributes and choose if the new Instances got or not
	 * only different following rows.
	 * 
	 * @param data
	 * 				The Instances to extract the new Instances. 
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
	private static Instances buildInstancesP (Instances data, String attributes_filter, boolean differentNext, double percent_start, double percent_end) throws Exception
	{		
			/* Security (on percent_start and percent_end) */
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

			/* Attributes Filter */
		if (attributes_filter.length()>0) instances = Weka_ManageInstances.attributeSelection(instances, attributes_filter);

			/* Delete rows whose next is same. */
		if(differentNext) instances = Weka_ManageInstances.differentNextSelection(instances);

		return(instances);
	}

	/**
	 * Used to train a Random Forest.
	 * 
	 * @throws Exception
	 */
	private void trainModel() throws Exception
	{
		classifier = new NaiveBayes();
		classifier.setUseSupervisedDiscretization(true);

		//classifier = new RandomForest();
		//classifier.setNumFeatures(6);
		//classifier.setNumTrees(500);
		classifier.buildClassifier(trainingInstances);
	}
	
	/**
	 * Used to evaluate an Instances of test and display it.
	 * 
	 * @param displayResults
	 * 		  Print results to console
	 * 
	 * @throws Exception
	 */
	protected void evaluate (boolean displayResults) throws Exception
	{
		/*for(Instance i : testInstances) {
			// Remove music features to evaluate recommendation functionality
			for (int j = 1; j < 20; j++) {
				i.setMissing(j);
			}

			// Alter values (always 100% accuracy)
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

	private void loadData() throws Exception{
		File f = new File(filepath + ".json");
		if(f.exists()) {
			// Load from JSON file
			JSONLoader loader = new JSONLoader();
			loader.setSource(f);
			instances = loader.getDataSet();
		} else {
			// First use for user
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

	public static Instances loadNewInstances(String newInstances) throws IOException{
		// Replace single quotation marks (syntax error)
		newInstances = newInstances.replace("\'", "\"");

		// Load instances from string
		JSONLoader loader = new JSONLoader();
		loader.setSource(new ByteArrayInputStream(newInstances.getBytes()));
		return loader.getDataSet();

	}
}