package com.migie.smith;

import agent.auctionSolution.ontologies.GiveOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class MBCGiveOntology extends GiveOntology{

	
	// Name of the ontology
	public static final String ONTOLOGY_NAME = "MBC-Give-Ontology";
		
	
	// The singleton instance of this ontology
	private static Ontology theInstance = new MBCGiveOntology();

	// This is the method to access the singleton music shop ontology object
	public static Ontology getInstance() {
		return theInstance;
	}
	
	public MBCGiveOntology(){
		super();
		
		try {
			add(MBCAccountant.class);
		} catch (BeanOntologyException oe) {
			oe.printStackTrace();
		}
		
	}
	
}