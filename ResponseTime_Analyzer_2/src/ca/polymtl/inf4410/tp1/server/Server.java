package ca.polymtl.inf4410.tp1.server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.ServerInterface.Command;

public class Server implements ServerInterface {

	public static void main(String[] args) {
		Server server = new Server();
		server.loadAllFiles();
		server.run();
	}

	public Server() {
		super();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/*
	 * Méthode accessible par RMI. Additionne les deux nombres passés en
	 * paramètre.
	 */
	@Override
	public int execute(int a, int b) throws RemoteException {
		return a + b;
	}

	public void executeCommand(int command /* TODO : add parameters for GET, LOCK? Or find cleaner way to do */) throws RemoteException
	{
		if (command >= 0 && command <= 5)
		{
			switch(Command.values()[command]) 
			{
				case LIST : 
					System.out.println("Executing command LIST...");
					printListFiles();
					break;
				case GET : 
					System.out.println("Executing command GET...");
					sendFile(/*nom, checksum from parameters?*/);
					break;
				case LOCK : 
					System.out.println("Executing command LOCK...");
					lockFile(/*nom, clientID, checksum form parameters?*/);
					break;
				case CREATE : 
					System.out.println("Executing command CREATE...");
					createFile(/*nom from parameters?*/);
					break;
				case PUSH : 
					System.out.println("Executing command PUSH...");
					pushFile(/*nom, contenu, clientID from parameters?*/);
					break;
				case SYNCLOCALDIR : 
					System.out.println("Executing command SYNCLOCALDIR...");
					syncLocalDirectory();
					break;
				default : 
					System.out.println("Commande inconnue.");
					break;
			}
		}
		else
		{
			System.out.println("Commande inconnue.");
		}
	}

	private void loadAllFiles()
	{
		// TODO : Load all the files from the server in a vector or map?
	}

	private void printListFiles()
	{
		// TODO : Print each files in the vector or map, with the UserID that has it locked
	}
	
	private void sendFile(/*nom, checksum from parameters?*/)
	{
		// TODO : Send file with name = nom if parameterChecksum different from localChecksum (in vector or map)
	}

	private void lockFile(/*nom, clientID, checksum form parameters?*/)
	{
		// TODO : change lockID of file with name = nom for clientID
		// TODO : Something with checksum apparently
	}

	private void createFile(/*nom from parameters?*/)
	{
		// TODO : Create file with name = nom
	}

	private void pushFile(/*nom, contenu, clientID from parameters?*/)
	{
		// TODO : Save file with name = nom, content = contenu, and owner as clientID?
	}
	
	private void syncLocalDirectory()
	{
		// TODO : Send all files to client or send only files with different checksums
	}
}
