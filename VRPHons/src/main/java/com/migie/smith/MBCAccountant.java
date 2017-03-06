package com.migie.smith;

import jade.content.Concept;
import jade.core.AID;

public class MBCAccountant implements Concept{

	// The owner of the MBCAccountant
	public AID bidder;
	// The balance of the bidder
	public double balance = 0.0d;

	/**
	 * @param profit The value to increment/decrement the balance by
	 */
	protected void updateBalance(double profit){
		this.balance += profit;
	}
	
	public String toString(){
		return "{Bidder: "+ getBidder() +", Balance: "+ getBalance() +"}";
	}
	

	// Getters and Setters required by a Concept
	
	public AID getBidder() {
		return bidder;
	}

	public void setBidder(AID bidder) {
		this.bidder = bidder;
	}

	public double getBalance() {
		return balance;
	}
	
	public void setBalance(double balance) {
		this.balance = balance;
	}

}
