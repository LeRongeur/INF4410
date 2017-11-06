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



public class Repartiteur {

	private static Vector<String> calcIPs = new Vector<String>();
	private static Vector<Integer> calcQs = new Vector<Integer>();
	private static Vector<Float> calcMs = new Vector<Float>();
	private static Vector<ServerInterfaceCalculator> calcStubs = new Vector<ServerInterfaceCalculator>();

	private static Vector<Pair<Integer, Integer>> allOps = new Vector<Pair<Integer, Integer>>();

	// Lorsqu'on lance le serveur, on l'initialise, on remplit la HashMap avec les fichiers sur le serveur et on lance le serveur
	public static void main(String[] args) {
		String fileName = null;
		Boolean isSecured = true;
		if (args.length > 1)
		{	
			System.out.println("Debut lecture des arguments...");
			fileName = args[0];
			isSecured = Boolean.valueOf(args[1]);
			System.out.println("Fin lecture arguments.");
			System.out.println("Debut chargement des operations...");
			loadCalculations(fileName);
			System.out.println("Fin chargement des operations.");
			System.out.println("Operations : " + allOps);
			System.out.println("Debut chargement des serveurs de calculs...");
			loadAllCalculators();
			System.out.println("Fin chargement des serveurs de calculs.");
			if(isSecured)
			{
				repartirCalcsSecu();
			}
			else
			{
				//repartirCalcsUnsecu();
			}
		}
	}

	private static void loadAllCalculators()
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
		   System.out.println(z);	
		}
		System.out.println(calcIPs);
		System.out.println(calcStubs);
		System.out.println(calcQs);
	}

	private static void loadCalculator(String line)
	{
		String[] splitted = line.split("\\s+");
		calcIPs.add(splitted[0]);
		calcQs.add(Integer.parseInt(splitted[1]));
		calcMs.add(Float.parseFloat(splitted[2]));
		calcStubs.add(loadServerStubC(splitted[0]));
	}

	private static void loadCalculations(String fileName)
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
			allOps.add(pair);
		    }
		} 
		catch (IOException z) 
		{
		   System.out.println(z);	
		}
	}

	private static ServerInterfaceCalculator loadServerStubC(String hostname) {
		ServerInterfaceCalculator stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
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

	private static void repartirCalcsSecu()
	{
		Boolean acceptedDemand;
		int nbreOp;
		int indexRendu = 0;		
		int resultat = 0;
		System.out.println("Debut de la reparition... ");
		System.out.println("Operations : " + allOps);
		while(indexRendu < allOps.size())
		{
			System.out.println("Nouvelle iteration sur les serveurs de calculs dispos");
			System.out.println("Reste " + (allOps.size() - indexRendu) + "operation a distribuer");
			for(int indexCalc = 0; indexCalc < calcStubs.size(); indexCalc++)
			{
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
					}	
					catch (RemoteException e) 
					{
						System.out.println("Erreur appel demandeOp accepted : " + e.getMessage());
						acceptedDemand = false;
					}
				}
				System.out.println("Debut trouver u qui est accepte");
				while(!acceptedDemand)
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
					}
				}
				System.out.println("Trouve : " + nbreOp);
				List<Pair<Integer, Integer>> subList = allOps.subList(indexRendu,indexRendu + nbreOp);
				try
				{
					System.out.println("Essai calcul avec : " + subList);
					ArrayList<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>(subList);
					resultat += calcStubs.get(indexCalc).calculate(list);
					System.out.println("Resultat obtenu = " + resultat);
					indexRendu += nbreOp;
				}	
				catch (RemoteException e) 
				{
					System.out.println("Erreur appel calculate : " + e.getMessage());
				}
				System.out.println("Fin de literation");
			}
		}
		System.out.println("Done doing calculations in Repartiteur...");
		System.out.println("Result is: " + resultat);
	}
}
