package server;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import model.Car;
import model.User;
import server.authentication.AuthInterface;
import server.storage.ServerRole;
import server.storage.StorageInterface;

public class Gateway implements GatewayInterface {

	static String authenticationHostName = "Authentication";
	static String storageHostName[] = {"Storage1","Storage2","Storage3"};
	static AuthInterface authServer;
	static StorageInterface storServer;
	static List<StorageInterface> stores;
	public static Registry register;
	
	public static void main(String[] args) {
		
		Gateway gateway = new Gateway();
		stores = new ArrayList<>();
		
		try {
			Registry authRegister = LocateRegistry.getRegistry("127.0.0.1", 5001);
			authServer = (AuthInterface) authRegister.lookup(authenticationHostName);
			
			Registry stgRegister = LocateRegistry.getRegistry("127.0.0.2", 5002);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[0]);
			stores.add(storServer);

			stgRegister = LocateRegistry.getRegistry("127.0.0.3", 5003);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[1]);
			stores.add(storServer);

			stgRegister = LocateRegistry.getRegistry("127.0.0.4", 5004);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[2]);
			stores.add(storServer);

			storServer = stores.get(0);
			
			GatewayInterface protocol = (GatewayInterface) UnicastRemoteObject.exportObject(gateway, 0);
			
			LocateRegistry.createRegistry(5000);
			register = LocateRegistry.getRegistry(5000);
			register.bind("Gateway", protocol);
			
			System.out.println("Gateway ligado...");
			
		} catch (RemoteException | AlreadyBoundException | NotBoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void register(User newUser) {
		try {
			authServer.registerUser(newUser);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public User login(String cpf, String password) {
		try {
			User connected = authServer.loginUser(cpf, password);
			
			return connected;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void addCar(Car newCar) throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				storServer.addCar(newCar);
				System.out.println("Carro adicionado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				storServer.addCar(newCar);
				System.out.println("Carro adicionado com sucesso.");
			}
		}
	}
	
	@Override
	public void editCar(String renavam, Car editedCar) throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				storServer.editCar(renavam, editedCar);
				System.out.println("Carro de renavam " + renavam + " editado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				storServer.editCar(renavam, editedCar);
				System.out.println("Carro de renavam " + renavam + " editado com sucesso.");
			}
		}
	}

	@Override
	public void deleteCar(String renavam) throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				storServer.deleteCar(renavam);
				System.out.println("Carro de renavam " + renavam + " deletado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				storServer.deleteCar(renavam);
				System.out.println("Carro de renavam " + renavam + " deletado com sucesso.");
			}
		}
	}
	
	@Override
	public void deleteCars(String name) throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				storServer.deleteCars(name);
				System.out.println("Todos os carros " + name + " deletados com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				storServer.deleteCars(name);
				System.out.println("Todos os carros " + name + " deletados com sucesso.");
			}
		}
	}
	
	@Override
	public List<Car> listCars() throws RemoteException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros enviada.");
			return storServer.listCars();
		}
		storServer = storServer.startElections();
		System.out.println("Lista de carros enviada.");
		return storServer.listCars();
	}

	@Override
	public List<Car> listCars(int category) throws RemoteException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros da categoria " + category + " enviada.");
			return storServer.listCars(category);
		}
		storServer = storServer.startElections();
		System.out.println("Lista de carros da categoria " + category + " enviada.");
		return storServer.listCars(category);
	}
	
	@Override
	public Car searchCar(String renavam) throws RemoteException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Carro encontrado com sucesso!");
			return storServer.searchCar(renavam);
		}
		storServer = storServer.startElections();
		System.out.println("Carro encontrado com sucesso!");
		return storServer.searchCar(renavam);
	}

	@Override
	public List<Car> searchCars(String name) throws RemoteException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros encontrada com sucesso!");
			return storServer.searchCars(name);
		}
		storServer = storServer.startElections();
		System.out.println("Lista de carros encontrada com sucesso!");
		return storServer.searchCars(name);
	}
	
	@Override
	public Car buyCar(String renavam) throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				System.out.println("Carro de renavam " + renavam + " foi comprado.");
				return storServer.buyCar(renavam);
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = storServer.startElections();
			}
		}
		return null;
	}
	
	@Override
	public int getAmount(int category) throws RemoteException {
		return storServer.getAmount(category);
	}

	@Override
	public void putToSleep() throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				store.setRole(ServerRole.OUTOFORDER);
			}
		}
	}
	
}
