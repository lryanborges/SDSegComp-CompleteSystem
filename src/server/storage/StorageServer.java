package server.storage;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;

import model.Car;
import model.EconomicCar;
import model.ExecutiveCar;
import model.IntermediaryCar;

public class StorageServer implements StorageInterface {

	private static String path[] = {"src/server/storage/cars.txt", "src/server/storage/cars2.txt", "src/server/storage/cars3.txt"};
	private static HashMap<String, Car> cars;
	private static ObjectOutputStream fileOutput;
	private static ObjectInputStream fileInput;
	private static int currentDatabase = 0;
	private static int economicAmount = 0, intermediaryAmount = 0, executiveAmount = 0;
	private static ServerRole role;
	private static StorageInterface followerServer1;
	private static StorageInterface followerServer2;
	private static int id;
	private static Scanner scanner; 
	
	public StorageServer(ServerRole r) {
		
		for(int i = 0; i < 3; i++) {
			try { // tenta abrir
				//fileOutput = new ObjectOutputStream(new FileOutputStream(path[i]));
				fileInput = new ObjectInputStream(new FileInputStream(path[currentDatabase]));
			} catch (IOException e) { // se nao abrir pq nao existe/funciona
				
				currentDatabase = (currentDatabase + 1) % 3;
				System.out.println("Novo banco de dados: " + currentDatabase);
				/*try { // ele faz o output pra poder criar o arquivo certo e dps abre
					fileOutput = new ObjectOutputStream(new FileOutputStream(path[currentDatabase]));
					fileInput = new ObjectInputStream(new FileInputStream(path[currentDatabase]));
				} catch (IOException e1) {
					e1.printStackTrace();
				}*/
			}	
		}	

		cars = getFileCars();
		role = r;
	}
	
	public static void main(String[] args) {
		
		StorageServer storServer = new StorageServer(ServerRole.LEADER);
		scanner = new Scanner(System.in);

		try {
			StorageInterface server = (StorageInterface) UnicastRemoteObject.exportObject(storServer, 0);

			LocateRegistry.createRegistry(5002);
			Registry register = LocateRegistry.getRegistry("127.0.0.2", 5002);
			register.bind("Storage1", server);

			scanner.nextLine();

			Registry follower = LocateRegistry.getRegistry(5003);
			followerServer1 = (StorageInterface) follower.lookup("Storage2");

			follower = LocateRegistry.getRegistry(5004);
			followerServer2 = (StorageInterface) follower.lookup("Storage3");

			System.out.println("Servidor de Armazenamento-1 ligado.");

		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void addCar(Car newCar) {
		cars = getFileCars(); // pega do arquivo e bota no mapa

		if(role == ServerRole.LEADER) {
			cars.put(newCar.getRenavam(), newCar); // add no mapa
			attServer(); // salva o mapa num arquivo
			System.out.println("Carro adicionado com sucesso.");
		}
	}

	@Override
	public void editCar(String renavam, Car editedCar) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		Car editCar = searchCar(renavam);
		
		if(role == ServerRole.LEADER) {
			if(editedCar.getName() != null) {
				editCar.setName(editedCar.getName());
			}
			if(editedCar.getCategory() != 0) {
				switch(editCar.getCategory()) {
				case 1:
					economicAmount--;
					break;
				case 2:
					intermediaryAmount--;
					break;
				case 3:
					executiveAmount--;
					break;
				}
				switch(editedCar.getCategory()) {
				case 1:
					economicAmount++;
					break;
				case 2:
					intermediaryAmount++;
					break;
				case 3:
					executiveAmount++;
					break;
				}
				editCar.setCategory(editedCar.getCategory());
			}
			if(editedCar.getManufactureYear() != null) {
				editCar.setManufactureYear(editedCar.getManufactureYear());
			}
			if(editedCar.getPrice() != 0.0) {
				editCar.setPrice(editedCar.getPrice());
			}
			
			System.out.println("Carro de renavam " + renavam + " editado com sucesso.");
			
			attServer(); // salva o mapa num arquivo
		}
		
	}

	@Override
	public void deleteCar(String renavam) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		Car deleteCar = searchCar(renavam);
		
		if(role == ServerRole.LEADER) {
			if(deleteCar != null) {
				cars.remove(renavam, deleteCar);
				System.out.println("Carro de renavam " + renavam + " deletado com sucesso.");
			}
			
			attServer(); // salva o mapa num arquivo
		}
	}
	
	@Override
	public void deleteCars(String name) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> deleteCars = searchCars(name);
		
