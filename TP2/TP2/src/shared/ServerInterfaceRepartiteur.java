package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javafx.util.Pair;
import java.util.*;
import java.nio.file.*;
import java.nio.*;
import java.io.*;

public interface ServerInterfaceRepartiteur extends Remote {
	// Liste des fonctions du serveur appelables par le client
	int calculateServer(Vector<Pair<Integer, Integer>> operations, Boolean isSecured) throws RemoteException;
	int generateClientId() throws RemoteException;
}
