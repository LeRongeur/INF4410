package repartiteur;

import shared.ServerInterfaceCalculator;
import java.util.*;
import java.nio.file.*;
import java.nio.*;
import java.io.*;
import javafx.util.Pair;
import java.util.List;
import java.lang.Thread;
import java.rmi.RemoteException;

public class CalcThread extends Thread {
	private ArrayList<Pair<Integer, Integer>> operations = new ArrayList<Pair<Integer, Integer>>();
	private ServerInterfaceCalculator stub; //Contient le stub du serveur associé à ce thread
	private int result = 0; //Contient le résultat que le thread va retourner lors du callback
	private int id; //Contient l'id du thread
	private Boolean isSecured; //

	public CalcThread(int id,ServerInterfaceCalculator stub, Boolean isSecured)
	{
		this.stub = stub;
		this.id = id;
		this.isSecured = isSecured;
	}

	//Fonction qui effectue le callback pour ajouter un résultat lorsque celui-ci a été calculé
	public void register1(Callback callback) {
		callback.addResult(this.result);
	}

	//Fonction qui effectue le callback lorsque le serveur ne fonctionne plus
	public void register2(Callback callback) {
		callback.putBackOps(this.operations, this.id);
	}

	//Fonction d'exécution du thread qui va lancer le calcul sur le stub avec les opérations qui correspondent. 
	public void run()
	{	
		//System.out.println("Cool :");
		try
		{
			if(this.isSecured)
			{
				this.result = this.stub.calculateSecured(this.operations);
			}
			else
			{
				this.result = this.stub.calculateUnsecured(this.operations);
			}
			//System.out.println("Cool! my result is :" + result);
			
			//Lance un callback avec le résultat dans le cas où il a terminé de calculer
			Callback callBack = new Repartiteur();
			this.register1(callBack);
		}
		//Dans le cas où il y a une exception, un envoie un callback pour gérer le fait qu'un serveur ne fonctionne plus		
		catch(Exception e)
		{
			System.out.println("Erreur : " + e);
			Callback callBack = new Repartiteur();
			this.register2(callBack);
		}
	}

	//Retourne le résultat en int
	public int getResult()
	{
		return this.result;
	}

	//Retourne la liste d'opérations du thread
	public List<Pair<Integer, Integer>> getOperations()
	{
		return this.operations;
	}

	//Set les opérations du thread
	public void setOperations(ArrayList<Pair<Integer, Integer>> ops)
	{
		this.operations = ops;
	}
}
