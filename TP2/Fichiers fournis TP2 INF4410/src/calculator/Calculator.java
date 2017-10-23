package calculator;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javafx.util.Pair;
import java.util.*;
import operations.Operations;

public class Calculator {
	public static void main(String[] args)
	{
		Pair<Integer, Integer> pair1 = new Pair<Integer, Integer>(0, 5);
		Vector<Pair<Integer, Integer>> vec = new Vector<Pair<Integer, Integer>>();
		vec.add(pair1);
		System.out.println(calculate(vec));
	}
	public static int calculate(Vector<Pair<Integer, Integer>> operations)
	{
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
		return resultat;
	}
}
