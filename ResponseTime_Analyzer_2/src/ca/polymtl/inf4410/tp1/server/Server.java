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

	public String executeList() throws RemoteException
	{
		System.out.println("Executing command LIST...");
		// TODO : Print each files in the vector or map, with the UserID that has it locked
		String returnString = "";
	    for (String name: files.keySet())
	    {
	    	if(files.get(name) > -1)
	    	{
	    		String value = files.get(name).toString();
	    		returnString += name + " verrouillé par client " + value + "\n"; 
	    	}
	    	else
	    	{
	    		returnString += name + " non verrouillé" + "\n";	
	    	}
 
		}
		returnString += files.size() + " fichier(s)";
		return returnString;
	}
	
	public Pair<Boolean, String> executeGet(String nom, String checkSum) throws RemoteException
	{
		System.out.println("Executing command GET...");
		// TODO : Send file with name = nom if parameterChecksum different from localChecksum (in vector or map)
		String returnString = "";
		Boolean isDifferent;
		String checkSumServer = getCheckSum(nom);

		if(!files.containsKey(nom))
		{
			returnString = "Fichier non existant.";
			isDifferent = false;
		}
		else if(!checkSumServer.equals(checkSum))
		{
			// TODO : Send File
			returnString = getContenu(nom);
			isDifferent = true;
		}
		else
		{
			// TODO : ...
			returnString = "Fichier déjà à jour.";
			isDifferent = false;
		}

		return new Pair<Boolean, String>(isDifferent, returnString); 
	}

	public Pair<Boolean, String> executeLock(String nom, int clientId, String checkSum) throws RemoteException
	{
		System.out.println("Executing command LOCK...");
		// TODO : change lockID of file with name = nom for clientID
		// TODO : Something with checksum apparently
		String returnString = "";
		Boolean isDifferent = false;
		String checkSumServer = getCheckSum(nom);

		if(!files.containsKey(nom))
		{
			returnString = "Fichier non existant.";
			isDifferent = false;
		}
		else if(files.get(nom) != -1)
		{
			returnString = "Fichier est déjà verrouillé.";
			isDifferent = false;
		}
		else if(!checkSumServer.equals(checkSum))
		{
			// TODO : Send File
			isDifferent = true;
			returnString = getContenu(nom);
			files.put(nom, clientId);
		}
		else
		{
			// TODO : ...
			isDifferent = false;
			returnString = "Fichier identique à la copie locale.";
			files.put(nom, clientId);
		}

		return new Pair<Boolean, String>(isDifferent, returnString);
	}

	public Boolean executeCreate(String name) throws RemoteException
	{
		System.out.println("Executing command CREATE...");
		// TODO : Create file with name = nom
		String returnString = "";
		Boolean isSuccess = false;
		if(files.containsKey(name))
		{
			returnString += "Opération refusée: le fichier existe déjà";
			isSuccess = false;
		}
		else
		{
		   Path p = Paths.get("./Files/" + name);

		   try (OutputStream out = new BufferedOutputStream(
		     Files.newOutputStream(p, CREATE, APPEND))) 
		   {
		     files.put(name, -1);
		     returnString += name + " Fichier ajouté.";
		   }
		   catch (IOException y) 
		   {
		     returnString += y;
		   }
		   isSuccess = true;
		}
		return isSuccess;
	}

	public Boolean executePush(String nom, String contenu, int clientId) throws RemoteException
	{
		System.out.println("Executing command PUSH...");
		// TODO : Save file with name = nom, content = contenu, and owner as clientID?
		Boolean isSuccess = false;

		if(!files.containsKey(nom))
		{
			System.out.println("Fichier non existant.");
			isSuccess = false;
		}
		else if(clientId == files.get(nom))
		{
			byte data[] = contenu.getBytes();
			Path p = Paths.get("./Files/" + nom);

			try (OutputStream out = new BufferedOutputStream(
			 Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) 
			{
			 out.write(data, 0, data.length);
			}
			catch (IOException y) 
			{
			 	System.out.println("Erreur write push. " + y.getMessage());
			}
			files.put(nom, -1);
			isSuccess = true;
		}
		else
		{
			isSuccess = false;
		}

		return isSuccess;
	}
	
	public HashMap<String, String> executeSyncLocalDirectory() throws RemoteException
	{
		System.out.println("Executing command SYNCLOCALDIR...");
		// TODO : Send all files to client or send only files with different checksums
		HashMap<String, String> contentAllServerFiles = new HashMap<String, String>();

		Iterator it = files.entrySet().iterator();
		while (it.hasNext()) 
		{
	        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
	        contentAllServerFiles.put(pair.getKey(),getContenu(pair.getKey()));
	    }

		return contentAllServerFiles;
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
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./Files/" + fileName), charset)) 
		{
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		        content += line + "\n";
		    }
		} 
		catch (IOException e) 
		{
			System.out.println("Erreur getContenu : " + e.getMessage());
			// TODO : Get sur un fichier qui nest pas sur le serveur
		}
		return content;
	}

	private String getCheckSum(String fileName)
	{
		String checkSum = "";
		String contenu = getContenu(fileName);

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(contenu.getBytes());
			byte[] messageDigestMD5 = messageDigest.digest();
			StringBuffer stringBuffer = new StringBuffer();
			for (byte bytes : messageDigestMD5) {
				stringBuffer.append(String.format("%02x", bytes & 0xff));
			}

			checkSum = stringBuffer.toString();
		} catch (NoSuchAlgorithmException exception) {
			// TODO Auto-generated catch block
			System.out.println("Erreur getCheckSum : ");
			exception.printStackTrace();
		}
		return checkSum;
	}
}
