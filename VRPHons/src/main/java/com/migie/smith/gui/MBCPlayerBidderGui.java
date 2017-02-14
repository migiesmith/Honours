package com.migie.smith.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Line2D;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.migie.smith.MBCAccountant;
import com.migie.smith.MBCBidderMove;
import com.migie.smith.MBCHelper;
import com.migie.smith.MBCPlayerBidder;
import com.migie.smith.TimingRepresentation;
import com.migie.smith.TimingRepresentation.TimeType;

import agent.auctionSolution.dataObjects.VisitData;

public class MBCPlayerBidderGui extends JFrame implements WindowListener{

	private JPanel contentPane;
	private	MBCPlayerBidder player;
	private JTextPane txtInformation;
	private JLabel lblGameState;
	// The spinner for selecting where to insert a visit
	private JList<Integer> lsInsertLocation;
	
	// The label for displaying the bidder's balance
	JLabel lblBalance;
	// Cost and Reward for adding at the selected location
	private JLabel lblSuggestedBid;
	private JLabel lblMaxBid;
	private JLabel lblNetMultiplier;
	
	
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
	List<Double> costingInfo;
	List<VisitData> availableVisits;

	private JPanel timingPanel;
	private JSpinner currentBidSpinner;
	private JLabel lblTimeFrame;
	private JLabel lblLog;
	private JLabel lblAddAt;
	private JLabel lblNet;
	private JLabel lblRanking;
	
	
	public void showMessage(String message){
		txtInformation.setText(message + txtInformation.getText());
	}
	
	public void setGameState(String message){
		lblGameState.setText(message);
	}
	
	public void setBalance(double balance){
		lblBalance.setText("Balance: " + String.valueOf(balance));
	}
	
	public void setAllVisits(List<VisitData> allVisits){
		this.allVisits = allVisits;
	}
	public void setCostingInfo(List<Double> costingInfo){
		this.costingInfo = costingInfo;
	}
	public void setAvailableVisits(List<VisitData> availableVisits){
		this.availableVisits = availableVisits;
	}
	
