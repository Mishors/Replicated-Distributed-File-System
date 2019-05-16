package classes;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashSet;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import interfaces.MasterServerClientInterface;
import interfaces.ReplicaServerClientInterface;
import classes.FileContent;

public class MasterServer implements MasterServerClientInterface {

	private ConcurrentHashMap<String, ReplicaLoc[]> locMap;
	private ConcurrentHashMap<String, Lock> fileLock;
	private Set<String> metaDataFiles = new HashSet<String>();
	private ReplicaLoc[] replicaServerAddresses;
	private AtomicInteger txnID, timeStamp;
	private static int NUM_REPLICA_PER_FILE = 3;
	FileWriter metaDataWriter;

	private static Random r = new Random(System.nanoTime());

	private static synchronized int rand(int size) {
		return r.nextInt(size) % size;
	}

	public MasterServer(File metaData, TreeMap<String, ReplicaLoc> nameToLocMap) throws IOException {
		locMap = new ConcurrentHashMap<String, ReplicaLoc[]>();
		fileLock = new ConcurrentHashMap<String, Lock>();
		txnID = new AtomicInteger(0);
		timeStamp = new AtomicInteger(0);
		replicaServerAddresses = new ReplicaLoc[nameToLocMap.size()];

		int ii = 0;
		for (ReplicaLoc loc : nameToLocMap.values()) {
			replicaServerAddresses[ii++] = loc;
		}

		Scanner scanner = new Scanner(metaData);
		while (scanner.hasNext()) {
			StringTokenizer tok = new StringTokenizer(scanner.nextLine());
			String fName = tok.nextToken();
			ReplicaLoc[] fileLocations = new ReplicaLoc[tok.countTokens()];
			for (int i = 0; i < fileLocations.length; i++) {
				fileLocations[i] = nameToLocMap.get(tok.nextToken());
			}
			locMap.put(fName, fileLocations);
		}
		scanner.close();

		metaDataWriter = new FileWriter(metaData, true);
	}

	@Override
	public ReplicaLoc[] read(String fileName) throws FileNotFoundException, IOException, RemoteException {
		return locMap.get(fileName);
	}

	private ReplicaLoc[] selectRandomReplicas() {
		ReplicaLoc[] result = new ReplicaLoc[NUM_REPLICA_PER_FILE];
		boolean[] visited = new boolean[replicaServerAddresses.length];
		for (int i = 0; i < result.length; i++) {
			int randomReplicaServer = rand(replicaServerAddresses.length);
			System.out.println(visited.length + " " + randomReplicaServer);
			while (visited[randomReplicaServer])
				randomReplicaServer = rand(replicaServerAddresses.length);
			visited[randomReplicaServer] = true;
			result[i] = replicaServerAddresses[randomReplicaServer];
		}
		return result;
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		String fileName = data.getFileName();

		if (data.getData() == null && metaDataFiles.contains(fileName))
			return null;

		// check if this is a commit acknowledgment
		if (data.getData() == null && !metaDataFiles.contains(fileName)) {
			synchronized (this) {
				metaDataFiles.add(fileName);
				ReplicaLoc[] locations = locMap.get(fileName);
				metaDataWriter.write(fileName);
				for (int i = 0; i < locations.length; i++) {
					metaDataWriter.write(" " + locations[i].getName());
				}
				metaDataWriter.write("\n");
				metaDataWriter.flush();
			}
			return null;
		} else {			
			Lock lock = null;
			try {
				if (!fileLock.containsKey(fileName)) {
					lock = new ReentrantLock();
					fileLock.put(fileName, lock);
				} else {
					lock = fileLock.get(fileName);
				}
				lock.lock();
				int tId = txnID.incrementAndGet();
				int ts = timeStamp.incrementAndGet();
				ReplicaLoc[] locations = null;
				if (locMap.containsKey(fileName)) {
					locations = locMap.get(fileName);
				} else {
					locations = selectRandomReplicas();
				}
				locMap.put(fileName, locations);
				ReplicaLoc primary = locations[0];
				ReplicaServerClientInterface primaryServer = null;
				try {
					primaryServer = (ReplicaServerClientInterface) LocateRegistry
							.getRegistry(primary.getHost(), primary.getPort()).lookup(primary.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
				primaryServer.write(tId, 1, data);
				return new WriteMsg(tId, ts, primary);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				lock.unlock();
			}
		}
	}

	private static void sshConnection(String host, String user, String password, String command1) {

		try {

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, 22);
			session.setPassword(password);
			session.setConfig(config);
			session.connect();			

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command1);
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			channel.connect();
			channel.disconnect();
			session.disconnect();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws FileNotFoundException {
		String masterName = "masterServer";
		String masterAdd = "127.0.0.1";
		int masterPort = new Integer(8090);
		String serverDir = "/home/mo-raafat/eclipse-workspace/\"Replicated Distributed File System\"";
		File metaData = new File("metadata.txt");
		File repServers = new File("replicaServers.txt");
		TreeMap<String, ReplicaLoc> nameToLocMap = new TreeMap<String, ReplicaLoc>();

		Scanner scanner = new Scanner(repServers);

		while (scanner.hasNext()) {
			String repName = scanner.next(), repAddress = scanner.next();
			int repPort = scanner.nextInt();
			nameToLocMap.put(repName, new ReplicaLoc(repAddress, repName, repPort));
		}
		scanner.close();

		try {
			Registry registry = null;
			try {
				registry = LocateRegistry.createRegistry(masterPort);
			} catch (Exception e) {
				registry = LocateRegistry.getRegistry(masterPort);
			}

			MasterServer masterServerObj = new MasterServer(metaData, nameToLocMap);
			MasterServerClientInterface masterServerStub = (MasterServerClientInterface) UnicastRemoteObject
					.exportObject(masterServerObj, 0);
			registry.bind(masterName, masterServerStub);
		} catch (Exception e) {
			e.printStackTrace();			
		}

		try {
			System.out.println("Master Server Started...");
			System.setProperty("java.rmi.server.hostname", "127.0.0.1");

			for (ReplicaLoc repLoc : nameToLocMap.values()) {
				String cmd1 = "ssh -tt " + repLoc.getHost();
				String cmd2 = ";cd /home/mo-raafat/eclipse-workspace/\"Replicated Distributed File System\"";
				String cmd3 = ";cd \"Replicated Distributed File System\"";
				String cmd4 = ";javac interfaces/ReplicaServerClientInterface.java classes/ReplicaServer.java";
				String cmd5 = ";java  classes/ReplicaServer " + repLoc.getName() + " " + serverDir + " "
						+ repLoc.getPort() + " " + masterName + " " + masterAdd + " " + masterPort;
				String cmd6 = ";rmic classes/MasterServer";
				String cmd7 = ";exit";

				sshConnection("127.0.0.1", "mo-raafat", "1234", cmd1 + cmd2 + cmd4 + cmd5 + cmd6 + cmd7);

				System.out.println("SSHConnection to Start " + repLoc.getName());
			}
		} catch (Exception e1) {			
			e1.printStackTrace();
		}
	}
}
