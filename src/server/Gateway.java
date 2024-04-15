package server;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import model.User;
import server.authentication.AuthInterface;
import server.storage.ServerRole;
import server.storage.StorageInterface;

public class Gateway implements GatewayInterface {

	private static Keys myKeys;
	private static Map<Integer, Keys> myClientKeys;
	
	static String authenticationHostName = "Authentication";
	static String storageHostName[] = {"Storage1","Storage2","Storage3"};
	static AuthInterface authServer;
	static StorageInterface storServer;
	static List<StorageInterface> stores;
	public static Registry register;
	
	private static List<Permission> permissions;
	
	public static void main(String[] args) {
		
		Gateway gateway = new Gateway();
		stores = new ArrayList<>();
		myKeys = new Keys();
		myClientKeys = new HashMap<Integer, Keys>();
		permissions = new ArrayList<Permission>();
		Gateway.setPermissions();
		
		// geração de chaves
		myKeys.setVernamKey(MyKeyGenerator.generateKeyVernam());
		myKeys.setAesKey(MyKeyGenerator.generateKeyAes());
		myKeys.setHMACKey(MyKeyGenerator.generateKeyHMAC());
		RSAKeys myRsaKeys = MyKeyGenerator.generateKeysRSA();
		// nao adiciona a chave privada ainda pq ainda vai enviar pros servers
		myKeys.setRsaKeys(new RSAKeys(myRsaKeys.getPublicKey(), myRsaKeys.getnMod()));
		
		try {
			Registry authRegister = LocateRegistry.getRegistry("127.0.0.1", 5001);
			authServer = (AuthInterface) authRegister.lookup(authenticationHostName);
			
			Registry stgRegister = LocateRegistry.getRegistry("127.0.0.2", 5002);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[0]);
			storServer.addNewClientKeys(myKeys);
			stores.add(storServer);

			stgRegister = LocateRegistry.getRegistry("127.0.0.3", 5003);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[1]);
			storServer.addNewClientKeys(myKeys);
			stores.add(storServer);

