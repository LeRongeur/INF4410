package ca.polymtl.inf4410.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javafx.util.Pair;

public interface ServerInterface extends Remote {
	public enum Command {
		LIST, GET, LOCK, CREATE, PUSH, SYNCLOCALDIR
	}

	int execute(int a, int b) throws RemoteException;
	Pair<Boolean, String> executeCommand(int command, String nom, int clientId, String contenu, String checkSum) throws RemoteException;
	int generateClientId() throws RemoteException;
}
