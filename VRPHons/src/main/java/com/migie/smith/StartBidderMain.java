package com.migie.smith;

import javax.swing.JOptionPane;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class StartBidderMain {

	public static void main(String[] args){

		// Request the user for the IP and Port
		String[] hostAddress = JOptionPane.showInputDialog("Enter IP:", "127.0.0.1:1099").split(":");
		
		
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
		    
			
		    AgentController bidder = myContainer.createNewAgent("Bidder"+port, MBCPlayerBidder.class.getCanonicalName(), new Object[]{true});
		    bidder.start();

		} catch(StaleProxyException e) {
		    //e.printStackTrace();
		}
		
	}
	
}
