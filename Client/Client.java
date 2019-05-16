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
	
	public FileContent write(String fileName, String serverMetaDataFile) throws Exception {
		// Read Master Server meta Data
				readServerMetaData(serverMetaDataFile);

				System.out.println("Connected To " + masterServerName + " Address:" + 
									masterServerAddress + " Port:" + masterServerPort);	
				
				FileContent data = new FileContent(fileName);		
				
				System.out.println("Writing...");
				for (int i = 0; i < 10; i++) {
					data.appendData(i + "\n");			
				}		
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
	
	public void read (String fileName, String serverMetaDataFile) throws Exception {
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
		}catch(Exception e) {
			System.out.println("Couldn't read");
		}		
	}

	public static void main(String[] args) throws Exception {
		// Write for Client
		Client client1 = new Client();
		

		String fileName1 = "file1.txt";
		String fileName2 = "file2.txt";
		String serverMetaDataFile = "Client/Server_meta_data.txt";
		//FileContent data1 = null;

		FileContent data1 = client1.write(fileName1, serverMetaDataFile);
		System.out.println("Client1 finished write without in file 1 commit");		
		client1.commit(data1);
		System.out.println("Client 1 Commited file1");
		
		
		client1.read(fileName1, serverMetaDataFile);
		System.out.println("Client1 finished read");
		
		
		client1.read(fileName2, serverMetaDataFile);
		System.out.println("Client1 finished read from file2");
		

	}

}
