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
    	
    	String host = MBCHelper.getIP();
    	String port = "1099";
    	
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

	    	System.out.println(host +":"+ port);
	    	
		    JOptionPane joPane = new JOptionPane();
		    joPane.setMessage("Bidders can connect via "+ host +":"+ port);
		    joPane.setOptions(new Object[]{"Confirm"});
		    JDialog ipPrompt = joPane.createDialog("IP prompt");
		    ipPrompt.setAlwaysOnTop(false);
		    ipPrompt.setVisible(true);
		    ipPrompt.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		    
		} catch(StaleProxyException e) {
		    e.printStackTrace();
		}
        
    }
}