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
	private ServerInterfaceCalculator stub;
	private int result = 0;
	private int id;
	private Boolean isSecured;

	public CalcThread(int id,ServerInterfaceCalculator stub, Boolean isSecured)
	{
		this.stub = stub;
		this.id = id;
		this.isSecured = isSecured;
	}

	public void register1(Callback callback) {
		callback.addResult(this.result);
	}

	public void register2(Callback callback) {
		callback.putBackOps(this.operations, this.id);
	}

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
			Callback callBack = new Repartiteur();
			this.register1(callBack);
		}		
		catch(Exception e)
		{
			System.out.println("Erreur : " + e);
			Callback callBack = new Repartiteur();
			this.register2(callBack);
		}
	}

	public int getResult()
	{
		return this.result;
	}

	public List<Pair<Integer, Integer>> getOperations()
	{
		return this.operations;
	}

	public void setOperations(ArrayList<Pair<Integer, Integer>> ops)
	{
		this.operations = ops;
	}
}
