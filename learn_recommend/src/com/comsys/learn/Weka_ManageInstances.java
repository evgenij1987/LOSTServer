package com.comsys.learn;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.PKIDiscretize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToNominal;

/**
 * <b>Weka_ManageInstances is the class used to:
 * <ul>
 * 		<li>Select some particular data of an Instances.</li>
 * 		<li>Build and destroy an Instances.</li>
 * </ul></b>
 * <p>
 * Weka_ManageInstances is used by <i>Weka_Use</i> which loads an Instances from an ARFF files 
 * and uses these methods to select some specific attributes or lines.
 * </p>
 * <p>
 * There are some methods with "_dontCareOfLastAtt" at the end of their name. It's because, I use to put
 * the prediction at the end of Instance (so it's the last Attributes).
 * </p>
 */
public class Weka_ManageInstances {

	/**
	 * <b>[Columns selection]</b>Select some attributes from a given Instances.
	 *   
	 * @param data
	 * 				An Instances of the data.
	 * @param option
	 * 				String which represents the attributes to remove.
	 * 				<i>"1-4"</i>  | <i>"28"</i>  | <i>"1-70,45,68-72"</i> | <i>""</i> | <i>...</i>
	 * 
	 * @return The new Instances of data without undesired attributes.
	 * 
	 * @throws Exception
	 */
	public static Instances attributeSelection (Instances data, String option) throws Exception
	{
	    String[] options = new String[2];
	    options[0] = "-R";
	    options[1] = option;
	    Remove remove = new Remove();
	    remove.setOptions(options);
	    remove.setInputFormat(data);
	    Instances newData = Filter.useFilter(data, remove);

	    if (newData.classIndex() == -1)
	    	newData.setClassIndex(newData.numAttributes() - 1);
	    return newData;
	}

	/**
	 * <b>[Rows selection]</b> Used to choose some lines of data by indicating between what
	 * percents select the rows.
	 *
	 * @param data
	 * 				An Instances of the data.
	 * @param start
	 * 				Percent indicating the first line of the selection.
	 * @param end
	 * 				Percent indicating the last line of the selection.
	 *
	 * @return The new Instances of data with only the desired rows.
	 */
	public static Instances percentSelection (Instances data, double start, double end)
	{
		if(end<start){
			double temp = start;
			start = end;
			end = temp;
		}
	    int to_start   = (int) Math.round(data.numInstances() * start);
	    int to_end = Math.max ( (int) Math.round(data.numInstances() * end) - to_start, 1);

	    Instances newData = new Instances(data, to_start, to_end);
		if (newData.classIndex() == -1)
	    	newData.setClassIndex(newData.numAttributes() - 1);
	    return (newData);
	}


	/**
	 * <b>[Rows selection]</b> Delete every lines followed by a row with the same values.
	 * Use equalsInstance().
	 *
	 * @param data
	 * 				An Instances of the data.
	 *
	 * @return The new Instances of data with only the desired rows.
	 */
	public static Instances differentNextSelection (Instances data)
	{
		Instances newData = data;

		for(int index = newData.numInstances()-1; index>0; index--)
		{
			Instance inst = newData.instance(index);
			Instance next = newData.instance(index-1);
			if (equalsInstance(inst, next))
				newData.delete(index);
		}
		return (newData);
	}

	/**
	 * Used to know if two Instance are equal.
	 *
	 * @param inst1
	 * 				The first Instance to compare.
	 * @param inst2
	 * 				The second Instance to compare.
	 *
	 * @return (inst1 == inst2)
	 */
	public static boolean equalsInstance (Instance inst1, Instance inst2)
	{
		if(inst1 == null && inst2 == null)
			return true;
		if(inst1 == null || inst2 == null)
			return false;

		if(inst1.numValues() != inst2.numValues())
			return false;
		for(int index=0; index<inst1.numValues(); index++){
			if (inst1.value(index) != inst2.value(index) && !(Double.isNaN(inst1.value(index)) && Double.isNaN(inst2.value(index))))
				return false;
		}
		return true;
	}


	public static void scaleNumericAttributes(Instances data) throws Exception
	{
		for(int i = 0; i < data.numAttributes(); i++) {
			if(data.attribute(i).isNumeric()) {

				double min = Math.abs(data.attributeStats(i).numericStats.min);

				DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
				format.setMaximumFractionDigits(20);
				format.setGroupingUsed(false);

				// Print dot instead of comma as separator
				DecimalFormatSymbols custom=new DecimalFormatSymbols();
				custom.setDecimalSeparator('.');
				format.setDecimalFormatSymbols(custom);

				String text = format.format(Math.abs(min));
				int decimalPlaces = text.length() - text.indexOf('.') - 1;

				if(decimalPlaces > 4) {
					for(int j = 0; j < data.numInstances(); j++) {
						data.instance(j).setValue(i,Math.pow(10,decimalPlaces) * data.instance(j).value(i));
					}
				}
			}
		}
	}
	
	public static void normalizeNumericAttributes(Instances data) throws Exception
	{
		for(int i = 1; i < data.numAttributes(); i++) {
			if(data.attribute(i).isNumeric() && (i != 19 && i != 24)) {

				double min = data.attributeStats(i).numericStats.min;
			    double range = data.attributeStats(i).numericStats.max - min;
			   
				for(int j = 0; j < data.numInstances(); j++) {
					data.instance(j).setValue(i, (data.instance(j).value(i) - min) / range);
				}
			}
		}
	}

	public static Instances discretizeNumericAttributes(Instances data, String option) throws Exception {
		PKIDiscretize discretize= new PKIDiscretize();
		String[] options= new String[2];
		options[0]="-R";
		options[1]=option;  //range of variables to make nominal

		discretize.setOptions(options);
		discretize.setInputFormat(data);
		//System.out.println(discretize.getBins());
		//discretize.setFindNumBins(true);
		discretize.setUseEqualFrequency(true);

		Instances newData = Filter.useFilter(data, discretize);

		return newData;
	}

	public static Instances convertNumericAttributes(Instances data, String option) throws Exception
	{
		NumericToNominal convert= new NumericToNominal();
		String[] options= new String[2];
		options[0]="-R";
		options[1]=option;  //range of variables to make nominal

		convert.setOptions(options);
		convert.setInputFormat(data);
		Instances newData = Filter.useFilter(data, convert);

		return newData;
	}

	public static Instances convertStringAttributes(Instances data, String option) throws Exception
	{
		StringToNominal convert= new StringToNominal();
		String[] options= new String[2];
		options[0]="-R";
		options[1]=option;  //range of variables to make nominal

		convert.setOptions(options);
		convert.setInputFormat(data);
		Instances newData = Filter.useFilter(data, convert);

		return newData;
	}
}
