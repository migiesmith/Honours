package com.migie.smith;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import agent.auctionSolution.Bidder;
import agent.auctionSolution.JourneyInfoHelper;
import agent.auctionSolution.auction.AuctionBehaviour;
import agent.auctionSolution.auction.Auctioneer;
import agent.auctionSolution.dataObjects.CostVariables;
import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.JourneyData;
import agent.auctionSolution.dataObjects.Problem;
import agent.auctionSolution.dataObjects.Route;
import agent.auctionSolution.dataObjects.VisitData;
import agent.auctionSolution.ontologies.GiveObjectPredicate;
import agent.auctionSolution.ontologies.GiveOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BasicOntology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.schema.ConceptSchema;
import jade.content.schema.PrimitiveSchema;
import jade.content.schema.TermSchema;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MBCAuctioneer extends Auctioneer{

	String results = "";

	List<AID> mbcBidders = new ArrayList<AID>();
	
	AID institution;

	List<MBCAccountant> bidderBalances = null;
	
	@Override
	protected AuctionBehaviour getAuctionBehaviour() {
		if(auction == null && problem != null){
			auction = new Auction(problem.visits, bidders, problem.depot);
		}
		return auction;
	}
	
	@Override
	public void start() {
		// Return values to default
		auction = null;
		bidders = new ArrayList<AID>();
		renderOffset = new Rectangle2D.Double(0, 0, 0, 0);
		
		// Reset the list of MBCBidders
		mbcBidders = new ArrayList<AID>();
		
		// Find agents to use and create more if needed
		setupAgents(noBidders.get(currentProblem));

		getInstitution();
		
		// If there is already an auction behaviour, remove it
		if(auction != null)
			removeBehaviour(auction);
		// Add a new auction behaviour
		addBehaviour(getAuctionBehaviour());
		
	}

	/**
	 *  Search the DF Service for an institution agent
	 */
	protected void getInstitution(){
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("institution");
		dfd.addServices(sd);

		DFAgentDescription[] searchResult = null;
		try {
			searchResult = DFService.search(this, dfd);
			if(searchResult.length > 0)
				institution = searchResult[0].getName();						
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	
	// Find agents to use and create more if needed
	protected void setupAgents(int biddersNeeded) {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("bidder");
		dfd.addServices(sd);

		DFAgentDescription[] searchResult = null;
		try {
			searchResult = DFService.search(this, dfd);
			System.out.println(searchResult.length + " bidders available on DF");

			ACLMessage bidderArgs = new ACLMessage(ACLMessage.INFORM);
			bidderArgs.setConversationId("bidder-arguments");
			
			for(int i = 0; i < searchResult.length; i++){
				System.out.println(" |" + searchResult[i].getName().getLocalName());
				bidders.add(searchResult[i].getName());
				mbcBidders.add(searchResult[i].getName());
				bidderArgs.addReceiver(searchResult[i].getName());
			}
			// Return limit, minimiseFactor, transportMode, isCarShareEnabled
			bidderArgs.setContent(gui.getReturnableVisits() +","+ minimiseFactor +","+ transportMode +","+ gui.getAllowCarSharing());
			send(bidderArgs);
			
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		int biddersToCreate = biddersNeeded;
		if(searchResult != null){
			biddersToCreate -= searchResult.length;
		}
		// Create more agents if needed
		if(biddersToCreate > 0){
			createAgents(Bidder.class, biddersNeeded - biddersToCreate, biddersToCreate);
		}
		
		bidderBalances = new ArrayList<MBCAccountant>();
		for(AID bidder : bidders){
			MBCAccountant acc = new MBCAccountant();
			acc.setBalance(0.0d);
			acc.setBidder(bidder);
			bidderBalances.add(acc);
		}
		
	}
	
	@Override
	protected Problem getProblem() {
		return problem;
	}

	@Override
	public void loadFromFile() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(files.get(currentProblem)));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			br.close();
			String problemName = files.get(currentProblem);
			problemName = problemName.substring(problemName.lastIndexOf("\\")+1, problemName.lastIndexOf("."));
			problem = new Problem(problemName, sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public void loadFromFiles(List<String> files, List<Integer> noBidders) {
		this.files = files;
		this.noBidders = noBidders;
		currentProblem = 0;
		gui.setStatus(files.size() + (files.size() == 1 ? " problem" : " problems") +" ready to run.");
	}
	
	private void nextProblem(){
		currentProblem++;
		if(currentProblem < files.size()){
			loadFromFile();
			start();
		}
	}
	
	// TODO Update this as it is a copy of the original in 'MyAuctioneer'
	private class Auction extends AuctionBehaviour{

		Auction(List<VisitData> visits, List<AID> bidders, Depot depot) {
			super(visits, bidders, depot);
		}
				
		@Override
		protected boolean initialise() {
			// Register MBCGiveOntology
			myAgent.getContentManager().registerOntology(MBCGiveOntology.getInstance());
			
			gui.canLoadFile(false);
			sendAllVisitsToInstitution();
			
			sendAllVisits();
			return true;
		}

		@Override
		protected AID getHighestBidder(){
			sendAllVisits();
			return super.getHighestBidder();
		}
		
		@Override
		protected boolean auctionVisit(){	
			
			informBiddersOfBalances();
			
			// Call the super function, we only want to add to it
			boolean canAuctionVisit = super.auctionVisit();
			
			// If we can auction a visit, send the reward associated with the visit
			if(canAuctionVisit){
				// Create an inform message
				ACLMessage maxBidMsg = new ACLMessage(ACLMessage.INFORM);
				// Set the conversation id
				maxBidMsg.setConversationId("visit-max-bid");
				// Add all of the receivers
				for(AID bidder : bidders){
					maxBidMsg.addReceiver(bidder);
				}
				// Add the reward value to the message and send it
				maxBidMsg.setLanguage(new SLCodec().getName());
				maxBidMsg.setOntology(GiveOntology.getInstance().getName());
				try {
					GiveObjectPredicate give = new GiveObjectPredicate();
					give.setData(getMaxBid(getUpForAuction()));
					myAgent.getContentManager().fillContent(maxBidMsg, give);
					myAgent.send(maxBidMsg);					
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
			}
			
			return canAuctionVisit;
		}
		
		protected void informBiddersOfBalances(){
			// Check for winner messages
			ACLMessage winnerBalance = myAgent.receive(MessageTemplate.MatchConversationId("balance-inform"));
			while(winnerBalance != null){
				// Find the index of the winner
				int winnerIdx = 0;
				for(int idx = 1; idx < bidderBalances.size(); idx++){
					if(bidderBalances.get(idx).bidder.equals(winnerBalance.getSender())){
						winnerIdx = idx;
						break;
					}
				}
				// Update the winners balance
				bidderBalances.get(winnerIdx).setBalance(Double.valueOf(winnerBalance.getContent()));
				// Check for another message
				winnerBalance = myAgent.receive(MessageTemplate.MatchConversationId("balance-inform"));
			}
			
			// Send a message to all bidders about the balance of each bidder
			ACLMessage informBalanceAll = new ACLMessage(ACLMessage.INFORM);
			informBalanceAll.setLanguage(new SLCodec().getName());		
			informBalanceAll.setOntology(MBCGiveOntology.getInstance().getName());
			informBalanceAll.setConversationId("all-balance-inform");
			for(AID bidder : mbcBidders){
				informBalanceAll.addReceiver(bidder);
			}
			try{
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(bidderBalances);
				myAgent.getContentManager().fillContent(informBalanceAll, give);
				myAgent.send(informBalanceAll);
			}catch(CodecException e){
				e.printStackTrace();
			}catch(OntologyException e){
				e.printStackTrace();
			}
		}
		
		protected double getMaxBid(VisitData v){
			JourneyInfoHelper jHelper = new JourneyInfoHelper();
			JourneyData journeyData = jHelper.getJourneyData(myAgent, problem.depot.location, v.location);
			CostVariables costVars = jHelper.getCostVariables(myAgent);
			double maxBid = 0.0d;
			if (journeyData != null) {
				if(transportMode.equals("Car")){
					if(minimiseFactor.equals("Emissions")){
						maxBid = journeyData.carEm;
					}else if(minimiseFactor.equals("Cost")){
						maxBid = ((journeyData.carDist * costVars.carCostsPerKM) + ((journeyData.carTime/60.0d) * costVars.staffCostPerHour));
					}
				}else if(transportMode.equals("Public Transport")){
					if(minimiseFactor.equals("Emissions")){
						maxBid = journeyData.ptEm;
					}else if(minimiseFactor.equals("Cost")){
						maxBid = (journeyData.ptTime/60.0d) * costVars.staffCostPerHour;
					}
				}else if(transportMode.equals("Car Share")){
					if(minimiseFactor.equals("Emissions")){
						maxBid = 0;
					}else if(minimiseFactor.equals("Cost")){
						maxBid = ((journeyData.carTime/60.0d) * costVars.staffCostPerHour);
					}
				}
			}
			
			return maxBid * 2;
		}
		
		protected void sendAllVisits(){
			// Update the institution with the available visits
			updateInstitution();
			
			ACLMessage initialVisitsMsg = new ACLMessage(ACLMessage.INFORM);
			initialVisitsMsg.setConversationId("available-visits");
			// Add all of the receivers
			for(AID bidder : bidders){
				initialVisitsMsg.addReceiver(bidder);
			}
			
			initialVisitsMsg.setLanguage(new SLCodec().getName());
			initialVisitsMsg.setOntology(GiveOntology.getInstance().getName());
			try{
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(getVisits());
				myAgent.getContentManager().fillContent(initialVisitsMsg, give);
				myAgent.send(initialVisitsMsg);
			}catch(CodecException e){
				e.printStackTrace();
			}catch(OntologyException e){
				e.printStackTrace();
			}
			
			// Get the costing information from the institution
			List<Double> costingInfo = getCostingFromInstitution();
			
			// Send the costing information to the bidders
			ACLMessage costingMessage = new ACLMessage(ACLMessage.INFORM);
			costingMessage.setConversationId("costing-update");
			// Add all of the receivers
			for(AID bidder : bidders){
				costingMessage.addReceiver(bidder);
			}
			
			costingMessage.setLanguage(new SLCodec().getName());
			costingMessage.setOntology(GiveOntology.getInstance().getName());
			try{
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(costingInfo);
				myAgent.getContentManager().fillContent(costingMessage, give);
				myAgent.send(costingMessage);
			}catch(CodecException e){
				e.printStackTrace();
			}catch(OntologyException e){
				e.printStackTrace();
			}
		}
		
		/**
		 * Sends all visits to the institution
		 */
		protected void sendAllVisitsToInstitution(){
			// Create a message to send the visits
			ACLMessage allVisits = new ACLMessage(ACLMessage.INFORM);
			allVisits.setConversationId("all-visits");
			allVisits.addReceiver(institution);
			
			// Add the list of visits and send
			allVisits.setLanguage(new SLCodec().getName());
			allVisits.setOntology(GiveOntology.getInstance().getName());
			try{
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(getVisits());
				myAgent.getContentManager().fillContent(allVisits, give);
				myAgent.send(allVisits);
			}catch(CodecException e){
				e.printStackTrace();
			}catch(OntologyException e){
				e.printStackTrace();
			}			
		}
		
		/**
		 * Update the institution with the visits that are still available
		 */
		protected void updateInstitution(){
			// Create a message to send the available visits
			ACLMessage updateVisits = new ACLMessage(ACLMessage.INFORM);
			updateVisits.setConversationId("update-visits");
			updateVisits.addReceiver(institution);
			
			// Add the list of visits and send
			updateVisits.setLanguage(new SLCodec().getName());
			updateVisits.setOntology(GiveOntology.getInstance().getName());
			try{
				GiveObjectPredicate give = new GiveObjectPredicate();
				give.setData(getVisits());
				myAgent.getContentManager().fillContent(updateVisits, give);
				myAgent.send(updateVisits);
			}catch(CodecException e){
				e.printStackTrace();
			}catch(OntologyException e){
				e.printStackTrace();
			}		
			
		}

		protected List<Double> getCostingFromInstitution(){
			// Send request for the costing information
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.setConversationId("costing-request");
			request.addReceiver(institution);
			myAgent.send(request);

			List<Double> costingInfo = null;
			
			ACLMessage response = myAgent.blockingReceive(MessageTemplate.MatchConversationId(request.getConversationId()));
			try {
				ContentElement d = myAgent.getContentManager().extractContent(response);
				if (d instanceof GiveObjectPredicate) {
					// Read out the visit data to our visits list
					costingInfo = (List<Double>) ((GiveObjectPredicate) d).getData();
				}
			} catch (UngroundedException e) {
				e.printStackTrace();
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
			
			return costingInfo;
		}
		
		
		private int indexOfBidderInSolution(String bidderLocalName){
			List<Route> solution = getAuctionBehaviour().getSolution();
			for(int i = 0; i < solution.size(); i++){
				System.out.println(solution.get(i).getRouteOwner());
				if(solution.get(i).getRouteOwner() != null && solution.get(i).getRouteOwner().getLocalName().equals(bidderLocalName))
					return i;
			}
			return -1;
		}
		
		protected void handleBidderLogs(String timestamp){
			List<List<String>> logs = new ArrayList<List<String>>();
			List<String> logBidders = new ArrayList<String>();
			// Receive log messages
			for(int i = 0; i < mbcBidders.size(); i++){				
				// Wait for log
				ACLMessage bidLog = myAgent.blockingReceive(MessageTemplate.MatchConversationId("bidder-log"));
				
				// Read the log from the message and store it in our log list
				try {
					ContentElement d = myAgent.getContentManager().extractContent(bidLog);
					if (d instanceof GiveObjectPredicate) {
						logs.add((List<String>) ((GiveObjectPredicate) d).getData());
					}
				} catch (UngroundedException e) {
					e.printStackTrace();
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
				
				logBidders.add(bidLog.getSender().getLocalName());
			}
			
			// Get the length of the longest log
			int logLength = 0;
			for(List<String> log : logs){
				if(log.size() > logLength){
					logLength = log.size();
				}
			}
			
			// Save the aggregated log data
			try {
				
				String logPath = files.get(currentProblem);
				String problemName = logPath.substring(logPath.lastIndexOf("\\")+1, logPath.length() - 4);
				logPath = logPath.substring(0, logPath.lastIndexOf("\\")+1) + "results\\"+ problemName +"\\log_" + problemName +"_"+ timestamp;
				System.out.println(logPath);
				File f = new File(logPath.substring(0, logPath.lastIndexOf("\\")));
				f.mkdirs();
				f = new File(logPath);
				f.createNewFile();
				
				FileWriter writer = new FileWriter(logPath);
								
				// Write out the bidder names who had logs
				for(int i = 0; i < logBidders.size(); i++){
					writer.append(","+ logBidders.get(i) +"(Route "+ indexOfBidderInSolution(logBidders.get(i)) +"),,");
				}
				writer.append("\n");
				/*
				// Write out the labels for the data
				for(int i = 0; i < bidders.size(); i++){
					writer.append("bid, cost, maxbid,");
				}
				writer.append("\n");
				*/
				
				// Write out all of the log data
				for(int i = 0; i < logLength; i++){
					for(List<String> log : logs){
						if(log.size() > i){
							writer.append(log.get(i));
						}
						writer.append(",");
					}
					writer.append("\n");
				}
				
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		protected boolean finishUp() {
			calcRenderOffsets();
			
			// Get the current date and time for the result and log file name
			String timestamp = new SimpleDateFormat("yyyy-MM-dd-hhmm'.csv'").format(new Date());
			
			handleBidderLogs(timestamp);
			
			// Sum up the total visits
			int totalVisits = 0;
			double totalEmissions = 0.0d;
			double totalTime = 0.0d;
			double totalDist = 0.0d;
			double totalCost = 0.0d;
			for(Route route : getSolution()){
				totalVisits += route.getNoOfVisitsMade();
				totalEmissions += route.totalEmissions;
				totalTime += route.totalTime;
				totalDist += route.totalDist;
				totalCost += route.totalCost;
			}
			
			if(totalVisits == problem.visits.size()){
				gui.setStatus("Done: Routes="+ getSolution().size() +" Visits="+ totalVisits +" Cost= £:"+ (int)totalCost +" E:"+ (int)totalEmissions +", T:"+ (int)totalTime +", D:"+ (int)totalDist);				
				results += problem.name +"\t"+ getSolution().size() +"\t"+ totalVisits +"\t"+ totalCost +"\t"+ totalEmissions +"\t"+ totalTime +"\t"+ totalDist +"\t"+ gui.getReturnableVisits() +"\t"+ transportMode +"\t"+ minimiseFactor +"\n";
			}else{
				gui.setStatus("Invalid solution. There are only "+ totalVisits +" of the supplied "+ problem.visits.size() +"visits.");
				results += problem.name +"\t"+ getSolution().size() +",INVALID "+ totalVisits +" of "+ problem.visits.size() +".\n";
			}
			System.out.println(problem.name +" Done.");
			gui.setStatus(problem.name +" Done. ("+ (currentProblem+1) +" of "+ files.size() +")");
			
			/*
			System.out.println("Solution:\n"
								+ "- routes = " + getSolution().size() + "\n"
								+ "- visits = " + totalVisits + "\n"
								+ "- cost = Money/Emissions/Time/Distance:"+ moneyCost +"\t"+ totalEmissions +"\t"+ totalTime +"\t"+ totalDist);
			System.out.println(problem.name);
			*/
			
			
			// Save the results
			String resPath = files.get(currentProblem);
			String problemName = resPath.substring(resPath.lastIndexOf("\\")+1, resPath.length() - 4);
			resPath = resPath.substring(0, resPath.lastIndexOf("\\")+1) + "results\\"+ problemName +"\\res_" + problemName +"_"+ timestamp;
			saveResults(resPath);
			
			String tranPath = files.get(currentProblem);
			tranPath = tranPath.substring(0, tranPath.lastIndexOf("\\")+1) + "results\\"+ problemName +"\\trans_" + problemName +"_"+ timestamp;
			saveTransactions(tranPath);
			
			if(currentProblem < files.size()){
				nextProblem();
			}
			if(currentProblem == files.size()){
				gui.repaint();				
				gui.canLoadFile(true);
				results = "Problem\tRoutes\tVisits\tCost\tEmissions\tTime\tDistance\tReturns\tTransport Mode\tmin. Factor\n" + results;
				System.out.println("\n"+ results);
				
				// Create a JTextArea for the results
				JTextArea textArea = new JTextArea(30, 120);
				textArea.setText(results);
				textArea.setEditable(false);

				// Wrap it in a scrollpane
				JScrollPane scrollPane = new JScrollPane(textArea);

				// Display the results
				JOptionPane.showMessageDialog(null, scrollPane);
				results = "";
			}
			
			return true;
		}
		
	}

	
}
