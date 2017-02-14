package com.migie.smith;

import java.util.ArrayList;
import java.util.List;

public class Log {

	protected List<String> logData;
	
	Log(){
		logData = new ArrayList<String>();
	}
	
	// Get the contents of the log
	public List getLog(){
		return logData;
	}
	
	// Add an entry to the log
	public void log(String message){
		logData.add(message);
	}
	
}
