package classes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import interfaces.MasterServerClientInterface;
import interfaces.ReplicaServerClientInterface;
import test.MessageNotFoundException;

public class ReplicaServer implements ReplicaServerClientInterface {

	private ConcurrentHashMap<Long, String> fileNameTransaction;
	private ConcurrentHashMap<String, Semaphore> fileLock;
	private ConcurrentHashMap<String, ReadWriteLock> fileReadWriteLock;
	private ConcurrentHashMap<String, FileContent> cache;
	private MasterServerClientInterface masterServer;
	private String dir;
	private String name;

	public ReplicaServer(String dir, String name,
			MasterServerClientInterface masterServer) {
		fileNameTransaction = new ConcurrentHashMap<>();
		fileLock = new ConcurrentHashMap<>();
		fileReadWriteLock = new ConcurrentHashMap<String, ReadWriteLock>();
		cache = new ConcurrentHashMap<String, FileContent>();
		this.dir = dir;
		this.name = name;
		this.masterServer = masterServer;
	}

	@Override
	public WriteMsg write(long txnID, long msgSeqNum, FileContent data)
			throws RemoteException, IOException {
		String fileName = data.getFileName();
		// if this is the first message, we obtain a lock on file first
		if (msgSeqNum == 1) {
			Semaphore lock = null;
			if (!fileLock.containsKey(fileName)) {
				lock = new Semaphore(1);
				fileLock.put(fileName, lock);
			} else {
				lock = fileLock.get(fileName);
			}
			try {
				lock.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			fileNameTransaction.put(txnID, fileName);
			cache.put(fileName, data);
		} else {
			cache.get(fileName).appendData(data.getData());
		}
		return null;
	}

	@Override
	public FileContent read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {
		FileContent data = new FileContent(fileName);
		ReadWriteLock lock = null;
		if (!fileReadWriteLock.containsKey(fileName)) {
			lock = new ReentrantReadWriteLock();
			fileReadWriteLock.put(fileName, lock);
		} else {
			lock = fileReadWriteLock.get(fileName);
		}

		lock.readLock().lock();
		Scanner myScanner = new Scanner(new File(dir + "/" + fileName));
		while (myScanner.hasNext()) {
			data.appendData(myScanner.nextLine());
			if (myScanner.hasNext())
				data.appendData("\n");
		}
		myScanner.close();
		lock.readLock().unlock();

		return data;
	}

	@Override
	public boolean commit(long txnID, long numOfMsgs)
			throws MessageNotFoundException, RemoteException {
		if (fileNameTransaction.containsKey(txnID)) {
			String fileName = fileNameTransaction.remove(txnID);
			FileContent content = cache.remove(fileName);
			ReplicaLoc[] locations = null;
			try {
				locations = masterServer.read(fileName);
			} catch (Exception e1) {
				e1.printStackTrace();
				System.out.println("errrrrror13");
			}
			boolean success = true;
			System.out.println(locations[0]);
			// check if this is the master replica
			if (locations[0].getName().equals(name)) {
				for (int i = 1; i < locations.length; i++) {
					ReplicaLoc loc = locations[i];
					Registry reg = LocateRegistry.getRegistry(loc.getHost(),
							loc.getPort());
					try {
						ReplicaServerClientInterface repServer = (ReplicaServerClientInterface) reg
								.lookup(loc.getName());
						repServer.write(txnID, 1, content);
						success |= repServer.commit(txnID, 1);
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("errrrrror12");
					}
				}
			}
			try {
				FileContent fc = new FileContent(fileName);
				fc.setData(null);
				masterServer.write(fc);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.out.println("errrrrror11");
			}

			ReadWriteLock lock = null;
			if (!fileReadWriteLock.containsKey(fileName)) {
				lock = new ReentrantReadWriteLock();
				fileReadWriteLock.put(fileName, lock);
			} else {
				lock = fileReadWriteLock.get(fileName);
			}
			try {
				lock.writeLock().lock();
				File f = new File(dir + "/" + fileName);
				System.out.println("created");
				FileWriter fw = new FileWriter(f, true);
				fw.write(content.getData());
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
				success = false;
				System.out.println("errrrrror");
			} finally {
				lock.writeLock().unlock();
				System.out.println("errrrrror1");
			}
			fileLock.get(fileName).release();
			return success;
		}
		return false;
	}

	@Override
	public boolean abort(long txnID) throws RemoteException {
		if (fileNameTransaction.containsKey(txnID)) {
			String fileName = fileNameTransaction.remove(txnID);
			cache.remove(fileName);
			fileLock.get(fileName).release();
			return true;
		}
		return false;
	}

	// Arguments: name dir port masterName masterAddress masterPort
	public static void main(String[] args) {
		String replicaName = args[0];
		String serverDir = args[1];
		int port = new Integer(args[2]);
		String masterServerName = args[3];
		String masterServerAdd = args[4];
		int masterServerPort = new Integer(args[5]);

		System.out.println("innnnn");

		
		try {
			File dir1 = new File(serverDir);
			if (!dir1.exists())
				dir1.mkdir();
			File subDir = new File(serverDir + "/" + replicaName);
			if (!subDir.exists())
				subDir.mkdir();
			Registry registry = null;
			try {
				registry = LocateRegistry.createRegistry(port);
				System.out.println("hasdaj");
			} catch (Exception e) {
				registry = LocateRegistry.getRegistry(port);
				System.out.println("errrrrror2");
			}

			Registry reg2 = LocateRegistry.getRegistry(masterServerAdd,
					masterServerPort);
			MasterServerClientInterface masterServer = (MasterServerClientInterface) reg2
					.lookup(masterServerName);

			ReplicaServer replicaServerObj = new ReplicaServer(serverDir
					+ "/" + replicaName, replicaName, masterServer);
			ReplicaServerClientInterface replicaServerStub = (ReplicaServerClientInterface) UnicastRemoteObject
					.exportObject(replicaServerObj, 0);
			System.out.println("here!!");
			System.out.println(replicaName);
			System.out.println(replicaServerStub);
//			LocateRegistry.createRegistry(2020);
//			Runtime.getRuntime().exec("rmiregistry " + port);
			registry.rebind(replicaName, replicaServerStub);

			// System.out.println("----" +
			// masterServer.read("file1.txt").length);

			System.out.println("here also!!");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("errrrrror3");
		}
	}
}
