package visual;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import circuit.Connection;
import circuit.PackedCircuit;

public class ConnectionPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	Connection con;
	Vector<JCheckBox> checkBoxConnection;
	Vector<Boolean> checkedConnection;
	Vector<Connection> connections;

	ItemListener itemListener;

	public ConnectionPanel(ItemListener itemListener, PackedCircuit c)
	{
		super();
		this.itemListener = itemListener;
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.add(new JLabel(c.toString()));
		checkBoxConnection = new Vector<JCheckBox>();
		checkedConnection = new Vector<Boolean>();
		connections = new Vector<Connection>();
	}

	public void setCon(Connection con) {
		this.con = con;
		this.removeAll();

		JCheckBox button = new JCheckBox(con.toString());
		button.setSelected(false);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.addItemListener(this.itemListener);
		this.add(button);
		checkBoxConnection.add(button);
		connections.add(con);
		checkedConnection.add(new Boolean(false));

		JPanel connectionPanel = new JPanel();
		BoxLayout connectionLayout = new BoxLayout(connectionPanel,
				BoxLayout.PAGE_AXIS);
		connectionPanel.setLayout(connectionLayout);

		this.add(connectionPanel);

		this.validate();
		this.repaint();
	}

	public Vector<Boolean> getSelection() {
		return checkedConnection;
	}

	public Dimension getPreferredSize() {
		return new Dimension(300, 900);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	void setChecked(ItemEvent e) {
		int index = checkBoxConnection.indexOf(e.getItemSelectable());
		if (index != -1) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				checkedConnection.set(index, true);
			} else {
				checkedConnection.set(index, false);
			}
		} else {
			index = checkedConnection.indexOf(e.getItemSelectable());
			System.out.println(index);
			if (index != -1) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					checkedConnection.set(index, true);
				} else {
					checkedConnection.set(index, false);
				}
			}
		}
	}

	public Vector<Connection> selectedConnections() {
		Vector<Connection> result = new Vector<Connection>();
		for (int i = 0; i < checkedConnection.size(); i++) {
			if (checkedConnection.get(i)) {
				result.add(connections.get(i));
			}
		}
		return result;
	}

	public Vector<Connection> nonselectedConnections() {
		Vector<Connection> result = new Vector<Connection>();
		for (int i = 0; i < checkedConnection.size(); i++) {
			if (!checkedConnection.get(i)) {
				result.add(connections.get(i));
			}
		}
		return result;
	}
}
