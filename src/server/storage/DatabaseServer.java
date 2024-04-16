package server.storage;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import model.Car;
import model.EconomicCar;
import model.ExecutiveCar;
import model.IntermediaryCar;

public class DatabaseServer implements DatabaseInterface {

    private static String path[] = {"src/server/storage/cars.txt", "src/server/storage/cars2.txt", "src/server/storage/cars3.txt"};
	private static HashMap<String, Car> cars;
	private static ObjectOutputStream fileOutput;
	private static ObjectInputStream fileInput;
    private static int currentDatabase = 0;
	private static int economicAmount = 0, intermediaryAmount = 0, executiveAmount = 0;
    private static int myPath;

    public DatabaseServer(int rep) {
        myPath = rep;	
        try {
            fileInput = new ObjectInputStream(new FileInputStream(path[myPath]));
        }
        catch(IOException e) {
            int databaseRep = myPath+1;
            System.out.println("Banco-" + databaseRep);
        }

		cars = getFileCars();
    }

    public static void main(String[] args) {
        DatabaseServer dataServer = new DatabaseServer(0);
        
        try {
            DatabaseInterface database = (DatabaseInterface) UnicastRemoteObject.exportObject(dataServer, 0);
            LocateRegistry.createRegistry(5010+myPath);
            Registry register = LocateRegistry.getRegistry("localhost",5010+myPath);
            int databaseRep = myPath+1;
            register.bind("Database"+databaseRep, database);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    synchronized public void addCar(Car newCar) throws RemoteException {
        cars = getFileCars(); // pega do arquivo e bota no mapa
        cars.put(newCar.getRenavam(), newCar); // add no mapa
    }

    @Override
    synchronized public void editCar(String renavam, Car editedCar) throws RemoteException {
        cars = getFileCars(); // att o mapa pra versao mais recente
		Car editCar = searchCar(renavam);

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
    }

    @Override
    synchronized public void deleteCar(String renavam) throws RemoteException {
        cars = getFileCars(); // att o mapa pra versao mais recente
		Car deleteCar = searchCar(renavam);

        if(deleteCar != null) {
			cars.remove(renavam, deleteCar);
        }
    }

    @Override
    synchronized public void deleteCars(String name) throws RemoteException {
        cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> deleteCars = searchCars(name);

        if(deleteCars != null) {
            for(Car toDeleteCar : deleteCars) {
                cars.remove(toDeleteCar.getRenavam(), toDeleteCar);
            }
            
        }
    }

    @Override
    public List<Car> listCars() throws RemoteException {
        cars = getFileCars(); // att o mapa pra versao mais recente
		List<Car> list = new ArrayList<Car>();
		
		for (Entry<String, Car> car : cars.entrySet()) {
			list.add(car.getValue());
		}
        // processo de ordenar por nome
		Comparator<Car> comparator = Comparator.comparing(Car::getName);
		Collections.sort(list, comparator);
        return list;
    }

    @Override
    public List<Car> listCars(int category) throws RemoteException {
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
        return list;
    }

    @Override
    public Car searchCar(String renavam) throws RemoteException {
        cars = getFileCars(); // att o mapa pra versao mais recente
		
		Car finded = null;
		for (Entry<String, Car> car : cars.entrySet()) {
			if (renavam.equals(car.getKey()) && renavam.equals(car.getValue().getRenavam())) {
				finded = car.getValue();
				break;
			}
		}
        return finded;
    }

    @Override
    public List<Car> searchCars(String name) throws RemoteException {
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
    synchronized public Car buyCar(String renavam) throws RemoteException {
        Car purchased = searchCar(renavam);
        deleteCar(renavam);
        return purchased;
    }

    @Override
    public int getAmount(int category) throws RemoteException {
        cars = getFileCars();
		
		
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
    synchronized public void attServer() {

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

		//System.out.println("Buscado no BD atual: " + myPath);
		
		return cars;
	}
    
}
