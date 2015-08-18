package visual;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import circuit.PackedCircuit;
import circuit.Net;
import circuit.Connection;

import architecture.old.FourLutSanitized;
import architecture.old.RouteNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import java.util.*;

public class MapViewer {
	JPanel mainPanel;

	TconPanel tconPanel;
	ConnectionPanel conPanel;

	FourLutSanitized a;
	PackedCircuit c;
	ArchitecturePanel architecturePanel;
	Tcon selectedTcon;
	Connection selectedCon;

	private void createAndShowGUI() {
		System.out.println("Created GUI on EDT? "
				+ SwingUtilities.isEventDispatchThread());
		JFrame f = new JFrame("Mapping result viewer");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setExtendedState(f.getExtendedState() | JFrame.MAXIMIZED_BOTH);

		architecturePanel = new ArchitecturePanel(700, a, c);

		architecturePanel.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				RouteNode pressedNode = architecturePanel.getNode(e.getX(),
						e.getY());
				System.out.println(pressedNode);

				architecturePanel.setNodeColor(Color.GRAY);

				if (c.conRouted) {
					for (Connection con : c.cons) {
						if (con.routeNodes.contains(pressedNode)) {
							selectedCon = con;
							conPanel.setCon(selectedCon);
						} else {
							System.out.println("yeah");
						}
					}
				} else {
					for (Tcon tcon : c.tcons.values()) {
						if (tcon.routeNodes.contains(pressedNode)) {
							selectedTcon = tcon;
							tconPanel.setTcon(selectedTcon);
						}
					}
				}

				architecturePanel.setNodeColor(selectedTcon, Color.RED);

				architecturePanel.repaint();
			}
		});

		if (c.conRouted) {
			conPanel = new ConnectionPanel(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					conPanel.setChecked(e);

					for (Connection con : conPanel.selectedConnections()) {
						architecturePanel.setNodeColor(con, Color.BLUE);
					}
					for (Connection con : conPanel.nonselectedConnections()) {
						architecturePanel.setNodeColor(con, Color.GREEN);
					}

					architecturePanel.repaint();

				}

			}, c);

			for (Connection con : conPanel.connections) {
				architecturePanel.setNodeColor(con, Color.BLUE);
			}

		} else {
			tconPanel = new TconPanel(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					tconPanel.setChecked(e);

					architecturePanel.setNodeColor(selectedTcon, Color.RED);
					for (Pattern scheme : tconPanel.selectedSchemes()) {
						architecturePanel.setNodeColor(scheme, Color.BLUE);
					}
					for (Net net : tconPanel.selectedNets()) {
						architecturePanel.setNodeColor(net, Color.GREEN);
					}

					architecturePanel.repaint();
				}

			});
		}

		mainPanel = new JPanel();
		mainPanel.add(architecturePanel);
		if (c.conRouted) {
			mainPanel.add(conPanel);
		} else {
			mainPanel.add(tconPanel);
		}

		f.setContentPane(mainPanel);
		f.setExtendedState(f.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		f.setVisible(true);

	}

	public MapViewer(FourLutSanitized a, PackedCircuit c) {
		super();
		this.a = a;
		this.c = c;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

	}
}
