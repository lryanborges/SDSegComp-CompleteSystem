package server.storage;

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
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import crypto.Encrypter;
import crypto.Hasher;
import crypto.MyKeyGenerator;
import datagrams.Message;
import datagrams.Permission;
import model.Car;
import model.EconomicCar;
import model.ExecutiveCar;
import model.IntermediaryCar;
import model.Keys;
import model.RSAKeys;

public class StorageServer implements StorageInterface {

	private static Keys gatewayKeys;
	private static RSAKeys myRSAKeys;
	
	private static ServerRole role;
	private static StorageInterface followerServer1;
	private static StorageInterface followerServer2;
	private static DatabaseInterface database;
	private static int id;
	private static Scanner scanner; 
	
	private static Permission gatewayPermission;
	
	public StorageServer(ServerRole r) {
		role = r;
	}
	
	public static void main(String[] args) {
		
		StorageServer storServer = new StorageServer(ServerRole.LEADER);
		scanner = new Scanner(System.in);
		myRSAKeys = MyKeyGenerator.generateKeysRSA();

		try {
			StorageInterface server = (StorageInterface) UnicastRemoteObject.exportObject(storServer, 0);
			
			LocateRegistry.createRegistry(5002);
			Registry register = LocateRegistry.getRegistry("127.0.0.2", 5002);
			register.bind("Storage1", server);

			gatewayPermission = new Permission("26.15.5.193", "26.15.5.193", 5002, "Loja1", true);

			scanner.nextLine();

			Registry follower = LocateRegistry.getRegistry(5003);
			followerServer1 = (StorageInterface) follower.lookup("Storage2");

			follower = LocateRegistry.getRegistry(5004);
			followerServer2 = (StorageInterface) follower.lookup("Storage3");

			Registry base = LocateRegistry.getRegistry(5010);
			database = (DatabaseInterface) base.lookup("Database1");

			System.out.println("Servidor de Armazenamento-1 ligado.");

		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void addCar(Car newCar) throws RemoteException {
		if(role == ServerRole.LEADER) {
			database.addCar(newCar);
			database.attServer();
			System.out.println("Carro adicionado com sucesso.");
		}
	}

	@Override
	public void editCar(String renavam, Car editedCar) throws RemoteException {
		
		if(role == ServerRole.LEADER) {
			database.editCar(renavam, editedCar);
			database.attServer();
			System.out.println("Carro de renavam " + renavam + " editado com sucesso.");
		}
		
	}

	@Override
	public void deleteCar(String renavam) throws RemoteException {
		if(role == ServerRole.LEADER) {
			database.deleteCar(renavam);
			database.attServer();
			System.out.println("Carro de renavam " + renavam + " deletado com sucesso.");
		}
	}
	
	@Override
	public void deleteCars(String name) throws RemoteException {
		if(role == ServerRole.LEADER) {
			database.deleteCars(name);
			database.attServer();
			System.out.println("Todos os carros " + name + " deletados com sucesso.");
		}
	}

	@Override
	public List<Car> listCars() throws RemoteException {
		System.out.println("Lista de carros enviada.");
		return database.listCars();
	}
	
	@Override
	public List<Car> listCars(int category) throws RemoteException {
		System.out.println("Lista de carros da categoria " + category + " enviada.");
		return database.listCars(category);
	}

	@Override
	public Car searchCar(String renavam) throws RemoteException {
		Car car = database.searchCar(renavam);
		return car;
	}

	@Override
	public List<Car> searchCars(String name) throws RemoteException {
		List<Car> list = searchCars(name);
		return list;
	}

	@Override
	public Car buyCar(String renavam) throws RemoteException {
		
		if(role == ServerRole.LEADER) {
			Car car = database.buyCar(renavam);
			database.attServer();
			System.out.println("Carro de renavam " + renavam + " foi comprado.");
			return car;
		}
		
		return null;
	}

	@Override
	public int getAmount(int category) throws RemoteException {
		int amount = database.getAmount(category);
		return amount;
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
	
	public RSAKeys getRSAKeys() throws RemoteException {
		return new RSAKeys(myRSAKeys.getPublicKey(), myRSAKeys.getnMod());
	}
	
	@Override
	public Message<String> receiveMessage(Message<String> msg) throws RemoteException {
		// permissões p serviço da loja
		if(StorageServer.getPermission()) {
			Keys currentClient = gatewayKeys;
			
			if(currentClient != null) {
				String decryptedMsg = Encrypter.fullDecrypt(currentClient, msg.getContent());
				String realHMAC;
				
				try {
					realHMAC = Hasher.hMac(currentClient.getHMACKey(), decryptedMsg);
					
					boolean validSignature = Encrypter.verifySignature(currentClient.getRsaKeys(), realHMAC, msg.getMessageSignature());
					
					String hmac;
					String msgEncrypted;
					String signature;
					String toEncrypt;
					if(validSignature) {
						switch(msg.getOperation()) {
						case 1:
							List<Car> response = this.listCars(Integer.parseInt(decryptedMsg)); // MUDARR O NUMERO PRO NUMERO DA MSG

							toEncrypt = "";
							for(Car car : response) {
								toEncrypt = toEncrypt + "¬" + car.toString();
							}
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), toEncrypt);
							msgEncrypted = Encrypter.fullEncrypt(currentClient, toEncrypt);
							signature = Encrypter.signMessage(myRSAKeys, hmac);
							
							return new Message<String>(1, msgEncrypted, signature);
						case 111: 
							List<Car> response2 = this.listCars();
							
							toEncrypt = "";
							for(Car car : response2) {
								toEncrypt = toEncrypt + "¬" + car.toString();
							}
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), toEncrypt);
							msgEncrypted = Encrypter.fullEncrypt(currentClient, toEncrypt);
							signature = Encrypter.signMessage(myRSAKeys, hmac);
							
							return new Message<String>(111, msgEncrypted, signature);
						case 2:
							Car response3 = this.searchCar(decryptedMsg);

							hmac = Hasher.hMac(currentClient.getHMACKey(), response3.toString());
							msgEncrypted = Encrypter.fullEncrypt(currentClient, response3.toString());
							signature = Encrypter.signMessage(myRSAKeys, hmac);
							
							return new Message<String>(2, msgEncrypted, signature);
						case 222:
							List<Car> response4 = this.searchCars(decryptedMsg);

							toEncrypt = ""; 
							for(Car car : response4) {
								toEncrypt = toEncrypt + "¬" + car.toString();
							}
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), toEncrypt);
							msgEncrypted = Encrypter.fullEncrypt(currentClient, toEncrypt);
							signature = Encrypter.signMessage(myRSAKeys, hmac);
							
							return new Message<String>(222, msgEncrypted, signature);
						case 3:
							Car response5 = this.buyCar(decryptedMsg);

							hmac = Hasher.hMac(currentClient.getHMACKey(), response5.toString());
							msgEncrypted = Encrypter.fullEncrypt(currentClient, response5.toString());
							signature = Encrypter.signMessage(myRSAKeys, hmac);
							
							return new Message<String>(3, msgEncrypted, signature);
						case 4:
							Integer response6 = this.getAmount(Integer.parseInt(decryptedMsg));

							hmac = Hasher.hMac(currentClient.getHMACKey(), String.valueOf(response6));
							msgEncrypted = Encrypter.fullEncrypt(currentClient, String.valueOf(response6));
							signature = Encrypter.signMessage(myRSAKeys, hmac);
							
							return new Message<String>(4, msgEncrypted, signature);
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