			stgRegister = LocateRegistry.getRegistry("127.0.0.4", 5004);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[2]);
			storServer.addNewClientKeys(myKeys);
			stores.add(storServer);

			storServer = stores.get(0);
			myKeys.getRsaKeys().setPrivateKey(myRsaKeys.getPrivateKey()); // agora sim add a chave privada dps de enviar pro server sem
			
			GatewayInterface protocol = (GatewayInterface) UnicastRemoteObject.exportObject(gateway, 0);
			
			LocateRegistry.createRegistry(5000);
			register = LocateRegistry.getRegistry("127.0.0.10", 5000);
			register.bind("Gateway", protocol);
			
			System.out.println("Gateway ligado...");
			
		} catch (RemoteException | AlreadyBoundException | NotBoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void register(User newUser) {
		if(Gateway.getPermission(5001)) {
			try {
				authServer.registerUser(newUser);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public User login(String cpf, String password) {
		if(Gateway.getPermission(5001)) {
			try {
				User connected = authServer.loginUser(cpf, password);
				
				return connected;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void addCar(Car newCar) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), newCar.toString());
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, newCar.toString());
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(5, msgEncrypted, signature));
				
				System.out.println("Carro adicionado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				this.addCar(newCar);
			}
		}
	}
	
	public void editCar(String renavam, Car editedCar) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), editedCar.toString());
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, editedCar.toString());
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(6, msgEncrypted, signature));
				
				System.out.println("Carro de renavam " + renavam + " editado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				this.editCar(renavam, editedCar);
			}
		}
	}

	public void deleteCar(String renavam) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), renavam);
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, renavam);
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(7, msgEncrypted, signature));
				System.out.println("Carro de renavam " + renavam + " deletado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				this.deleteCar(renavam);
			}
		}
	}
	
	public void deleteCars(String name) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), name);
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, name);
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(777, msgEncrypted, signature));
				
				System.out.println("Todos os carros " + name + " deletados com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				this.deleteCars(name);
			}
		}
	}
	
	public List<Car> listCars() throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros enviada.");
			
			String hmac = Hasher.hMac(myKeys.getHMACKey(), "push to pull");
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, "push to pull");
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<Car> response = (Message<Car>) storServer.receiveMessage(new Message<String>(111, msgEncrypted, signature));
			List<Car> findedCars = response.getListContent();
			return findedCars;
		}
		storServer = storServer.startElections();
		return this.listCars();
	}

	public List<Car> listCars(int category) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros da categoria " + category + " enviada.");

			String hmac = Hasher.hMac(myKeys.getHMACKey(), String.valueOf(category));
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, String.valueOf(category));
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<Car> response = (Message<Car>) storServer.receiveMessage(new Message<String>(1, msgEncrypted, signature));
			List<Car> findedCars = response.getListContent();
			return findedCars;
		}
		storServer = storServer.startElections();
		return this.listCars(category);
	}
	
	public Car searchCar(String renavam) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Carro encontrado com sucesso!");
			
			String hmac = Hasher.hMac(myKeys.getHMACKey(), renavam);
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, renavam);
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<Car> response = (Message<Car>) storServer.receiveMessage(new Message<String>(2, msgEncrypted, signature));
			Car findedCar = response.getContent();
			return findedCar;
		}
		storServer = storServer.startElections();
		return this.searchCar(renavam);
	}

	public List<Car> searchCars(String name) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros encontrada com sucesso!");

			String hmac = Hasher.hMac(myKeys.getHMACKey(), name);
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, name);
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<Car> response = (Message<Car>) storServer.receiveMessage(new Message<String>(222, msgEncrypted, signature));
			List<Car> findedCars = response.getListContent();
			return findedCars;
		}
		storServer = storServer.startElections();
		return this.searchCars(name);
	}
	
	public Car buyCar(String renavam) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				System.out.println("Carro de renavam " + renavam + " foi comprado.");

				String hmac = Hasher.hMac(myKeys.getHMACKey(), renavam);
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, renavam);
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				Message<Car> response = (Message<Car>) storServer.receiveMessage(new Message<String>(3, msgEncrypted, signature));
				Car findedCar = response.getContent();
				return findedCar;
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = storServer.startElections();
			}
		}
		return null;
	}
	
	public int getAmount(int category) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		String hmac = Hasher.hMac(myKeys.getHMACKey(), String.valueOf(category));
		String msgEncrypted = Encrypter.fullEncrypt(myKeys, String.valueOf(category));
		String signature = Encrypter.signMessage(myKeys, hmac);
		
		Message<Integer> responseInt = (Message<Integer>) storServer.receiveMessage(new Message<String>(4, msgEncrypted, signature));
		int amount = responseInt.getContent();
		
		return amount;
	}

	@Override
	public void putToSleep() throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				store.setRole(ServerRole.OUTOFORDER);
			}
		}
	}
	
	@Override
	public void addNewClientKeys(Integer clientNumber, Keys newClientKeys) throws RemoteException {
		myClientKeys.put(clientNumber, newClientKeys);
		System.out.println("Cliente: " + clientNumber + ", Chave pública: " + newClientKeys.getRsaKeys().getPublicKey());
		System.out.println("Chave privada: " + newClientKeys.getRsaKeys().getPrivateKey());
	}
	
	@Override
	public Message receiveMessage(Message<String> msg) throws RemoteException{
		// permissões p serviço da loja
		if(Gateway.getPermission(5002) || Gateway.getPermission(5003) || Gateway.getPermission(5004)) {
			Keys currentClient = myClientKeys.get(msg.getClientSendingMsg());
			
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
							return new Message<Car>(1, response, "aqui fica a assinatura do gateway dps");
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
	
	public static boolean getPermission(int destPort) {	
		String clientHost = "";
		
		try {
			clientHost = RemoteServer.getClientHost();
		} catch (ServerNotActiveException e1) {
			e1.printStackTrace();
		}
		
		for(Permission perm : permissions) {
			if(perm.getSourceIp().equals(clientHost) && perm.getDestinationPort() == destPort) {
				String aux;
				if(perm.isAllow()) {
					aux = "permitido";
				} else {
					aux = "negado";
				}
				
				System.out.println("------------------------");
				System.out.println("Firewall --> Pacote " + aux + ". Acesso: " + perm.getName() + ", source: " + clientHost);
				return true;
			}
		}
		
		return false; // negou acesso
	}
	
	public static void setPermissions() {
		permissions.add(new Permission("127.0.0.10", "127.0.0.1", 5001, "Autenticação", true));
		permissions.add(new Permission("127.0.0.10", "127.0.0.2", 5002, "Loja1", true));
		permissions.add(new Permission("127.0.0.10", "127.0.0.3", 5003, "Loja2", true));
		permissions.add(new Permission("127.0.0.10", "127.0.0.4", 5004, "Loja3", true));
		
		permissions.add(new Permission("192.168.1.105", "127.0.0.1", 5001, "Autenticação", true));
		permissions.add(new Permission("192.168.1.105", "127.0.0.2", 5002, "Loja1", true));
		permissions.add(new Permission("192.168.1.105", "127.0.0.3", 5003, "Loja2", true));
		permissions.add(new Permission("192.168.1.105", "127.0.0.4", 5004, "Loja3", true));
	}
	
}