		if(role == ServerRole.LEADER) {
			if(deleteCars != null) {
				for(Car toDeleteCar : deleteCars) {
					cars.remove(toDeleteCar.getRenavam(), toDeleteCar);
				}
				System.out.println("Todos os carros " + name + " deletados com sucesso.");
			}
			
			attServer(); // salva o mapa num arquivo
		}
	}

	@Override
	public List<Car> listCars() {
		cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> list = new ArrayList<Car>();
		
		for (Entry<String, Car> car : cars.entrySet()) {
			list.add(car.getValue());
		}
		
		// processo de ordenar por nome
		Comparator<Car> comparator = Comparator.comparing(Car::getName);
		Collections.sort(list, comparator);
		
		System.out.println("Lista de carros enviada.");
		
		return list;
	}
	
	@Override
	public List<Car> listCars(int category) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> list = new ArrayList<Car>();
		
		for (Entry<String, Car> car : cars.entrySet()) {
			if(car.getValue().getCategory() == category) {
				list.add(car.getValue());	
			}
		}
		
		// processo de ordenar por nome
		Comparator<Car> comparator = Comparator.comparing(Car::getName);
		Collections.sort(list, comparator);
				
		System.out.println("Lista de carros da categoria " + category + " enviada.");
		
		return list;
	}

	@Override
	public Car searchCar(String renavam) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		
		Car finded = null;
		for (Entry<String, Car> car : cars.entrySet()) {
			if (renavam.equals(car.getKey()) && renavam.equals(car.getValue().getRenavam())) {
				System.out.println("Carro encontrado com sucesso! Nome: " + car.getValue().getName() + ".");
				finded = car.getValue();
				break;
			}
		}
		
		return finded;

	}

	@Override
	public List<Car> searchCars(String name) {
		cars = getFileCars(); // att o mapa pra versao mais recente
		
		List<Car> findeds = new ArrayList<Car>();
		for (Entry<String, Car> car : cars.entrySet()) {
			if (name.equalsIgnoreCase(car.getValue().getName())) {
				System.out.println("Renavam: " + car.getValue().getRenavam() + ".");
				findeds.add(car.getValue());
			}
		}
		
		return findeds;
	}

	@Override
	public Car buyCar(String renavam) {
		
		if(role == ServerRole.LEADER) {
			Car purchased = searchCar(renavam);
			System.out.println("Carro de renavam " + renavam + " foi comprado.");
			deleteCar(renavam);
			
			return purchased;
		}
		
		return null;
	}
	
	private static HashMap<String, Car> getFileCars() {
		boolean eof = false;
		
		cars = new HashMap<String, Car>();
		
		try {	
			fileInput = new ObjectInputStream(new FileInputStream(path[currentDatabase]));
			
			while (!eof) {
				Car registeredCar = null;
				
	            try {
	                registeredCar = (Car) fileInput.readObject();
	            } catch (EOFException e) { // fim do arquivo
	                eof = true;
	            }
	            
	            if (registeredCar != null) {
	                cars.put(registeredCar.getRenavam(), registeredCar);
	            }
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			currentDatabase = (currentDatabase + 1) % 3;
			cars = getFileCars();
		} 

		System.out.println("Buscado no BD atual: " + currentDatabase);
		
		return cars;
	}
	
	private void attServer() {

		try {
			for(int i = 0; i < 3; i++) {
				fileOutput = new ObjectOutputStream(new FileOutputStream(path[i]));
				
				economicAmount = 0;
				intermediaryAmount = 0;
				executiveAmount = 0;
				for (Entry<String, Car> car : cars.entrySet()) {
					fileOutput.writeObject(car.getValue());
					
					switch(car.getValue().getCategory()) {
					case 1:
						economicAmount++;
						break;
					case 2:
						intermediaryAmount++;
						break;
					case 3:
						executiveAmount++;
						break;
					default:
					}
				}	
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public int getAmount(int category) throws RemoteException {
		cars = getFileCars();
		attServer();
		
		switch(category) {
		case 1:
			EconomicCar.setAmount(economicAmount);
			return economicAmount;
		case 2:
			IntermediaryCar.setAmount(intermediaryAmount);
			return intermediaryAmount;
		case 3:
			ExecutiveCar.setAmount(executiveAmount);
			return executiveAmount;
		default:
			EconomicCar.setAmount(economicAmount);
			IntermediaryCar.setAmount(intermediaryAmount);
			ExecutiveCar.setAmount(executiveAmount);
			return economicAmount + intermediaryAmount + executiveAmount;	
		}
	}

	@Override
	public ServerRole getRole() throws RemoteException {
		return role;
	}

	@Override
	public void setRole(ServerRole ro) throws RemoteException {
		role = ro;
	}

	@Override
	public int getId() throws RemoteException {
		return id;
	}

	@Override
	public void setId(int ID) throws RemoteException {
		id = ID;
	}

	@Override
	public void setFollowers() throws RemoteException {
		followerServer1.setRole(ServerRole.FOLLOWER);
		followerServer2.setRole(ServerRole.FOLLOWER);
	}

	@Override
	public StorageInterface startElections() throws RemoteException {
		followerServer1.setRole(ServerRole.CANDIDATE);
		followerServer2.setRole(ServerRole.CANDIDATE);

		Random rand = new Random();
		followerServer1.setId(rand.nextInt());
		followerServer2.setId(rand.nextInt());

		if(followerServer1.getId() > followerServer2.getId()) {
			followerServer1.setRole(ServerRole.LEADER);
			followerServer1.setFollowers();
			return followerServer1;
		}
		followerServer2.setRole(ServerRole.LEADER);
		followerServer2.setFollowers();
		return followerServer2;
	}
	
}
