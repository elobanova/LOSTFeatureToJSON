package model;

import java.io.Serializable;

/**
 * Created by ekaterina on 05.07.2015.
 */
public class AudioFileListItem implements Serializable {
	private String fileName;

	public AudioFileListItem(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
