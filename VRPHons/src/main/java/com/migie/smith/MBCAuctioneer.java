package com.migie.smith;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
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
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MBCAuctioneer extends Auctioneer{

	String results = "";

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
		
		// Find agents to use and create more if needed
		setupAgents(noBidders.get(currentProblem));
		/*
		// Create all agents including one player agent
		createAgents(Bidder.class, 0, noBidders.get(currentProblem) - 1);
		createAgents(MBCPlayerBidder.class, noBidders.get(currentProblem) - 1, 1);
		*/		
		
		// If there is already an auction behaviour, remove it
		if(auction != null)
			removeBehaviour(auction);
		// Add a new auction behaviour
		addBehaviour(getAuctionBehaviour());
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
			gui.canLoadFile(false);
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
			ACLMessage initialVisitsMsg = new ACLMessage(ACLMessage.INFORM);
			// Add all of the receivers
			for(AID bidder : bidders){
				initialVisitsMsg.addReceiver(bidder);
			}
			initialVisitsMsg.setConversationId("available-visits");
			
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
		}
		
		protected void handleBidderLogs(){
			ACLMessage bidLog = myAgent.receive(MessageTemplate.MatchConversationId("bidder-log"));
			List<List<String>> logs = new ArrayList<List<String>>();
			List<String> bidders = new ArrayList<String>();
			while(bidLog != null){
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
				
				bidders.add(bidLog.getSender().getLocalName());
				
				// Check for another log
				bidLog = myAgent.receive(MessageTemplate.MatchConversationId("bidder-log"));
			}
			
			// Get the length of the longest log
			int logLength = 0;
			for(List<String> log : logs){
				if(log.size() > logLength){
					logLength = log.size();
				}
			}
			
			// Save the aggregated log data
			// TODO

			try {
				// Get the current date and time for the log file name
				String timestamp = new SimpleDateFormat("yyyy-MM-dd-hhmm'.csv'").format(new Date());
				
				String logPath = files.get(currentProblem);
				logPath = logPath.substring(0, logPath.lastIndexOf("\\")+1) + "results\\log_" + logPath.substring(logPath.lastIndexOf("\\")+1, logPath.length() - 4) +"_"+ timestamp;
				System.out.println(logPath);
				File f = new File(logPath.substring(0, logPath.lastIndexOf("\\")));
				f.mkdirs();
				f = new File(logPath);
				f.createNewFile();
				
				FileWriter writer = new FileWriter(logPath);

				
				
				for(int i = 0; i < bidders.size(); i++){
					writer.append(","+ bidders.get(i)+ ",,");
				}
				writer.append("\n");
				for(int i = 0; i < bidders.size(); i++){
					writer.append("bid, cost, maxbid,");
				}
				writer.append("\n");
				
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

			handleBidderLogs();
			
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
			resPath = resPath.substring(0, resPath.lastIndexOf("\\")+1) + "results\\res_" + resPath.substring(resPath.lastIndexOf("\\")+1);
			saveResults(resPath);
			String tranPath = files.get(currentProblem);
			tranPath = tranPath.substring(0, tranPath.lastIndexOf("\\")+1) + "results\\trans_" + tranPath.substring(tranPath.lastIndexOf("\\")+1);
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
