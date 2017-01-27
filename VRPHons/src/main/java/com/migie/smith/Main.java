package com.migie.smith;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import com.migie.smith.institution.MBCInstitution;

import agent.auctionSolution.MapServer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Main{
	
    public static void main(String[] args){	
    	
    	String host = getIP();
    	String port = "1099";
    	System.out.println(host +":"+ port);
    	
		Runtime myRuntime = Runtime.instance();

		// prepare the settings for the platform that we're going to start
		Profile myProfile = new ProfileImpl();		
		// Increase the maximum results from the DF (yellow-pages)
		myProfile.setParameter("jade_domain_df_maxresult","1000");

		// create the main container
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);		
		
		try {
			//JADE gui
		    AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
		    rma.start();
		    
		    
		    AgentController auctioneer = myContainer.createNewAgent("Auctioneer", MBCAuctioneer.class.getCanonicalName(), null);
		    auctioneer.start();
		    
		    AgentController mapServer = myContainer.createNewAgent("Map-Server", MapServer.class.getCanonicalName(), null);
		    mapServer.start();

		    AgentController institution = myContainer.createNewAgent("Institution", MBCInstitution.class.getCanonicalName(), null);
		    institution.start();
		    
		} catch(StaleProxyException e) {
		    e.printStackTrace();
		}
        
    }
    
    public static String getIP(){
    	String ip = "";
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(
    	                whatismyip.openStream()));

	    	ip = in.readLine(); // Read the ip address
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ip;
    }
}