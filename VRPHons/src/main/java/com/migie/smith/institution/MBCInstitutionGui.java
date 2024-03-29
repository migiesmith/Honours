package com.migie.smith.institution;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.migie.smith.MBCHelper;

import agent.auctionSolution.dataObjects.Depot;
import agent.auctionSolution.dataObjects.VisitData;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class MBCInstitutionGui extends JFrame {

	// The panel containing all other gui components
	private JPanel contentPane;
	// Spinner used to modify costing values
	private JSpinner spinnerMultiplier;
	// Buttons to toggle pausing of the entire system
	private JButton btnDone, btnEdit;
	// Reference to the institution owning this GUI
	private MBCInstitution institution;
	// List for displaying and selecting available visits
	private JList<String> visitList;
	// Panel used for rendering the visits
	JPanel visitPanel;
	// The currently selected visit
	private VisitData selectedVisit;
	
	public void paint(Graphics g) {
		super.paint(g);
		
		// Render the visits to visitPanel
		List<VisitData> allVisits = institution.getVisits();
		if(allVisits != null){
			
			if(selectedVisit == null)
				selectedVisit = allVisits.get(0);
			
			Depot depot = new Depot();
			depot.x = selectedVisit.x;
			depot.y = selectedVisit.y;
			MBCHelper.drawVisits(visitPanel, allVisits, depot, new VisitData(), institution.getAvailableVisits(), institution.getCostingInformation(), null, null, 0);

		}
		
	}

	/**
	 * Updates the visitList to display the correct information
	 */
	public void updateSelections(){
		// Add all available visits to the list model
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		if(institution.getAvailableVisits() != null){
			for(int i = 0; i < institution.getAvailableVisits().size(); i++){
				listModel.add(i, institution.getAvailableVisits().get(i).name);
			}
		}
		// Update the selected position
		visitList.setModel(listModel);
	}
	
	/**
	 * Create the frame.
	 */
	public MBCInstitutionGui(final MBCInstitution institution) {
		setTitle("Institution");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 640, 480);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		// Set the reference to the institution
		this.institution = institution;
		
		
		
		// Initialise the GUI (created using WindowBuilder in eclipse)
		
		visitPanel = new JPanel();
		visitPanel.setToolTipText("View of all visits. Greyed out visits have been won and cannot be modified. Red outline indicates a multiplier between 0 and 1. Blue outline indicates a multiplier greater than 1");
		visitPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				List<VisitData> allVisits = institution.getVisits();
				if(allVisits != null){					
					if(selectedVisit == null)
						selectedVisit = allVisits.get(0);
					
					Depot depot = new Depot();
					depot.x = selectedVisit.x;
					depot.y = selectedVisit.y;
					VisitData newSelection = MBCHelper.visitAtPosition(e.getX(), e.getY(), visitPanel, allVisits, depot, institution.getAvailableVisits());
					if(newSelection != null){
						selectedVisit = allVisits.get(MBCHelper.visitPosInList(allVisits, newSelection));
						spinnerMultiplier.setValue(institution.getCostingInformation().get(MBCHelper.visitPosInList(institution.getVisits(), selectedVisit)));
						repaint();
					}
				}
			}
		});
		
		JLabel lblVisitList = new JLabel("Visits:");
		lblVisitList.setToolTipText("All visits that are yet to be bidded on. Ordered in terms of their occurence");
		
		JLabel lblMultiplier = new JLabel("Multiplier:");
		lblMultiplier.setToolTipText("The multiplier that will be applied to the profit of the winner");
		
		spinnerMultiplier = new JSpinner();
		spinnerMultiplier.setToolTipText("The multiplier that will be applied to the profit of the winner");
		spinnerMultiplier.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(selectedVisit != null)
					institution.getCostingInformation().set(MBCHelper.visitPosInList(institution.getVisits(), selectedVisit), Double.valueOf(spinnerMultiplier.getValue().toString()));
				repaint();
			}
		});
		spinnerMultiplier.setModel(new SpinnerNumberModel(1.0d, 0.0, Double.MAX_VALUE, 0.1));
		
		JScrollPane scrollPane = new JScrollPane();
		
		btnDone = new JButton("Done For Now");
		btnDone.setToolTipText("Stop editting the multipliers and allow the auction to resume");
		btnDone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnDone.setEnabled(false);
				btnEdit.setEnabled(true);
				selectedVisit = null;
				institution.setPaused(false);
			}
		});
		
		btnEdit = new JButton("Edit");
		btnEdit.setToolTipText("Pause the auction and modify the multiplier of visits");
		btnEdit.setEnabled(false);
		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnDone.setEnabled(true);
				btnEdit.setEnabled(false);
				selectedVisit = null;
				institution.setPaused(true);
			}
		});
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addComponent(visitPanel, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(btnEdit, GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
						.addComponent(btnDone, GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
						.addComponent(scrollPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
						.addComponent(lblVisitList)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(lblMultiplier, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(spinnerMultiplier, GroupLayout.PREFERRED_SIZE, 87, GroupLayout.PREFERRED_SIZE)))
					.addGap(14))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addComponent(visitPanel, GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)
				.addGroup(Alignment.LEADING, gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMultiplier)
						.addComponent(spinnerMultiplier, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblVisitList)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 147, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(btnDone)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(btnEdit)
					.addContainerGap(149, Short.MAX_VALUE))
		);
		
		visitList = new JList<String>();
		visitList.setToolTipText("All visits that are yet to be bidded on. Ordered in terms of their occurence");
		visitList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(visitList.getSelectedIndex() != -1){
					int index = MBCHelper.visitPosInList(institution.getVisits(), institution.getAvailableVisits().get(visitList.getSelectedIndex()));
					selectedVisit = institution.getVisits().get(index);
				}else{
					selectedVisit = null;
				}
				spinnerMultiplier.setValue(1.0d);
				repaint();
			}
		});
		scrollPane.setViewportView(visitList);
		visitList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		contentPane.setLayout(gl_contentPane);
		
		setVisible(true);
		
	}
	
}
