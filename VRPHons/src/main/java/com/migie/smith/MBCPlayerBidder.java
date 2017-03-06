package com.migie.smith;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import com.migie.smith.TimingRepresentation.TimeType;
import com.migie.smith.gui.MBCPlayerBidderGui;

import agent.auctionSolution.Bidder;
import agent.auctionSolution.JourneyInfoHelper;
import agent.auctionSolution.bidder.BidderBehaviour;
import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.VisitData;
import agent.auctionSolution.dataObjects.carShare.CarShare;
import agent.auctionSolution.dataObjects.carShare.CarShareRequest;
import agent.auctionSolution.dataObjects.carShare.CarShareResult;
import agent.auctionSolution.ontologies.GiveObjectPredicate;
import agent.auctionSolution.ontologies.GiveOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.schema.ConceptSchema;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * MBCPlayerBidder extends the Bidder class to allow for user interaction
 * with the system. Doing so allows the user to compete with the bidders
 * (which can include other players) in attempt to improve the overall
 * solution.
 * @author Grant Smith
 */
public class MBCPlayerBidder extends Bidder {

	// The gui for this bidder
	protected MBCPlayerBidderGui gui;
	// The behaviour of the bidder
	protected MBCBidderBehaviour behaviour;
	// List of all visits
	protected List<VisitData> allVisits = null;
	// List of all visits yet to be bidded on
	protected List<VisitData> availableVisits = null;

	// List of costing multipliers for every visit
	protected List<Double> costingInfo;
	
	// The log used to document bidder's bids and strategy
	protected Log bidLog;
	
	// The balance of every bidder (used for the ranking system)
	protected List<MBCAccountant> bidderBalances = null;
	
	// The Accountant for this Bidder Agent
	protected MBCAccountant accountant = new MBCAccountant();
	
	// The max bid allowed for the current visit
	protected double maxBidForVisit = 0.0d;
	
	@Override
	protected void setup(){
		// Start the bidder's behaviour
		behaviour = new MBCBidderBehaviour();		
		addBehaviour(behaviour);
		
		// Start the bidder's GUI
		gui = new MBCPlayerBidderGui(this);
		
		// Initialise the log
		bidLog = new Log();
		bidLog.log("bid, cost, maxbid,");
		
		// Register Agent with the DF Service so that the Auctioneer can contact it
		this.registerWithDF();
	}

