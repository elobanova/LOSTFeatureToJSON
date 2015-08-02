import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import json.JSONHandler;
import model.AudioFileListItem;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import xml.XMLHandler;
import xml.model.Feature;
import xml.model.Pair;

public class jAudioFE {
	private static final String JSON_FOLDER_NAME = "/json/";
	private static final String JSON_EXTENSION = ".json";
	private static final String XML_EXTENSION = "xml";
	private static final String WAV_EXTENSION = ".wav";
	private static final String MP3_EXTENSION = "mp3";
	private static final int NUMBER_OF_FEATURES = 19;

	private static List<Pair<AudioFileListItem, List<Feature>>> listOflist = new ArrayList<>();

	public static void main(String[] args) {
		File folderWithMusic = new File(args[0]);
		List<Pair<AudioFileListItem, Pair<File, Double>>> listOfClosest = new ArrayList<>();
		String settingsLocation = args[1];
		String outputFolderName = args[2];
		String[] commandLineArgs = new String[4];
		commandLineArgs[0] = "-s";
		commandLineArgs[1] = settingsLocation;
		FilenameFilter mp3Filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return MP3_EXTENSION.equals(FilenameUtils.getExtension(name));
			}
		};

		if (folderWithMusic.exists() && folderWithMusic.isDirectory()) {
			for (File musicFile : folderWithMusic.listFiles(mp3Filter)) {
				commandLineArgs[2] = outputFolderName + "/" + musicFile.getName();
				commandLineArgs[3] = musicFile.getAbsolutePath();
				JAudioCommandLine.execute(commandLineArgs, args[3]);
				// remove wav
				try {
					FileUtils.forceDelete(new File(commandLineArgs[3].substring(0, commandLineArgs[3].lastIndexOf("."))
							+ WAV_EXTENSION));

					AudioFileListItem item = new AudioFileListItem(musicFile.getName());
					JSONHandler handler = new JSONHandler(item, outputFolderName);
					List<Feature> features = handler.getFeatures();
					if (features.size() == NUMBER_OF_FEATURES) {
						listOflist.add(new Pair<AudioFileListItem, List<Feature>>(item, features));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// normalize
			int numberOfFeatures = listOflist.get(0).secondArg.size();
			int numberOfSongs = listOflist.size();

			for (int i = 0; i < numberOfFeatures; i++) {
				double[] vectorToCheckCov = new double[numberOfSongs];
				for (int j = 0; j < numberOfSongs; j++) {
					vectorToCheckCov[j] = listOflist.get(j).secondArg.get(i).getValueWithoutNaN();
				}
				double minold = findmin(vectorToCheckCov);
				double maxold = findmax(vectorToCheckCov);
				for (int p = 0; p < numberOfSongs; p++) {
					double newvalue = (vectorToCheckCov[p] - minold) / (maxold - minold);
					listOflist.get(p).secondArg.get(i).setValue(newvalue);
				}
			}

			for (File musicFile : folderWithMusic.listFiles(mp3Filter)) {
				String musicFileName = musicFile.getName();
				AudioFileListItem audioFileItem = new AudioFileListItem(musicFileName);
				try {
					generateJson(musicFileName, outputFolderName);
					listOfClosest.addAll(XMLHandler.getSimilarObjectsForAudio(audioFileItem, args[0], args[2],
							listOflist));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// now we have features in a list for all songs
			outputsortedascsv(listOfClosest, args[2]);

			// remove xml
			for (File xmlFileWithFetures : new File(outputFolderName).listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return XML_EXTENSION.equals(FilenameUtils.getExtension(name));
				}
			})) {
				try {
					FileUtils.forceDelete(xmlFileWithFetures);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static double findmax(double[] vectorToCheckCov) {
		double max = vectorToCheckCov[0];
		for (int i = 1; i < vectorToCheckCov.length; i++) {
			if (max < vectorToCheckCov[i]) {
				max = vectorToCheckCov[i];
			}
		}
		return max;
	}

	private static double findmin(double[] vectorToCheckCov) {
		double min = vectorToCheckCov[0];
		for (int i = 1; i < vectorToCheckCov.length; i++) {
			if (min > vectorToCheckCov[i]) {
				min = vectorToCheckCov[i];
			}
		}
		return min;
	}

	private static void outputsortedascsv(List<Pair<AudioFileListItem, Pair<File, Double>>> sortedFirstKSimilarObjects,
			String xmlFolderName) {
		String folderName = xmlFolderName + "/csvextformatnorm";
		File folder = new File(folderName);
		folder.mkdir();
		File file = new File(folder, "result.csv");

		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(file));
			try {
				output.append("sep=;");
				output.newLine();
				for (int i = 0; i < XMLHandler.DEFAULT_K; i++) {
					for (int j = 0; j < sortedFirstKSimilarObjects.size() / XMLHandler.DEFAULT_K; j++) {
						Pair<AudioFileListItem, Pair<File, Double>> line = sortedFirstKSimilarObjects.get(i + j * 10);
						output.append(line.secondArg.secondArg.toString());
						output.append(";");
					}
					output.newLine();
				}
				output.newLine();
			} finally {
				output.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static void generateJson(String musicFileName, String outputFolderName) throws IOException {
		AudioFileListItem item = new AudioFileListItem(musicFileName);
		List<Feature> features = new ArrayList<>();
		for (Pair<AudioFileListItem, List<Feature>> listOfFeatures : listOflist) {
			if (listOfFeatures.firstArg.getFileName().equalsIgnoreCase(item.getFileName())) {
				features.addAll(listOfFeatures.secondArg);
			}
		}
		if (features.size() != NUMBER_OF_FEATURES)
			return;
		JSONHandler handler = new JSONHandler(features);
		List<AudioFileListItem> items = new ArrayList<>();
		items.add(item);
		JSONObject jsonObject = handler.generateJSON(items);
		String jsonWithWeightPrecise = jsonObject.toString().replaceAll("\"weight\":1", "\"weight\":1.0");
		String jsonWithDoubles = jsonWithWeightPrecise.replace("/\"", "").replace("\"/", "");
		FileUtils.writeStringToFile(
				new File(outputFolderName + JSON_FOLDER_NAME + FilenameUtils.getBaseName(item.getFileName())
						+ JSON_EXTENSION), jsonWithDoubles);

	}
}