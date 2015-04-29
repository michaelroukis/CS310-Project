package edu.duke.raft;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class StartClient {
  public static void main (String[] args) {
        if (args.length != 2) {
      System.out.println ("usage: java edu.duke.raft.StartServer -Djava.rmi.server.codebase=<codebase url> <int: rmiregistry port> <int: server id>");
      System.exit(1);
    }
    int port = Integer.parseInt (args[0]);    
    int id = Integer.parseInt (args[1]);
    
    String url = "rmi://localhost:" + port + "/S" + id;
    System.out.println ("Testing S" + id);
    System.out.println ("Contacting server via rmiregistry " + url);

 // roukis test
 		// http://stackoverflow.com/questions/1823305/rmi-connection-refused-with-localhost
 		boolean noRMI = true;
 		
 		try {
 			System.out.println(LocateRegistry.createRegistry(port));
 		} catch (RemoteException e) {
 			System.out.println("No reg found.");
 		}
 		
 		if (noRMI) {
 			
 			try {
 				Runtime.getRuntime().exec("rmiregistry "+port);
 			} catch (IOException e) {
 				System.out.println("error making RMI.");
 			}
 			
 		}
 		
    
    try {
      RaftServer server = (RaftServer) Naming.lookup(url);
      server.requestVote (0, 0, 0, 0);
      server.appendEntries (0, 0, 0, 0, null, 0);
    } catch (MalformedURLException me) {
      System.out.println (me.getMessage());
    } catch (RemoteException re) {
      System.out.println (re.getMessage());
    } catch (NotBoundException nbe) {
      System.out.println (nbe.getMessage());
    }    
  }
}
