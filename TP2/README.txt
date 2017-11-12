Tests de performance - mode s�curis�

1- Ouvrir les registre rmi sur les ports de votre choix (un port entre 5000 et 5050 diff�rent pour chaque serveur) avec la commande rmiregistry dans le dossier bin
2- Lancer les serveurs (Calculator) avec en param�tres son port respectif, son q et son m (facultatif pour le mode s�curis�).
3- Ajouter dans le fichier CalculatorIPs.txt les informations pour chaque serveur, 1 ligne par serveur. En premier son adresse IP, son port et son q.
Exemple: 127.0.0.1 5004 7 30
4-Lancer le r�partiteur avec en param�tres le nom du fichier poss�dant les informations sur les serveurs (CalculatorIPs.txt) ainsi qu'un bool�an correspondant au mode s�curis� ou non (true = s�curis� et false = non-s�curis�)
5- Le r�sultat devrait apparaitre dans la console du r�partiteur. 

Note: Les nombreux System.out.println ont �t� mis en commentaires pour les tests de performance. Ils peuvent �tre remis afin de bien voir le fonctionnement de notre syst�me.



Tests de performance - mode non-s�curis�
*La seule r�elle diff�rente est dans l'�tape 2 o� il faut s'assurer de bien rajouter le param�tre m*
1- Si ce n'est pas d�j� fait, ouvrir les registre rmi sur les ports de votre choix (un port entre 5000 et 5050 diff�rent pour chaque serveur) avec la commande rmiregistry dans le dossier bin
2- Lancer les serveurs (Calculator) avec en param�tres son port respectif, son q et son m.
3- Ajouter dans le fichier CalculatorIPs.txt les informations pour chaque serveur, 1 ligne par serveur. En premier son adresse IP, son port, son q.
Exemple: 127.0.0.1 5004 7
4-Lancer le r�partiteur avec en param�tres le nom du fichier poss�dant les informations sur les serveurs (CalculatorIPs.txt) ainsi qu'un bool�an correspondant au mode s�curis� ou non (true = s�curis� et false = non-s�curis�)
5- Le r�sultat devrait apparaitre dans la console du r�partiteur. 
