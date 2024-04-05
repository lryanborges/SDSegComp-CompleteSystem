package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import model.Car;
import model.Client;
import model.EconomicCar;
import model.Employee;
import model.ExecutiveCar;
import model.IntermediaryCar;
import model.User;
import server.GatewayInterface;

public class ProcessClient {
	
	private static boolean connected = false;
	private static Scanner scan;
	private static GatewayInterface gateway;
	private static User connectedUser;
	private static List<Car> myCars;
	
	public static void main(String[] args) {
		
		scan = new Scanner(System.in);
		myCars = new ArrayList<Car>();
		
		try {
			Registry authRegister = LocateRegistry.getRegistry(5000);
			gateway = (GatewayInterface) authRegister.lookup("Gateway");
			
			while(!connected) {
				
				System.out.println("\tAUTENTICAÇÃO");
				System.out.println("------------------------");
				System.out.println("[1] - Login");
				System.out.println("[2] - Registrar-se");
				System.out.println("------------------------");
				System.out.print("Opção: ");
				int opc = scan.nextInt();
				scan.nextLine();
				System.out.println("------------------------");
				
				switch(opc) {
				case 1:
					connectedUser = login();
					if(connectedUser != null) {
						connected = true;
					}
					break;
				case 2:
					register();
					break;
				default:
					System.out.println("Opção inválida.");
				} 
				
			}
			
			while(connected) {
				System.out.println("------------------");
				System.out.println("LOJA DE CARROS");
				System.out.println("------------------");
				System.out.println("[1] - Listar carros");
				System.out.println("[2] - Pesquisar carro");
				System.out.println("[3] - Comprar carro");
				System.out.println("[4] - Ver quantidade de carros");
				if(connectedUser.isEmployee()) {
					System.out.println("[5] - Adicionar carro");
					System.out.println("[6] - Alterar carro");
					System.out.println("[7] - Excluir carro");
				}
				System.out.println("[0] - Carros comprados nessa ida à loja");
				System.out.println("------------------");
				System.out.print("Opção: ");
				int opc = scan.nextInt();
				scan.nextLine();
				System.out.println("------------------");
				
				switch(opc) {
				
				case 1:
					
					System.out.println("\tLISTAGEM");
					System.out.println("------------------");
					System.out.println("[1] - Carros econômicos");
					System.out.println("[2] - Carros intermediários");
					System.out.println("[3] - Carros executivos");
					System.out.println("[4] - Todos os tipos");
					System.out.println("------------------");
					System.out.print("Opção: ");
					int listOpc = scan.nextInt();
					scan.nextLine();
					
					List<Car> cars = new ArrayList<Car>();
					
					switch(listOpc) {
					case 1:
					case 2:
					case 3:
						cars = gateway.listCars(listOpc);
						break;
					case 4:
						cars = gateway.listCars();
						break;
					default:
						System.out.println("Opção inválida.");
					}
					
					for(Car car : cars) {
						System.out.println("------------------");
						System.out.println("Renavam: " + car.getRenavam());
						System.out.println("Nome: " + car.getName());
						System.out.println("Categoria: " + car.getStringCategory());
						System.out.println("Ano de fabricação: " + car.getManufactureYear());
						System.out.println("Preço: R$" + car.getPrice());
					}
					
					break;
				case 2:
					
					System.out.println("PESQUISA");
					System.out.println("------------------");
					System.out.println("[1] - Renavam");
					System.out.println("[2] - Nome");
					System.out.println("------------------");
					System.out.print("Opção: ");
					int searchOpc = scan.nextInt();
					scan.nextLine();
					System.out.println("------------------");
					
					switch(searchOpc) {
					case 1:
						System.out.print("Digite o renavam: ");
						String renavam = scan.nextLine();
						
						Car findedCar = gateway.searchCar(renavam);
						if(findedCar != null) {
							System.out.println("------------------");
							System.out.println("Renavam: " + findedCar.getRenavam());
							System.out.println("Nome: " + findedCar.getName());
							System.out.println("Categoria: " + findedCar.getStringCategory());
							System.out.println("Ano de fabricação: " + findedCar.getManufactureYear());
							System.out.println("Preço: R$" + findedCar.getPrice());
						} else {
							System.out.println("Carro não encontrado.");
						}
						
						break;
					case 2:
						System.out.print("Digite o nome: ");
						String name = scan.nextLine();
						
						List<Car> findedsCar = gateway.searchCars(name);
						for(Car car : findedsCar) {
							System.out.println("------------------");
							System.out.println("Renavam: " + car.getRenavam());
							System.out.println("Nome: " + car.getName());
							System.out.println("Categoria: " + car.getStringCategory());
							System.out.println("Ano de fabricação: " + car.getManufactureYear());
							System.out.println("Preço: R$" + car.getPrice());
						} 
						break;
					default:
						System.out.println("Opção inválida.");
					}
					
					break;
				case 3:
					buyCar();
					
					break;
				case 4:
					System.out.println("[1] - Econômico");
					System.out.println("[2] - Intermediário");
					System.out.println("[3] - Executivo");
					System.out.println("[4] - Todas as categorias");
					System.out.println("--------------------");
					System.out.print("Categoria: ");
					int category = scan.nextInt();
					
					int amount = gateway.getAmount(category);
					System.out.println("--------------------");
					System.out.println("Quantidade de carros na loja: " + amount);
					
					break;
				case 5:
					if(connectedUser.isEmployee()) {
						addCar();
					}
					
					break;
				case 6:
					if(connectedUser.isEmployee()) {
						editCar();
					}
					
					break;
				case 7:
					if(connectedUser.isEmployee()) {
						deleteCar();
					}
					
					break;
				case 0:
					System.out.println("--------------------");
					System.out.println("CARROS COMPRADOS");
					for(Car purchased : myCars) {
						System.out.println("--------------------");
						System.out.println("Renavam: " + purchased.getRenavam());
						System.out.println("Nome: " + purchased.getName());
						System.out.println("Categoria: " + purchased.getStringCategory());
						System.out.println("Ano de fabricação: " + purchased.getManufactureYear());
						System.out.println("Preço: R$" + purchased.getPrice());
					}
					
					break;
				case 4545:
					gateway.putToSleep();
					
					break;
				default:
					System.out.println("Opção inválida.");
				
				}
				
			}
			
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
	}
	
	private static void register() throws RemoteException {
		
		System.out.println("\tCADASTRO");
		System.out.println("------------------");
		System.out.println("[1] - Cliente");
		System.out.println("[2] - Funcionário");
		System.out.println("------------------");
		System.out.print("Opção: ");
		int opc = scan.nextInt();
		scan.nextLine();

		System.out.print("Digite o seu nome: ");
		String name = scan.nextLine();
		System.out.print("Digite o seu CPF: ");
		String cpf = scan.nextLine();
		System.out.print("Digite a sua senha: ");
		String password = scan.nextLine();
		System.out.println("------------------------");

		switch (opc) {
		case 1:
			gateway.register(new Client(cpf, password, name)); 
			break;
		case 2:
			gateway.register(new Employee(cpf, password, name));
			break;
		default:
			gateway.register(new Client(cpf, password, name));  // por padrão cria cliente
			System.out.println("Opção inválida. Cadastrado como cliente");
		}
	}
	
	private static User login() throws RemoteException {
		System.out.println("\tLOGIN");
		System.out.println("------------------------");
		System.out.print("CPF: ");
		String cpf = scan.nextLine();
		System.out.print("Senha: ");
		String password = scan.nextLine();
		System.out.println("------------------------");
		
		User connected = gateway.login(cpf, password);
		
		return connected;
		
	}
	
	private static void addCar() throws RemoteException {
		System.out.print("Renavam: ");
		String newRenavam = scan.nextLine();
		System.out.print("Nome: ");
		String newName = scan.nextLine();
		System.out.print("Ano de fabricação: ");
		String newYear = scan.nextLine();
		System.out.print("Preço: R$");
		double newPrice = scan.nextDouble();
		scan.nextLine();
		System.out.println("--------------------");
		System.out.println("[1] - Econômico");
		System.out.println("[2] - Intermediário");
		System.out.println("[3] - Executivo");
		System.out.println("--------------------");
		System.out.print("Categoria: ");
		int newCategory = scan.nextInt();
		scan.nextLine();
		
		switch(newCategory) {
		case 1:
			gateway.addCar(new EconomicCar(newRenavam, newName, newCategory, newYear, newPrice));
			break;
		case 2:
			gateway.addCar(new IntermediaryCar(newRenavam, newName, newCategory, newYear, newPrice));
			break;
		case 3:
			gateway.addCar(new ExecutiveCar(newRenavam, newName, newCategory, newYear, newPrice));
			break;
		default:
			System.out.println("Opção inválida.");
		}
		
		if(newCategory >= 1 && newCategory <= 3) {
			System.out.println("--------------------");
			System.out.println("Cadastrado com sucesso.");
		}
	}
	
	private static void editCar() throws RemoteException {
		System.out.print("Digite o renavam do carro a ser alterado: ");
		String toEditRenavam = scan.nextLine();
		
		System.out.println("EDIÇÃO");
		System.out.println("------------------");
		System.out.println("[1] - Nome");
		System.out.println("[2] - Categoria");
		System.out.println("[3] - Preço");
		System.out.println("[4] - Ano de fabricação");
		System.out.println("------------------");
		System.out.print("Opção:");
		int editOpc = scan.nextInt();
		scan.nextLine();
		
		String editedName = null, editedYear = null;
		int editedCategory = 0;
		double editedPrice = 0.0;
		switch(editOpc) {
		case 1:
			System.out.print("Novo nome: ");
			editedName = scan.nextLine();
			break;
		case 2:
			System.out.println("[1] - Econômico");
			System.out.println("[2] - Intermediário");
			System.out.println("[3] - Executivo");
			System.out.println("--------------------");
			System.out.print("Nova categoria: ");
			editedCategory = scan.nextInt();
			scan.nextLine();
			break;
		case 3:
			System.out.print("Novo preço: ");
			editedPrice = scan.nextDouble();
			scan.nextLine();
			break;
		case 4:
			System.out.print("Novo ano de fabricação: ");
			editedYear = scan.nextLine();
			break;
		default:
			System.out.println("Opção inválida.");
		}
		
		gateway.editCar(toEditRenavam, new Car(toEditRenavam, editedName, editedCategory, editedYear, editedPrice));
		
		System.out.println("--------------------");
		System.out.println("Editado com sucesso.");
	}
	
	private static void deleteCar() throws RemoteException {
		System.out.println("EXCLUSÃO");
		System.out.println("------------------");
		System.out.println("[1] - Renavam");
		System.out.println("[2] - Nome");
		System.out.println("------------------");
		System.out.print("Opção:");
		int deleteOpc = scan.nextInt();
		scan.nextLine();
		
		switch(deleteOpc) {
		case 1:
			System.out.print("Digite o renavam do carro a ser excluído: ");
			String toDeleteRenavam = scan.nextLine();
			
			gateway.deleteCar(toDeleteRenavam);
			
			System.out.println("--------------------");
			System.out.println("Carro com renavam " + toDeleteRenavam + " excluído com sucesso.");
			break;
		case 2:
			System.out.print("Digite o nome dos carros a serem excluídos: ");
			String toDeleteName = scan.nextLine();
			
			gateway.deleteCars(toDeleteName);
			
			System.out.println("--------------------");
			System.out.println("Todos os carros " + toDeleteName + " excluídos com sucesso.");
			break;
		default:
			System.out.println("Opção inválida");
		}
		
	}
	
	private static void buyCar() throws RemoteException {
		System.out.println("COMPRA DE CARRO");
		System.out.println("------------------");
		System.out.print("Renavam do carro:");
		String renavamToBuy = scan.nextLine();
		Car buyCar = gateway.searchCar(renavamToBuy);
		
		System.out.println("Nome: " + buyCar.getName());
		System.out.println("Categoria: " + buyCar.getStringCategory());
		System.out.println("Ano de fabricação: " + buyCar.getManufactureYear());
		System.out.println("Preço: R$" + buyCar.getPrice());
		System.out.println("------------------");
		System.out.print("Confirmar compra? (S/N)");
		String confirm = scan.nextLine();
		
		switch(confirm.charAt(0)) {
		case 'Y':
		case 'y':
		case 'S':
		case 's':
			Car purchased = gateway.buyCar(renavamToBuy);
			if(purchased != null) {
				System.out.println("Compra efetuada com sucesso.");
				System.out.println("Carro: " + purchased.getRenavam());
				myCars.add(purchased);
			} else {
				System.out.println("------------------");
				System.out.println("Já compraram esse carro enquanto você pensava.");
			}
			
			break;
		case 'N':
		case 'n':
			System.out.println("Compra cancelada.");
			break;
		default:
			System.out.println("Opção inválida. Compra cancelada.");
		}
	}
	
}
