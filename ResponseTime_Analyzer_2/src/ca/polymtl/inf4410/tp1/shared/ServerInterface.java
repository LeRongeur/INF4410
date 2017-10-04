package ca.polymtl.inf4410.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javafx.util.Pair;
import java.util.*;
import java.nio.file.*;
import java.nio.*;
import java.io.*;

public interface ServerInterface extends Remote {
	enum Command {
	LIST, GET, LOCK, CREATE, PUSH, SYNCLOCALDIR
}

	String executeList() throws RemoteException;
	Pair<Boolean, String> executeGet(String nom, String checkSum) throws RemoteException;
	Pair<Boolean, String> executeLock(String nom, int clientId, String checkSum) throws RemoteException;
	Boolean executeCreate(String nom) throws RemoteException;
	Boolean executePush(String nom, String contenu, int clientId) throws RemoteException;
	HashMap<String, String> executeSyncLocalDirectory() throws RemoteException;
	int generateClientId() throws RemoteException;
}
