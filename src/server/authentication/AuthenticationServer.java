package server.authentication;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map.Entry;

import datagrams.Permission;
import model.User;

public class AuthenticationServer implements AuthInterface {

	private static String path = "src/server/authentication/users.txt";
	private static HashMap<String, User> users;
	private static ObjectOutputStream fileOutput;
	private static ObjectInputStream fileInput;

	private static Permission gatewayPermission;
	
	public AuthenticationServer() {
		try { // tenta abrir
			fileInput = new ObjectInputStream(new FileInputStream(path));
		} catch (IOException e) { // se nao abrir pq nao existe/funciona
			
			try { // ele faz o output pra poder criar o arquivo certo e dps abre
				fileOutput = new ObjectOutputStream(new FileOutputStream(path));
				fileInput = new ObjectInputStream(new FileInputStream(path));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		users = getFileUsers();

	}

	public static void main(String[] args) {

		AuthenticationServer authServer = new AuthenticationServer();

		try {
			AuthInterface server = (AuthInterface) UnicastRemoteObject.exportObject(authServer, 0);

			LocateRegistry.createRegistry(5001);
			Registry register = LocateRegistry.getRegistry("127.0.0.1", 5001);
			register.bind("Authentication", server);

			gatewayPermission = new Permission("10.215.34.54", "10.215.34.54", 5001, "Autenticação", true);
			
			System.out.println("Servidor de Autenticação ligado.");

		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		}

	}
	
	private void attServer() {

		try {
			fileOutput = new ObjectOutputStream(new FileOutputStream(path));
			
			for (Entry<String, User> user : users.entrySet()) {
				fileOutput.writeObject(user.getValue());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void registerUser(User newUser) {
		if(AuthenticationServer.getPermission()) {
			users = getFileUsers(); // pega do arquivo e bota no mapa
			users.put(newUser.getCpf(), newUser); // add no mapa
			attServer(); // salva o mapa num arquivo
			
			System.out.println("Registrado com sucesso.");	
		}
	}

	@Override
	public User loginUser(String cpf, String password) {
		if(AuthenticationServer.getPermission()) {
			for (Entry<String, User> user : users.entrySet()) {
				if (cpf.equals(user.getKey()) && password.equals(user.getValue().getPassword())) {
					System.out.println("Logado com sucesso! Bem-vindo, " + user.getValue().getName() + ".");

					return user.getValue();
				}
			}	
		}
		return null;
	}

	private static HashMap<String, User> getFileUsers() {
		boolean eof = false;
		
		if(users == null) {
			users = new HashMap<String, User>();
		}

		try {
			while (!eof) {
				User account = (User) fileInput.readObject();
				users.put(account.getCpf(), account);
			}

		} catch (IOException e) {
			eof = true;
 		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return users;
	}
	
	public static boolean getPermission() {
		
		String sourceIp = "";
		
		try {
			sourceIp = RemoteServer.getClientHost();
		} catch (ServerNotActiveException e1) {
			e1.printStackTrace();
		}
		
		if(gatewayPermission.getSourceIp().equals(sourceIp) && gatewayPermission.getDestinationPort() == 5001) {
			System.out.println("------------------------");
			System.out.println("Firewall --> Pacote permitido. Acesso: " + gatewayPermission.getName() + ", source: " + gatewayPermission.getSourceIp());
			return true;
		} else {
			System.out.println("------------------------");
			System.out.println("Firewall --> Pacote negado. Acesso: " + gatewayPermission.getName() + ", source: " + gatewayPermission.getSourceIp());
			return false;
		}
	}
	
}
