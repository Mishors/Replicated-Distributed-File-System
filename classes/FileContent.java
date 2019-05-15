package classes;


import java.io.Serializable;

public class FileContent implements Serializable {

	private static final long serialVersionUID = 8969701885826822440L;
	private String fileName;

	public String getFileName() {
		return fileName;
	}

	private StringBuilder data;

	public FileContent(String name) {
		fileName = name;
		data = new StringBuilder();
	}

	public void appendData(String dataChunk) {
		data.append(dataChunk);
	}

	public String getData() {
		if (data == null)
			return null;
		return data.toString();
	}
	
	public void setData(StringBuilder sb) {
		data = sb;
	}
}
