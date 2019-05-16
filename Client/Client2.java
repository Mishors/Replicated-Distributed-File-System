package Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import classes.FileContent;
import classes.ReplicaLoc;
import classes.WriteMsg;
import interfaces.MasterServerClientInterface;
import interfaces.ReplicaServerClientInterface;
import test.MessageNotFoundException;

public class Client2 {
	private String masterServerName = null, masterServerAddress = null;
	private int masterServerPort = 0;
	
	
	private void readServerMetaData (String serverMetaDataFile) throws FileNotFoundException {
		File serverFile = new File(serverMetaDataFile);
		Scanner scanner = new Scanner(serverFile);
		
		while (scanner.hasNext()) {
			masterServerName = scanner.next();
			masterServerAddress = scanner.next();
			masterServerPort = scanner.nextInt();
		}
		scanner.close();
	}
	
	public FileContent write(String fileName, String serverMetaDataFile) throws Exception {
		// Read Master Server meta Data
		readServerMetaData(serverMetaDataFile);

		//System.out.println("Connected To " + masterServerName + " Address:" + 
		//					masterServerAddress + " Port:" + masterServerPort);
		
		//Connecting Remotely with Master Server via RMI
		Registry reg = LocateRegistry.getRegistry(new Integer(masterServerPort));
		MasterServerClientInterface masterServer = (MasterServerClientInterface) reg.lookup("masterServer");
		
		FileContent data = new FileContent(fileName);
		WriteMsg msg = null;
		ReplicaServerClientInterface repServer = null;
		
		System.out.println("Writing...");
		for (int i = 0; i < 10; i++) {
			data.appendData(i + "\n");
			
			//if (i % 3 == 0) {				
				//if (msg != null) {
					//repServer.commit(msg.getTransactionId(), 3);
				//}
				//data = new FileContent(fileName);
				//data.appendData("\n\n");
				
				//msg = masterServer.write(data);
				
				//data = new FileContent(fileName);
				//data.appendData(i + "\n");
				//ReplicaLoc loc = msg.getLoc();
				//repServer = (ReplicaServerClientInterface) reg.lookup(loc.getName());

				//repServer.write(msg.getTransactionId(), 0, data);
			//} else {
				//data = new FileContent(fileName);
				//data.appendData(i + "\n");
				//ReplicaLoc loc = msg.getLoc();
				//repServer = (ReplicaServerClientInterface) reg.lookup(loc.getName());
				//repServer.write(msg.getTransactionId(), 0, data);
			//}
			// Thread.sleep(2000);
		}
		//Committing Last Write
		//repServer.commit(msg.getTransactionId(), 3);
		System.out.println("Finished Writing");
		return data;
	}
	
	public void commit(FileContent data) throws Exception {
		//Connecting Remotely with Master Server via RMI
		Registry reg = LocateRegistry.getRegistry(new Integer(masterServerPort));
		MasterServerClientInterface masterServer = (MasterServerClientInterface) reg.lookup("masterServer");
		WriteMsg msg = masterServer.write(data);
		ReplicaLoc loc = msg.getLoc();
		ReplicaServerClientInterface repServer = (ReplicaServerClientInterface) reg.lookup(loc.getName());
		repServer.write(msg.getTransactionId(), 0, data);
		repServer.commit(msg.getTransactionId(), 3);
	}
	
	public boolean read (String fileName, String serverMetaDataFile) throws Exception {
		// Read Master Server meta Data
		readServerMetaData(serverMetaDataFile);
		//System.out.println("Connected To " + masterServerName + " Address:" + 
		//					masterServerAddress + " Port:" + masterServerPort);
		
		//Connecting Remotely with Master Server via RMI
		Registry reg = LocateRegistry.getRegistry(new Integer(masterServerPort));
		MasterServerClientInterface masterServer = (MasterServerClientInterface) reg
													.lookup("masterServer");
		
		try{
			ReplicaLoc primReplicaLoc = masterServer.read(fileName)[0];
		
			ReplicaServerClientInterface primReplica = (ReplicaServerClientInterface) reg
												 .lookup(primReplicaLoc.getName());
			System.out.println("Reading ..");
			System.out.println(primReplica.read(fileName).getData());		
			System.out.println("Finished Reading ..");
			return true;
		}catch(Exception e) {
			System.out.println("Couldn't read");
			return false;
		}		
	}

	public static void main(String[] args) throws Exception {
		// Write for Client
		
		Client2 client2 = new Client2();

		String fileName1 = "file1.txt";
		String fileName2 = "file2.txt";
		String serverMetaDataFile = "Client/Server_meta_data.txt";
		//FileContent data1 = null;

		
		while(!client2.read(fileName1, serverMetaDataFile));
		System.out.println("Client2 finish read from file1");
		
		
		client2.write(fileName1, serverMetaDataFile);
		System.out.println("Client 2 finished write in file 1 without commit");
		
		
		client2.write(fileName2, serverMetaDataFile);
		System.out.println("Client 2 finished write in file2 without commit");
		

	}

}
