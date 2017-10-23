package ca.polymtl.inf4410.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class Client {
	public static void main(String[] args) {
		String distantHostname = null;
		int x = 0;
		if (args.length > 0) {
			distantHostname = args[0];
			if (args.length > 1) {
				x = Integer.parseInt(args[1]);
			}
		}

		Client client = new Client(distantHostname);
		//x = client.clamp(x);		
		int size = (int) Math.pow(10, x);
		byte[] var = new byte[size];
		client.run(var);
	}

	FakeServer localServer = null; // Pour tester la latence d'un appel de
									// fonction normal.
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServer = new FakeServer();
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	private void run(byte[] table) {
		appelNormal(table);

		if (localServerStub != null) {
			appelRMILocal(table);
		}

		if (distantServerStub != null) {
			appelRMIDistant(table);
		}
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private void appelNormal(byte[] table) {
		long start = System.nanoTime();
		localServer.fonctionVide(table);
		long end = System.nanoTime();

		System.out.println("Temps écoulé appel normal: " + (end - start)
				+ " ns");
	}

	private void appelRMILocal(byte[] table) {
		try {
			long start = System.nanoTime();
			localServerStub.fonctionVide(table);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI local: " + (end - start)
					+ " ns");
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	private void appelRMIDistant(byte[] table) {
		try {
			long start = System.nanoTime();
			distantServerStub.fonctionVide(table);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI distant: "
					+ (end - start) + " ns");
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	private void fonctionVide(byte[] var)
	{
	
	}

	private int clamp(int clamping)
	{
		if(clamping < 1)
		{
			clamping = 1;		
		}
		if(clamping > 7)
		{
			clamping = 7;
		}
		System.out.println("Clamped : " + clamping);
		return clamping;
	}
}
