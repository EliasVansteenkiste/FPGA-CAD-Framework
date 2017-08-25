package place.placers.analytical;

import place.placers.analytical.GradientLegalizer.LegalizerBlock;
import place.util.TimingTree;

class PushingSpreader {
	private int iteration;

	private LegalizerBlock[] blocks;

	private final int width, height;
	private final int gridWidth, gridHeight;
    private final float[][] massMap;

    PushingSpreader(
    		int width, 
    		int height,
    		TimingTree timingTree){

    	this.width = width;
    	this.height = height;
    	
    	this.gridWidth = (this.width + 2) * 2;
    	this.gridHeight = (this.height + 2) * 2;

    	this.massMap = new float[this.gridWidth][this.gridHeight];
    }

    protected void doSpreading(LegalizerBlock[] blocks, int iterations, double gridForce) {
    	this.blocks = blocks;

    	this.trimToSize();
    	
    	this.initializeBlocks();
    	this.initializeMassMap();
    	
    	float overlap;
    	
    	this.iteration = 0;
    	do{
        	this.applyPushingForces(gridForce);
        	this.iteration++;
        	
        	overlap = this.calculateOverlap();
        	
    	}while(overlap > 10 && this.iteration < iterations);
    	
    	this.trimToSize();
    }
    private void initializeBlocks(){
    	for(LegalizerBlock block:this.blocks){
    		block.update();
    	}
    }
    private void initializeMassMap(){
    	for(int x = 0; x < this.gridWidth; x++){
    		for(int y = 0; y < this.gridHeight; y++){
    			this.massMap[x][y] = 0;
    		}
    	}

    	for(LegalizerBlock block:this.blocks){
    		int x = block.ceilx;
    		int y = block.ceily;
    		
    		for(int h = 0; h < block.height; h++){
        		this.massMap[x - 1][y - 1] += block.sw;
        		this.massMap[x][y - 1] += block.se + block.sw;
        		this.massMap[x + 1][y - 1] += block.se;
        		
        		this.massMap[x - 1][y] += block.sw + block.nw;
        		this.massMap[x][y] += 0.25;
        		this.massMap[x + 1][y] += block.se + block.ne;
        		
        		this.massMap[x - 1][y + 1] += block.nw;
        		this.massMap[x][y + 1] += block.nw + block.ne;
        		this.massMap[x + 1][y + 1] += block.ne;

        		y += 2;
    		}
    	}
    }

