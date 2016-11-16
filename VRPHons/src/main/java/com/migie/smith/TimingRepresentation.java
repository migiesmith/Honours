package com.migie.smith;

public class TimingRepresentation {
	public enum TimeType{
		Travel, Visit, Empty
	};
	
	public int timeTaken;
	public TimeType type;
	
	TimingRepresentation(int timeTaken,	TimeType type){
		this.timeTaken = timeTaken;
		this.type = type;
	}
}
