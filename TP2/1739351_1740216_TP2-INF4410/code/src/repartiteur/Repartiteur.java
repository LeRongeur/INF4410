package repartiteur;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import shared.ServerInterfaceCalculator;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

// Interface callback pour les threads
// De cette façon, lorsqu'un thread fini de calculer, il appelle un callback qui ajoute le résultat de la tâche au résultat qu'on avait avant
// Aussi, si un serveur tombe en panne, on appelle un callback qui rajoute les opérations que ce serveur devait effectuer à la liste des opérations à effectuer (mode sécurisé seulement)
interface Callback {
    void putBackOps(List<Pair<Integer, Integer>> operations, int id);
    void addResult(int result);
}

// Classe Repartiteur qui répartit charge les stubs de chacun des serveurs et répartit les calculs.
public class Repartiteur implements Callback {

	private static Boolean isSecured = true; // Booléen qui indique si les calculs sont effectués en mode sécurisé ou non.
	private static AtomicBoolean resultVerified = new AtomicBoolean(); // Booléen qui est mis a true lorsqu'un résultat est obtenu 2 fois pour la même tâche

	private Vector<String> calcIPs = new Vector<String>(); // Vecteur de tous les adresses IP lues dans le fichier CalculatorIPs.txt
	private Vector<Integer> calcPorts = new Vector<Integer>(); // Vecteur de tous les ports lus dans le fichier CalculatorIPs.txt
	private Vector<Integer> calcQs = new Vector<Integer>(); // Vecteur de tous les q lus dans le fichier CalculatorIPs.txt

	private Vector<ServerInterfaceCalculator> calcStubs = new Vector<ServerInterfaceCalculator>(); // Vecteur de tous les stubs des serveurs définis dans le fichier CalculatorIPs.txt

	private static List calcAvailability = Collections.synchronizedList(new ArrayList()); // Liste de Booléen qui indique quels serveurs sont disponibles. (Mis a false lorsqu'un serveur tombe en panne)

	private static List allOps = Collections.synchronizedList(new ArrayList()); // Liste de tous les opérations qui restent à effectuer.

	private static ConcurrentHashMap<Integer, Integer> threadResults = new ConcurrentHashMap<Integer, Integer>(); // Map des résultats obtenus selon le thread. (Utilisé seulement en mode non-sécurisé)
	private static ConcurrentHashMap<Integer, Integer> verifiedResults = new ConcurrentHashMap<Integer, Integer>(); // Map des résultats obtenus selon le thread. Dans le mode non-sécurisé, les résultats sont mis dans cette map que lorsqu'ils sont vérifiés.
	
	private Vector<CalcThread> calcThreads = new Vector<CalcThread>(); // Vecteur de tous les threads utilisés. (Un par serveur de calcul)
	private static AtomicInteger compteurHash = new AtomicInteger(); // Sert de clé dans les maps threadResults et verifiedResults.

	// Lorsqu'on lance le serveur, on l'initialise, on remplit la HashMap avec les fichiers sur le serveur et on lance le serveur
	public static void main(String[] args) {
		// Initialisation des variables
		String fileName = null;
		Repartiteur repartiteur = new Repartiteur();
		resultVerified.set(false);

		// Lecture des arguments
		if (args.length > 1)
		{	
			//System.out.println("Debut lecture des arguments...");
			fileName = args[0];
			Repartiteur.isSecured = Boolean.valueOf(args[1]);
			//System.out.println("Secured : " + Repartiteur.isSecured);
			//System.out.println("Fin lecture arguments.");
			//System.out.println("Debut chargement des operations...");
			repartiteur.loadCalculations(fileName);
			//System.out.println("Fin chargement des operations.");
			//System.out.println("Operations : " + Repartiteur.allOps);
			//System.out.println("Debut chargement des serveurs de calculs...");
			repartiteur.loadAllCalculators();
			//System.out.println("Fin chargement des serveurs de calculs.");

			// Appel de la fonction de répartition selon le mode.
			if(Repartiteur.isSecured)
			{
				repartiteur.repartirCalcsSecu();
			}
			else
			{
				repartiteur.repartirCalcsUnsecu();
			}
		}
	}

