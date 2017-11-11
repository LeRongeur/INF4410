package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javafx.util.Pair;
import java.util.*;
import java.nio.file.*;
import java.nio.*;
import java.io.*;

public interface ServerInterfaceCalculator extends Remote {
	// Liste des fonctions du serveur appelables par le Repartiteur
	int calculateSecured(ArrayList<Pair<Integer, Integer>> operations) throws RemoteException;
	int calculateUnsecured(ArrayList<Pair<Integer, Integer>> operations) throws RemoteException;
	Boolean demandeOp(int nombreOp) throws RemoteException;
}
