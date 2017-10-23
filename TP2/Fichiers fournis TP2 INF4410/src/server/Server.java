package server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import shared.ServerInterface;
import shared.ServerInterface.Command;
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

	// HashMap qui contiendra la totalité des noms des fichiers qui se trouve sur le serveur ainsi que le ID du client qui détient le lock (-1 si personne ne détient le lock).
	private HashMap<String, Integer> files = new HashMap<String, Integer>();

	// Lorsqu'on lance le serveur, on l'initialise, on remplit la HashMap avec les fichiers sur le serveur et on lance le serveur
	public static void main(String[] args) {
		Server server = new Server();
		server.loadAllFiles();
		server.run();
	}

	public Server() {
		super();
	}

	// Fonction qui lie le server stub au RMIRegistry
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

	// Fonction qui charge tous les noms de fichiers dans la HashMap et indique que personne ne détient le lock (en insérant -1).
	private void loadAllFiles()
	{
		// On indique que le dossier qui contient les fichiers est "./Client/"
		File file = new File("./Files/");
		for (final File fileEntry : file.listFiles()) {
			if (!fileEntry.isDirectory()) 
			{
				// Pour chaque fichier, on insère le nom dans la HashMap et on indique que personne ne détient le lock (en insérant -1).
				files.put(fileEntry.getName(), -1);
			}
		}
	}

	// Fontion appelée par les clients qui exécute la commande LIST
	// Elle retourne un String qui ne devra qu'être écrit dans la console du client.
	// Ce string contient les noms des fichiers ainsi qu'une indication concernant qui détient le lock de ces fichiers.
	public String executeList() throws RemoteException
	{
		System.out.println("Executing command LIST...");
		String returnString = "";

		// Pour chaque fichier dans la HashMap, on fait une concaténation du nom du fichier ainsi qu'une indication concernant qui détient le lock de ce fichier.
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
	
	// Fonction appelée par les clients qui exécute la commande GET
	// Elle prend en paramètre le nom du fichier et le checkSum du contenu de ce fichier désiré.
	// Elle retourne une paire qui contient un booléen et un String.
	// Ce booléen indique si l'opération à réussie ou échouée et le String contient le contenu du fichier si l'opération a 
	// réussie ou le message d'erreur si l'opération a échouée.
	public Pair<Boolean, String> executeGet(String nom, String checkSum) throws RemoteException
	{
		System.out.println("Executing command GET...");
		// String qui sera retourné
		String returnString = "";
		// Booléen qui sera retourné et qui indique si l'opération a échouée ou réussie
		Boolean isDifferent;

		// On trouve le checkSum de la version du serveur du fichier désiré.
		String checkSumServer = getCheckSum(nom);

		// On s'assure que le fichier existe sur le serveur
		if(!files.containsKey(nom))
		{
			returnString = "Fichier non existant.";
			isDifferent = false;
		}
		// On compare les deux checkSum (celui du client et celui du serveur)
		else if(!checkSumServer.equals(checkSum))
		{
			// Si les deux fichiers sont différents, on retourne le contenu
			returnString = getContenu(nom);
			isDifferent = true;
		}
		else
		{
			// Si les deux fichiers sont pareils, on ne transfert pas le fichier pour rien
			// On met donc le booléen à false pour que le client ne fasse qu'écrire le message dans sa console
			returnString = "Fichier déjà à jour.";
			isDifferent = false;
		}

		return new Pair<Boolean, String>(isDifferent, returnString); 
	}

	// Fonction appelée par les clients qui exécute la commmande LOCK.
	// Elle prend en paramètre le nom du fichier désiré, le ID du client et le checkSum de la version client du fichier désiré.
	// Elle retourne une paire qui contient un booléen et un String.
	// Ce booléen indique si l'opération à réussie ou échouée et le String contient le contenu du fichier si l'opération a 
	// réussie ou le message d'erreur si l'opération a échouée.
	public Pair<Boolean, String> executeLock(String nom, int clientId, String checkSum) throws RemoteException
	{
		System.out.println("Executing command LOCK...");
		// String qui sera retourné
		String returnString = "";
		// Booléen qui sera retourné et qui indique si l'opération a échouée ou réussie
		Boolean isDifferent = false;

		// On trouve le checkSum de la version du serveur du fichier désiré.
		String checkSumServer = getCheckSum(nom);

		// On s'assure que le fichier existe sur le serveur
		if(!files.containsKey(nom))
		{
			returnString = "Fichier non existant.";
			isDifferent = false;
		}
		// Si le fichier est déjà verrouillé, l'opération échoue
		else if(files.get(nom) != -1)
		{
			returnString = "Fichier est déjà verrouillé.";
			isDifferent = false;
		}
		// On compare les deux checkSum (celui du client et celui du serveur)
		else if(!checkSumServer.equals(checkSum))
		{
			// Si les deux fichiers sont différents, on retourne le contenu
			isDifferent = true;
			returnString = getContenu(nom);
			// On indique que le client à verrouiller le fichier
			files.put(nom, clientId);
		}
		else
		{
			// Si les deux fichiers sont pareils, on ne transfert pas le fichier pour rien
			// On met donc le booléen à false pour que le client ne fasse qu'écrire le message dans sa console
			isDifferent = false;
			returnString = "Fichier identique à la copie locale.";
			// On indique que le client à verrouiller le fichier
			files.put(nom, clientId);
		}

		return new Pair<Boolean, String>(isDifferent, returnString);
	}

	// Fonction appelée par les clients qui exécute la fonction CREATE.
	// Elle prend en paramètre le nom du fichier à créer.
	// Elle retourne un booléen qui indique si l'opération a échouée.
	public Boolean executeCreate(String name) throws RemoteException
	{
		System.out.println("Executing command CREATE...");
		// Booléen qui sera retourné et qui indique si l'opération a échouée ou réussie
		Boolean isSuccess = false;

		// On vérifie que le nom du fichier à créer ne se retrouve pas déjà dans la HashMap du serveur
		if(files.containsKey(name))
		{
			// On indique que l'opération échoue, puisque le fichier existe déjà.
			isSuccess = false;
		}
		else
		{
			// On indique que le répertoire dans lequel le fichier sera situé est "./Files/"
			Path p = Paths.get("./Files/" + name);

			// On indique que si le fichier n'existe pas, on le crée et qu'on remplace sont contenu (au lieu de le concaténer par exemple)
			try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND))) 
			{
				// On insère le nom dans la HashMap et on indique que personne ne détient le lock (en insérant -1).
				files.put(name, -1);
				// On indique que l'opération est réussie.
				isSuccess = true;
			}
			catch (IOException y) 
			{
				// Si une exception survient, on indique que l'opération échoue.
				isSuccess = false;
			}
		}
		return isSuccess;
	}

	// Fonction appelée par les clients qui exécute la commande PUSH.
	// Elle prend en paramètre le nom du fichier, son contenu et le ID du client qui désire faire un PUSH.
	// Elle retourne un booléen qui indique si l'opération a réussie ou échouée.
	public Boolean executePush(String nom, String contenu, int clientId) throws RemoteException
	{
		System.out.println("Executing command PUSH...");
		// Booléen qui sera retourné et qui indique si l'opération a échouée ou réussie
		Boolean isSuccess = false;
	
		// On vérifie que le fichier existe sur le serveur
		if(!files.containsKey(nom))
		{
			// On indique que l'opération échoue.
			System.out.println("Fichier non existant.");
			isSuccess = false;
		}
		// On vérifie que l'utilisateur détient le lock de ce fichier
		else if(clientId == files.get(nom))
		{
			// On met à jour le fichier avec le contenu de la version du client
			byte data[] = contenu.getBytes();

			// On indique que le répertoire dans lequel le fichier sera situé est "./Files/"
			Path p = Paths.get("./Files/" + nom);

			// On indique que si le fichier n'existe pas, on le crée et qu'on remplace sont contenu (au lieu de le concaténer par exemple)
			try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) 
			{
				// On insère le contenu passé en paramètre dans le fichier crée
				out.write(data, 0, data.length);
				// On met à jour la valeur de lock dans la HashMap pour indiquer que personne ne détient le lock.
				files.put(nom, -1);
				// On indique que l'opération est réussie.
				isSuccess = true;
			}
			catch (IOException y) 
			{
				// Si l'opération échoue, on l'écrit dans la console du serveur
				System.out.println("Erreur write push. " + y.getMessage());
				// On indique que l'opération échoue.
				isSuccess = false;
			}
		}
		else
		{
			// On indique que l'opération échoue.
			isSuccess = false;
		}

		return isSuccess;
	}
	
	// Fonction appelée par les clients qui exécute la commande SYNCLOCALDIR
	// Elle retourne une HashMap qui contient les noms des fichiers et leurs contenus.
	public HashMap<String, String> executeSyncLocalDirectory() throws RemoteException
	{
		System.out.println("Executing command SYNCLOCALDIR...");
		// Déclaration de la HashMap qui sera retournée
		HashMap<String, String> contentAllServerFiles = new HashMap<String, String>();

		// Pour tous les fichiers dans la HashMap du serveur, on remplit la HashMap à retourner avec les noms et les contenus des fichiers
		Iterator it = files.entrySet().iterator();
		while (it.hasNext()) 
		{
			Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
			contentAllServerFiles.put(pair.getKey(),getContenu(pair.getKey()));
		}

		return contentAllServerFiles;
	}

	// Fonction appelée par les clients qui retourne un ID.
	public int generateClientId() throws RemoteException 
	{
		// Retourne le compteur
		// De cette manière, tous les utilisateurs auront un ID différent qui correspondera à l'ordre dans lequel ils ont demandé leur ID au serveur.
		return compteur++;
	}

	// Compteur utilisé pour générer les IDs client
	private int compteur = 0;

	// Fonction utilitaire qui retourne le contenu du fichier désiré.
	// Elle prend en paramètre le nom du fichier dont on veut le contenu
	// Elle retourne un String qui contient le contenu du fichier désiré.
	private String getContenu(String fileName)
	{
		String content = "";
		Charset charset = Charset.forName("US-ASCII");
		// On vérifie que le fichier existe.
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./Files/" + fileName), charset)) 
		{
			// On fait un concaténation de chacune des lignes du fichier à notre String
		    	String line = null;
		    	while ((line = reader.readLine()) != null) 
		    	{	
				content += line + "\n";
		    	}
		} 
		catch (IOException e) 
		{
			System.out.println("Erreur getContenu : " + e.getMessage());
		}
		return content;
	}

	// Fonction utilitaire qui retourne le checkSum MD5 du fichier désiré.
	// Elle prend en paramètre le nom du fichier dont on veut le checkSum MD5.
	// Elle retourne un String correspondant au checkSum MD5 du fichier désiré.
	private String getCheckSum(String fileName)
	{
		String checkSum = "";
		// On récupère le contenu du fichier
		String contenu = getContenu(fileName);

		MessageDigest messageDigest;
		try 
		{
			// On initialise le digéreur de message
			messageDigest = MessageDigest.getInstance("MD5");
			// On indique que la String à digérer est le contenu du fichier désiré.
			messageDigest.update(contenu.getBytes());
			// On applique l'algorithme de digestion
			byte[] messageDigestMD5 = messageDigest.digest();
			// On met la valeur digérée (checkSum) dans un string
			StringBuffer stringBuffer = new StringBuffer();
			for (byte bytes : messageDigestMD5) 
			{
				stringBuffer.append(String.format("%02x", bytes & 0xff));
			}

			checkSum = stringBuffer.toString();
		} 
		catch (NoSuchAlgorithmException exception) 
		{
			System.out.println("Erreur getCheckSum : ");
			exception.printStackTrace();
		}
		return checkSum;
	}
}