    //PUSHING GRAVITY FORCES
    private void applyPushingForces(double gridForce){
    	for(LegalizerBlock block:this.blocks){
    		float sw = 0;
        	float se = 0;
        	float nw = 0;
        	float ne = 0;

    		for(int h = 0; h < block.height; h++){
    			
    			int offset = h + h;

        		sw += block.sw * this.massMap[block.ceilxlow][block.ceilylow + offset];
        		sw += block.nw * this.massMap[block.ceilxlow][block.ceily + offset];
        		sw += block.se * this.massMap[block.ceilx][block.ceilylow + offset];
        		sw += block.ne * this.massMap[block.ceilx][block.ceily + offset];
        		
        		se += block.sw * this.massMap[block.ceilx][block.ceilylow + offset];
        		se += block.nw * this.massMap[block.ceilx][block.ceily + offset];
        		se += block.se * this.massMap[block.ceilxhigh][block.ceilylow + offset];
        		se += block.ne * this.massMap[block.ceilxhigh][block.ceily + offset];
        		
        		nw += block.sw * this.massMap[block.ceilxlow][block.ceily + offset];
        		nw += block.nw * this.massMap[block.ceilxlow][block.ceilyhigh + offset];
        		nw += block.se * this.massMap[block.ceilx][block.ceily + offset];
        		nw += block.ne * this.massMap[block.ceilx][block.ceilyhigh + offset];
        		
        		ne += block.sw * this.massMap[block.ceilx][block.ceily + offset];
        		ne += block.nw * this.massMap[block.ceilx][block.ceilyhigh + offset];
        		ne += block.se * this.massMap[block.ceilxhigh][block.ceily + offset];
        		ne += block.ne * this.massMap[block.ceilxhigh][block.ceilyhigh + offset];
    		}

    		float xval = block.horizontal.coordinate - (float)Math.floor(block.horizontal.coordinate);
    		float yval = block.vertical.coordinate - (float)Math.floor(block.vertical.coordinate);

    		float xGrid, yGrid;
    		
    		if(xval < 0.25){
    			xGrid = - xval;
    		}else if(xval < 0.75){
    			xGrid = -0.5f + xval;
    		}else{
    			xGrid = 1 - xval;
    		}
    		
    		if(yval < 0.25){
    			yGrid = - yval;
    		}else if(yval < 0.75){
    			yGrid = -0.5f + yval;
    		}else{
    			yGrid = 1 - yval;
    		}
    		
    		xGrid *= gridForce;
    		yGrid *= gridForce;

    		float mass = sw + nw + se + ne;
    		
    		float horizontalForce = (sw + nw - se - ne + xGrid) / (mass + xGrid);
    		float verticalForce = (sw - nw + se - ne + yGrid) / (mass + yGrid);

        	block.horizontal.setForce(horizontalForce);
        	block.vertical.setForce(verticalForce);

        	for(int h = 0; h < block.height; h++){
        		
        		int offset = h + h;
        		
            	this.massMap[block.ceilxlow][block.ceilylow + offset] -= block.sw;
            	this.massMap[block.ceilx][block.ceilylow + offset] -= block.se + block.sw;
            	this.massMap[block.ceilxhigh][block.ceilylow + offset] -= block.se;
            	
            	this.massMap[block.ceilxlow][block.ceily + offset] -= block.sw + block.nw;
            	this.massMap[block.ceilx][block.ceily + offset] -= 0.25;
            	this.massMap[block.ceilxhigh][block.ceily + offset] -= block.se + block.ne;
            	
            	this.massMap[block.ceilxlow][block.ceilyhigh + offset] -= block.nw;
            	this.massMap[block.ceilx][block.ceilyhigh + offset] -= block.nw + block.ne;
            	this.massMap[block.ceilxhigh][block.ceilyhigh + offset] -= block.ne;
       		}

    		//Horizontal
    		block.horizontal.solve();
    		if(block.horizontal.coordinate > this.width) block.horizontal.coordinate = this.width;
    		if(block.horizontal.coordinate < 1) block.horizontal.coordinate = 1;

    		//Vertical
    		block.vertical.solve();
    		if(block.vertical.coordinate + block.height - 1 > this.height) block.vertical.coordinate = this.height - block.height + 1;
    		if(block.vertical.coordinate < 1) block.vertical.coordinate = 1;

    		block.update();

        	for(int h = 0; h < block.height; h++){
        		
        		int offset = h + h;
        		
            	this.massMap[block.ceilxlow][block.ceilylow + offset] += block.sw;
            	this.massMap[block.ceilx][block.ceilylow + offset] += block.se + block.sw;
            	this.massMap[block.ceilxhigh][block.ceilylow + offset] += block.se;
            	
            	this.massMap[block.ceilxlow][block.ceily + offset] += block.sw + block.nw;
            	this.massMap[block.ceilx][block.ceily + offset] += 0.25;
            	this.massMap[block.ceilxhigh][block.ceily + offset] += block.se + block.ne;
            	
            	this.massMap[block.ceilxlow][block.ceilyhigh + offset] += block.nw;
            	this.massMap[block.ceilx][block.ceilyhigh + offset] += block.nw + block.ne;
            	this.massMap[block.ceilxhigh][block.ceilyhigh + offset] += block.ne;
        	}
    	}
    }

    private float calculateOverlap(){
    	float overlap = 0;
    	for(int i = 0; i < this.massMap.length; i++){
    		for(int j = 0; j < this.massMap[i].length; j++){
    			if(this.massMap[i][j] > 0.25){
    				overlap += this.massMap[i][j] - 0.25;
    			}
    		}
    	}
    	return overlap;
    }
    private void trimToSize(){
    	for(LegalizerBlock block:this.blocks){
    		if(block.horizontal.coordinate > this.width) block.horizontal.coordinate = this.width;
    		if(block.horizontal.coordinate < 1) block.horizontal.coordinate = 1;

    		if(block.vertical.coordinate + block.height - 1 > this.height) block.vertical.coordinate = this.height - block.height + 1;
    		if(block.vertical.coordinate < 1) block.vertical.coordinate = 1;
    	}
    }
}
