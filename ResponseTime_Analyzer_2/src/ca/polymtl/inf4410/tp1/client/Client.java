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
import javafx.util.Pair;
import java.security.DigestInputStream;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
 
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./ClientId.txt"), charset)) 
		{
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		        clientId = Integer.parseInt(line);
		    }
		} 
		catch (IOException z) 
		{
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
			appelRMILocal(command, argFileName);
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

	private void appelRMILocal(int command, String fileName) {
		try {
			String checkSum = "";
			String contenu = "";

			if(command >= 0 && command <= 5)
			{
				switch(Command.values()[command])
				{
					case LIST : 
						System.out.println(localServerStub.executeList());
						break;
					case GET : 
						checkSum = getCheckSum(fileName);
						Pair<Boolean, String> retourGet = localServerStub.executeGet(fileName, checkSum);
						traiterRetourGet(fileName, retourGet);
						break;
					case LOCK : 
						checkSum = getCheckSum(fileName);
						Pair<Boolean, String> retourLock = localServerStub.executeLock(fileName, clientId, checkSum);
						traiterRetourLock(fileName, retourLock);
						break;
					case CREATE : 
						Boolean retourCreate = localServerStub.executeCreate(fileName);
						traiterRetourCreate(fileName, retourCreate);
						break;
					case PUSH : 
						contenu = getContenu(fileName);
						if(!contenu.equals("-1"))
						{
							Boolean retourPush = localServerStub.executePush(fileName, contenu, clientId);
							traiterRetourPush(fileName, retourPush);
						}
						break;
					case SYNCLOCALDIR : 
						HashMap<String, String> retourSync = localServerStub.executeSyncLocalDirectory();
						traiterRetourSync(retourSync);
						break;
					default : 
						System.out.println("Commande inconnue.");
						break;
				}
			}
			else
			{
				System.out.println("Commande inconnue.");
			}
		} 
		catch (RemoteException e) 
		{
			System.out.println("Erreur applRMILocal : " + e.getMessage());
		}
	}

	/*private void appelRMIDistant(int command, String fileName, int clientId, String contenu, String checkSum) {
		try {
			long start = System.nanoTime();
			System.out.println(distantServerStub.executeCommand(command, fileName, clientId, contenu, checkSum));
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI distant: "
					+ (end - start) + " ns");
		} catch (RemoteException e) {
			System.out.println("Erreur appelRMIDistant : " + e.getMessage());
		}
	}*/

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
			System.out.println("Erreur generateId : " + e.getMessage());
			return -1;
		}
	} 

	private String getContenu(String fileName)
	{
		String content = "";
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./FilesClient/" + fileName), charset)) 
		{
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		        content += line + "\n";
		    }
		} 
		catch (IOException e) 
		{
			System.out.println("Erreur lors de la lecture du fichier. Assurez-vous d'utiliser les caractères US-ASCII");
			return "-1";
		}
		return content;
	}

	private String getCheckSum(String fileName)
	{
		String checkSum = "";
		String contenu = getContenu(fileName);

		if(!contenu.equals("-1"))
		{
			MessageDigest messageDigest;
			try 
			{
				messageDigest = MessageDigest.getInstance("MD5");
				messageDigest.update(contenu.getBytes());
				byte[] messageDigestMD5 = messageDigest.digest();
				StringBuffer stringBuffer = new StringBuffer();
				for (byte bytes : messageDigestMD5) 
				{
					stringBuffer.append(String.format("%02x", bytes & 0xff));
				}

				checkSum = stringBuffer.toString();
			} 
			catch (NoSuchAlgorithmException exception) 
			{
				// TODO Auto-generated catch block
				System.out.println("Erreur getCheckSum client: ");
				exception.printStackTrace();
				checkSum = "-1";
			}
		}
		else
		{
			checkSum = "-1";
		}
		
		return checkSum;
	}

	private void traiterRetourGet(String name, Pair<Boolean, String> retourGet)
	{
		if(retourGet.getKey())
		{
			// TODO : Save file
			writeFile(name, retourGet.getValue());
			System.out.println(name + " synchronisé.");
		}
		else
		{
			System.out.println(retourGet.getValue());
		}
	}

	private void traiterRetourLock(String name, Pair<Boolean, String> retourLock)
	{
		if(retourLock.getKey())
		{
			// TODO : Save file
			writeFile(name, retourLock.getValue());
			System.out.println(name + " synchronisé.");
			System.out.println(name + " verrouillé.");
		}
		else
		{
			System.out.println(retourLock.getValue());
		}
	}

	private void traiterRetourCreate(String name, Boolean retourCreate)
	{
		if(retourCreate)
		{
			System.out.println(name + " ajouté.");
		}
		else
		{
			System.out.println("Opération échouée. Le fichier existe déjà.");
		}
	}

	private void traiterRetourPush(String name, Boolean retourPush)
	{
		if(retourPush)
		{
			System.out.println(name + " a été envoyé au serveur.");
		}
		else
		{
			System.out.println("Opération refusée : vous devez d'abord verrouiller le fichier.");
		}
	}

	private void traiterRetourSync(HashMap<String, String> contentAllFilesServer)
	{
		Iterator it = contentAllFilesServer.entrySet().iterator();
		while (it.hasNext()) 
		{
	        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
	        writeFile(pair.getKey(), pair.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	     System.out.println("Les fichiers du serveur ont été synchronisés.");
	}

	private void writeFile(String name, String content)
	{
		byte data[] = content.getBytes();
		Path p = Paths.get("./FilesClient/" + name);

		try (OutputStream out = new BufferedOutputStream(
		 Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) 
		{
		 out.write(data, 0, data.length);
		} 
		catch (IOException y)
		{
			System.out.println("Création échouée.");
		}
	}
}
