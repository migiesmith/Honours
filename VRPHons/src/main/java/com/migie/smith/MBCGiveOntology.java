package com.migie.smith;

import agent.auctionSolution.ontologies.GiveOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

/**
 * Extends GiveOntology to allow for the encoding and 
 * decoding of MBCAccountant.
 * 
 * @see GiveOntology
 * @author Grant
 */
@SuppressWarnings("serial")
public class MBCGiveOntology extends GiveOntology{

	
	// Name of the ontology
	public static final String ONTOLOGY_NAME = "MBC-Give-Ontology";
	
	// The singleton instance of this ontology
	private static Ontology theInstance = new MBCGiveOntology();

	/**
	 * @return The instance of the ontology
	 */
	public static Ontology getInstance() {
		return theInstance;
	}
	
	/**
	 * Calls the parent class to initialise and then adds 
	 * MBCAccountant to the ontology
	 */
	public MBCGiveOntology(){
		super();
		
		try {
			add(MBCAccountant.class);
		} catch (BeanOntologyException oe) {
			oe.printStackTrace();
		}
		
	}
	
}