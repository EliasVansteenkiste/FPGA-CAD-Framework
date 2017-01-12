package place.visual;

import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Map;

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
    private final Color hardBlockColor = new Color(255, 255, 255, 50);

    private transient Placement placement;

    private int blockSize;
    private int left, top;

    PlacementPanel(Logger logger) {
        this.logger = logger;
    }

    void setPlacement(Placement placement) {
        this.placement = placement;

        // Redraw the panel
        super.repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(this.placement != null) {
            this.setDimensions();

            this.drawGrid(g);

            this.drawBlocks(g);
        }
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
    }


    private void drawGrid(Graphics g) {
        int circuitWidth = this.placement.getWidth() + 2;
        int circuitHeight = this.placement.getHeight() + 2;

        int left = this.left;
        int top = this.top;

        int right = left + this.blockSize * circuitWidth;
        int bottom = top + this.blockSize * circuitHeight;

        g.setColor(this.gridColorLight);
        for(int x = left; x <= right; x += this.blockSize) {
        	if(x == left || x == right){
        		 g.drawLine(x, top + this.blockSize, x, bottom - this.blockSize);
        	}else if( (x-left)/this.blockSize % 10 == 1){
        		g.setColor(this.gridColorDark);
        		g.drawLine(x, top, x, bottom);
        		g.setColor(this.gridColorLight);
         	}else{
         		g.drawLine(x, top, x, bottom);
        	}
           
        }
        for(int y = top; y <= bottom; y += this.blockSize) {
        	if(y == top || y == bottom){
        		g.drawLine(left + this.blockSize, y, right - this.blockSize, y);
        	}else if( (y-top)/this.blockSize % 10 == 1){
        		g.setColor(this.gridColorDark);
        		g.drawLine(left, y, right, y);
        		g.setColor(this.gridColorLight);
        	}else{
        		g.drawLine(left, y, right, y);
        	}
            
        }
    }

    private void drawBlocks(Graphics g) {
        for(Map.Entry<GlobalBlock, Coordinate> blockEntry : this.placement.blocks()) {
            this.drawBlock(blockEntry.getKey(), blockEntry.getValue(), g);
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
}
