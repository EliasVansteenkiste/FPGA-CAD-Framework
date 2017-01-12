package place.visual;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PlacementVisualizer {

    private Logger logger;

    private JFrame frame;
    private JLabel placementLabel;
    private PlacementPanel placementPanel;

    private boolean enabled = false;
    private Circuit circuit;

    private int currentPlacement;
    private BlockType currentType;
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
    public void addPlacement(String name, Map<GlobalBlock, NetBlock> blockIndexes, int[] x, int[] y, HashMap<BlockType,ArrayList<int[]>> legalizationAreas) {
        if(this.enabled) {
            this.placements.add(new Placement(name, this.circuit, blockIndexes, x, y, legalizationAreas));
        }
    }
    public void addPlacement(String name, Map<GlobalBlock, NetBlock> blockIndexes, double[] x, double[] y, HashMap<BlockType,ArrayList<int[]>> legalizationAreas) {
        if(this.enabled) {
            this.placements.add(new Placement(name, this.circuit, blockIndexes, x, y, legalizationAreas));
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
        
        //Legalization buttons
        JPanel legalizationPanel = new JPanel();
        pane.add(legalizationPanel, BorderLayout.PAGE_END);
        JButton legalisationButton = new JButton("None");
        legalisationButton.addActionListener(new LegalizationActionListener(this, null));
        legalizationPanel.add(legalisationButton, BorderLayout.CENTER);
        for(BlockType type:this.circuit.getGlobalBlockTypes()){
        	if(this.placements.get(1).getLegalizationAreas().containsKey(type)){	
                legalisationButton = new JButton(type.getName());
                legalisationButton.addActionListener(new LegalizationActionListener(this, type));
                legalizationPanel.add(legalisationButton, BorderLayout.CENTER);
        	}
        }

        this.placementPanel = new PlacementPanel(this.logger);
        pane.add(this.placementPanel);

        this.drawPlacement(this.placements.size() - 1);
    }

    private void drawPlacement(int index) {
        this.currentPlacement = index;

        Placement placement = this.placements.get(index);
        this.placementLabel.setText(placement.getName());
        this.placementPanel.setLegalAreaBlockType(this.currentType);
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
    
    void drawLegalizationAreas(BlockType type) {
    	this.currentType = type;
        this.drawPlacement(this.currentPlacement);
    }

    private class LegalizationActionListener implements ActionListener {

        private PlacementVisualizer vizualizer;
        private BlockType drawLegalAreaBlockType;

        LegalizationActionListener(PlacementVisualizer vizualizer, BlockType blockType) {
            this.drawLegalAreaBlockType = blockType;
            this.vizualizer = vizualizer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.vizualizer.drawLegalizationAreas(this.drawLegalAreaBlockType);
        }
    }

    private class NavigateActionListener implements ActionListener {

        private PlacementVisualizer vizualizer;
        private int step;

        NavigateActionListener(PlacementVisualizer vizualizer, int step) {
            this.step = step;
            this.vizualizer = vizualizer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.vizualizer.navigate(this.step);
        }
    }
}
