package model;

import java.io.Serializable;

public abstract class User implements Serializable {

	protected String name;
	protected String cpf;
	protected boolean isEmployee;
	protected String password;
	
	public void list() {}
	
	public Car search(long renavam) {
		return null;
	}
	
	public int getAmount() {
		return 0;
	}
	
	public Car buyCar(Car carToBuy) {
		return carToBuy;
	}
	
	public Car buyCar(long carIdToBuy) {
		return null;
	}
	
	public Car buyCar(String nameCarToBuy) {
		return null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public boolean isEmployee() {
		return isEmployee;
	}

	public void setEmployee(boolean isEmployee) {
		this.isEmployee = isEmployee;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
