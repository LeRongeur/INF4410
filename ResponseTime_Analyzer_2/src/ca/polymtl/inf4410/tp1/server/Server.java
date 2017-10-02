package ca.polymtl.inf4410.tp1.server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.ServerInterface.Command;
import java.util.*;
import java.nio.file.*;
import java.nio.*;
import java.io.*;
import static java.nio.file.StandardOpenOption.*;
import javafx.util.Pair;
import java.security.DigestInputStream;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.Charset;

public class Server implements ServerInterface {

	private HashMap<String, Integer> files = new HashMap<String, Integer>();

	public static void main(String[] args) {
		Server server = new Server();
		server.loadAllFiles();
		server.run();
	}

	public Server() {
		super();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/*
	 * Méthode accessible par RMI. Additionne les deux nombres passés en
	 * paramètre.
	 */
	@Override
	public int execute(int a, int b) throws RemoteException {
		return a + b;
	}

	public Pair<Boolean, String> executeCommand(int command, String nom, int clientId, String contenu, String checkSum) throws RemoteException
	{
		if (command >= 0 && command <= 5)
		{
			switch(Command.values()[command]) 
			{
				case LIST : 
					System.out.println("Executing command LIST...");
					return printListFiles();		
				case GET : 
					System.out.println("Executing command GET...");
					return sendFile(nom, checkSum);
				case LOCK : 
					System.out.println("Executing command LOCK...");
					return lockFile(nom, clientId, checkSum);
				case CREATE : 
					System.out.println("Executing command CREATE...");
					return createFile(nom);
				case PUSH : 
					System.out.println("Executing command PUSH...");
					return pushFile(nom, contenu, clientId);
				case SYNCLOCALDIR : 
					System.out.println("Executing command SYNCLOCALDIR...");
					return syncLocalDirectory();
				default : 
					return new Pair<Boolean, String>(false, "Commande inconnue.");
			}
		}
		else
		{
			return new Pair<Boolean, String>(false, "Commande inconnue.");
		}
	}

	private void loadAllFiles()
	{
		// TODO : Load all the files from the server in a vector or map?
		File file = new File("./Files/");
	    for (final File fileEntry : file.listFiles()) {
	        if (!fileEntry.isDirectory()) 
	        {
	        	files.put(fileEntry.getName(), -1);
	        }
	    }
	}

	private Pair<Boolean, String> printListFiles()
	{
		// TODO : Print each files in the vector or map, with the UserID that has it locked
		String returnString = "";
	    for (String name: files.keySet())
	    {
	    	if(files.get(name) > -1)
	    	{
	    		String value = files.get(name).toString();
	    		returnString += name + " verrouille par client " + value + "\n"; 
	    	}
	    	else
	    	{
	    		returnString += name + " non verrouille" + "\n";	
	    	}
 
		}
		return new Pair<Boolean, String>(true, returnString += files.size() + " fichier(s)");
	}
	
	private Pair<Boolean, String> sendFile(String nom, String checkSum)
	{
		// TODO : Send file with name = nom if parameterChecksum different from localChecksum (in vector or map)
		String returnString = "";
		return new Pair<Boolean, String>(true, returnString);
	}

	private Pair<Boolean, String> lockFile(String nom, int clientID, String checkSumClient)
	{
		// TODO : change lockID of file with name = nom for clientID
		// TODO : Something with checksum apparently
		String returnString = "";
		Boolean isSuccess = false;
		String checkSumServer = "";

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(getContenu(nom).getBytes());
			byte[] messageDigestMD5 = messageDigest.digest();
			StringBuffer stringBuffer = new StringBuffer();
			for (byte bytes : messageDigestMD5) {
				stringBuffer.append(String.format("%02x", bytes & 0xff));
			}

			checkSumServer = stringBuffer.toString();
			if(checkSumServer != checkSumClient)
			{
				// TODO : Append le contenu dans le string
			}
			else
			{

			}
		} catch (NoSuchAlgorithmException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}

		return new Pair<Boolean, String>(isSuccess, returnString);
	}

	private Pair<Boolean, String> createFile(String name)
	{
		// TODO : Create file with name = nom
		String returnString = "";
		Boolean isSuccess = false;
		if(files.containsKey(name))
		{
			returnString += "Operation refusee: le fichier existe deja";
			isSuccess = false;
		}
		else
		{
		   byte data[] = name.getBytes();
		   Path p = Paths.get("./Files/" + name + ".txt");

		   try (OutputStream out = new BufferedOutputStream(
		     Files.newOutputStream(p, CREATE, APPEND))) 
		   {
		     out.write(data, 0, data.length);
		     files.put(name, -1);
		     returnString += name + " Fichier ajoute.";
		   } 
		   catch (IOException y) 
		   {
		     returnString += y;
		   }
		   isSuccess = true;
		}
		return new Pair<Boolean, String>(isSuccess, returnString);
	}

	private Pair<Boolean, String> pushFile(String nom, String contenu, int clientId)
	{
		// TODO : Save file with name = nom, content = contenu, and owner as clientID?
		String returnString = "";
		Boolean isSuccess = false;
		return new Pair<Boolean, String>(isSuccess, returnString);
	}
	
	private Pair<Boolean, String> syncLocalDirectory()
	{
		// TODO : Send all files to client or send only files with different checksums
		String returnString = "";
		Boolean isSuccess = false;
		return new Pair<Boolean, String>(isSuccess, returnString);
	}

	public int generateClientId() throws RemoteException 
	{
		// TODO : generate client id and save it in a local file
		return compteur++;
	}


	private int compteur = 0;

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
}
