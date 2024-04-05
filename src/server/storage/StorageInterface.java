package server.storage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import model.Car;

public interface StorageInterface extends Remote {

	public void addCar(Car newCar) throws RemoteException;
	public void editCar(String renavam, Car editedCar) throws RemoteException;
	public void deleteCar(String renavam) throws RemoteException;
	public void deleteCars(String name) throws RemoteException;
	public List<Car> listCars() throws RemoteException;
	public List<Car> listCars(int category) throws RemoteException;
	public Car searchCar(String renavam) throws RemoteException;
	public List<Car> searchCars(String category) throws RemoteException;
	public Car buyCar(String renavam) throws RemoteException;
	public int getAmount(int category) throws RemoteException;
	
}
