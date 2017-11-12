Tests de performance - mode sécurisé

1- Ouvrir les registres rmi sur les ports de votre choix (un port entre 5000 et 5050 différent pour chaque serveur) avec la commande rmiregistry dans le dossier bin
2- Lancer les serveurs (Calculator) avec en paramètres son port respectif, son q et son m (m facultatif pour le mode sécurisé).
Exemple : ./calculator 132.207.12.30 5004 7
3- Ajouter dans le fichier CalculatorIPs.txt les informations pour chaque serveur, 1 ligne par serveur. En premier son adresse IP, son port et son q.
Exemple: 127.0.0.1 5004 7 30
4-Lancer le répartiteur avec en paramètres le nom du fichier possédant les informations sur les serveurs (CalculatorIPs.txt) ainsi qu'un booléen correspondant au mode sécurisé ou non (true = sécurisé et false = non-sécurisé)
Exemple : ./repartiteur CalulatorIPs.txt true
5- Le résultat devrait apparaître dans la console du répartiteur. 

Note: Les nombreux System.out.println ont été mis en commentaires pour les tests de performance. Ils peuvent être remis afin de bien voir le fonctionnement de notre système.



Tests de performance - mode non-sécurisé
*La seule réelle différence est dans l'étape 2 où il faut s'assurer de bien rajouter le paramètre m*
1- Si ce n'est pas déjà fait, ouvrir les registres rmi sur les ports de votre choix (un port entre 5000 et 5050 différent pour chaque serveur) avec la commande rmiregistry dans le dossier bin
2- Lancer les serveurs (Calculator) avec en paramètres son port respectif, son q et son m.
Exemple : ./calculator 132.207.12.30 5004 7 50
3- Ajouter dans le fichier CalculatorIPs.txt les informations pour chaque serveur, 1 ligne par serveur. En premier son adresse IP, son port, son q.
Exemple: 127.0.0.1 5004 7
4-Lancer le répartiteur avec en paramètres le nom du fichier possédant les informations sur les serveurs (CalculatorIPs.txt) ainsi qu'un booléan correspondant au mode sécurisé ou non (true = sécurisé et false = non-sécurisé)
./repartiteur CalulatorIPs.txt false
5- Le résultat devrait apparaitre dans la console du répartiteur.
