package Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import classes.FileContent;
import classes.ReplicaLoc;
import classes.WriteMsg;
import interfaces.MasterServerClientInterface;
import interfaces.ReplicaServerClientInterface;

public class Client {
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
	
	public void write(String fileName, String serverMetaDataFile) throws Exception {
		// Read Master Server meta Data
		readServerMetaData(serverMetaDataFile);

		System.out.println("Connected To " + masterServerName + " Address:" + 
							masterServerAddress + " Port:" + masterServerPort);
		
		//Connecting Remotely with Master Server via RMI
		Registry reg = LocateRegistry.getRegistry(new Integer(masterServerPort));
		MasterServerClientInterface masterServer = (MasterServerClientInterface) reg.lookup("masterServer");
		
		FileContent data = new FileContent(fileName);
		WriteMsg msg = null;
		ReplicaServerClientInterface repServer = null;
		
		System.out.println("Writing...");
		for (int i = 0; i < 10; i++) {
			
			if (i % 3 == 0) {				
				if (msg != null) {
					repServer.commit(msg.getTransactionId(), 3);
				}
				data = new FileContent(fileName);
				//data.appendData("\n\n");
				msg = masterServer.write(data);
				data = new FileContent(fileName);
				data.appendData(i + "\n");
				ReplicaLoc loc = msg.getLoc();
				repServer = (ReplicaServerClientInterface) reg.lookup(loc.getName());

				repServer.write(msg.getTransactionId(), 0, data);
			} else {
				data = new FileContent(fileName);
				data.appendData(i + "\n");
				ReplicaLoc loc = msg.getLoc();
				repServer = (ReplicaServerClientInterface) reg.lookup(loc.getName());
				repServer.write(msg.getTransactionId(), 0, data);
			}
			// Thread.sleep(2000);
		}
		//Committing Last Write
		repServer.commit(msg.getTransactionId(), 3);
		System.out.println("Finished Writing");
	}
	
	public void read (String fileName, String serverMetaDataFile) throws Exception {
		// Read Master Server meta Data
		readServerMetaData(serverMetaDataFile);
		System.out.println("Connected To " + masterServerName + " Address:" + 
							masterServerAddress + " Port:" + masterServerPort);
		
		//Connecting Remotely with Master Server via RMI
		Registry reg = LocateRegistry.getRegistry(new Integer(masterServerPort));
		MasterServerClientInterface masterServer = (MasterServerClientInterface) reg
													.lookup("masterServer");
		ReplicaLoc primReplicaLoc = masterServer.read(fileName)[0];
		ReplicaServerClientInterface primReplica = (ReplicaServerClientInterface) reg
												 .lookup(primReplicaLoc.getName());
		
		System.out.println("Reading ..");
		System.out.println(primReplica.read(fileName).getData());		
		System.out.println("Finished Reading ..");
	}

	public static void main(String[] args) throws Exception {
		// Write for Client
		Client client = new Client();

		String fileName = "file.txt";
		String serverMetaDataFile = "Client/Server_meta_data.txt";

		client.write(fileName, serverMetaDataFile);
		
		//Read for Client
		client.read(fileName, serverMetaDataFile);

	}

}
