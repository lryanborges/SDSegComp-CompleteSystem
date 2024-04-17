package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import model.Car;
import model.User;
import server.GatewayInterface;
import server.authentication.AuthInterface;
import server.storage.StorageInterface;

// invasor
public class Backdoor {

	private static GatewayInterface gateway;
	private static AuthInterface authServer;
	private static StorageInterface storServer;
	private static List<User> stealedUsers;
	private static List<Car> stealedCars;
	
	public static void main(String[] args) {
		
		stealedUsers = new ArrayList<User>();
		stealedCars = new ArrayList<Car>();
		
		try {
			Registry gatRegister = LocateRegistry.getRegistry("192.168.218.218", 5000);
			gateway = (GatewayInterface) gatRegister.lookup("Gateway");
			
			Registry authRegister = LocateRegistry.getRegistry("192.168.218.218", 5001);
			authServer = (AuthInterface) authRegister.lookup("Authentication");
			
			Registry storRegister = LocateRegistry.getRegistry("192.168.218.218", 5002);
			storServer = (StorageInterface) storRegister.lookup("Storage1");
			
			stealCars();
			
			System.out.println("--------------------------------");
			System.out.println("LISTA ROUBADA DE CARROS");
			System.out.println("--------------------------------");
			
			for(Car car : stealedCars) {
				System.out.println("Renavam: " + car.getRenavam());
				System.out.println("Nome: " + car.getName());
				System.out.println("Categoria: " + car.getStringCategory());
				System.out.println("Ano de fabricação: " + car.getManufactureYear());
				System.out.println("Preço: R$" + car.getPrice());
				System.out.println("------------------");
			}
			
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void stealCars() throws RemoteException {
		Backdoor.stealedCars = storServer.listCars();
	}
	
}
