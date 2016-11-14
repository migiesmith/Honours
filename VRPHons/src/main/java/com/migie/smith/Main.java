package com.migie.smith;


import agent.auctionSolution.MapServer;
import agent.auctionSolution.MyAuctioneer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Main{
	

    public static void main( String[] args )
    {	
		Runtime myRuntime = Runtime.instance();

		// prepare the settings for the platform that we're going to start
		Profile myProfile = new ProfileImpl();
		// Increase the maximum results from the DF (yellow-pages)
		myProfile.setParameter("jade_domain_df_maxresult","1000");

		// create the main container
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);		
		
		try {
			//JADE gui
		    //AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
		    //rma.start();
		    
		    
		    AgentController auctioneer = myContainer.createNewAgent("Auctioneer", MBCAuctioneer.class.getCanonicalName(), null);
		    auctioneer.start();
		    
		    AgentController mapServer = myContainer.createNewAgent("Map-Server", MapServer.class.getCanonicalName(), null);
		    mapServer.start();
		} catch(StaleProxyException e) {
		    e.printStackTrace();
		}
        
    }
}