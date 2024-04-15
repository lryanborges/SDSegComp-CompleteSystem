package server.storage;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;

import crypto.Encrypter;
import crypto.Hasher;
import datagrams.Message;
import datagrams.Permission;
import model.Car;
import model.EconomicCar;
import model.ExecutiveCar;
import model.IntermediaryCar;
import model.Keys;
import server.Gateway;

public class StorageServer implements StorageInterface {

	private static Keys gatewayKeys;
	
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
	
	private static Permission gatewayPermission;
	
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
		
		StorageServer storServer = new StorageServer(ServerRole.FOLLOWER);
		scanner = new Scanner(System.in);

		try {
			StorageInterface server = (StorageInterface) UnicastRemoteObject.exportObject(storServer, 0);

			LocateRegistry.createRegistry(5004);
			Registry register = LocateRegistry.getRegistry("127.0.0.4", 5004);
			register.bind("Storage3", server);

			gatewayPermission = new Permission("192.168.1.105", "192.168.1.105", 5004, "Loja3", true);

			scanner.nextLine();

			Registry follower = LocateRegistry.getRegistry(5002);
			followerServer1 = (StorageInterface) follower.lookup("Storage1");

			follower = LocateRegistry.getRegistry(5003);
			followerServer2 = (StorageInterface) follower.lookup("Storage2");

			System.out.println("Servidor de Armazenamento-3 ligado.");


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
			if(!editedCar.getName().equals(null) && !editedCar.getName().equals("null")) {
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
			if(!editedCar.getManufactureYear().equals(null) && !editedCar.getManufactureYear().equals("null")) {
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
	
	@Override
	public void addNewClientKeys(Keys newClientKeys) throws RemoteException {
		gatewayKeys = newClientKeys;
	}
	
	@Override
	public Message receiveMessage(Message<String> msg) throws RemoteException {
		// permissões p serviço da loja
		if(StorageServer.getPermission()) {
			Keys currentClient = gatewayKeys;
			
			if(currentClient != null) {
				String decryptedMsg = Encrypter.fullDecrypt(currentClient, msg.getContent());
				String realHMAC;
				try {
					realHMAC = Hasher.hMac(currentClient.getHMACKey(), decryptedMsg);
					
					boolean validSignature = Encrypter.verifySignature(currentClient.getRsaKeys(), realHMAC, msg.getMessageSignature());
					
					if(validSignature) {
						switch(msg.getOperation()) {
						case 1:
							List<Car> response = this.listCars(Integer.parseInt(decryptedMsg)); // MUDARR O NUMERO PRO NUMERO DA MSG
							return new Message<Car>(1, response, "aqui fica a assinatura do server dps");
						case 111: 
							List<Car> response2 = this.listCars();
							return new Message<Car>(111, response2, "aqui fica assinatura dps");
						case 2:
							Car response3 = this.searchCar(decryptedMsg);
							return new Message<Car>(2, response3, "aqui fica assinatura");
						case 222:
							List<Car> response4 = this.searchCars(decryptedMsg);
							return new Message<Car>(222, response4, "assinatura");
						case 3:
							Car response5 = this.buyCar(decryptedMsg);
							return new Message<Car>(3, response5, "assinatura");
						case 4:
							Integer response6 = this.getAmount(Integer.parseInt(decryptedMsg));
							return new Message<Integer>(4, response6, "assinatura");
						case 5:
							String[] carPart = decryptedMsg.split("°");
							int typeOfCar = Integer.parseInt(carPart[2]);
							switch(typeOfCar) {
							case 1:
								this.addCar(new EconomicCar(carPart[0], carPart[1], typeOfCar, carPart[3], Double.parseDouble(carPart[4])));
								break;
							case 2:
								this.addCar(new IntermediaryCar(carPart[0], carPart[1], typeOfCar, carPart[3], Double.parseDouble(carPart[4])));
								break;
							case 3:
								this.addCar(new ExecutiveCar(carPart[0], carPart[1], typeOfCar, carPart[3], Double.parseDouble(carPart[4])));
								break;
							default:
								System.out.println("Tipo inválido.");
							}
							return null; // fica tipo o return void
						case 6:
							String[] carPart2 = decryptedMsg.split("°");
							String toEditRenavam = carPart2[0];
							this.editCar(toEditRenavam, new Car(toEditRenavam, carPart2[1], Integer.parseInt(carPart2[2]), carPart2[3], Double.parseDouble(carPart2[4])));
							return null; // fica tipo return void
						case 7:
							this.deleteCar(decryptedMsg);
							return null; // return void
						case 777:
							this.deleteCars(decryptedMsg);
							return null; // return void
						default: 
							System.out.println("Opção inválida.");
						}
					} else {
						System.out.println("Assinatura inválida. Cliente não autorizado.");
					}
					
				} catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} else {
				System.out.println("Cliente não existe.");
			}
		}
		
		return null;
	}
	
	public static boolean getPermission() {
		
		String sourceIp = "";
		
		try {
			sourceIp = RemoteServer.getClientHost();
		} catch (ServerNotActiveException e1) {
			e1.printStackTrace();
		}
		
		if(gatewayPermission.getSourceIp().equals(sourceIp) && (gatewayPermission.getDestinationPort() >= 5002 && gatewayPermission.getDestinationPort() <= 5004)) {
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
