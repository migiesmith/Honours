package com.migie.smith;

import java.util.Random;

import javax.swing.JOptionPane;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class StartBidderMain {

	public static void main(String[] args){

		String userInput = "";
		String[] hostAddress = new String[0];
		while(hostAddress.length != 2){
			// Request the user for the IP and Port
			userInput = JOptionPane.showInputDialog("Enter IP:", "127.0.0.1:1099");
			// Exit if the user closes the dialog
			if(userInput == null)
				System.exit(0);
			
			// Update the host address
			hostAddress = userInput.split(":");
		}
		
		String bidderName = "";
		while(bidderName.equals("")){
			// Request the user for a bidder name (default to a random one)
			bidderName = JOptionPane.showInputDialog("Enter a name:", "Bidder"+ (new Random().nextInt(10000)));
			// Exit if the user closes the dialog
			if(userInput == null)
				System.exit(0);
		}
		
		// Separate the host and port from host address
		String host = hostAddress[0];
		String port = hostAddress[1];
		
		// Get the JADE runtime
		Runtime myRuntime = Runtime.instance();
		
		// Prepare the settings for the platform that we're going to start
		Profile myProfile = new ProfileImpl();
		myProfile.setParameter(Profile.MAIN_HOST, host);
		myProfile.setParameter(Profile.MAIN_PORT, port);
		myProfile.setParameter(Profile.MTPS, "jade.mtp.http.MessageTransportProtocol(http://"+host+":"+port+"/acc)");
		// Increase the maximum results from the DF (yellow-pages)
		myProfile.setParameter("jade_domain_df_maxresult","1000");
		
		try {
			// Create an agent container to connect to a main container
			ContainerController myContainer = myRuntime.createAgentContainer(myProfile);
			
			
			if(myContainer.isJoined()){
				// Create and start the MBCPlayerBidder
			    AgentController bidder = myContainer.createNewAgent(bidderName, MBCPlayerBidder.class.getCanonicalName(), new Object[]{true});
			    bidder.start();
			}
		    
			//JADE gui - Only needed for debugging
		    //AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
		    //rma.start();
			

		} catch(StaleProxyException e) {
		    e.printStackTrace();
		}
		
	}
	
}
