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
	private static int q = -1; // Capacite
	private static float m = -1; // Taux de mauvaises reponses



	public static void main(String[] args) throws RemoteException
	{
		String distantHostName = null;
		if (args.length > 2)
		{
			distantHostName = args[0];
			Calculator.q = Integer.parseInt(args[1]);
			m = Float.parseFloat(args[2]);
		}

		Calculator calculator = new Calculator();

		calculator.run();
		//System.out.println(calculate(vec)); 
 
		/* 
		Replace this by set up of server and then just wait for calculation requests...
		Calculator calculator = new Calculator();
		calculator.run();
		*/
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

			Registry registry = LocateRegistry.getRegistry();
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

	public int calculate(ArrayList<Pair<Integer, Integer>> operations) throws RemoteException
	{
		System.out.println("Executing calculations...");
		int resultat = 0;
		for(int i = 0; i < operations.size(); i++)
		{
			switch(operations.get(i).getKey())
			{
				case 0 :
					// TODO : return pell de ...
					resultat += Operations.pell(operations.get(i).getValue());
					break;
				case 1 :
					// TODO : return prime de ...
					resultat += Operations.prime(operations.get(i).getValue());
					break;
				default :
					System.out.println("Operation inconnue.");
					break;
			}
			resultat %= 4000;
		}
		System.out.println("Done doing calculations in calculator...");
		System.out.println("Calculated : " + resultat);
		return resultat;
	}

	public Boolean demandeOp(int nombreOp) throws RemoteException
	{
		System.out.println("Executing demande...");
		System.out.println("NombreOp = " + nombreOp);
		int tauxAcceptation = 100 - ((nombreOp - Calculator.q) / (5*q) * 100);
		System.out.println("Taux acceptation = " + tauxAcceptation);
		Random rand = new Random();

    		int randomNum = rand.nextInt(101);
		System.out.println("Randomly got : " + randomNum);
		System.out.println("Returning : " + (randomNum <= tauxAcceptation));
		System.out.println("Done executing demande...");
		return randomNum <= tauxAcceptation;
	}
}
