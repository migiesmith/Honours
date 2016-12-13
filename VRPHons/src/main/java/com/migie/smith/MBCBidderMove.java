package com.migie.smith;

public class MBCBidderMove {

	int insertLocation = 0;
	boolean acceptVisit = false;
	double bid = 0.0d;
	
	public MBCBidderMove(int insertLocation, double bid, boolean acceptVisit){
		this.insertLocation = insertLocation;
		this.bid = bid;
		this.acceptVisit = acceptVisit;
	}
	
}
