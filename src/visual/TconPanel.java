package visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import circuit.Net;

public class TconPanel extends JPanel {

	Tcon tcon;
	Vector<JCheckBox> checkBoxScheme;
	Vector<Boolean> checkedScheme;
	Vector<Pattern> schemes;

	Vector<JCheckBox> checkBoxNet;
	Vector<Boolean> checkedNet;
	Vector<Net> nets;

	ItemListener itemListener;

	public TconPanel(ItemListener itemListener) {
		super();
		this.itemListener = itemListener;
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setAlignmentX(Component.LEFT_ALIGNMENT);

	}

	public void setTcon(Tcon tcon) {
		this.tcon = tcon;
		this.removeAll();

		this.add(new JLabel(tcon.name));
		checkBoxScheme = new Vector<JCheckBox>();
		checkedScheme = new Vector<Boolean>();
		schemes = new Vector<Pattern>();

		checkBoxNet = new Vector<JCheckBox>();
		checkedNet = new Vector<Boolean>();
		nets = new Vector<Net>();

		for (Pattern scheme : tcon.pattern) {
			JCheckBox button = new JCheckBox(scheme.toString());
			button.setSelected(false);
			button.setAlignmentX(Component.LEFT_ALIGNMENT);
			button.addItemListener(this.itemListener);
			this.add(button);
			checkBoxScheme.add(button);
			schemes.add(scheme);
			checkedScheme.add(new Boolean(false));

			JPanel schemePanel = new JPanel();
			BoxLayout schemeLayout = new BoxLayout(schemePanel,
					BoxLayout.PAGE_AXIS);
			schemePanel.setLayout(schemeLayout);
			for (Net net : scheme.nets) {
				JCheckBox netCheckBox = new JCheckBox(net.name);
				netCheckBox.setSelected(false);
				netCheckBox.addItemListener(this.itemListener);

				JPanel netPanel = new JPanel();
				BoxLayout netLayout = new BoxLayout(netPanel,
						BoxLayout.PAGE_AXIS);
				netPanel.setLayout(netLayout);

				// netPanel.add(Box.createRigidArea(new Dimension(10,0)));
				netPanel.add(netCheckBox);
				checkBoxNet.add(netCheckBox);
				nets.add(net);
				checkedNet.add(new Boolean(false));

				schemePanel.add(netPanel);
			}
			this.add(schemePanel);
		}

		this.validate();
		this.repaint();
	}

	public Vector<Boolean> getSelection() {
		return checkedScheme;
	}

	public Dimension getPreferredSize() {
		return new Dimension(300, 900);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	void setChecked(ItemEvent e) {
		int index = checkBoxScheme.indexOf(e.getItemSelectable());
		if (index != -1) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				checkedScheme.set(index, true);
			} else {
				checkedScheme.set(index, false);
			}
		} else {
			index = checkBoxNet.indexOf(e.getItemSelectable());
			System.out.println(index);
			if (index != -1) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					checkedNet.set(index, true);
				} else {
					checkedNet.set(index, false);
				}
			}
		}
	}

	public Vector<Pattern> selectedSchemes() {
		Vector<Pattern> result = new Vector<Pattern>();
		for (int i = 0; i < checkedScheme.size(); i++) {
			if (checkedScheme.get(i)) {
				result.add(schemes.get(i));
			}
		}
		return result;
	}

	public Vector<Net> selectedNets() {
		Vector<Net> result = new Vector<Net>();
		for (int i = 0; i < checkedNet.size(); i++) {
			if (checkedNet.get(i)) {
				result.add(nets.get(i));
			}
		}
		return result;
	}

}
