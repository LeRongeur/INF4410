package ca.polymtl.inf4410.tp1.client;
import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.ServerInterface.Command;

public class FakeServer {
	int execute(int a, int b) {
		return a + b;
	}
	public void executeCommand(int command)
	{
		if (command >= 0 && command <= 5)
		{
			switch(Command.values()[command]) 
			{
				case LIST : 
					System.out.println("Executing command LIST...");
					break;
				case GET : 
					System.out.println("Executing command GET...");
					break;
				case LOCK : 
					System.out.println("Executing command LOCK...");
					break;
				case CREATE : 
					System.out.println("Executing command CREATE...");
					break;
				case PUSH : 
					System.out.println("Executing command PUSH...");
					break;
				case SYNCLOCALDIR : 
					System.out.println("Executing command SYNCLOCALDIR...");
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
}
