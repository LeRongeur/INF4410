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

		//Vecteurs qui contient les commandes possibles pour la conversion Commande -> int
		commands.add("LIST");
		commands.add("GET");
		commands.add("LOCK");
		commands.add("CREATE");
		commands.add("PUSH");
		commands.add("SYNCLOCALDIR");

		Client client = new Client(distantHostname);
 		
		// On regarde si le client possède un fichier avec son ID
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./ClientId.txt"), charset)) 
		{
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		        clientId = Integer.parseInt(line);
		    }
		} 
		// Sinon, on fait la demande au server pour un nouveau ID et on crée le fichier
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

		// On execute la commande demandée
		int command = convertStringCommandToInt(argCommand);
		client.runCommand(command, argFileName);
	}

	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	// Initialisation du serverStub
	// Prend en entrée l'adresse IP pour un serveur distant
	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	// Appel de la fonction qui traite la commande par le bon serveur (RMI Local pour nos tests, RMI Distant selon les requis du TP)
	// Prend en entrée l'entier qui correspond à la commande désirée ainsi que le nom du fichier qui est fourni comme paramètre lorsque nécessaire.
	private void runCommand(int command, String argFileName) {
		//appelNormal(command);

		if (localServerStub != null) {
			appelRMILocal(command, argFileName);
		}

		//if (distantServerStub != null) {
		//	appelRMIDistant(command);
		//}
	}

	// Fonction réutiliser de la première partie, elle charger le server stub
	// Prend en paramètre l'adresse IP du serveur distant.
	// Elle retourne le server stub
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

	// Fonction qui selon la commande, appelle la bonne fonction du serveur
	// Avant d'appeler les fonctions, elle va chercher les contenus et checkSum, lorsque nécessaire
	// Elle appelle aussi les méthodes pour traiter les valeurs retournées.
	// Elle prend en paramètre l'entier qui correspond à la commande désirée ainsi que le nom du fichier qui est fourni comme paramètre lorsque nécessaire.
	private void appelRMILocal(int command, String fileName) {
		try {
			String checkSum = "";
			String contenu = "";

			if(command >= 0 && command <= 5)
			{
				// Appels des fonctions du serveur ainsi que les fonctions qui traite les valeurs de retour selon la commande
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

	// Fonction qui convertit la commande entrée en entier pour respecter le Switch case de la fonction appelRMI
	// Elle utilise le vecteur initialisé au début du programme
	// Elle prend en entrée la commande entrée par l'utilisateur (sous forme de String)
	// Elle retourne l'entier correspondant à la commande
	private static int convertStringCommandToInt(String command)
	{
		String upperCaseCommand = command.toUpperCase();
		if(commands.contains(upperCaseCommand))
		{
			return commands.indexOf(upperCaseCommand);
		}
		return -1;
	}

	// Fonction qui appel la méthode qui génère un ID unique sur le serveur.
	// Elle retourne le ID généré.
	private int generateId() {
		try {

			return distantServerStub.generateClientId();
		} catch (RemoteException e) {
			System.out.println("Erreur generateId : " + e.getMessage());
			return -1;
		}
	} 

	// Fonction qui retourne le contenu du fichier désiré.
	// Elle prend en paramètre le nom du fichier dont on veut le contenu
	// Elle retourne un String contenant le contenu du fichier désiré.
	private String getContenu(String fileName)
	{
		String content = "";
		Charset charset = Charset.forName("US-ASCII");
		// On vérifie que le fichier existe.
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./FilesClient/" + fileName), charset)) 
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
			System.out.println("Erreur lors de la lecture du fichier. Assurez-vous d'utiliser les caractères US-ASCII");
			return "-1";
		}
		return content;
	}

	// Fonction qui retourne le checkSum MD5 du fichier désiré.
	// Elle prend en paramètre le nom du fichier dont on veut le checkSum MD5.
	// Elle retourne un String correspondant au checkSum MD5 du fichier désiré.
	private String getCheckSum(String fileName)
	{
		String checkSum = "";
		// On récupère le contenu du fichier
		String contenu = getContenu(fileName);

		// On s'assure que la lecture du fichier a fonctionné
		if(!contenu.equals("-1"))
		{
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
				System.out.println("Erreur getCheckSum client: ");
				exception.printStackTrace();
				checkSum = "-1";
			}
		}
		else
		{
			checkSum = "-1";
		}
		// On retourne le checkSum
		return checkSum;
	}

	// Fonction qui traite le retour de la commande GET, elle enregistre le contenu obtenu du serveur dans le fichier local.
	// Elle prend en paramètre le nom du fichier en question et la paire qui contient un booléen et un String.
	// Ce booléen indique si l'opération à réussie ou échouée et le String contient le contenu du fichier si l'opération a 
	// réussie ou le message d'erreur si l'opération a échouée.
	private void traiterRetourGet(String name, Pair<Boolean, String> retourGet)
	{
		if(retourGet.getKey())
		{
			// Si l'opération a réussie, on sauvegarde le contenu retourné dans le fichier local.
			writeFile(name, retourGet.getValue());
			System.out.println(name + " synchronisé.");
		}
		else
		{
			// Si l'opération a échouée on écrit le message d'erreur dans la console.
			System.out.println(retourGet.getValue());
		}
	}

	// Fonction qui traite le retour de la commande LOCK, elle enregistre le contenu obtenu du serveur dans le fichier local.
	// Elle prend en paramètre le nom du fichier en question et la paire qui contient un booléen et un String.
	// Ce booléen indique si l'opération à réussie ou échouée et le String contient le contenu du fichier si l'opération a 
	// réussie ou le message d'erreur si l'opération a échouée.
	private void traiterRetourLock(String name, Pair<Boolean, String> retourLock)
	{
		if(retourLock.getKey())
		{
			// Si l'opération a réussie, on sauvegarde le contenu retourné dans le fichier local.
			writeFile(name, retourLock.getValue());
			System.out.println(name + " synchronisé.");
			System.out.println(name + " verrouillé.");
		}
		else
		{
			// Si l'opération a échouée on écrit le message d'erreur dans la console.
			System.out.println(retourLock.getValue());
		}
	}

	// Fonction qui traite le retour de la commande CREATE, elle ne fait qu'écrire le résultat de l'opoération dans la console.
	// Elle prend en paramètre le nom du fichier en question et le booléen retourné par la fonction du serveur
	// Ce booléen indique si l'opération à réussie ou échouée.
	private void traiterRetourCreate(String name, Boolean retourCreate)
	{
		if(retourCreate)
		{
			// Si l'opération a réussie, on écrit que le fichier a bel et bien été crée.
			System.out.println(name + " ajouté.");
		}
		else
		{
			// Si l'opération a échouée on écrit le message d'erreur dans la console.
			System.out.println("Opération échouée. Le fichier existe déjà.");
		}
	}

	// Fonction qui traite le retour de la commande PUSH, elle ne fait qu'écrire le résultat de l'opoération dans la console.
	// Elle prend en paramètre le nom du fichier en question et le booléen retourné par la fonction du serveur
	// Ce booléen indique si l'opération à réussie ou échouée.
	private void traiterRetourPush(String name, Boolean retourPush)
	{
		if(retourPush)
		{
			// Si l'opération a réussie, on écrit que le fichier a bel et bien été envoyé au serveur.
			System.out.println(name + " a été envoyé au serveur.");
		}
		else
		{
			// Si l'opération a échouée on écrit le message d'erreur dans la console.
			System.out.println("Opération refusée : vous devez d'abord verrouiller le fichier.");
		}
	}

	// Fonction qui traite le retour de la commande SYNCLOCALDIR, elle enregistre les contenus obtenus du serveur dans les fichiers locaux.
	// Elle prend en paramètre la HashMap qui contient les noms et les contenus de tous les fichiers sur le serveur.
	private void traiterRetourSync(HashMap<String, String> contentAllFilesServer)
	{
		// Pour chacune des entrées de la HashMap, on écrit dans le fichier correspondant le contenu du serveur
		Iterator it = contentAllFilesServer.entrySet().iterator();
		while (it.hasNext()) 
		{
			Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
			writeFile(pair.getKey(), pair.getValue());
			it.remove(); // avoids a ConcurrentModificationException
		}
		System.out.println("Les fichiers du serveur ont été synchronisés.");
	}

	// Fonction utilitaire qui écrit le contenu désiré dans le fichier désiré.
	// Elle prend en paramètre le nom du fichier à créer ou à modifier ainsi que le contenu à y inscrire.
	private void writeFile(String name, String content)
	{
		// On convertit le contenu à inscrire en tableau de Bytes
		byte data[] = content.getBytes();
		// On indique que le répertoire dans lequel le fichier sera situé est "./FilesClient/"
		Path p = Paths.get("./FilesClient/" + name);

		// On indique que si le fichier n'existe pas, on le crée et qu'on remplace sont contenu (au lieu de le concaténer par exemple)
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) 
		{
			// On écrit le contenu
			out.write(data, 0, data.length);
		} 
		catch (IOException y)
		{
			// On reporte un échec.
			System.out.println("Création échouée.");
		}
	}
}