	protected void registerWithDF() {
		// Register this Agent with the DF Service
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("bidder");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	@Override
	protected void takeDown() {
		try {
			// Remove from DF Service
			DFService.deregister(this);
		} catch (Exception e) {}
	}
	
	/**
	 * Updates the behaviour with the next move
	 * @param nextMove The move passed to the behaviour
	 */
	public void makeMove(MBCBidderMove nextMove){
		behaviour.moveMade = true;
		behaviour.nextMove = nextMove;
	}
	
	/**
	 * @return The depot of the current problem
	 */
	public Depot getDepot(){
		return behaviour.depot;
	}
	
	/**
	 * Returns the cost for adding a visit at a certain position
	 * @param v Visit to be added
	 * @param position Index to be inserted
	 * @return Cost of adding v at position
	 */
	public double getCost(VisitData v, int position){
		return behaviour.costForAddingAt(v, position);
	}
	
	/**
	 * @param v The visit to get the max bid for
	 * @return The max bid for v
	 */
	public double getMaxBid(VisitData v){
		/*
		 * This is set per turn by the auctioneer
		 * so passing in v in this instance means
		 * nothing but it is left in for any
		 * extending classes.
		*/
		return maxBidForVisit;
	}
	
	/**
	 * @return The current balances of all bidders
	 */
	public List<MBCAccountant> getBalances(){
		return this.bidderBalances;
	}
	
	/**
	 * @return The balance of this bidder
	 */
	public double getBalance(){
		return this.accountant.getBalance();
	}
	
	/**
	 * The core logic of MBCPlayerBidder. Handles the entire bidding process.
	 * @author Grant
	 */
	private class MBCBidderBehaviour extends BidderBehaviour{
		
		// The location to insert the visit if it is won
		private int addAt = -1;
		// Determines if a move has been made by the player
		private boolean moveMade = false;
		// The move made by the player
		private MBCBidderMove nextMove;

		/**
		 * Registers the MBCGiveOntology and receives all visits from the auctioneer
		 */
		@Override
		protected boolean initialise() {
			// Register MBCGiveOntology
			myAgent.getContentManager().registerOntology(MBCGiveOntology.getInstance());
			
			setJourneyInfoHelper(new JourneyInfoHelper());
			receiveAvailableVisits();
			return true;
		}
		
		/**
		 * Check if there is a message from the auctioneer containing 
		 * the conversation id "available-visits". If there is the update 
		 * the all visits list or the available visits list. Also receives 
		 * costing information updates.
		 */
		protected void receiveAvailableVisits(){
			// Receive an available visits message
			ACLMessage allVisitsMsg = myAgent.receive(MessageTemplate.MatchConversationId("available-visits"));
			// Check if there is a message to receive
			if(allVisitsMsg != null){
				try {
					ContentElement d = myAgent.getContentManager().extractContent(allVisitsMsg);
					if (d instanceof GiveObjectPredicate) {
						if(allVisits == null){
							// Set all visits and update the list with valid coordinates
							allVisits = (List<VisitData>) ((GiveObjectPredicate) d).getData();
							getXYCoords(allVisits);
							gui.setAllVisits(allVisits);
						}else{
							// Set the available visits list and update the list with valid coordinates
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
			
			// Get a list of costing values for each visit
			ACLMessage costingUpdate = myAgent.receive(MessageTemplate.MatchConversationId("costing-update"));
			// Check if there was a message to receive
			if(costingUpdate != null){
				try {
					ContentElement d = myAgent.getContentManager().extractContent(costingUpdate);
					if (d instanceof GiveObjectPredicate) {
						costingInfo = (List<Double>) ((GiveObjectPredicate) d).getData();
						gui.setCostingInfo(costingInfo);
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
		
		/**
		 * Waits for a message from the auctioneer containing the value 
		 * of the max bid for the next item to be bidded on.
		 */
		protected void getMaxBid(){
			ACLMessage allVisitsMsg = myAgent.blockingReceive(MessageTemplate.MatchConversationId("visit-max-bid"));
			// Check if there was a message to receive
			if(allVisitsMsg != null){
				try {
					ContentElement d = myAgent.getContentManager().extractContent(allVisitsMsg);
					if (d instanceof GiveObjectPredicate) {
						// Update the max bid
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
		
		/**
		 * Start the player's turn, updating relevant information including 
		 * max bid, available visits, and gui information.
		 * @param v
		 */
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

			// Update the balance of every bidder
			ACLMessage balanceInform = myAgent.blockingReceive(MessageTemplate.MatchConversationId("all-balance-inform"));
			try {
				ContentElement d = myAgent.getContentManager().extractContent(balanceInform);
				if (d instanceof GiveObjectPredicate) {
					// Read out the visit data to our visits list
					bidderBalances = (List<MBCAccountant>) ((GiveObjectPredicate) d).getData();
				}
			} catch (UngroundedException e) {
				e.printStackTrace();
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
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

		/**
		 * Update the x and y coordinates of the visits passed in
		 * and the depot.
		 * @param visits
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
			if(depot != null){
				visitCoords = requestCoordinates(depot.location);
				depot.setX(visitCoords.x);
				depot.setY(visitCoords.y);
			}
		}
		
		/**
		 * @param location String identifier of the location (from a visit)
		 * @return The x and y coordinates of a location
		 */
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
			double bid = nextMove.bid;
			// Set the insert location
			addAt = nextMove.insertLocation;

			// If the player did not reject the visit
			if(nextMove.acceptVisit){
				// Update the log
				bidLog.log(nextMove.bid +","+ costForAddingAt(v, nextMove.insertLocation) +","+ maxBidForVisit);
			}
			
			// Return the bid (or reject bid). This value is processed by the parent clas
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
				
				return Math.abs((cost0 + cost1) - currentCost);
				
			}else{
				double cost0 = getJourneyCost(position-1 < 0 ? depot.location : route.visits.get(position-1).location, v.location, minimiseFactor, v.transport);
				double cost1 = getJourneyCost(v.location, route.visits.get(position).location, minimiseFactor, v.transport);
				double currentCost = getJourneyCost(position-1 < 0 ? depot.location : route.visits.get(position-1).location, route.visits.get(position).location, minimiseFactor, v.transport);
				
				return Math.abs((cost0 + cost1) - currentCost);
			}
		}		
		
		@Override
		public boolean canAllocateToRoute(VisitData v) {
			// Start the best cost really high
			double bestCost = Double.MAX_VALUE;
			// Set the add at location to show that it is currently invalid
			addAt = -1;
			int i = 0;
			// Loop through each visit in the route and determine which insert location is the best
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
			// Store the current balance to determine if a profit/loss was made
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
				accountant.updateBalance((nextMove.bid  - costForAddingAt(v, addAt)) * costingInfo.get(MBCHelper.visitPosInList(allVisits, v)));
				
				route.visits.add(addAt + 1, v);
				System.out.println("CARSHARE");
			}else{
				// Determine the transport mode and add the visit
				v.transport = determineTransportMode(v, addAt);
				
				// Update the accountant
				accountant.updateBalance((nextMove.bid - Math.abs(costForAddingAt(v, addAt))) * costingInfo.get(MBCHelper.visitPosInList(allVisits, v)));
				
				route.visits.add(addAt, v);
			}			

			// Inform the auctioneer of our new balance
			ACLMessage balanceInform = new ACLMessage(ACLMessage.INFORM);
			balanceInform.setConversationId("balance-inform");
			balanceInform.addReceiver(this.auctioneerAID);
			balanceInform.setContent(String.valueOf(accountant.getBalance()));
			myAgent.send(balanceInform);
			
			if(gui != null){
				// Inform of won bid and show gain / loss
				double diff = Math.abs(oldBalance - accountant.getBalance());
				gui.showMessage("Won: "+ v.name +" with bid of "+ (Math.round(nextMove.bid * 10)/10.0) +" ("+ (Math.round(diff * 10)/10.0) + (diff < 0 ? " Loss" : " Profit") +")\n");
			}
			
		}
		
		@Override
		public void resetBidder(){
			super.resetBidder();
			
			// Clear the lists used to update the GUI
			allVisits = null;
			availableVisits = null;

			// Reset the costing info
			costingInfo = null;
			
			// Create a new log
			bidLog = new Log();
			bidLog.log("bid, cost, maxbid,");

			// Reset the accountant
			accountant = new MBCAccountant(); // TODO maybe leave this out to leave a running total so that players can compare overall score

			// Return maxBidForVisit to its default value
			maxBidForVisit = 0.0d;

			// Return addAt to its default value
			addAt = -1;
			// Return moveMade to its default value
			moveMade = false;
			// Clear the nextMove
			nextMove = null;
		}
		
		@Override
		public boolean finishUp(){
			if(gui != null){
				gui.showMessage("Bidding Complete.\n");
				gui.setGameState("Auction Closed.");
			}
			
			// Ask user for their strategy
			String userStrategy = JOptionPane.showInputDialog("What was your strategy? (Leave blank if there wasn't one).");
			// Add to the log
			bidLog.log("Strategy");
			bidLog.log(userStrategy != null ? userStrategy : "");
			
			// Send log to the Auctioneer
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setConversationId("bidder-log");
			msg.addReceiver(this.auctioneerAID);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontologyGive.getName());
			try {
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(bidLog.getLog());
				myAgent.getContentManager().fillContent(msg, give);
				myAgent.send(msg);
				System.out.println("Sent my log.");
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
			
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
