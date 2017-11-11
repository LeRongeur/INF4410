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

interface Callback {
    void putBackOps(List<Pair<Integer, Integer>> operations, int id);
    void addResult(int result);
}

public class Repartiteur implements Callback {

	private static Boolean isSecured = true;
	private static AtomicBoolean resultVerified = new AtomicBoolean();

	private Vector<String> calcIPs = new Vector<String>();
	private Vector<Integer> calcPorts = new Vector<Integer>();
	private Vector<Integer> calcQs = new Vector<Integer>();
	private Vector<Float> calcMs = new Vector<Float>();

	private Vector<ServerInterfaceCalculator> calcStubs = new Vector<ServerInterfaceCalculator>();

	private static List calcAvailability = Collections.synchronizedList(new ArrayList());

	//private static Vector<Pair<Integer, Integer>> allOps = new Vector<Pair<Integer, Integer>>(); //TODO changer pour un concurrent something
	private static List allOps = Collections.synchronizedList(new ArrayList());

	private static ConcurrentHashMap<Integer, Integer> threadResults = new ConcurrentHashMap<Integer, Integer>();
	private static ConcurrentHashMap<Integer, Integer> verifiedResults = new ConcurrentHashMap<Integer, Integer>();
	
	private Vector<CalcThread> calcThreads = new Vector<CalcThread>();
	private static AtomicInteger compteurHash = new AtomicInteger();

