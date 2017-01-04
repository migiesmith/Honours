package com.migie.smith;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
		
		// Create all agents including one player agent
		createAgents(Bidder.class, 0, noBidders.get(currentProblem) - 1);
		createAgents(MBCPlayerBidder.class, noBidders.get(currentProblem) - 1, 1);
		
		// If there is already an auction behaviour, remove it
		if(auction != null)
			removeBehaviour(auction);
		// Add a new auction behaviour
		addBehaviour(getAuctionBehaviour());
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
			System.out.println("Waiting");
			ACLMessage bidLog = myAgent.receive(MessageTemplate.MatchConversationId("bidder-log"));
			while(bidLog != null){
				System.out.println("Got a log");
				/*
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
				*/
				
				// Check for another log
				bidLog = myAgent.receive(MessageTemplate.MatchConversationId("bidder-log"));
			}
			
			// Save the aggregated log data
			// TODO
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
