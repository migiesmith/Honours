package com.migie.smith;

public class MBCAccountant {

	protected double balance = 0.0d;
	
	protected double getBalance(){
		return this.balance;
	}
	
	protected void updateBalance(double profit){
		this.balance += profit;
	}
	
}