	// Lorsqu'on lance le serveur, on l'initialise, on remplit la HashMap avec les fichiers sur le serveur et on lance le serveur
	public static void main(String[] args) {
		String fileName = null;
		Repartiteur repartiteur = new Repartiteur();
		resultVerified.set(false);
		if (args.length > 1)
		{	
			System.out.println("Debut lecture des arguments...");
			fileName = args[0];
			Repartiteur.isSecured = Boolean.valueOf(args[1]);
			System.out.println("Secured : " + Repartiteur.isSecured);
			System.out.println("Fin lecture arguments.");
			System.out.println("Debut chargement des operations...");
			repartiteur.loadCalculations(fileName);
			System.out.println("Fin chargement des operations.");
			System.out.println("Operations : " + Repartiteur.allOps);
			System.out.println("Debut chargement des serveurs de calculs...");
			repartiteur.loadAllCalculators();
			System.out.println("Fin chargement des serveurs de calculs.");
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


	private void loadAllCalculators()
	{
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./CalculatorIPs.txt"), charset)) 
		{
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
			loadCalculator(line);
		    }
		} 
		catch (IOException z) 
		{
		   System.out.println("CatchExceptionInLoadAllCalculators");
		   System.out.println(z);	
		}
		System.out.println(calcIPs);
		System.out.println(calcStubs);
		System.out.println(calcQs);
	}

	private void loadCalculator(String line)
	{
		String[] splitted = line.split("\\s+");
		calcIPs.add(splitted[0]);
		calcPorts.add(Integer.parseInt(splitted[1]));
		calcQs.add(Integer.parseInt(splitted[2]));
		calcMs.add(Float.parseFloat(splitted[3]));
		ServerInterfaceCalculator stub = loadServerStubC(splitted[0],Integer.parseInt(splitted[1]));
		calcStubs.add(stub);
		calcThreads.add(new CalcThread(calcQs.size()-1,stub, Repartiteur.isSecured));
		calcAvailability.add(true);
	}

	private void loadCalculations(String fileName)
	{
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), charset)) 
		{
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		        // TODO : 
			int op;
			String[] splitted = line.split("\\s+");
			System.out.println("Operation : " + splitted[0]);
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

	private ServerInterfaceCalculator loadServerStubC(String hostname, int port) {
		ServerInterfaceCalculator stub = null;

		try {
			System.out.println("hostname : " + hostname);
			System.out.println("port : " + port);
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

	private void repartirCalcsSecu()
	{
		Boolean acceptedDemand;
		int nbreOp;		
		int resultat = 0;
		Boolean finOperations = false;
		System.out.println("Debut de la reparition... ");
		System.out.println("Operations : " + Repartiteur.allOps);
		while(Repartiteur.allOps.size() > 0)
		{
			System.out.println("Nouvelle iteration sur les serveurs de calculs dispos");
			System.out.println("Reste " + (Repartiteur.allOps.size()) + "operation a distribuer");
			for(int indexCalc = 0; indexCalc < calcStubs.size() && allOps.size() > 0; indexCalc++)
			{
				if(!(Boolean)calcAvailability.get(indexCalc))
					continue;

				System.out.println("Server numero " + indexCalc);
				nbreOp = calcQs.get(indexCalc) + 1;
				acceptedDemand = true;
				System.out.println("");
				System.out.println("Debut trouver u qui nest plus accepte");
				while(acceptedDemand)
				{	
					nbreOp += 0.2 * nbreOp + 1;
					try
					{	
						System.out.println("Essai avec : " + nbreOp);
						acceptedDemand = calcStubs.get(indexCalc).demandeOp(nbreOp);
						System.out.println("Demande acceptee : " + acceptedDemand);
						if(nbreOp >= Repartiteur.allOps.size())
						{
							acceptedDemand = false;
							nbreOp = Repartiteur.allOps.size();
							finOperations = true;
						}
					}	
					catch (RemoteException e) 
					{
						System.out.println("Erreur appel demandeOp accepted : " + e.getMessage());
						acceptedDemand = false;
						calcAvailability.set(indexCalc,false);
					}
				}
				System.out.println("Debut trouver u qui est accepte");
				while(!acceptedDemand && !finOperations)
				{	
					nbreOp--;
					try
					{
						System.out.println("Essai avec : " + nbreOp);
						acceptedDemand = calcStubs.get(indexCalc).demandeOp(nbreOp);
						System.out.println("Demande refusee : " + !acceptedDemand);
					}	
					catch (RemoteException e) 
					{
						System.out.println("Erreur appel demandeOp !accepted: " + e.getMessage());
						acceptedDemand = true;
						calcAvailability.set(indexCalc,false);
					}
				}
				System.out.println("Trouve : " + nbreOp);
				List<Pair<Integer, Integer>> subList = Repartiteur.allOps.subList(0,nbreOp);

				System.out.println("Essai calcul avec : " + subList);
				ArrayList<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>(subList);
				calcThreads.get(indexCalc).setOperations(list);
				System.out.println();
				calcThreads.get(indexCalc).run();
				removeOps(nbreOp);
				System.out.println("Fin de literation");
			}
			if(!calcAvailability.contains(true))
			{
				System.out.println("No more servers available. Exiting.");
				return;
			}
		}
		System.out.println("Done doing calculations in Repartiteur...");
		System.out.println(Repartiteur.verifiedResults);
		System.out.println("Result is: " + finalSum());
	}
	
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
	
	public void putBackOps(List<Pair<Integer, Integer>> operations, int id) {
		System.out.println("I've been called back to put back ops");
		if(Repartiteur.isSecured)
		{
			// TODO verify somehow? with the continu later
			for(int i = 0; i < operations.size(); i++)
			{
				Repartiteur.allOps.add(operations.get(i));
			}
		}
		calcAvailability.set(id,false);
	}

	private void repartirCalcsUnsecu()
	{
		int resultat = 0;
		System.out.println("Debut de la reparition non-sécurisée... ");
		System.out.println("Operations : " + Repartiteur.allOps);
		int nbreOp = Collections.min(calcQs);
		Boolean acceptedDemand = false;
		while(Repartiteur.allOps.size() > 0)
		{
			nbreOp = nbreOp > allOps.size() ? allOps.size() : nbreOp;
			for(int indexCalc = 0; indexCalc < calcStubs.size(); indexCalc++)
			{
				if(!(Boolean)calcAvailability.get(indexCalc))
					continue;

				try
				{
					acceptedDemand = calcStubs.get(indexCalc).demandeOp(nbreOp);
				}
				catch(RemoteException e)
				{
					System.out.println("Erreur appel demandeOp : " + e.getMessage());
					calcAvailability.set(indexCalc,false);
				}
				if(acceptedDemand)
				{
					System.out.println("Trouve : " + nbreOp);
					List<Pair<Integer, Integer>> subList = Repartiteur.allOps.subList(0,nbreOp);

					System.out.println("Essai calcul avec : " + subList);
					ArrayList<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>(subList);
					calcThreads.get(indexCalc).setOperations(list);
					System.out.println();
					calcThreads.get(indexCalc).run();
				}
			}
			if(resultVerified.get())
			{
				removeOps(nbreOp);
				Repartiteur.resultVerified.set(false);
				addToVerifiedResults();
				Repartiteur.threadResults.clear();
			}
			if(!calcAvailability.contains(true))
			{
				System.out.println("No more servers available. Exiting.");
				return;
			}
		}
		System.out.println("Done doing calculations in Repartiteur...");
		System.out.println(Repartiteur.verifiedResults);
		System.out.println("Result is: " + finalSum());
	}

	public void addResult(int result) {
		System.out.println("I've been called back and result is : " + result);
		System.out.println("Am I secured? : " + Repartiteur.isSecured);
		if(Repartiteur.isSecured)
		{
			Repartiteur.verifiedResults.put(compteurHash.get(),result);
		}
		else
		{
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

	private void removeOps(int nbreOp)
	{
		for(int i = 0; i < nbreOp; i++)
		{
			Repartiteur.allOps.remove(0);
		}
	}

	private void addToVerifiedResults()
	{
		int resultVerified = -1;
		int keyVerified = -1;
		System.out.println("ThreadResults in addToVerifiedResults : " + Repartiteur.threadResults);
		for(Integer keyI : Repartiteur.threadResults.keySet())
		{
			System.out.println("Entered I : " + keyI);
			for(Integer keyJ : Repartiteur.threadResults.keySet())
			{
				System.out.println("Entered J : " + keyJ);
				if(keyI != keyJ)
				{
					System.out.println("Testing : " + threadResults.get(keyI));
					System.out.println("With : " + threadResults.get(keyJ));
					
					if(Integer.compare(threadResults.get(keyI), threadResults.get(keyJ)) == 0)
					{
						resultVerified = threadResults.get(keyI);
						keyVerified = keyI;
						System.out.println("Found twice : " + resultVerified);
					}
				}
			}
		}
		System.out.println("Found verified = " + keyVerified + "," + resultVerified);
		Repartiteur.verifiedResults.put(keyVerified, resultVerified);
	}
}
