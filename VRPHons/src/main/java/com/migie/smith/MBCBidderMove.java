package com.migie.smith;

public class MBCBidderMove {
	
	// Location to insert the new visit
	int insertLocation = 0;
	// Whether a bid was made
	boolean acceptVisit = false;
	// Value to bid
	double bid = 0.0d;
	
	public MBCBidderMove(int insertLocation, double bid, boolean acceptVisit){
		this.insertLocation = insertLocation;
		this.bid = bid;
		this.acceptVisit = acceptVisit;
	}
	
}
