package visual;

import interfaces.Logger;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import circuit.Circuit;
import circuit.block.GlobalBlock;

public class PlacementVisualizer {

    private Logger logger;

    private JFrame frame;
    private JLabel placementLabel;
    private PlacementPanel placementPanel;

    private boolean enabled = false;
    private Circuit circuit;

    private int currentPlacement;
    private List<Placement> placements = new ArrayList<Placement>();


    public PlacementVisualizer(Logger logger) {
        this.logger = logger;
    }

    public void setCircuit(Circuit circuit) {
        this.enabled = true;
        this.circuit = circuit;
    }

    public void addPlacement(String name) {
        if(this.enabled) {
            this.placements.add(new Placement(name, this.circuit));
        }
    }
    public void addPlacement(String name, Map<GlobalBlock, Integer> blockIndexes, int[] x, int[] y) {
        if(this.enabled) {
            this.placements.add(new Placement(name, this.circuit, blockIndexes, x, y));
        }
    }
    public void addPlacement(String name, Map<GlobalBlock, Integer> blockIndexes, double[] x, double[] y) {
        if(this.enabled) {
            this.placements.add(new Placement(name, this.circuit, blockIndexes, x, y));
        }
    }


    public void createAndDrawGUI() {
        if(!this.enabled) {
            return;
        }

        this.addPlacement("Final placement");


        this.frame = new JFrame("Placement visualizer");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.frame.setSize(500, 450);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        this.frame.setLocation(
                (int) (screen.getWidth() - this.frame.getWidth()) / 2,
                (int) (screen.getHeight() - this.frame.getHeight()) / 2);

        this.frame.setExtendedState(this.frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        this.frame.setVisible(true);

        Container pane = this.frame.getContentPane();

        JPanel navigationPanel = new JPanel();
        pane.add(navigationPanel, BorderLayout.PAGE_START);

        this.placementLabel = new JLabel("");
        navigationPanel.add(this.placementLabel, BorderLayout.LINE_START);

        JButton previousFastButton = new JButton("<<");
        previousFastButton.addActionListener(new NavigateActionListener(this, -2));
        navigationPanel.add(previousFastButton, BorderLayout.CENTER);

        JButton previousButton = new JButton("<");
        previousButton.addActionListener(new NavigateActionListener(this, -1));
        navigationPanel.add(previousButton, BorderLayout.CENTER);

        JButton nextButton = new JButton(">");
        nextButton.addActionListener(new NavigateActionListener(this, 1));
        navigationPanel.add(nextButton, BorderLayout.CENTER);

        JButton nextFastButton = new JButton(">>");
        nextFastButton.addActionListener(new NavigateActionListener(this, 2));
        navigationPanel.add(nextFastButton, BorderLayout.CENTER);

        this.placementPanel = new PlacementPanel(this.logger);
        pane.add(this.placementPanel);

        this.drawPlacement(this.placements.size() - 1);
    }

    private void drawPlacement(int index) {
        this.currentPlacement = index;

        Placement placement = this.placements.get(index);
        this.placementLabel.setText(placement.getName());
        this.placementPanel.setPlacement(this.placements.get(index));
    }

    void navigate(int step) {
        int numPlacements = this.placements.size();

        int newIndex = (this.currentPlacement + step) % numPlacements;
        if(newIndex < 0) {
            newIndex += numPlacements;
        }

        this.drawPlacement(newIndex);
    }



    private class NavigateActionListener implements ActionListener {

        private PlacementVisualizer frame;
        private int step;

        NavigateActionListener(PlacementVisualizer frame, int step) {
            this.step = step;
            this.frame = frame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.frame.navigate(this.step);
        }
    }
}