	// Fonction qui charge les serveurs de calculators selon les entrées dans le fichier CalculatorIPs.txt
	private void loadAllCalculators()
	{
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./CalculatorIPs.txt"), charset)) 
		{
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		    	// Chargement du serveur correspondant à la ligne lue
				loadCalculator(line);
		    }
		} 
		catch (IOException z) 
		{
		   System.out.println("CatchExceptionInLoadAllCalculators");
		   System.out.println(z);	
		}
		//System.out.println(calcIPs);
		//System.out.println(calcStubs);
		//System.out.println(calcQs);
	}

	// Fonction qui charge le serveur selon la ligne passée en paramètre
	private void loadCalculator(String line)
	{
		// On sépare les informations dans la ligne et on entre chacune d'elle dans les vecteurs respectifs. On charge aussi le stub dans le vectuer correspondant, ainsi que le thread.
		String[] splitted = line.split("\\s+");
		calcIPs.add(splitted[0]);
		calcPorts.add(Integer.parseInt(splitted[1]));
		calcQs.add(Integer.parseInt(splitted[2]));
		ServerInterfaceCalculator stub = loadServerStubC(splitted[0],Integer.parseInt(splitted[1]));
		calcStubs.add(stub);
		calcThreads.add(new CalcThread(calcQs.size()-1,stub, Repartiteur.isSecured));
		calcAvailability.add(true);
	}

	// Fonction qui charge les opérations lues à partir du fichier d'opérations
	private void loadCalculations(String fileName)
	{
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), charset)) 
		{
		    String line = null;
		    // Pour chaque ligne si on lit pell x, on crée une entré <0, x> dans le vecteur d'opérations et si on lit prime x, on crée une entré <1, x> dans le vecteur d'opérations.
		    while ((line = reader.readLine()) != null) 
		    {
				int op;
				String[] splitted = line.split("\\s+");
				//System.out.println("Operation : " + splitted[0]);
				if(splitted[0].equals("pell"))
				{
					op = 0;
				}
				else if (splitted[0].equals("prime"))
				{
					op = 1;
				}
				else
				{
					op = -1;
				}
				Pair<Integer, Integer> pair = new Pair<Integer, Integer>(op, Integer.parseInt(splitted[1]));
				Repartiteur.allOps.add(pair);
		    }
		} 
		catch (IOException z) 
		{
		   System.out.println(z);	
		}
	}

	// Fonction qui charge un stub selon l'adresse IP et le port passé en paramètre.
	private ServerInterfaceCalculator loadServerStubC(String hostname, int port) {
		ServerInterfaceCalculator stub = null;

		try {
			//System.out.println("hostname : " + hostname);
			//System.out.println("port : " + port);
			Registry registry = LocateRegistry.getRegistry(hostname,port);
			stub = (ServerInterfaceCalculator) registry.lookup("server");
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

	// Fonction qui répartit les opérations aux serveurs de calculs en mode sécurisé.
	private void repartirCalcsSecu()
	{
		// Initialisation des variables.
		Boolean acceptedDemand;
		int nbreOp;		
		int resultat = 0;
		Boolean finOperations = false;
		//System.out.println("Debut de la reparition... ");
		//System.out.println("Operations : " + Repartiteur.allOps);

		//	 Tant qu'il reste des opérations non calculées :
		while(Repartiteur.allOps.size() > 0)
		{
			//System.out.println("Nouvelle iteration sur les serveurs de calculs dispos");
			//System.out.println("Reste " + (Repartiteur.allOps.size()) + "operation a distribuer");

			// Pour chaque serveurs existant :
			for(int indexCalc = 0; indexCalc < calcStubs.size() && allOps.size() > 0; indexCalc++)
			{
				// Si le serveur est en panne, on passe au suivant
				if(!(Boolean)calcAvailability.get(indexCalc))
					continue;

				//System.out.println("Server numero " + indexCalc);
				// On tente de lui passer q+1 opérations
				nbreOp = calcQs.get(indexCalc) + 1;
				acceptedDemand = true;
				//System.out.println("");
				//System.out.println("Debut trouver u qui nest plus accepte");

				// Tant qu'il accepte la demande, on continue de lui demander en augmentant le u
				while(acceptedDemand)
				{	
					// Augmentation du u
					nbreOp += 0.2 * nbreOp + 1;
					try
					{	
						//System.out.println("Essai avec : " + nbreOp);
						acceptedDemand = calcStubs.get(indexCalc).demandeOp(nbreOp);
						//System.out.println("Demande acceptee : " + acceptedDemand);

						// On ne veut pas lui passer plus d'opérations qu'il n'en reste
						if(nbreOp >= Repartiteur.allOps.size())
						{
							acceptedDemand = false;
							nbreOp = Repartiteur.allOps.size();
							finOperations = true;
						}
					}	
					catch (RemoteException e) 
					{
						// Si le serveur est tombé en panne, on indique qu'il n'est plus disponible.
						System.out.println("Erreur appel demandeOp accepted : " + e.getMessage());
						acceptedDemand = false;
						calcAvailability.set(indexCalc,false);
					}
				}
				//System.out.println("Debut trouver u qui est accepte");

				// Une fois qu'on a trouvé un nombre u qu'il refuse, on diminue u, jusqu'à ce qu'il l'accepte.
				while(!acceptedDemand && !finOperations)
				{	
					// Diminution de u
					nbreOp--;
					try
					{
						//System.out.println("Essai avec : " + nbreOp);
						acceptedDemand = calcStubs.get(indexCalc).demandeOp(nbreOp);
						//System.out.println("Demande refusee : " + !acceptedDemand);
					}	
					catch (RemoteException e) 
					{
						// Si le serveur est tombé en panne, on indique qu'il n'est plus disponible.
						System.out.println("Erreur appel demandeOp !accepted: " + e.getMessage());
						acceptedDemand = true;
						calcAvailability.set(indexCalc,false);
					}
				}
				//System.out.println("Trouve : " + nbreOp);

				// On trouve la tâche qu'on doit envoyer au serveur selon le nombre d'opérations trouvé.
				List<Pair<Integer, Integer>> subList = Repartiteur.allOps.subList(0,nbreOp);

				//System.out.println("Essai calcul avec : " + subList);
				ArrayList<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>(subList);
				// On associe les opérations aux threads du serveur
				calcThreads.get(indexCalc).setOperations(list);
				//System.out.println();
				// On fait le calcul
				calcThreads.get(indexCalc).run();
				// On ôte les opérations qu'on vient de passer au serveur de la liste des opérations restantes
				removeOps(nbreOp);
				//System.out.println("Fin de literation");
			}
			// S'il ne reste aucun serveur disponible, on arrête le programme
			if(!calcAvailability.contains(true))
			{
				//System.out.println("No more servers available. Exiting.");
				return;
			}
		}
		//System.out.println("Done doing calculations in Repartiteur...");
		//System.out.println(Repartiteur.verifiedResults);

		// On affiche le résultat final
		System.out.println("Result is: " + finalSum());
	}
	
	// Fonction qui calcule la somme finale selon les résultats obtenus
	public int finalSum()
	{
		int resultat = 0;
		for(Integer value : Repartiteur.verifiedResults.values())
		{
			resultat += value;
			resultat %= 4000;
		}
		return resultat;
	}
	
	// Fonction callback qui est appelée en mode sécurisé lorqu'un serveur tombe en panne, elle remet les opérations qui lui avaient été demandées dans la liste d'opérations restantes
	public void putBackOps(List<Pair<Integer, Integer>> operations, int id) {
		//System.out.println("I've been called back to put back ops");
		if(Repartiteur.isSecured)
		{
			// TODO verify somehow? with the continu later
			for(int i = 0; i < operations.size(); i++)
			{
				Repartiteur.allOps.add(operations.get(i));
			}
		}
		// On indique aussi que ce serveur n'est plus disponible.
		calcAvailability.set(id,false);
	}

	// Fonction qui répartit les opérations à effectuer en mode non sécurisé
	private void repartirCalcsUnsecu()
	{
		// Initialisation des variavbles.
		int resultat = 0;
		//System.out.println("Debut de la reparition non-sécurisée... ");
		//System.out.println("Operations : " + Repartiteur.allOps);

		// On met le paramètre u égal au plus petit q des serveurs
		int nbreOp = Collections.min(calcQs);
		Boolean acceptedDemand = false;

		// Tant qu'il reste des opérations à calculer
		while(Repartiteur.allOps.size() > 0)
		{
			// On ne veut pas faire plus d'opérations qu'il n'en reste
			nbreOp = nbreOp > allOps.size() ? allOps.size() : nbreOp;

			// Pour chaque serveur de calcul :
			for(int indexCalc = 0; indexCalc < calcStubs.size(); indexCalc++)
			{
				// Si le serveur est en panne, on passe au prochain
				if(!(Boolean)calcAvailability.get(indexCalc))
					continue;

				try
				{
					//	 On fait la demande pour u opérations
					acceptedDemand = calcStubs.get(indexCalc).demandeOp(nbreOp);
				}
				catch(RemoteException e)
				{
					// Si le serveur est tombé en panne, on indique qu'il n'est plus disponible.
					System.out.println("Erreur appel demandeOp : " + e.getMessage());
					calcAvailability.set(indexCalc,false);
				}
				// Si le serveur a accepté la demande, on effectue les calculs
				if(acceptedDemand)
				{
					//System.out.println("Trouve : " + nbreOp);

					// On trouve la tâche qu'on doit envoyer au serveur selon le nombre d'opérations trouvé.
					List<Pair<Integer, Integer>> subList = Repartiteur.allOps.subList(0,nbreOp);

					//System.out.println("Essai calcul avec : " + subList);
					ArrayList<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>(subList);
					// On associe les opérations aux threads du serveur
					calcThreads.get(indexCalc).setOperations(list);
					//System.out.println();

					// On fait le calcul
					calcThreads.get(indexCalc).run();
				}
			}
			// Une fois que tous les serveurs ont fini de calculer, si on a obtenu au moins 2 fois la même réponse
			if(resultVerified.get())
			{
				// On enlève les opérations effectuées de la liste d'opérations restantes
				removeOps(nbreOp);
				Repartiteur.resultVerified.set(false);
				// On ajoute le résultats dans la liste de résultats vérifiés
				addToVerifiedResults();
				// On efface la liste des résultats obtenus pour la tâche en cours.
				Repartiteur.threadResults.clear();
			}
			// S'il ne reste aucun serveur disponible, on arrête le programme
			if(!calcAvailability.contains(true))
			{
				//System.out.println("No more servers available. Exiting.");
				return;
			}
		}
		//System.out.println("Done doing calculations in Repartiteur...");
		//System.out.println(Repartiteur.verifiedResults);

		// On affiche le résultat final
		System.out.println("Result is: " + finalSum());
	}

	// Fonction callback qui ajoute le résultat obtenu pour la tâche au vecteur threadResults ou verifiedResults selon le mode utilisé
	public void addResult(int result) {
		//System.out.println("I've been called back and result is : " + result);
		//System.out.println("Am I secured? : " + Repartiteur.isSecured);
		if(Repartiteur.isSecured)
		{
			// Si nous sommes en mode sécurié, nous ajoutons le résultat directement dans le vecteurs verifiedResults.
			Repartiteur.verifiedResults.put(compteurHash.get(),result);
		}
		else
		{
			// Si nous sommes en mode non sécurisé, nous ajoutons le résultat dans le vecteur threadResults
			// Si ce vecteur contenait déjà ce résultat, on indique que ce dernier est vérifié
			if(Repartiteur.threadResults.contains(result))
			{
				Repartiteur.threadResults.put(compteurHash.get(),result);
				Repartiteur.resultVerified.set(true);
			}
			else
			{
				Repartiteur.threadResults.put(compteurHash.get(),result);
			}
		}
		Repartiteur.compteurHash.incrementAndGet();
	}

	// Fonction qui enlève nbreOp opérations de la liste d'opérations restantes
	private void removeOps(int nbreOp)
	{
		for(int i = 0; i < nbreOp; i++)
		{
			Repartiteur.allOps.remove(0);
		}
	}

	// Fonction qui ajoute le résultat qui se retrouve en double dans le vecteur threadResults dans le vecteur verifiedResults
	private void addToVerifiedResults()
	{
		int resultVerified = -1;
		int keyVerified = -1;
		//System.out.println("ThreadResults in addToVerifiedResults : " + Repartiteur.threadResults);
		for(Integer keyI : Repartiteur.threadResults.keySet())
		{
			//System.out.println("Entered I : " + keyI);
			for(Integer keyJ : Repartiteur.threadResults.keySet())
			{
				//System.out.println("Entered J : " + keyJ);
				if(keyI != keyJ)
				{
					//System.out.println("Testing : " + threadResults.get(keyI));
					//System.out.println("With : " + threadResults.get(keyJ));
					
					if(Integer.compare(threadResults.get(keyI), threadResults.get(keyJ)) == 0)
					{
						// Le résultat qui se trouve en double
						resultVerified = threadResults.get(keyI);
						keyVerified = keyI;
						//System.out.println("Found twice : " + resultVerified);
					}
				}
			}
		}
		//System.out.println("Found verified = " + keyVerified + "," + resultVerified);
		// On insère le résultat dans verifiedResults
		Repartiteur.verifiedResults.put(keyVerified, resultVerified);
	}
}
