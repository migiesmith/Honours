package com.migie.smith;

import java.util.ArrayList;
import java.util.List;

public class Log {

	// List of every log entry
	protected List<String> logData;
	
	// Default constructor
	Log(){
		// Create an empty list of strings for the log
		logData = new ArrayList<String>();
	}
	
	// Get the contents of the log
	public List<String> getLog(){
		return logData;
	}
	
	// Add an entry to the log
	public void log(String message){
		logData.add(message);
	}
	
}