	public void renderTurn(List<VisitData> route, List<Integer> possibleLocations, VisitData newVisit, List<TimingRepresentation> times){
		this.route = route;
		this.possibleLocations = possibleLocations;
		this.newVisit = newVisit;
		this.times = times;

		// Add all possible locations to the route index list
		DefaultListModel<Integer> listModel = new DefaultListModel<Integer>();
		for(int i = 0; i < possibleLocations.size(); i++){
			listModel.add(i, possibleLocations.get(i));
		}
		// Update the selected position on the route index list
		lsInsertLocation.setModel(listModel);
		lsInsertLocation.setSelectedIndex(0);

		// Update the current bid
		double maxBid = player.getMaxBid(newVisit);
		double suggestedBid = Math.abs(player.getCost(newVisit, (Integer)lsInsertLocation.getSelectedValue()));
		currentBidSpinner.setModel(new SpinnerNumberModel((suggestedBid < maxBid ? suggestedBid : maxBid), 0.0, maxBid, 0.1));
		
		// We can make a move, enable the buttons
		Component[] components = getContentPane().getComponents();
		for(Component c : components){
			if(c.getClass() == JButton.class){
				c.setEnabled(true);
			}
		}
		
		// Redraw the GUI
		repaint();
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		
		List<MBCAccountant> bidderBalances = player.getBalances();
		if(bidderBalances != null){
			int playerRank = 1;
			for(int i = 0; i < bidderBalances.size(); i++){
				if(bidderBalances.get(i).getBalance() > player.getBalance() && !bidderBalances.get(i).getBidder().equals(player.getAID())){
				playerRank++;
				}
				System.out.println(bidderBalances.get(i).getBalance() + " - " + bidderBalances.get(i).getBidder());
			}
			lblRanking.setText("Ranked "+ playerRank +" of "+ bidderBalances.size());
		}
		
		// Draw the visits to turnPanel
		if(allVisits != null){
			MBCHelper.drawVisits(turnPanel, allVisits, player.getDepot(), newVisit, availableVisits, costingInfo, route, possibleLocations, (Integer)lsInsertLocation.getSelectedValue());
		}

		// Draw the timing information
		Graphics gTimingPanel = timingPanel.getGraphics();
		if(times != null){
			// Calculate the total time
			int total = 0;
			for(TimingRepresentation tr : times){
				total += tr.timeTaken;
			}
			
			int selectedVal = 0;
			if(lsInsertLocation.getSelectedValue() != null)
				selectedVal = lsInsertLocation.getSelectedValue();
			int visitCount = 0;
			// If we have something to draw
			if(total > 0){
				double ratio = (double)timingPanel.getWidth() / (double)total;
				// Store x to add items after each other
				int x = 0;
				// Check the type of the timing information and colour code it
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
					// Render the piece of timing info
					gTimingPanel.fillRect(x + (selectedVal == visitCount && tr.type == TimeType.Travel ? 1 : 0), 0, (int)Math.ceil(((double)tr.timeTaken) * ratio), timingPanel.getHeight());
					
					// Draw a line indicating where the new visit will be added
					if(selectedVal == visitCount && tr.type == TimeType.Visit){
						gTimingPanel.setColor(Color.red);
		                Graphics2D g2TimingPanel = (Graphics2D) gTimingPanel;
		                g2TimingPanel.setStroke(new BasicStroke(2));
		                g2TimingPanel.draw(new Line2D.Float(x + (int)(tr.timeTaken * ratio), 0, x + (int)(tr.timeTaken * ratio), timingPanel.getHeight()));
		                g2TimingPanel.setStroke(new BasicStroke(1));
					}
					// Increment x for the next visit
					x += Math.round(((double)tr.timeTaken) * ratio);
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
	
	protected void endTurn(){
		newVisit = null;
		currentBidSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 0.1));

		// We cannot make a move, disable the buttons
		Component[] components = getContentPane().getComponents();
		for(Component c : components){
			if(c.getClass() == JButton.class){
				c.setEnabled(false);
			}
		}
		repaint();
	}
	
	/**
	 * Create the frame.
	 */
	public MBCPlayerBidderGui(final MBCPlayerBidder player) {
		setTitle("Player Bidder");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 720, 500);
		setMinimumSize(this.getBounds().getSize());
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		this.addWindowListener(this);
		
		this.repaint();
		
		JLabel lblNewLabel = new JLabel("Player Bidder");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		turnPanel = new JPanel();
		turnPanel.setToolTipText("<html>\r\nAll visits, greyed out visits have been won already and yellow visits are yet to be bidded on. \r\n<br>\r\nRed lines show the route, grey lines show possible connections to the new route, and the\r\n<br>\r\ngreen line shows the selected possible connection.\r\n</html>");
		
		JButton btnBid = new JButton("Bid");
		btnBid.setToolTipText("Confirm your bid value and insert location. If the bid is won, the Net value will be added to the balance and the visit will be inserted into the route.");
		btnBid.setEnabled(false);
		btnBid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(newVisit != null){
					player.makeMove(new MBCBidderMove((Integer)lsInsertLocation.getSelectedValue(), Double.valueOf(currentBidSpinner.getValue().toString()), true));
				}
				endTurn();
			}
		});
		
		JButton btnReject = new JButton("Reject");
		btnReject.setToolTipText("Refuse to bid for the visit and wait for the next one. This causes no changes to your route.");
		btnReject.setEnabled(false);
		btnReject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.makeMove(new MBCBidderMove(0, 0.0d, false));
				endTurn();
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		
		txtInformation = new JTextPane();
		txtInformation.setToolTipText("Information about previous turns");
		scrollPane.setViewportView(txtInformation);
		
		lblGameState = new JLabel(" ");
		
		scrollPane_1 = new JScrollPane();
		
		timingPanel = new JPanel();
		timingPanel.setToolTipText("Visual representation of the route's time frame. Orange = travelling, Green = visit, Grey = unused.");
		

		lsInsertLocation = new JList<Integer>();
		lsInsertLocation.setToolTipText("<html>\r\nThe position within the route that the new visit should be placed at should it be won.\r\n<br>\r\nLook at the visit graphic to the left to see where it will be placed\r\n</html>");
		lsInsertLocation.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				repaint();
				if(lsInsertLocation.getSelectedValue() != null){
					double suggestedBid = Math.abs(player.getCost(newVisit, (Integer)lsInsertLocation.getSelectedValue()));
					lblSuggestedBid.setText("Cost: " + Math.round(suggestedBid * 100) / 100.0d);
					lblMaxBid.setText("Max Bid: " + Math.round(player.getMaxBid(newVisit) * 100) / 100.0d);

					currentBidSpinner.setValue(suggestedBid);
					
				}
			}
		});

		currentBidSpinner = new JSpinner();
		currentBidSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 0.1));
		currentBidSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				double suggestedBid = Math.abs(player.getCost(newVisit, (Integer)lsInsertLocation.getSelectedValue()));
				double costingMult = costingInfo.get(MBCHelper.visitPosInList(allVisits, newVisit));
				lblNetMultiplier.setText("Net Multiplier: " + costingMult);
				lblNet.setText("Net: " + Math.round((Double.valueOf(currentBidSpinner.getValue().toString()) - suggestedBid) * costingMult * 100) / 100.0d);
			}			
		});
		currentBidSpinner.setToolTipText("The value that will be used as the bid.");
		
		lblBalance = new JLabel("Balance:");
		lblBalance.setToolTipText("The overall profit or loss of this route.");
		
		lblTimeFrame = new JLabel("Time Frame:");
		lblTimeFrame.setToolTipText(timingPanel.getToolTipText());
		
		lblLog = new JLabel("Log:");
		lblLog.setToolTipText(txtInformation.getToolTipText());
		
		lblAddAt = new JLabel("Add At:");
		lblAddAt.setHorizontalAlignment(SwingConstants.CENTER);
		lblAddAt.setToolTipText(lsInsertLocation.getToolTipText());
		
		lblSuggestedBid = new JLabel("Cost:");
		lblSuggestedBid.setToolTipText("How much it will cost to insert the visit at the selected location in the route.");
		
		lblNetMultiplier = new JLabel("Net Multiplier:");
		lblNetMultiplier.setToolTipText("This value is set by the institution and modifies the Net value. See Net's tooltip for the usage.");
		
		lblMaxBid = new JLabel("Max Bid:");
		lblMaxBid.setToolTipText("The maximum value that can be used for the bid. The auctioneer sets this value.");
		
		lblNet = new JLabel("Net:");
		lblNet.setToolTipText("The profit or loss that will occur as a result of winning the bid. Net = (bid - cost) * multiplier");
		
		JLabel lblBid = new JLabel("Bid:");
		lblBid.setToolTipText(currentBidSpinner.getToolTipText());
		
		
		JLabel lblTip = new JLabel("* Red outline on node = Reduction in reward, Blue on node = Increase in reward");
		lblTip.setHorizontalAlignment(SwingConstants.CENTER);
		
		lblRanking = new JLabel("Ranked X of Y");
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
						.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(lblTip, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE)
								.addComponent(turnPanel, GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE))
							.addGap(5)))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addComponent(lblGameState, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
								.addComponent(btnBid, GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
								.addComponent(btnReject, GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
								.addComponent(lblTimeFrame, GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
								.addComponent(timingPanel, GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
										.addComponent(lblLog, GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
										.addComponent(scrollPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE))
									.addGap(18)
									.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING, false)
										.addComponent(lblAddAt, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(lblBalance, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
									.addGap(3))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(lblSuggestedBid)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(lblNetMultiplier)
									.addGap(35))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(lblMaxBid)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(lblNet, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
									.addGap(20))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(lblBid)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(currentBidSpinner, GroupLayout.PREFERRED_SIZE, 140, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)))
							.addGap(0))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(lblRanking)
							.addContainerGap())))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblBalance)
							.addGap(5)
							.addComponent(btnBid)
							.addGap(5)
							.addComponent(btnReject)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblSuggestedBid)
								.addComponent(lblNetMultiplier))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblMaxBid)
								.addComponent(lblNet))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblBid)
								.addComponent(currentBidSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lblRanking)
							.addGap(29)
							.addComponent(lblTimeFrame)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(timingPanel, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblLog)
								.addComponent(lblAddAt))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
								.addComponent(scrollPane_1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(lblNewLabel)
							.addGap(5)
							.addComponent(turnPanel, GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblGameState)
						.addComponent(lblTip)))
		);
		
		
		
		
		scrollPane_1.setViewportView(lsInsertLocation);
		lsInsertLocation.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		contentPane.setLayout(gl_contentPane);
		setVisible(true);
		
		this.player = player;
	}

	
	public void windowClosing(WindowEvent e) {
		// Stop the agent associated with this window
		this.player.doDelete();
	}
    public void windowClosed(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
}
