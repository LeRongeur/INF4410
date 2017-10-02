package ca.polymtl.inf4410.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.ServerInterface.Command;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.*;
import static java.nio.file.StandardOpenOption.*;

public class Client {
	
	private static int clientId;
	public static Vector<String> commands = new Vector<String>();

	public static void main(String[] args) {
		String distantHostname = null;
		String argCommand = "";
		String argFileName = "";
		if (args.length > 0) {
			distantHostname = args[0];
			if (args.length > 1) {
				argCommand = args[1];
			}
			if (args.length > 2) {
				argFileName = args[2];
			}
		}

		commands.add("LIST");
		commands.add("GET");
		commands.add("LOCK");
		commands.add("CREATE");
		commands.add("PUSH");
		commands.add("SYNCLOCALDIR");

		Client client = new Client(distantHostname);
		System.out.println("cocou1");
 
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./ClientId.txt"), charset)) 
		{
			System.out.println("cocou2");
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		        clientId = Integer.parseInt(line);
		        System.out.println(clientId);
		    }
		} 
		catch (IOException z) 
		{
		   System.out.println("cocou3");
		   System.out.println(z);
		   clientId = client.generateId();
		   String clientIdString = Integer.toString(clientId);
		   byte data[] = clientIdString.getBytes();
		   Path p = Paths.get("./ClientId.txt");

		   try (OutputStream out = new BufferedOutputStream(
		     Files.newOutputStream(p, CREATE, APPEND))) 
		   {
		     out.write(data, 0, data.length);
		   } 
		   catch (IOException y) 
		   {
		     System.out.println(y);
		   }
		}

		

		//x = client.clamp(x);		
		//int size = (int) Math.pow(10, x);
		//byte[] var = new byte[size];
		//client.run(var);
		int command = convertStringCommandToInt(argCommand);
		client.runCommand(command, argFileName);
	}

	FakeServer localServer = null; // Pour tester la latence d'un appel de
									// fonction normal.
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServer = new FakeServer();
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	private void runCommand(int command, String argFileName) {
		//appelNormal(command);

		if (localServerStub != null) {
			String contenu;
			String checkSum;
			if(argFileName != "")
			{
				contenu = getContenu(argFileName);
				checkSum = getCheckSum(argFileName);
			}
			else
			{
				contenu = "-1";
				checkSum = "-1";
			}
			appelRMILocal(command, argFileName, clientId, contenu, checkSum);
		}

		//if (distantServerStub != null) {
		//	appelRMIDistant(command);
		//}
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	/*private void appelNormal(int command, String fileName, int clientId, String contenu, String checkSum) {
		long start = System.nanoTime();

		System.out.println(localServer.executeCommand(command, fileName, clientId, contenu, checkSum));
		long end = System.nanoTime();

		System.out.println("Temps écoulé appel normal: " + (end - start)
				+ " ns");
	}*/

	private void appelRMILocal(int command, String fileName, int clientId, String contenu, String checkSum) {
		try {
			long start = System.nanoTime();
			System.out.println(localServerStub.executeCommand(command, fileName, clientId, contenu, checkSum).getValue());
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI local: " + (end - start)
					+ " ns");
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	private void appelRMIDistant(int command, String fileName, int clientId, String contenu, String checkSum) {
		try {
			long start = System.nanoTime();
			System.out.println(distantServerStub.executeCommand(command, fileName, clientId, contenu, checkSum));
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI distant: "
					+ (end - start) + " ns");
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}
	private static int convertStringCommandToInt(String command)
	{
		String upperCaseCommand = command.toUpperCase();
		if(commands.contains(upperCaseCommand))
		{
			return commands.indexOf(upperCaseCommand);
		}
		return -1;
	}

		private int generateId() {
		try {

			return distantServerStub.generateClientId();
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
			return -1;
		}
	} 

	private String getContenu(String fileName)
	{
		String content = "";
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./" + fileName + ".txt"), charset)) 
		{
			System.out.println("cocou2");
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		        content += line;
		    }
		} 
		catch (IOException e) 
		{
			System.out.println("Erreur: " + e.getMessage());
		}
		return content;
	}

	private String getCheckSum(String fileName)
	{
		String checkSum = "-1";
		// TODO : Trouver le vrai checkSum
		return checkSum;
	}
}
