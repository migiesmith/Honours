package com.migie.smith;

import agent.auctionSolution.MapServer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class StartBidderMain {

	public static void main(String[] args){

		String host = "192.168.0.2";
		String port = "1099";
		
		Runtime myRuntime = Runtime.instance();
		
		// prepare the settings for the platform that we're going to start
		Profile myProfile = new ProfileImpl();
		myProfile.setParameter(Profile.MAIN_HOST, host);
		myProfile.setParameter(Profile.MAIN_PORT, "1099");
		myProfile.setParameter(Profile.MTPS, "jade.mtp.http.MessageTransportProtocol(http://"+host+":"+port+"/acc)");
		// Increase the maximum results from the DF (yellow-pages)
		myProfile.setParameter("jade_domain_df_maxresult","1000");

		// create the main container
		ContainerController myContainer = myRuntime.createAgentContainer(new ProfileImpl());
		
		try {
			//JADE gui
		    //AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
		    //rma.start();
		    
			
			// TODO
		    AgentController bidder = myContainer.createNewAgent("Bidder", MBCPlayerBidder.class.getCanonicalName(), null);
		    bidder.start();

		} catch(StaleProxyException e) {
		    e.printStackTrace();
		}
	}
	
}
