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

    private final Color gridColor = new Color(50, 50, 50);
    private final Color clbColor = new Color(255, 0, 0, 25);
    private final Color ioColor = new Color(0, 0, 255, 25);
    private final Color hardBlockColor = new Color(0, 255, 0, 25);

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

        int circuitWidth = this.placement.getWidth();
        int circuitHeight = this.placement.getHeight();

        this.blockSize = Math.min((maxWidth - 1) / circuitWidth, (maxHeight - 1) / circuitHeight);

        int width = circuitWidth * this.blockSize + 1;
        int height = circuitHeight * this.blockSize + 1;

        this.left = (maxWidth - width) / 2;
        this.top = (maxHeight - height) / 2;
    }


    private void drawGrid(Graphics g) {
        int circuitWidth = this.placement.getWidth();
        int circuitHeight = this.placement.getHeight();

        int left = this.left;
        int top = this.top;

        int right = left + this.blockSize * circuitWidth;
        int bottom = top + this.blockSize * circuitHeight;

        g.setColor(this.gridColor);
        for(int x = left; x <= right; x += this.blockSize) {
            g.drawLine(x, top, x, bottom);
        }
        for(int y = top; y <= bottom; y += this.blockSize) {
            g.drawLine(left, y, right, y);
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

            case HARDBLOCK:
                color = this.hardBlockColor;
                break;

            case CLB:
                color = this.clbColor;
                break;

            default:
                try {
                    throw new InvalidBlockCategoryException(block);
                } catch(InvalidBlockCategoryException error) {
                    this.logger.raise(error);
                }
                color = null;
        }

        g.setColor(color);
        g.fillRect(left, top, size, size);
    }
}
