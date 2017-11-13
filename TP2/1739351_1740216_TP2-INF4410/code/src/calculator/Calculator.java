package calculator;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javafx.util.Pair;
import java.util.*;
import operations.Operations;
import shared.ServerInterfaceCalculator;
import java.util.List;
import java.util.Random;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.ConnectException;

public class Calculator implements ServerInterfaceCalculator {
	private int q = -1; // Capacite du serveur
	private int m = 0; // Taux de mauvaises reponses
	private int port; // Port du serveur

	//Fonction main qui parse les paramètres et les assignes aux attributs en question. Lance également la fonction run
	public static void main(String[] args) throws RemoteException
	{
		Calculator calculator = new Calculator();
		String distantHostName = null;
		if (args.length > 1)
		{
			calculator.port = Integer.parseInt(args[0]);
			calculator.q = Integer.parseInt(args[1]);
			if(args.length > 2)
			{
				calculator.m = Integer.parseInt(args[2]);
			}
		}
		calculator.run();
	}


	public Calculator() {
		super();
	}

	// Fonction qui lie le server stub au RMIRegistry
	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterfaceCalculator stub = (ServerInterfaceCalculator) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry(port);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	//Fonction qui fait les calculs en mode non-sécurisé. Prend en paramètre un ArrayList contenant les opérations
	public int calculateUnsecured(ArrayList<Pair<Integer, Integer>> operations) throws RemoteException
	{
		//Trouve un nombre aléatoire pour déterminer si le serveur va retourner un faux résultat selon le m
		int tauxMalicieux = m;
		Random rand = new Random(System.nanoTime());

    	int randomNum = rand.nextInt(101);
		//System.out.println("Randomly got : " + randomNum);
		//System.out.println("Malicieux : " + (randomNum <= m));
		Boolean malicieux = (randomNum <= m);

		//System.out.println("Executing unsecured calculations...");
		int resultat = 0;
		for(int i = 0; i < operations.size(); i++)
		{
			switch(operations.get(i).getKey())
			{
				//0 correspond à pell
				case 0 :
					resultat += Operations.pell(operations.get(i).getValue());
					break;
				//1 correspond à prime
				case 1 :
					resultat += Operations.prime(operations.get(i).getValue());
					break;
				default :
					//System.out.println("Operation inconnue.");
					break;
			}
			resultat %= 4000;
		}
		//System.out.println("Done doing unsecured calculations in calculator...");
		//System.out.println("Calculated : " + resultat);
		//Retourne un nombre aléatoire dans le cas où le serveur est considéré malicieux, sinon retourne le bon résultat.
		if(malicieux)
		{
			return rand.nextInt(500000);
		}
		else
		{
			return resultat;
		}
	}

	//Fonction qui fait les calculs en mode sécurisé. Prend en paramètre un ArrayList contenant les opérations
	public int calculateSecured(ArrayList<Pair<Integer, Integer>> operations) throws RemoteException
	{
		//System.out.println("Executing secured calculations...");
		int resultat = 0;
		for(int i = 0; i < operations.size(); i++)
		{
			switch(operations.get(i).getKey())
			{
				case 0 :
					//0 correspond à pell
					resultat += Operations.pell(operations.get(i).getValue());
					break;
				case 1 :
					//1 correspond à prime
					resultat += Operations.prime(operations.get(i).getValue());
					break;
				default :
					//System.out.println("Operation inconnue.");
					break;
			}
			resultat %= 4000;
		}
		//System.out.println("Done doing secured calculations in calculator...");
		//System.out.println("Calculated : " + resultat);

		//Retourne le résultat
		return resultat;
	}

	//Fonction qui calcul le taux d'acceptation selon le nombre d'opération qu'il recoit et retourne s'il accepte ou non de calculer ce nombre d'opérations
	public Boolean demandeOp(int nombreOp) throws RemoteException
	{
		//System.out.println("Executing demande...");
		//System.out.println("NombreOp = " + nombreOp);
		int tauxAcceptation = (int)(100 - ((nombreOp - q) / (float)(5*q) * 100));
		//System.out.println("Taux acceptation = " + tauxAcceptation);
		Random rand = new Random(System.nanoTime());

    	int randomNum = rand.nextInt(101);
		//System.out.println("Randomly got : " + randomNum);
		//System.out.println("Returning : " + (randomNum <= tauxAcceptation));
		//System.out.println("Done executing demande...");
		return randomNum <= tauxAcceptation;
	}
}
