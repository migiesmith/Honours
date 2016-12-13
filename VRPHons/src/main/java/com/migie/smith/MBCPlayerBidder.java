package com.migie.smith;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.migie.smith.TimingRepresentation.TimeType;
import com.migie.smith.gui.MBCPlayerBidderGui;

import agent.auctionSolution.Bidder;
import agent.auctionSolution.JourneyInfoHelper;
import agent.auctionSolution.bidder.BidderBehaviour;
import agent.auctionSolution.dataObjects.CostVariables;
import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.VisitData;
import agent.auctionSolution.dataObjects.carShare.CarShare;
import agent.auctionSolution.dataObjects.carShare.CarShareRequest;
import agent.auctionSolution.dataObjects.carShare.CarShareResult;
import agent.auctionSolution.ontologies.GiveObjectPredicate;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MBCPlayerBidder extends Bidder {

	// The gui for this bidder
	protected MBCPlayerBidderGui gui;
	protected MBCBidderBehaviour behaviour;
	protected List<VisitData> allVisits = null;
	protected List<VisitData> availableVisits = null;

	// The Accountant for this Bidder Agent
	protected MBCAccountant accountant = new MBCAccountant();
	
	// The max bid allowed for the current visit
	protected double maxBidForVisit = 0.0d;
	
	@Override
	protected void setup(){
		gui = new MBCPlayerBidderGui(this);
		behaviour = new MBCBidderBehaviour();
		addBehaviour(behaviour);
	}

	public void makeMove(MBCBidderMove nextMove){
		behaviour.moveMade = true;
		behaviour.nextMove = nextMove;
	}
	
	public Depot getDepot(){
		return behaviour.depot;
	}
	
	public double getCost(VisitData v, int position){
		return behaviour.costForAddingAt(v, position);
	}
	
	public double getMaxBid(VisitData v){
		return maxBidForVisit;
	}
	
	
	private class MBCBidderBehaviour extends BidderBehaviour{

		private int addAt = -1;
		private boolean moveMade = false;
		private MBCBidderMove nextMove;

		@Override
		protected boolean initialise() {
			setJourneyInfoHelper(new JourneyInfoHelper());
			receiveAvailableVisits();
			return true;
		}
		
		protected void receiveAvailableVisits(){
			ACLMessage allVisitsMsg = myAgent.receive(MessageTemplate.MatchConversationId("available-visits"));
			if(allVisitsMsg != null){
				try {
					ContentElement d = myAgent.getContentManager().extractContent(allVisitsMsg);
					if (d instanceof GiveObjectPredicate) {
						if(allVisits == null){
							allVisits = (List<VisitData>) ((GiveObjectPredicate) d).getData();
							getXYCoords(allVisits);
							gui.setAllVisits(allVisits);
						}else{
							availableVisits = (List<VisitData>) ((GiveObjectPredicate) d).getData();
							getXYCoords(availableVisits);
							gui.setAvailableVisits(availableVisits);
						}
					}
				} catch (UngroundedException e) {
					e.printStackTrace();
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
			}
		}
		
		protected void getMaxBid(){
			ACLMessage allVisitsMsg = myAgent.blockingReceive(MessageTemplate.MatchConversationId("visit-max-bid"));
			if(allVisitsMsg != null){
				try {
					ContentElement d = myAgent.getContentManager().extractContent(allVisitsMsg);
					if (d instanceof GiveObjectPredicate) {
						maxBidForVisit = (Double) ((GiveObjectPredicate) d).getData();
					}
				} catch (UngroundedException e) {
					e.printStackTrace();
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
			}
		}
		
		protected void startTurn(VisitData v){
			// Get the max bid
			getMaxBid();
			// Receive updated list of visits (showing available and unavailable)
			receiveAvailableVisits();
			
			// Create a list of positions that the new visit can be added at
			List<Integer> possibleLocations = new ArrayList<Integer>();
			for(int i = 0; i <= route.visits.size(); i++){
				if(canAddAt(v, i)){
					possibleLocations.add(i);
				}
			}

			// Update the gui with the balance
			if(gui != null){
				gui.setBalance(accountant.getBalance());			
				// Get render coordinates for the visits
				getXYCoords(route.visits);
				
				// Get render coordinates for the new visit
				Point2D.Double visitCoords = requestCoordinates(v.location);
				v.setX(visitCoords.x);
				v.setY(visitCoords.y);
	
				// Create a list of timings for rendering
				List<TimingRepresentation> times = new ArrayList<TimingRepresentation>();
				if(route.visits.size() > 0){
					Calendar cal = Calendar.getInstance();
					cal.setTime(depot.commenceTime);
					int initTravelTime = (int) journeyInfo.getTravelTime(myAgent, depot.location, route.visits.get(0).location, route.visits.get(0).transport);
					times.add(new TimingRepresentation(initTravelTime, TimeType.Travel));
					cal.add(Calendar.MINUTE, initTravelTime);
					for(int i = 0; i < route.visits.size(); i++){
						VisitData vd = route.visits.get(i);
						if (cal.getTime().before(vd.windowStart)) {
							System.out.println(depot.commenceTime.getTime() +", "+ cal.getTime().getTime());
							if(i == 0){
								cal.setTime(route.visits.get(0).windowStart);
							    long diffInMillies = cal.getTime().getTime() - depot.commenceTime.getTime();
							    int timeDiff = (int) TimeUnit.MINUTES.convert(diffInMillies,TimeUnit.MINUTES);
								times.add(new TimingRepresentation(timeDiff, TimeType.Empty));
							}
						}
						
						cal.add(Calendar.MINUTE, (int) vd.visitLength);
						times.add(new TimingRepresentation((int)vd.visitLength, TimeType.Visit));
						
						if(vd instanceof CarShare){
							int travelTime = (int) journeyInfo.getTravelTime(myAgent, ((CarShare) vd).endLocation, (i+1 == route.visits.size() ? depot.location : route.visits.get(i+1).location), vd.transport);	
							cal.add(Calendar.MINUTE, travelTime);	
							times.add(new TimingRepresentation(travelTime, TimeType.Travel));
						}else{
							int travelTime = (int) journeyInfo.getTravelTime(myAgent, vd.location, (i+1 == route.visits.size() ? depot.location : route.visits.get(i+1).location), vd.transport);
							cal.add(Calendar.MINUTE, travelTime);
							times.add(new TimingRepresentation(travelTime, TimeType.Travel));
						}
						
						if(i == route.visits.size() - 1){
							Calendar endTime = Calendar.getInstance();
							endTime.setTime(cal.getTime());
							endTime.set(Calendar.MINUTE, 0);
							endTime.set(Calendar.HOUR_OF_DAY, 17);
						    long diffInMillies = endTime.getTime().getTime() - cal.getTime().getTime();
						    int timeDiff = (int) TimeUnit.MINUTES.convert(diffInMillies,TimeUnit.MILLISECONDS);
						    if(timeDiff > 0){
						    	times.add(new TimingRepresentation(timeDiff, TimeType.Empty));
						    }
						}
					}
				}
				
				// Update the GUI for this turn
				if(gui != null){
					gui.renderTurn(route.visits, possibleLocations, v, times);
					gui.setGameState("Bidding for " + v.name);
				}

			}
			moveMade = false;
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
			if(depot != null){
				visitCoords = requestCoordinates(depot.location);
				depot.setX(visitCoords.x);
				depot.setY(visitCoords.y);
			}
		}
		
		// Returns the x and y coordinates of a visit
		private Point2D.Double requestCoordinates(String location) {
			// Send message for coordinates for the depot
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setConversationId("visit-coordinates");
			msg.addReceiver(new AID("Map-Server", AID.ISLOCALNAME));
			msg.setContent(location);
			send(msg);
			// Wait for a response
			ACLMessage response = blockingReceive(MessageTemplate.MatchConversationId("visit-coordinates"));
			String[] content = response.getContent().split(",");
			// Store the coordinates
			return new Point2D.Double(Double.parseDouble(content[0]), Double.parseDouble(content[1]));
		}
		
		@Override
		public double getBid(VisitData v) {			
			startTurn(v);
			
			// Wait for the player to make a move
			while(!moveMade){
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Store the value of the bid
			double bid = nextMove.bid;//costForAddingAt(v, addAt);			
			addAt = nextMove.insertLocation;

			return (nextMove.acceptVisit ? bid : Double.MIN_VALUE);
		}

		/**
		 * Send a request to the CarShareServer for a CarShare
		 * @param v
		 * @param pos
		 * @return
		 */
		private double getCarShareBid(VisitData v, int pos){
			this.carShare = null;
			
			// Create the request
			CarShareRequest request = new CarShareRequest();
			request.from = (pos-1 <= 0) ? depot.location : route.visits.get(pos-1).location;
			request.to = v.location;
			request.transportMode = v.transport;
			
			if(pos-1 > 0 && route.visits.get(pos-1) instanceof CarShare)
				return Double.MIN_VALUE;
			
			request.route = route;

			// Send the request
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(new AID("car-share-server", AID.ISLOCALNAME));
			msg.setPerformative(ACLMessage.PROPOSE);
			msg.setConversationId("car-share-request");
			msg.setLanguage(codec.getName());
			msg.setOntology(ontologyGive.getName());
			try {
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(request);
				myAgent.getContentManager().fillContent(msg, give);
				myAgent.send(msg);
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
			
			// Wait for the response
			ACLMessage response = myAgent.blockingReceive(MessageTemplate.MatchConversationId("car-share-proposal"));
			carShare = response;
			CarShareResult result = getCarShareResults();
			
			// Determine the cost of adding the CarShare if there is one
			if(result != null){
				double currentCost = getJourneyCost(pos-1 < 0 ? depot.location : route.visits.get(pos-1).location, (pos == route.visits.size() ? depot.location : route.visits.get(pos).location), minimiseFactor, v.transport);
				double cost = (minimiseFactor.equals("Emissions") ? 0 : (costVars.staffCostPerHour * (result.carShare.visitLength / 60.0d)));
				cost += getJourneyCost((pos-1 < 0) ? depot.location : route.visits.get(pos-1).location, result.carShare.location, minimiseFactor, v.transport);
				cost += getJourneyCost(result.carShare.endLocation, v.location, minimiseFactor, "Public Transport");
				
				return (currentCost - (cost) + (minimiseFactor.equals("Emissions") ? 0 : (costVars.staffCostPerHour * (v.visitLength / 60.0d))) + (result.carShare.visitLength/60.0d));
			}else{
				carShare = null;
				return Double.MIN_VALUE;
			}			
		}
		
		/**
		 * @param v The visit being added
		 * @param position The position in the route to add it
		 * @return The cost associated with adding v at position in route.visits
		 */
		@Override
		public double costForAddingAt(VisitData v, int position){
			if(route.visits.size() == 0){
				return getJourneyCost(depot.location, v.location, minimiseFactor, v.transport) + getJourneyCost(v.location, depot.location, minimiseFactor, v.transport);
				
			}else if(position == route.visits.size()){
				double cost0 = getJourneyCost(route.visits.get(position-1).location, v.location, minimiseFactor, v.transport);
				double cost1 = getJourneyCost(v.location, depot.location, minimiseFactor, v.transport);
				double currentCost = getJourneyCost(route.visits.get(position-1).location, depot.location, minimiseFactor, v.transport);
				
				return -((cost0 + cost1) - currentCost);
				
			}else{
				double cost0 = getJourneyCost(position-1 < 0 ? depot.location : route.visits.get(position-1).location, v.location, minimiseFactor, v.transport);
				double cost1 = getJourneyCost(v.location, route.visits.get(position).location, minimiseFactor, v.transport);
				double currentCost = getJourneyCost(position-1 < 0 ? depot.location : route.visits.get(position-1).location, route.visits.get(position).location, minimiseFactor, v.transport);
				
				return -((cost0 + cost1) - currentCost);
			}
		}		
		
		@Override
		public boolean canAllocateToRoute(VisitData v) {
			double bestCost = Double.MAX_VALUE;
			addAt = -1;
			int i = 0;
			do{
				v.transport = determineTransportMode(v, i);
				if(canAddAt(v, i)){
					double cost = costForAddingAt(v, i);
					if(addAt == -1 || -cost < bestCost){
						addAt = i;
						bestCost = -cost;
					}
				}
				i++;
			}while(i <= route.visits.size());
			
			// If CarSharing is enabled then check if it is possible to add
			if(isCarShareEnabled){
				return (addAt != -1 && canChangeWithoutBreakingCarShare(addAt));
			}else{
				return addAt != -1;
			}
		}

		@Override
		public void handleWonBid(VisitData v) {
			double oldBalance = accountant.getBalance();
			
			if(carShare != null){
				// Inform the CarShareServer that you accept the CarShare
				CarShareResult result = getCarShareResults();
				ACLMessage accept = carShare.createReply();
				accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				accept.setLanguage(codec.getName());
				accept.setOntology(ontologyGive.getName());
				try {
					GiveObjectPredicate give = new GiveObjectPredicate();
					give.setData(result);
					myAgent.getContentManager().fillContent(accept, give);
					send(accept);

				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
				// Add the CarShare and the visit won
				result.carShare.transport = determineTransportMode(result.carShare, addAt);
				carShare = null;
				route.visits.add(addAt, result.carShare);
				v.transport = "Public Transport";
				// Update the accountant
				accountant.updateBalance(nextMove.bid - costForAddingAt(v, addAt));
				route.visits.add(addAt + 1, v);
				System.out.println("CARSHARE");
			}else{
				// Determine the transport mode and add the visit
				v.transport = determineTransportMode(v, addAt);
				// Update the accountant
				accountant.updateBalance(nextMove.bid - costForAddingAt(v, addAt));
				route.visits.add(addAt, v);
			}			

			if(gui != null){
				// Inform of won bid and show gain / loss
				double diff = Math.abs(oldBalance - accountant.getBalance());
				gui.showMessage("Won: "+ v.name +" with bid of "+ (Math.round(nextMove.bid * 10)/10.0) +" ("+ (Math.round(diff * 10)/10.0) + (diff < 0 ? " Loss" : " Profit") +")\n");
			}
			
		}

		@Override
		public boolean finishUp(){
			if(gui != null){
				gui.showMessage("Bidding Complete.");
				gui.setGameState("Auction Closed.");
			}
			doDelete();
			return true;
		}

		@Override
		public VisitData getVisitToReturn(VisitData visitData) {
			if (returnsLeft > 0) {
				double deltaEm = 0.0d;
				double deltaTime = 0.0d;
				VisitData bestVisit = null;
				
				// Store the current emission and time cost
				double currentEmissions = calcTotalEmissions();
				double currentTime = calcTotalTime();
				
				// Loop through every visit and check if an improvement can
				// be made by returning a visit and replacing it with visitData
				for(int i = 0; i < route.visits.size(); i++){
					if(route.visits.get(i) instanceof CarShare || route.visits.get(i).sharingWith.size() > 0 || !canChangeWithoutBreakingCarShare(i))
						continue;
					
					VisitData v = route.visits.remove(i);

					if(canAddAt(visitData, i)){
						route.visits.add(i, visitData);
						double newEmissions = calcTotalEmissions();
						double newTime = calcTotalTime();
						route.visits.remove(visitData);
						
						if(currentEmissions - newEmissions >= deltaEm && currentTime - newTime >= deltaTime){
							deltaEm = currentEmissions - newEmissions;
							deltaTime = currentTime - newTime;
							bestVisit = v;
						}
					}
					
					route.visits.add(i, v);
				}
				
				// If we have a valid visit to return then return it
				int index = route.visits.indexOf(bestVisit);
				if(!isCarShareEnabled || (index != -1 && canChangeWithoutBreakingCarShare(index))){
					return bestVisit;
				}else{
					return null;
				}
				
			}
			return null;
		}
		
	
	}
}
