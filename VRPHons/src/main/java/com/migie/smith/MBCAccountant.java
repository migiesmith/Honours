package com.migie.smith;

import jade.content.Concept;
import jade.core.AID;

public class MBCAccountant implements Concept{

	public AID bidder;
	
	public double balance = 0.0d;
	
	
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

	protected void updateBalance(double profit){
		this.balance += profit;
	}
	
	public String toString(){
		return "{Bidder: "+ getBidder() +", Balance: "+ getBalance() +"}";
	}
}
