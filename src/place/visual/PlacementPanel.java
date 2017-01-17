package place.visual;

import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class PlacementPanel extends JPanel {
    private static final long serialVersionUID = -2621200118414334047L;

    private Logger logger;

    private final Color gridColorLight = new Color(150, 150, 150);
    private final Color gridColorDark = new Color(0, 0, 0);
    private final Color clbColor = new Color(255, 0, 0, 50);
    private final Color macroColor = new Color(100, 0, 0, 50);
    private final Color ioColor = new Color(0, 0, 255, 50);
    private final Color dspColor = new Color(0, 255, 0, 50);
    private final Color m9kColor = new Color(255, 255, 0, 50);
    private final Color m144kColor = new Color(0, 255, 255, 50);
    private final Color hardBlockColor = new Color(0, 0, 0, 50);

    private transient Placement placement;

    private int blockSize;
    private int left, top, right, bottom;
    private BlockType drawLegalAreaBlockType;
    
    private boolean mouseEnabled = false, plotEnabled = false;
    private double[] bbCost;

    PlacementPanel(Logger logger) {
        this.logger = logger;
    }

    void setPlacement(Placement placement) {
        this.placement = placement;

        // Redraw the panel
        super.repaint();
    }
    void setLegalAreaBlockType(BlockType blockType){
    	this.drawLegalAreaBlockType = blockType;
    }
    void setMouseEnabled(boolean mouseEnabled){
    	this.mouseEnabled = mouseEnabled;
    }
    void setPlotEnabled(boolean plotEnabled, double[] bbCost){
    	this.plotEnabled = plotEnabled;
    	this.bbCost = bbCost;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(this.placement != null) {
        	if(!this.plotEnabled){
        		this.setDimensions();
        		this.drawGrid(g);
        		this.drawBlocks(g);
        		this.drawLegalizationAreas(g);
        		if(this.mouseEnabled)this.drawBlockInformation(g);
        	}else{
        		this.drawPlot(g);
        	}
        }
    }
    
    private void drawPlot(Graphics g){
    	int iteration = this.getIteration();

    	double alpha = 0.2;
    	double left = this.getWidth() * alpha;
    	double top = this.getHeight() * alpha;
    	double right = this.getWidth() * (1-alpha);
    	double bottom =  this.getHeight() * (1-alpha);

    	double maxbbcost = 0.0;
    	for(double bbCost:this.bbCost){
    		maxbbcost = Math.max(bbCost, maxbbcost);
    	}
    	
    	double graphWidth = this.bbCost.length;
    	double graphHeight = maxbbcost;

    	int numLabels = 10;
    	int labelWidthY = (int)Math.ceil(graphHeight / numLabels / 50) * 50;
    	int labelWidthX = (int)Math.ceil(graphWidth / numLabels / 5) * 5;

    	graphHeight = Math.ceil(graphHeight / labelWidthY) * labelWidthY;
    	graphWidth = Math.ceil(graphWidth /labelWidthX) * labelWidthX;

    	double scaleX = (right - left) / graphWidth;
    	double scaleY = (bottom - top) / graphHeight;
   
    	//create x and y axes
    	g.setColor(Color.BLACK);
    	FontMetrics metrics = g.getFontMetrics();
    	this.drawLine(g, left, bottom+1, right + metrics.getHeight()/2, bottom+1);
    	this.drawLine(g, left, bottom, right + metrics.getHeight()/2, bottom);
    	this.drawLine(g, left-1, top - metrics.getHeight()/2, left-1, bottom);
    	this.drawLine(g, left, top - metrics.getHeight()/2, left, bottom);

    	//create hatch marks for y axis. 
    	double y = bottom - labelWidthY * scaleY;
    	int label = labelWidthY;
    	while(y >= top-1){
    		this.drawLine(g, left, y, left+5, y);

    		String yLabel = label + "";
    		int labelWidth = metrics.stringWidth(yLabel);
    		int labelHeight = metrics.getHeight();
    		this.drawString(g, yLabel, left - labelWidth - 10, y + labelHeight / 4);

    		y -= labelWidthY * scaleY;
    		label += labelWidthY;
    	}

    	//create hatch marks for x axis.
    	double x = left + labelWidthX * scaleX;
    	label = labelWidthX;
    	while(x <= right){
    		this.drawLine(g, x, bottom, x, bottom-5);

    		String xLabel = label + "";
    		int labelWidth = metrics.stringWidth(xLabel);
    		int labelHeight = metrics.getHeight();
    		this.drawString(g, xLabel, x - labelWidth / 2, bottom + labelHeight / 2 + 10);

    		x += labelWidthX * scaleX;
    		label += labelWidthX;
    	}

    	//DRAW PLOT
    	g.setColor(Color.BLUE);
    	for(int i = 0; i < this.bbCost.length - 2; i+=2) {
    		this.drawLine(g, left + i * scaleX, bottom - this.bbCost[i] * scaleY, left + (i + 2) * scaleX, bottom - this.bbCost[i+2] * scaleY);
    	}
    	for(int i = 1; i < this.bbCost.length - 2; i+=2) {
    		this.drawLine(g, left + i * scaleX, bottom - this.bbCost[i] * scaleY, left + (i + 2) * scaleX, bottom - this.bbCost[i+2] * scaleY);
    	}

    	int d = 6;
    	for(int i = 0; i < this.bbCost.length; i+=1) {
    		if(iteration == i)g.setColor(Color.RED);
    		g.fillOval((int)Math.round(left + i * scaleX) - d/2, (int)Math.round(bottom - this.bbCost[i] * scaleY) - d/2, d, d);
    		if(iteration == i)g.setColor(Color.BLUE);
    	}
    }
    private int getIteration(){
    	String name = this.placement.getName();
    	if(name.contains("iteration") && name.contains("legal")){
    		return Integer.parseInt(name.split("iteration ")[1].split(": legal")[0]) * 2 + 1;
    	}else if(name.contains("iteration") && name.contains("linear")){
    		return Integer.parseInt(name.split("iteration ")[1].split(": linear")[0]) * 2;
    	}else{
    		return -1;
    	}
    }
    private void drawLine(Graphics g, double x1, double y1, double x2, double y2){
    	g.drawLine((int)Math.round(x1), (int)Math.round(y1), (int)Math.round(x2), (int)Math.round(y2));
    }
    private void drawString(Graphics g, String s, double x, double y){
    	g.drawString(s, (int)Math.round(x), (int)Math.round(y));
    }

    private void setDimensions() {
        int maxWidth = this.getWidth();
        int maxHeight = this.getHeight();

        int circuitWidth = this.placement.getWidth() + 2;
        int circuitHeight = this.placement.getHeight() + 2;

        this.blockSize = Math.min((maxWidth - 1) / circuitWidth, (maxHeight - 1) / circuitHeight);

        int width = circuitWidth * this.blockSize + 1;
        int height = circuitHeight * this.blockSize + 1;

        this.left = (maxWidth - width) / 2;
        this.top = (maxHeight - height) / 2;
        
        this.right = this.left + this.blockSize * circuitWidth;
        this.bottom = this.top + this.blockSize * circuitHeight;
    }


    private void drawGrid(Graphics g) {
        g.setColor(this.gridColorLight);
        for(int x = this.left; x <= this.right; x += this.blockSize) {
        	if(x == this.left || x == this.right){
        		 g.drawLine(x, this.top + this.blockSize, x, this.bottom - this.blockSize);
        	}else if( (x-this.left)/this.blockSize % 10 == 1){
        		g.setColor(this.gridColorDark);
        		g.drawLine(x, this.top, x, this.bottom);
        		g.setColor(this.gridColorLight);
         	}else{
         		g.drawLine(x, this.top, x, this.bottom);
        	}
        }
        for(int y = this.top; y <= this.bottom; y += this.blockSize) {
        	if(y == this.top || y == this.bottom){
        		g.drawLine(this.left + this.blockSize, y, this.right - this.blockSize, y);
        	}else if( (y-this.top)/this.blockSize % 10 == 1){
        		g.setColor(this.gridColorDark);
        		g.drawLine(this.left, y, this.right, y);
        		g.setColor(this.gridColorLight);
        	}else{
        		g.drawLine(this.left, y, this.right, y);
        	}
        }
    }

    private void drawBlocks(Graphics g) {
        for(Map.Entry<GlobalBlock, Coordinate> blockEntry : this.placement.blocks()) {
            this.drawBlock(blockEntry.getKey(), blockEntry.getValue(), g);
        }
    }
    
    private void drawLegalizationAreas(Graphics g) {
    	if(this.placement.getLegalizationAreas() != null){
        	g.setColor(this.gridColorDark);
        	if(this.drawLegalAreaBlockType != null){
            	for(BlockType type:this.placement.getLegalizationAreas().keySet()){
            		if(type.equals(this.drawLegalAreaBlockType)){
                    	for(int[] area: this.placement.getLegalizationAreas().get(type)){
                        	int top = this.top + this.blockSize * area[0];
                        	int bottom = this.top + this.blockSize * area[1];
                        	int left = this.left + this.blockSize * area[2];
                        	int right = this.left + this.blockSize * area[3];
                        	
                        	for(int i = -1;i<=1;i++){
                        		g.drawLine(left-i, top+i, left-i, bottom-i);
                        		g.drawLine(right+i, top+i, right+i, bottom-i);
                        		g.drawLine(left-i, top+i, right+i, top+i);
                        		g.drawLine(left+i, bottom+i, right-i, bottom+i);
                        	}
                    	}
            		}
            	}
        	}
    	}
    }

    private void drawBlock(GlobalBlock block, Coordinate coordinate, Graphics g) {
        int left = (int) (this.left + 1 + this.blockSize * coordinate.getX());
        int top = (int) (this.top + 1 + this.blockSize * coordinate.getY());
        int size = this.blockSize - 1;

        Color color;
        switch(block.getCategory()) {
            case IO:
                color = this.ioColor;
                break;

            case CLB:
                color = this.clbColor;
                break;
            	
            case HARDBLOCK:
            	if(block.getType().getName().equals("DSP")){
            		color = this.dspColor;
            	}else if(block.getType().getName().equals("M9K")){
            		color = this.m9kColor;
            	}else if(block.getType().getName().equals("M144K")){
            		color = this.m144kColor;
            	}else{
            		color = this.hardBlockColor;
            	}
            	break;

            default:
                try {
                    throw new InvalidBlockCategoryException(block);
                } catch(InvalidBlockCategoryException error) {
                    this.logger.raise(error);
                }
                color = null;
        }
        if(block.isInMacro()) color = this.macroColor;
        
        g.setColor(color);
        if(block.getType().getHeight() > 1){
            for(int i=0;i<block.getType().getHeight();i++){
            	top = (int) (this.top + 1 + this.blockSize * (coordinate.getY()+i));
            	g.fillRect(left, top, size, size);
            }
        }else{
        	g.fillRect(left, top, size, size);
        }
    }
    private void drawBlockInformation(Graphics g) {
    	final MouseLabelComponent mouseLabel = new MouseLabelComponent(this);
        //add component to panel
        this.add(mouseLabel);
        mouseLabel.setBounds(0, 0, this.getWidth(), this.getHeight());
        this.addMouseMotionListener(new MouseMotionAdapter(){
        	public void mouseMoved (MouseEvent me){
        		mouseLabel.x = me.getX();
        		mouseLabel.y = me.getY();
        	}
        });
    }
    
  	private class MouseLabelComponent extends JComponent{
  	  	//x , y are from mouseLocation relative to real screen/JPanel
  	  	//coorX, coorY are FPGA's
  		private static final long serialVersionUID = 1L;
  		private int x;
  		private int y;
  		private PlacementPanel panel;
  			
  		public MouseLabelComponent(PlacementPanel panel){
  			this.panel = panel;
  		}
  			
  		protected void paintComponent(Graphics g){
  			this.drawBlockCoordinate(this.x, this.y, g);
  		}
  		
  		public void drawBlockCoordinate(int x, int y, Graphics g){
  	    	int coorX = (int)(x-this.panel.left)/this.panel.blockSize;
  	    	int coorY = (int)(y-this.panel.top)/this.panel.blockSize;
  	    	if(this.onScreen(coorX, coorY)){
  	    		String s = "[" + coorX + "," + coorY + "]";
  	    		GlobalBlock globalBlock = this.getGlobalBlock(coorX, coorY);
  	    		if(globalBlock != null){
  	    			s += " " + globalBlock.getName();
  	    		}
  	        	int fontSize = 20;
  	      		g.setFont(new Font("TimesRoman", Font.BOLD, fontSize));
  	    		g.setColor(Color.BLUE);
  	    		g.drawString(s, x, y);
  	    	}
  	    }
  	  	public GlobalBlock getGlobalBlock(int x, int y){
  	        for(Map.Entry<GlobalBlock, Coordinate> blockEntry : this.panel.placement.blocks()) {
  	        	Coordinate blockCoor = blockEntry.getValue();
  	        	
  	        	if(Math.abs(blockCoor.getX() - x) < 0.25 && Math.abs(blockCoor.getY() - y) < 0.25){
  	        		return blockEntry.getKey();
  	        	}
  	        }
  	        return null;      
  	    }
  	    public boolean onScreen(int x, int y){
  	    	return (x > 0 && y > 0 && x < this.panel.placement.getWidth()+2 && y < this.panel.placement.getHeight()+2);
  	    }
  	}
}
