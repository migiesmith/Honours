package com.migie.smith.gui;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.migie.smith.MBCBidderMove;
import com.migie.smith.MBCPlayerBidder;
import com.migie.smith.TimingRepresentation;
import com.migie.smith.TimingRepresentation.TimeType;

import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.HasXandY;
import agent.auctionSolution.dataObjects.ReturnToCar;
import agent.auctionSolution.dataObjects.VisitData;
import agent.auctionSolution.dataObjects.carShare.CarShare;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JSpinner;
import javax.swing.JList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class MBCPlayerBidderGui extends JFrame {

	private JPanel contentPane;
	private	MBCPlayerBidder player;
	private JTextPane txtInformation;
	private JLabel lblGameState;
	// The spinner for selecting where to insert a visit
	private JList<Integer> lsInsertLocation;
	
	// Cost and Reward for adding at the selected location
	private JLabel lblCost;
	private JLabel lblReward;
	
	
	// The panel used to render the turns
	JPanel turnPanel;
	
	/* Turn information */
	// The current route
	List<VisitData> route;
	// The new visit to add
	VisitData newVisit;
	// The data storing timing information
	List<TimingRepresentation> times;
	// The locations that the new visit can be added
	List<Integer> possibleLocations;
	private JScrollPane scrollPane_1;
	List<VisitData> allVisits;
	List<VisitData> availableVisits;

	// Scale the route relative to all seen visits
	double renderScale = 1.0d;
	private JPanel timingPanel;
	
	
	public void showMessage(String message){
		txtInformation.setText(message + txtInformation.getText());
	}
	
	public void setGameState(String message){
		lblGameState.setText(message);
	}
	
	protected void calcRenderScale(){
		double maxX = 0.0d;
		double maxY = 0.0d;
		Depot depot = player.getDepot();
		
		Iterator<VisitData> it = allVisits.iterator();

		while(it.hasNext()){
			VisitData v = it.next();
			if(Math.abs(v.x - depot.x) > maxX)
				maxX = Math.abs(v.x - depot.x);
			if(Math.abs(v.y - depot.y) > maxY)
				maxY = Math.abs(v.y - depot.y);
		}
		this.renderScale = ((double)Math.min(turnPanel.getWidth()/2 * 0.9, turnPanel.getHeight()/2 * 0.9)) / ((double)Math.max(maxX, maxY));
	}
	
	public void setAllVisits(List<VisitData> allVisits){
		this.allVisits = allVisits;
	}
	public void setAvailableVisits(List<VisitData> availableVisits){
		this.availableVisits = availableVisits;
	}
	
	public void renderTurn(List<VisitData> route, List<Integer> possibleLocations, VisitData newVisit, List<TimingRepresentation> times){
		this.route = route;
		this.possibleLocations = possibleLocations;
		this.newVisit = newVisit;
		this.times = times;
		DefaultListModel<Integer> listModel = new DefaultListModel<Integer>();
		for(int i = 0; i < possibleLocations.size(); i++){
			listModel.add(i, possibleLocations.get(i));
		}
		lsInsertLocation.setModel(listModel);
		lsInsertLocation.setSelectedIndex(0);
		repaint();
	}

	protected boolean visitInList(List<VisitData> visitList, VisitData visit){
		for(VisitData v : visitList){
			if(v.name.equals(visit.name))
				return true;
		}
		return false;
	}
	
	public void paint(Graphics g) {
		super.paint(g);

		int nodeSize = 4;
		
		BufferedImage b = new BufferedImage(turnPanel.getWidth(), turnPanel.getHeight(), BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = (Graphics2D) b.getGraphics();
		FontMetrics fm = g2.getFontMetrics();
		g2.drawRect(0, 0, 640, 480);
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, turnPanel.getWidth(), turnPanel.getHeight());

		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// Center the rendering
		g2.translate(turnPanel.getWidth() / 2, turnPanel.getHeight() / 2);
		
		// Draw the depot
		Depot depot = player.getDepot();
		g2.setColor(Color.red);
		g2.fillOval(-2,-2,2,2);
		g2.setColor(Color.black);
		g2.drawOval(-2,-2,2,2);

		// Draw all the visits
		if(allVisits != null){
			calcRenderScale();			
			Iterator<VisitData> it = allVisits.iterator();
			while(it.hasNext()){
				VisitData v = it.next();
				if(availableVisits == null || visitInList(availableVisits, v)){
					g2.setColor(new Color(237, 242, 99, 100));
				}else{
					g2.setColor(new Color(100, 100, 100, 50));
				}
				g2.fillOval((int)((v.x - depot.x) * renderScale) - nodeSize/2, (int)((v.y - depot.y) * renderScale) - nodeSize/2, nodeSize, nodeSize);
				g2.setColor(new Color(0, 0, 0, 100));
				g2.drawOval((int)((v.x - depot.x) * renderScale) - nodeSize/2, (int)((v.y - depot.y) * renderScale) - nodeSize/2, nodeSize, nodeSize);				
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
					g2.fillOval((int)((v.x - depot.x) * renderScale) - nodeSize/2, (int)((v.y - depot.y) * renderScale) - nodeSize/2, nodeSize, nodeSize);
					g2.setColor(Color.black);
					g2.drawOval((int)((v.x - depot.x) * renderScale) - nodeSize/2, (int)((v.y - depot.y) * renderScale) - nodeSize/2, nodeSize, nodeSize);
					
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
			
			// Draw the new node
			g2.setColor(Color.green);
			g2.fillOval((int) ((newVisit.x - depot.x) * renderScale) - nodeSize / 2,
					(int) ((newVisit.y - depot.y) * renderScale) - nodeSize / 2, nodeSize, nodeSize);
			g2.setColor(Color.black);
			g2.drawOval((int) ((newVisit.x - depot.x) * renderScale) - nodeSize / 2,
					(int) ((newVisit.y - depot.y) * renderScale) - nodeSize / 2, nodeSize, nodeSize);
	
			// Draw possible connections to the new node
			possibleLocations.add((Integer)lsInsertLocation.getSelectedValue());
			Iterator<Integer> pit = possibleLocations.iterator();
			while (pit.hasNext()) {
				int loc = pit.next();
				if(loc == (Integer)lsInsertLocation.getSelectedValue()){
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
		
		// Draw to the screen
		Graphics gPanel = turnPanel.getGraphics();
		gPanel.drawImage(b, 0, 0, null);
		gPanel.setColor(Color.GRAY);
		gPanel.drawRect(2, 2, turnPanel.getWidth() - 5, turnPanel.getHeight() - 5);
		gPanel.drawRect(0, 0, turnPanel.getWidth() - 1, turnPanel.getHeight() - 1);
		gPanel.setColor(Color.BLACK);


		// Draw the timing information
		Graphics gTimingPanel = timingPanel.getGraphics();
		if(times != null){
			int total = 0;
			for(TimingRepresentation tr : times){
				total += tr.timeTaken;
			}
			int selectedVal = 0;
			if(lsInsertLocation.getSelectedValue() != null)
				selectedVal = lsInsertLocation.getSelectedValue();
			int visitCount = 0;
			if(total > 0){
				double ratio = (double)timingPanel.getWidth() / total;
				int x = 0;
				for(TimingRepresentation tr : times){
					switch(tr.type){
					case Empty:
						gTimingPanel.setColor(Color.gray);
						break;
					case Visit:
						gTimingPanel.setColor(Color.green);
							visitCount++;
						break;
					case Travel:
						gTimingPanel.setColor(Color.orange);
						break;
					}
					gTimingPanel.fillRect(x + (selectedVal == visitCount && tr.type == TimeType.Travel ? 1 : 0), 0, (int)(tr.timeTaken * ratio), timingPanel.getHeight());
					
					if(selectedVal == visitCount && tr.type == TimeType.Visit){
						gTimingPanel.setColor(Color.red);
		                Graphics2D g2TimingPanel = (Graphics2D) gTimingPanel;
		                g2TimingPanel.setStroke(new BasicStroke(2));
		                g2TimingPanel.draw(new Line2D.Float(x + (int)(tr.timeTaken * ratio), 0, x + (int)(tr.timeTaken * ratio), timingPanel.getHeight()));
		                g2TimingPanel.setStroke(new BasicStroke(1));
					}
					x += tr.timeTaken * ratio;
				}
			}
		}

		if(lsInsertLocation.getSelectedValue() != null && lsInsertLocation.getSelectedValue() == 0){
			gTimingPanel.setColor(Color.red);
            Graphics2D g2TimingPanel = (Graphics2D) gTimingPanel;
            g2TimingPanel.setStroke(new BasicStroke(2));
            g2TimingPanel.draw(new Line2D.Float(1, 0, 1, timingPanel.getHeight()));
            g2TimingPanel.setStroke(new BasicStroke(1));
		}	
		gTimingPanel.setColor(Color.BLACK);
		gTimingPanel.drawRect(0, 0, timingPanel.getWidth() - 1, timingPanel.getHeight() - 1);
	}
	
	/**
	 * Create the frame.
	 */
	public MBCPlayerBidderGui(final MBCPlayerBidder player) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 650, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JLabel lblNewLabel = new JLabel("Player Bidder");
		
		turnPanel = new JPanel();
		
		JButton btnBid = new JButton("Bid");
		btnBid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.makeMove(new MBCBidderMove((Integer)lsInsertLocation.getSelectedValue(), true));
			}
		});
		
		JButton btnReject = new JButton("Reject");
		btnReject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.makeMove(new MBCBidderMove(0, false));
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		
		txtInformation = new JTextPane();
		scrollPane.setViewportView(txtInformation);
		
		lblGameState = new JLabel(" ");
		
		scrollPane_1 = new JScrollPane();
		
		lblCost = new JLabel("Cost:");
		
		lblReward = new JLabel("Reward:");
		
		timingPanel = new JPanel();
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGap(101)
							.addComponent(lblNewLabel))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addGap(30)
									.addComponent(lblGameState, GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
									.addGap(37))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addContainerGap()
									.addComponent(turnPanel, GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
									.addPreferredGap(ComponentPlacement.RELATED)))
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
								.addComponent(btnBid, GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
								.addComponent(btnReject, GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
										.addComponent(timingPanel, GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
										.addComponent(lblReward, GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
										.addComponent(lblCost, GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE))
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(scrollPane_1, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)))))
					.addContainerGap())
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addComponent(lblNewLabel)
					.addGap(5)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(btnBid)
							.addGap(5)
							.addComponent(btnReject)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(lblCost)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblReward)
									.addPreferredGap(ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
									.addComponent(timingPanel, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)))
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE))
						.addComponent(turnPanel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(lblGameState))
		);
		
		lsInsertLocation = new JList<Integer>();
		lsInsertLocation.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				repaint();
				if(lsInsertLocation.getSelectedValue() != null){
					lblCost.setText("Cost: " + Math.abs(player.getCost(newVisit, (Integer)lsInsertLocation.getSelectedValue())));
					lblReward.setText("Reward: " + player.getReward(newVisit, (Integer)lsInsertLocation.getSelectedValue()));
				}
			}
		});
		scrollPane_1.setViewportView(lsInsertLocation);
		lsInsertLocation.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		contentPane.setLayout(gl_contentPane);
		setVisible(true);
		
		this.player = player;
	}
}
