package classes;

import java.io.Serializable;

public class ReplicaLoc implements Serializable {
	private static final long serialVersionUID = 1234567L;
	private String host;
	private String name;
	private int port;

	public ReplicaLoc(String host, String name, int port) {
		this.host = host;
		this.name = name;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}
}
