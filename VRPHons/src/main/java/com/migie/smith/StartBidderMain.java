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
			if(userInput == null)
				System.exit(0);
			
			hostAddress = userInput.split(":");
		}
		
		String bidderName = "";
		while(bidderName.equals("")){
			// Request the user for the IP and Port
			bidderName = JOptionPane.showInputDialog("Enter a name:", "Bidder"+ (new Random().nextInt(10000)));
			if(userInput == null)
				System.exit(0);
		}
		
		
		String host = hostAddress[0];
		String port = hostAddress[1];
		
		Runtime myRuntime = Runtime.instance();
		
		// prepare the settings for the platform that we're going to start
		Profile myProfile = new ProfileImpl();
		myProfile.setParameter(Profile.MAIN_HOST, host);
		myProfile.setParameter(Profile.MAIN_PORT, port);
		myProfile.setParameter(Profile.MTPS, "jade.mtp.http.MessageTransportProtocol(http://"+host+":"+port+"/acc)");
		// Increase the maximum results from the DF (yellow-pages)
		myProfile.setParameter("jade_domain_df_maxresult","1000");
		
		try {
			// create the main container
			ContainerController myContainer = myRuntime.createAgentContainer(myProfile);
			
			//JADE gui
		    //AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
		    //rma.start();
		    
			
		    AgentController bidder = myContainer.createNewAgent(bidderName, MBCPlayerBidder.class.getCanonicalName(), new Object[]{true});
		    bidder.start();

		} catch(StaleProxyException e) {
		    //e.printStackTrace();
		}
		
	}
	
}
