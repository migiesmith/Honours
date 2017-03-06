package com.migie.smith;


import javax.swing.JDialog;
import javax.swing.JOptionPane;

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
    	
    	// Get the IP for displaying
    	String host = MBCHelper.getIP();
    	String port = "1099";
    	
    	// Get an instance of the JADE runtime
		Runtime myRuntime = Runtime.instance();

		// Prepare the settings for the platform that we're going to start
		Profile myProfile = new ProfileImpl();		
		// Increase the maximum results from the DF (yellow-pages)
		myProfile.setParameter("jade_domain_df_maxresult","1000");

		// Create the main container
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);		
		
		try {
		    // Start the MBCAuctioneer
		    AgentController auctioneer = myContainer.createNewAgent("Auctioneer", MBCAuctioneer.class.getCanonicalName(), null);
		    auctioneer.start();
		    
		    // Start the MapServer
		    AgentController mapServer = myContainer.createNewAgent("Map-Server", MapServer.class.getCanonicalName(), null);
		    mapServer.start();

		    // Start the MBCInstitution
		    AgentController institution = myContainer.createNewAgent("Institution", MBCInstitution.class.getCanonicalName(), new Object[]{false}); // Last parameter determines if costings are random
		    institution.start();

		    // Write the IP to the console
	    	System.out.println(host +":"+ port);
	    	
	    	// Show a prompt containing the IP
		    JOptionPane joPane = new JOptionPane();
		    joPane.setMessage("Bidders can connect via "+ host +":"+ port);
		    joPane.setOptions(new Object[]{"Confirm"});
		    JDialog ipPrompt = joPane.createDialog("IP prompt");
		    ipPrompt.setAlwaysOnTop(false);
		    ipPrompt.setVisible(true);
		    ipPrompt.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

			//JADE gui - Only needed for debugging
		    //AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
		    //rma.start();
		    
		} catch(StaleProxyException e) {
		    e.printStackTrace();
		}
        
    }
}