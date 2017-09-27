package ca.polymtl.inf4410.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
	public enum Command {
		LIST, GET, LOCK, CREATE, PUSH, SYNCLOCALDIR
	}

	int execute(int a, int b) throws RemoteException;
	void executeCommand(int command) throws RemoteException;
}
