package com.migie.smith;

public class TimingRepresentation {
	
	// What can be used as an identifier for types in a time frame
	public enum TimeType{
		Travel, Visit, Empty
	};
	// Identifies what this represents in the time frame
	public TimeType type;
	// The duration of this portion of time
	public int timeTaken;
	
	TimingRepresentation(int timeTaken,	TimeType type){
		this.timeTaken = timeTaken;
		this.type = type;
	}
}
