package com.migie.smith;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.HasXandY;
import agent.auctionSolution.dataObjects.VisitData;

public class MBCHelper {

	private static int NODE_SIZE = 4;
	
	public static int visitPosInList(List<VisitData> visitList, VisitData visit){
		int index = 0;
		for(VisitData v : visitList){
			if(v.name.equals(visit.name))
				return index;
			
			index++;
		}
		return -1;
	}

	protected static double calcRenderScale(JPanel renderPanel, Depot depot, List<VisitData> allVisits){
		double maxX = 0.0d;
		double maxY = 0.0d;
		
		Iterator<VisitData> it = allVisits.iterator();
		while(it.hasNext()){
			VisitData v = it.next();
			if(Math.abs(v.x - depot.x) > maxX)
				maxX = Math.abs(v.x - depot.x);
			if(Math.abs(v.y - depot.y) > maxY)
				maxY = Math.abs(v.y - depot.y);
		}
		return ((double)Math.min(renderPanel.getWidth()/2 * 0.9, renderPanel.getHeight()/2 * 0.9)) / ((double)Math.max(maxX, maxY));
	}
	
	public static VisitData visitAtPosition(int mouseX, int mouseY, JPanel renderPanel, List<VisitData> allVisits, Depot depot, List<VisitData> availableVisits){
		int xOffset = renderPanel.getWidth() / 2;
		int yOffset = renderPanel.getHeight() / 2;

		// Scale the route relative to all seen visits
		double renderScale = 1.0d;
		if(depot != null){
			renderScale = calcRenderScale(renderPanel, depot, allVisits);		
		}
		
		for(int i = 0; i < allVisits.size(); i++){
			VisitData v = allVisits.get(i);
			int visitX = xOffset + (int)((v.x - depot.x) * renderScale);
			int visitY = yOffset + (int)((v.y - depot.y) * renderScale);
			
			if(Math.abs(mouseX - visitX) < NODE_SIZE*2 && Math.abs(mouseY - visitY) < NODE_SIZE*2 && visitPosInList(availableVisits, v) != -1){
				return v;
			}
		}
		
		return null;
	}
	
