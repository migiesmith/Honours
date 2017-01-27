package com.migie.smith.institution;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import agent.auctionSolution.JourneyInfoHelper;
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

	InstitutionBehaviour behaviour;
	
	List<VisitData> visits;
	List<VisitData> availableVisits;
	List<Double> costings;
	
	
	protected void setup(){
		this.behaviour = new InstitutionBehaviour();
		this.addBehaviour(this.behaviour);
		
		this.getContentManager().registerLanguage(new SLCodec());
		this.getContentManager().registerOntology(GiveOntology.getInstance());
		
		// Register Agent with the DF Service so that the Auctioneer can contact it
		this.registerWithDF();
	}
	
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
	
	
	public class InstitutionBehaviour extends Behaviour{

		boolean isDone = false;
		
		MessageTemplate receiveVisits, visitUpdate, costingRequest;
		
		// Used to get visit coordinates
		public JourneyInfoHelper journeyInfo;
		
		InstitutionBehaviour(){
			receiveVisits = MessageTemplate.MatchConversationId("all-visits");
			visitUpdate = MessageTemplate.MatchConversationId("update-visits");
			costingRequest = MessageTemplate.MatchConversationId("costing-request");
			
			journeyInfo = new JourneyInfoHelper();
		}
		
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
		
		protected void resetCostings(){
			if(visits != null){
				costings = new ArrayList<Double>();
				for(int i = 0; i < costings.size(); i++){
					costings.add(1.0d);
				}
			}
		}
		
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
		
		protected void handlCostingRequest(ACLMessage msg){
			ACLMessage response = msg.createReply();
			
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
					
				}else if(visitUpdate.match(msg)){
					updateAvailableVisits(msg);
					
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
