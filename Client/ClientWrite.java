package Client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import classes.FileContent;
import classes.ReplicaLoc;
import classes.WriteMsg;
import interfaces.MasterServerClientInterface;
import interfaces.ReplicaServerClientInterface;

public class ClientWrite {
	public static void main(String[] args) throws Exception {
		Registry reg = LocateRegistry.getRegistry(new Integer(8090));
		MasterServerClientInterface masterServer = (MasterServerClientInterface) reg
				.lookup("masterServer");
		FileContent data = new FileContent("file1.txt");
		//WriteMsg wmsg = masterServer.write(data);
		//ReplicaLoc loc = masterServer.read("Client/file1.txt")[0];
		//ReplicaLoc loc = wmsg.getLoc();
		//ReplicaServerClientInterface repServer = (ReplicaServerClientInterface) reg
				//.lookup(loc.getName());
		WriteMsg msg = null;
        ReplicaServerClientInterface repServer = null;
        for (int i = 0; i < 10; i++){
            if (i % 3 == 0) {
               
                if (msg != null) {                 
                    repServer.commit(msg.getTransactionId(), 3);
                    System.out.println(data.getData());
                    
                }
                data = new FileContent("file1.txt");
                System.out.println("hhhhhhhhhhhhhhh");
                data.appendData("\n\n");
                msg = masterServer.write(data);
                data = new FileContent("file1.txt");
                data.appendData(i + "===\n");
                ReplicaLoc loc = msg.getLoc();
                repServer = (ReplicaServerClientInterface) reg.lookup(loc.getName());
               
                repServer.write(msg.getTransactionId(), 0, data);
            } else {
            	data = new FileContent("file1.txt");
                System.out.println("hhhhhhh22222");
                //FileContent data = new FileContent("Client/file1.txt");
                data.appendData(i + "===\n");  
                ReplicaLoc loc = msg.getLoc();
                repServer = (ReplicaServerClientInterface) reg.lookup(loc.getName());
                repServer.write(msg.getTransactionId(), 0, data);
            }
            //Thread.sleep(2000);
        }
        //System.out.println(repServer.commit(msg.getTransactionId(), 3));
    }

}