	public static void drawVisits(JPanel renderPanel, List<VisitData> allVisits, Depot depot, VisitData newVisit, List<VisitData> availableVisits, List<Double> costingInfo, List<VisitData> route, List<Integer> possibleLocations, int insertLocation){		
		//int nodeSize = NODE_SIZE;
		
		BufferedImage b = new BufferedImage(renderPanel.getWidth(), renderPanel.getHeight(), BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = (Graphics2D) b.getGraphics();
		FontMetrics fm = g2.getFontMetrics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, renderPanel.getWidth(), renderPanel.getHeight());

		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// Center the rendering
		g2.translate(renderPanel.getWidth() / 2, renderPanel.getHeight() / 2);
		
		// Draw the depot
		if(depot != null){
			g2.setColor(Color.red);
			g2.fillOval(-2,-2,4,4);
			g2.setColor(Color.black);
			g2.drawOval(-2,-2,4,4);
	
			// Scale the route relative to all seen visits
			double renderScale = 1.0d;
			
			// Draw all the visits
			if(allVisits != null){
				renderScale = calcRenderScale(renderPanel, depot, allVisits);			
				Iterator<VisitData> it = allVisits.iterator();
				int visitIndex = 0;
				while(it.hasNext()){
					VisitData v = it.next();
					if(availableVisits == null || MBCHelper.visitPosInList(availableVisits, v) != -1){
						
						// Draw costing Value indicator
						if(costingInfo != null){
							double costingVal = costingInfo.get(visitIndex);
							int costingNodeSize = NODE_SIZE + 2;
							if(costingVal > 1.0d){
								g2.setColor(Color.blue);
								g2.drawOval((int)((v.x - depot.x) * renderScale) - costingNodeSize/2, (int)((v.y - depot.y) * renderScale) - costingNodeSize/2, costingNodeSize, costingNodeSize);
							}else if(costingVal < 1.0d){
								g2.setColor(Color.red);
								g2.drawOval((int)((v.x - depot.x) * renderScale) - costingNodeSize/2, (int)((v.y - depot.y) * renderScale) - costingNodeSize/2, costingNodeSize, costingNodeSize);
							}
						}
						
						g2.setColor(new Color(237, 242, 99, 100));
					}else{
						g2.setColor(new Color(100, 100, 100, 50));
					}
					g2.fillOval((int)((v.x - depot.x) * renderScale) - NODE_SIZE/2, (int)((v.y - depot.y) * renderScale) - NODE_SIZE/2, NODE_SIZE, NODE_SIZE);
					g2.setColor(new Color(0, 0, 0, 100));
					g2.drawOval((int)((v.x - depot.x) * renderScale) - NODE_SIZE/2, (int)((v.y - depot.y) * renderScale) - NODE_SIZE/2, NODE_SIZE, NODE_SIZE);			
	
					
					visitIndex++;
				}
				
			}
			
			if(route != null){
				Iterator<VisitData> it = route.iterator();
	
				// Check if there is a route to draw
				if(route.size() > 0){
					// Draw the current route
					HasXandY lastNode = depot;
					it = route.iterator();
					while(it.hasNext()){
						VisitData v = it.next();
						
						if(v.transport.equals("Car")){
							g2.setColor(Color.red);
						}else if(v.transport.equals("Public Transport")){
							g2.setColor(Color.blue);
						}
						g2.drawLine(
								(int)((lastNode.getX() - depot.x) * renderScale), (int)((lastNode.getY() - depot.y) * renderScale),
								(int)((v.x - depot.x) * renderScale), (int)((v.y - depot.y) * renderScale)
							);
						
						g2.setColor(Color.yellow);
						g2.fillOval((int)((v.x - depot.x) * renderScale) - NODE_SIZE/2, (int)((v.y - depot.y) * renderScale) - NODE_SIZE/2, NODE_SIZE, NODE_SIZE);
						g2.setColor(Color.black);
						g2.drawOval((int)((v.x - depot.x) * renderScale) - NODE_SIZE/2, (int)((v.y - depot.y) * renderScale) - NODE_SIZE/2, NODE_SIZE, NODE_SIZE);
						
						lastNode = v;
					}
					
					// Draw the connection back to the depot
					if(((VisitData)lastNode).transport.equals("Car")){
						g2.setColor(Color.red);
					}else if(((VisitData)lastNode).transport.equals("Public Transport")){
						g2.setColor(Color.blue);
					}
					g2.drawLine(
							(int)((lastNode.getX() - depot.x) * renderScale), (int)((lastNode.getY() - depot.y) * renderScale),
							0, 0
						);
		
				}
				
				// Check if we can take part in this turn
				if(newVisit != null){
					// Draw the new node
					g2.setColor(Color.green);
					g2.fillOval((int) ((newVisit.x - depot.x) * renderScale) - NODE_SIZE / 2,
							(int) ((newVisit.y - depot.y) * renderScale) - NODE_SIZE / 2, NODE_SIZE, NODE_SIZE);
					g2.setColor(Color.black);
					g2.drawOval((int) ((newVisit.x - depot.x) * renderScale) - NODE_SIZE / 2,
							(int) ((newVisit.y - depot.y) * renderScale) - NODE_SIZE / 2, NODE_SIZE, NODE_SIZE);
			
					// Draw possible connections to the new node
					if(possibleLocations != null){
					possibleLocations.add(insertLocation);
					Iterator<Integer> pit = possibleLocations.iterator();
					while (pit.hasNext()) {
						int loc = pit.next();
						if(loc == insertLocation){
							g2.setColor(Color.green);
						}else{
							g2.setColor(new Color(0,0,0,50));
							
						}
						if (loc == 0) { // From Depot
							g2.drawLine(0, 0, (int) ((newVisit.getX() - depot.x) * renderScale),
									(int) ((newVisit.getY() - depot.y) * renderScale));
						} else { // From Visit
							g2.drawLine((int) ((route.get(loc - 1).getX() - depot.x) * renderScale),
									(int) ((route.get(loc - 1).getY() - depot.y) * renderScale),
									(int) ((newVisit.getX() - depot.x) * renderScale),
									(int) ((newVisit.getY() - depot.y) * renderScale));
						}
						if (loc == route.size()) { // To Depot
							g2.drawLine((int) ((newVisit.getX() - depot.x) * renderScale),
									(int) ((newVisit.getY() - depot.y) * renderScale), 0, 0);
						} else { // To Visit
							g2.drawLine((int) ((newVisit.getX() - depot.x) * renderScale),
									(int) ((newVisit.getY() - depot.y) * renderScale),
									(int) ((route.get(loc).getX() - depot.x) * renderScale),
									(int) ((route.get(loc).getY() - depot.y) * renderScale));
						}
					}
					possibleLocations.remove(possibleLocations.size()-1);
					}
				}
			}
		}
		// Draw to the screen
		Graphics gPanel = renderPanel.getGraphics();
		gPanel.drawImage(b, 0, 0, null);
		gPanel.setColor(Color.GRAY);
		gPanel.drawRect(2, 2, renderPanel.getWidth() - 5, renderPanel.getHeight() - 5);
		gPanel.drawRect(0, 0, renderPanel.getWidth() - 1, renderPanel.getHeight() - 1);

		if(newVisit == null){
			gPanel.setColor(new Color(100, 100, 100, 100));
			gPanel.fillRect(0, 0, renderPanel.getWidth(), renderPanel.getHeight());
			gPanel.setColor(Color.BLACK);
			// Get values required to center text
		    FontMetrics metrics = gPanel.getFontMetrics(gPanel.getFont());
			gPanel.drawString("Waiting...", renderPanel.getWidth()/2 - metrics.stringWidth("Waiting...")/2, renderPanel.getHeight()/2);
		}
		
	}
	
}
