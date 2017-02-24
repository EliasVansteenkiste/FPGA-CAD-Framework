package place.visual;

import place.circuit.Circuit;
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
    private List<Placement> placements = new ArrayList<Placement>();
    private double[] bbCost;

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
    public void addPlacement(String name, Map<GlobalBlock, NetBlock> blockIndexes, int[] x, int[] y, double bbCost) {
        if(this.enabled) {
            this.placements.add(new Placement(name, this.circuit, blockIndexes, x, y, bbCost));
        }
    }
    public void addPlacement(String name, Map<GlobalBlock, NetBlock> blockIndexes, double[] x, double[] y, double bbCost) {
        if(this.enabled) {
            this.placements.add(new Placement(name, this.circuit, blockIndexes, x, y, bbCost));
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

        for(Placement placement:this.placements){
        	if(placement.getName().contains("linear")){
                JButton previousFastButton = new JButton("<<<");
                previousFastButton.addActionListener(new NavigateActionListener(this, -3));
                navigationPanel.add(previousFastButton, BorderLayout.CENTER);

                JButton previousButton = new JButton("<<");
                previousButton.addActionListener(new NavigateActionListener(this, -2));
                navigationPanel.add(previousButton, BorderLayout.CENTER);
                
                break;
        	}
        }
        JButton previousGradientButton = new JButton("<");
        previousGradientButton.addActionListener(new NavigateActionListener(this, -1));
        navigationPanel.add(previousGradientButton, BorderLayout.CENTER);

        JButton nextGradientButton = new JButton(">");
        nextGradientButton.addActionListener(new NavigateActionListener(this, 1));
        navigationPanel.add(nextGradientButton, BorderLayout.CENTER);

        for(Placement placement:this.placements){
        	if(placement.getName().contains("linear")){
                JButton nextButton = new JButton(">>");
                nextButton.addActionListener(new NavigateActionListener(this, 2));
                navigationPanel.add(nextButton, BorderLayout.CENTER);
                
        		JButton nextFastButton = new JButton(">>>");
                nextFastButton.addActionListener(new NavigateActionListener(this, 3));
                navigationPanel.add(nextFastButton, BorderLayout.CENTER);
                
                break;
        	}
        }
        
        JButton enableMouse = new JButton("Info");
        enableMouse.addActionListener(new MouseActionListener(this));
        navigationPanel.add(enableMouse, BorderLayout.CENTER);

        
        //BB Cost plot
        for(Placement placement:this.placements){
        	if(placement.getName().contains("linear")){
        		if(placement.hasBBCost()){
                    JButton enablePlot = new JButton("Plot");
                    enablePlot.addActionListener(new PlotActionListener(this));
                    navigationPanel.add(enablePlot, BorderLayout.CENTER);
                    
                    int bbPlacements = 0;
                    for(Placement bbPlacement:this.placements){
                    	if(bbPlacement.getName().contains("linear") || bbPlacement.getName().contains("legal")){
                    		bbPlacements += 1;
                    	}
                    }
                    this.bbCost = new double[bbPlacements];
                    
                    int i = 0;
                    for(Placement bbPlacement:this.placements){
                    	if(bbPlacement.getName().contains("linear") || bbPlacement.getName().contains("legal")){
                    		this.bbCost[i] = bbPlacement.getBBCost();
                    		i += 1;
                    	}
                    }
                }
        		break;
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
        this.placementPanel.setPlacement(this.placements.get(index));
    }

    void navigate(int type, int step) {
    	int numPlacements = this.placements.size();
    	int newIndex = this.currentPlacement;
    	
    	if(type == 1){
            newIndex = this.addStep(newIndex, step, numPlacements);
    	}else if(type == 2){
    		do{
    			newIndex = this.addStep(newIndex, step, numPlacements);
    		}while(!(this.getPlacementType(newIndex).equals(PlacementType.LEGAL) || this.getPlacementType(newIndex).equals(PlacementType.LINEAR)));
    	}else if(type == 3){
    		newIndex = this.currentPlacement;
    		PlacementType currentType = this.getPlacementType(newIndex);
    		do{
    			newIndex = this.addStep(newIndex, step, numPlacements);
    		}while(!this.getPlacementType(newIndex).equals(currentType));
    	}
    	this.drawPlacement(newIndex);
    }
    
    int addStep(int currentIndex, int step, int numPlacements){
    	int newIndex = (currentIndex + step) % numPlacements;
        if(newIndex < 0) {
            newIndex += numPlacements;
        }
        return newIndex;
    }
    
    void drawMouseInfo(boolean mouseEnabled) {
    	this.placementPanel.setMouseEnabled(mouseEnabled);
        this.drawPlacement(this.currentPlacement);
    }
    
    void drawPlot(boolean plotEnabled) {
    	this.placementPanel.setPlotEnabled(plotEnabled, this.bbCost);
    	this.drawPlacement(this.currentPlacement);
    }
    
    PlacementType getPlacementType(int index){
    	String name = this.placements.get(index).getName();
    	if(name.contains("gradient descent")){
    		return PlacementType.GRADIENT_DESCENT;
    	}else if(name.contains("gradient_expand")){
    		return PlacementType.GRADIENT_EXPAND;
    	}else if(name.contains("linear")){
    		return PlacementType.LINEAR;
    	}else if(name.contains("legal")){
    		return PlacementType.LEGAL;
    	}else if(name.contains("Final")){
    		return PlacementType.FINAL;
    	}else if(name.contains("Random")){
    		return PlacementType.RANDOM;
    	}else{
    		return  PlacementType.OTHER;
    	}
    }
    
    private enum PlacementType {
    	GRADIENT_DESCENT,
    	GRADIENT_EXPAND,
    	LINEAR,
    	LEGAL,
    	FINAL,
    	RANDOM,
    	OTHER
    }

    private class MouseActionListener implements ActionListener {

        private PlacementVisualizer vizualizer;
        private boolean mouseEnabled;

        MouseActionListener(PlacementVisualizer vizualizer) {
        	this.vizualizer = vizualizer;
            this.mouseEnabled = false;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        	this.mouseEnabled = !this.mouseEnabled;
            this.vizualizer.drawMouseInfo(this.mouseEnabled);
        }
    }
    
    private class PlotActionListener implements ActionListener {

        private PlacementVisualizer vizualizer;
        private boolean plotEnabled;

        PlotActionListener(PlacementVisualizer vizualizer) {
        	this.vizualizer = vizualizer;
            this.plotEnabled = false;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        	this.plotEnabled = !this.plotEnabled;
            this.vizualizer.drawPlot(this.plotEnabled);
        }
    }

    private class NavigateActionListener implements ActionListener {

        private PlacementVisualizer vizualizer;
        private int step;
        //1 Go to next placement
        //2 Go to next linear or legal
        //3 Go to next of same type

        NavigateActionListener(PlacementVisualizer vizualizer, int step) {
            this.step = step;
            this.vizualizer = vizualizer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.vizualizer.navigate(Math.abs(this.step), (int)Math.signum(this.step));
        }
    }
}
