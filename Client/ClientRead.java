package Client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import classes.ReplicaLoc;
import interfaces.MasterServerClientInterface;
import interfaces.ReplicaServerClientInterface;

public class ClientRead {
	
	public static void main(String[] args) {
		try {
			Registry reg = LocateRegistry.getRegistry(new Integer(8090));
			MasterServerClientInterface masterServer = (MasterServerClientInterface) reg
					.lookup("masterServer");
			ReplicaLoc loc = masterServer.read("file1.txt")[0];
			ReplicaServerClientInterface repServer = (ReplicaServerClientInterface) reg
					.lookup(loc.getName());

			System.out.println(repServer.read("file1.txt").getData());
				
			
		} catch (Exception e) {
			e.printStackTrace();
		}
}

}
