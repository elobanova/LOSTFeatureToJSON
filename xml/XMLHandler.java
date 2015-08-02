package xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import model.AudioFileListItem;

import org.apache.commons.io.FilenameUtils;

import xml.model.DataSet;
import xml.model.Feature;
import xml.model.FeatureVectorFile;
import xml.model.Pair;
import xml.parser.FeaturePullParser;

/**
 * Created by ekaterina on 05.07.2015.
 */
public class XMLHandler {
	private static final String POSTFIX = ".xml";
	public static final int DEFAULT_K = 10;

	public static List<Feature> extractFeatures(AudioFileListItem audioFileItem, String folderWithXMLPath)
			throws IOException {
		File XMLFile = new File(folderWithXMLPath + "/" + FilenameUtils.getBaseName(audioFileItem.getFileName())
				+ POSTFIX);
		FeatureVectorFile featureVectorFile = new FeaturePullParser().parse(XMLFile);
		DataSet dataSet = featureVectorFile.getDataSet();
		return dataSet == null ? new ArrayList<Feature>() : dataSet.getFeatures();
	}

	//
	// public static List<AudioFileListItem> getSimilarObjects(AudioFileListItem
	// audioFileItem,
	// String folderWithMusicPath, String folderWithXMLPath) throws IOException
	// {
	// List<AudioFileListItem> result = new ArrayList<>();
	// List<? extends Pair<AudioFileListItem, Pair<File, Double>>>
	// sortedFirstKSimilarObjects = getSimilarObjectsForAudio(
	// audioFileItem, folderWithMusicPath, folderWithXMLPath);
	// for (Pair<AudioFileListItem, Pair<File, Double>> closePair :
	// sortedFirstKSimilarObjects) {
	// result.add(new
	// AudioFileListItem(closePair.secondArg.firstArg.getName()));
	// }
	// return result;
	// }

	public static List<Pair<AudioFileListItem, Pair<File, Double>>> getSimilarObjectsForAudio(
			AudioFileListItem audioFileItem, String folderWithMusicPath, String folderWithXMLPath,
			List<Pair<AudioFileListItem, List<Feature>>> listOflist) throws IOException {
		List<Pair<AudioFileListItem, Pair<File, Double>>> similarAudioFiles = new ArrayList<>();
		File audioFile = new File(folderWithMusicPath + "/" + audioFileItem.getFileName());
		double[] featureVector = getVectorValues2(audioFile, folderWithXMLPath, listOflist);
		if (featureVector != null) {
			List<Pair<File, Double>> distances = new LinkedList<>();
			for (File anotherAudioFile : new File(folderWithMusicPath).listFiles()) {
				if (!audioFile.equals(anotherAudioFile)) {
					double[] anotherFeatureVector = getVectorValues2(anotherAudioFile, folderWithXMLPath, listOflist);
					if (anotherFeatureVector != null) {
						double L2Distance = calculateL2DistanceBetweenVectors(featureVector, anotherFeatureVector);

						// remember the distances from audio to all
						// other audio files
						distances.add(new Pair<>(anotherAudioFile, L2Distance));
					}
				}
			}

			// define ascending sorting for distances
			Collections.sort(distances, new Comparator<Pair<File, Double>>() {
				public int compare(Pair<File, Double> o1, Pair<File, Double> o2) {
					return o1.secondArg.compareTo(o2.secondArg);
				}
			});

			// choose only k nearest neighbors
			for (int i = 0; i < DEFAULT_K; i++) {
				similarAudioFiles.add(new Pair<>(audioFileItem, new Pair<>(distances.get(i).firstArg,
						distances.get(i).secondArg)));
			}
		}
		return similarAudioFiles;

	}

	private static double calculateL2DistanceBetweenVectors(double[] f1, double[] f2) {
		double squaredDistance = 0;
		for (int i = 0; i < f1.length; i++) {
			squaredDistance += Math.pow(f1[i] - f2[i], 2);
		}

		return Math.sqrt(squaredDistance);
	}

	// private static double[] getVectorValues(File audioFileItem, String
	// folderWithXMLPath) throws IOException {
	// List<Feature> features = extractFeatures(new
	// AudioFileListItem(audioFileItem.getName()), folderWithXMLPath);
	// double[] result = new double[features.size()];
	// for (int i = 0; i < features.size(); i++) {
	// double value = features.get(i).getValue();
	// result[i] = Double.isNaN(value) ? 0.0d : value;
	// }
	// return result;
	// }

	private static double[] getVectorValues2(File audioFileItem, String folderWithXMLPath,
			List<Pair<AudioFileListItem, List<Feature>>> listOflist) throws IOException {
		List<Feature> features = new ArrayList<>();
		for (Pair<AudioFileListItem, List<Feature>> listOfFeatures : listOflist) {
			if (listOfFeatures.firstArg.getFileName().equalsIgnoreCase(audioFileItem.getName())) {
				features.addAll(listOfFeatures.secondArg);
			}
		}

		// List<Feature> features = extractFeatures(new
		// AudioFileListItem(audioFileItem.getName()), folderWithXMLPath);
		double[] result = new double[features.size()];
		for (int i = 0; i < features.size(); i++) {
			double value = features.get(i).getValue();
			result[i] = Double.isNaN(value) ? 0.0d : value;
		}
		return result;
	}
}
