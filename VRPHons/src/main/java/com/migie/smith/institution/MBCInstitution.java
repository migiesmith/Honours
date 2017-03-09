package com.migie.smith.institution;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import agent.auctionSolution.JourneyInfoHelper;
import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.VisitData;
import agent.auctionSolution.dataObjects.carShare.CarShare;
import agent.auctionSolution.ontologies.GiveObjectPredicate;
import agent.auctionSolution.ontologies.GiveOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MBCInstitution extends Agent {

	// The behaviour of the insitution
	InstitutionBehaviour behaviour;
	
	// List containing all visits
	List<VisitData> visits;
	// List containing only available visits
	List<VisitData> availableVisits;
	// Cositng values for each visit
	List<Double> costings;
	
	// The GUI for this institution
	MBCInstitutionGui gui;
	
	// Whether the institution should pause the rest of the agents
	boolean isPaused = true;
	
	/**
	 * Initialise essential variables
	 */
	protected void setup(){
		// Set the behaviour
		this.behaviour = new InstitutionBehaviour();
		this.addBehaviour(this.behaviour);
		
		// Register the required ontologies
		this.getContentManager().registerLanguage(new SLCodec());
		this.getContentManager().registerOntology(GiveOntology.getInstance());
		
		// Register Agent with the DF Service so that the Auctioneer can contact it
		this.registerWithDF();
		
		// Start the GUI
		this.gui = new MBCInstitutionGui(this);
	}
	
	/**
	 * @param isPaused True to pause other agents, false to unpause
	 */
	public void setPaused(boolean isPaused){
		this.isPaused = isPaused;
	}
	
	/**
	 * @return Reference to list of all visits
	 */
	public List<VisitData> getVisits(){
		return visits;
	}
	
	/**
	 * @return References to list of available visits
	 */
	public List<VisitData> getAvailableVisits(){
		return availableVisits;
	}
	
	/**
	 * @return Reference to list of costing information
	 */
	public List<Double> getCostingInformation(){
		return costings;
	}
	
	/**
	 * Registers the institution with the JADE DF Service (allows for agents to look it up)
	 */
	protected void registerWithDF() {
		// Register this Agent with the DF Service
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("institution");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	/**
	 * The behaviour that controls the running of the institution
	 * @author Grant Smith
	 */
	@SuppressWarnings("serial")
	public class InstitutionBehaviour extends Behaviour{
		
		// Determines if the agent should be shutdown
		boolean isDone = false;
		
		// Templates used to process incoming messages
		MessageTemplate receiveVisits, visitUpdate, costingRequest;
		
		// Used to get visit coordinates
		public JourneyInfoHelper journeyInfo;
		
		// If costings should start randomly assigned (can be set by incoming arguments)
		public boolean randomCostings = false;
		
		/**
		 * Default constructor (initialises message templates)
		 */
		InstitutionBehaviour(){
			receiveVisits = MessageTemplate.MatchConversationId("all-visits");
			visitUpdate = MessageTemplate.MatchConversationId("update-visits");
			costingRequest = MessageTemplate.MatchConversationId("costing-request");
			
			journeyInfo = new JourneyInfoHelper();
			
		}
		
		/**
		 * Overrides setAgent to update randomCostings using arguments
		 */
		@Override
		public void setAgent(Agent a){
			super.setAgent(a);
			
			Object[] args = this.myAgent.getArguments();
			if(args != null && args.length > 0 && args[0] instanceof Boolean){
				randomCostings = (Boolean) args[0];
			}
		}
		
		/**
		 * Called when visits are initially received
		 * @param visitMsg The message to extract the visits from
		 */
		protected void receiveVisits(ACLMessage visitMsg){
			try {
				ContentElement d = myAgent.getContentManager().extractContent(visitMsg);
				if (d instanceof GiveObjectPredicate) {
					// Read out the visit data to our visits list
					visits = (List<VisitData>) ((GiveObjectPredicate) d).getData();
					// Set availableVisits as these are currently equal
					availableVisits = visits;
					// Get the coordinates of the visits
					getXYCoords(visits);
					// Reset the costing values
					resetCostings();
				}
			} catch (UngroundedException e) {
				e.printStackTrace();
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Set every costing value back to 1.0d or a random value (between 0 and 2) if randomCostings is true
		 */
		protected void resetCostings(){
			if(visits != null){
				costings = new ArrayList<Double>();
				Random r = new Random();
				for(int i = 0; i < visits.size(); i++){
					if(randomCostings){
						costings.add(Math.round((r.nextDouble() < 0.1d ? r.nextDouble() * 2.0d : 1.0d) * 100) / 100.0d);
					}else{
						costings.add(1.0d);
					}
				}
			}
		}
		
		/**
		 * Updates the list of available visits using a message
		 * @param visitMsg Message to extract the available visits from
		 */
		protected void updateAvailableVisits(ACLMessage visitMsg){
			try {
				ContentElement d = myAgent.getContentManager().extractContent(visitMsg);
				if (d instanceof GiveObjectPredicate) {
					// Read out the visit data to our available visits list
					availableVisits = (List<VisitData>) ((GiveObjectPredicate) d).getData();
					// Get the coordinates of the visits
					getXYCoords(visits);
				}
			} catch (UngroundedException e) {
				e.printStackTrace();
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Responds to a request with the information regarding the costings of all visits
		 * @param msg Creates a response to this message
		 */
		protected void handlCostingRequest(ACLMessage msg){
			while(isPaused){
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			ACLMessage response = msg.createReply();
			response.setConversationId("costing-response");
			
			// Add the costing array to the response
			response.setLanguage(new SLCodec().getName());
			response.setOntology(GiveOntology.getInstance().getName());
			try {
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(costings);
				myAgent.getContentManager().fillContent(response, give);
				myAgent.send(response);					
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Gets the x and y coordinates of each visit for rendering
		 * @param visits List of visits to update with coordinates
		 */
		private void getXYCoords(List<VisitData> visits){
			// For every visit, ask the DataStore for their x and y location
			Point2D.Double visitCoords;
			for(VisitData vd : visits){
				// Send message for coordinates
				if(vd.x == 0 || vd.y == 0){
					visitCoords = requestCoordinates(vd.location);
					vd.setX(visitCoords.x);
					vd.setY(visitCoords.y);
					if(vd.transport != null && vd.transport.equals("Car Share")){
						for(VisitData v : ((CarShare)vd).visits){
							visitCoords = requestCoordinates(v.location);
							v.setX(visitCoords.x);
							v.setY(visitCoords.y);
						}
					}
				}
			}
		}

		// Returns the x and y coordinates of a visit
		private Point2D.Double requestCoordinates(String location) {
			// Send message for coordinates for the depot
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setConversationId("visit-coordinates");
			msg.addReceiver(journeyInfo.getMapServerAID(myAgent));
			msg.setContent(location);
			send(msg);
			// Wait for a response
			ACLMessage response = blockingReceive(MessageTemplate.MatchConversationId("visit-coordinates"));
			String[] content = response.getContent().split(",");
			// Store the coordinates
			return new Point2D.Double(Double.parseDouble(content[0]), Double.parseDouble(content[1]));
		}
		
		
		// Handle the process at each step of its execution
		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			if(msg != null){
				if(receiveVisits.match(msg)){
					receiveVisits(msg);
					if(gui != null){
						// Update the visit data list
						gui.updateSelections();
						gui.repaint();
					}
					
				}else if(visitUpdate.match(msg)){
					updateAvailableVisits(msg);
					if(gui != null){
						// Update the visit data list
						gui.updateSelections();
						gui.repaint();
					}
					
				}else if(costingRequest.match(msg)){
					handlCostingRequest(msg);
					
				}else{
					System.out.println("Unhandled message - " + msg.getConversationId());
				}
			}
		}

		@Override
		public boolean done() {
			return isDone;
		}
		
	}
	
}
