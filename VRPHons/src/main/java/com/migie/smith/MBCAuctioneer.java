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
import agent.auctionSolution.auction.AuctionBehaviour;
import agent.auctionSolution.auction.Auctioneer;
import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.Problem;
import agent.auctionSolution.dataObjects.Route;
import agent.auctionSolution.dataObjects.VisitData;
import jade.core.AID;

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
			return true;
		}
		
		@Override
		protected boolean finishUp() {
			calcRenderOffsets();

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