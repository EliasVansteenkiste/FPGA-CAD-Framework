package place.placers.analytical;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.DelayTables;
import place.circuit.block.GlobalBlock;
import place.circuit.timing.TimingGraph;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNetBlock;

class CriticalityCalculator{

    private int width, height;

    private TimingGraph timingGraph;
    private DelayTables delayTables;
    private BlockCategory[] blockCategories;
    
    private double iotoio;
    private double iotoclb;
    private double clbtoio;
    private double clbtoclb;

    private List<TimingNet> nets;

    CriticalityCalculator(
            Circuit circuit,
            Map<GlobalBlock, NetBlock> netBlocks,
            List<TimingNet> nets) {

        this.width = circuit.getWidth();
        this.height = circuit.getHeight();

        this.timingGraph = circuit.getTimingGraph();
        this.delayTables = circuit.getArchitecture().getDelayTables();

        this.blockCategories = new BlockCategory[netBlocks.size()];
        for(Map.Entry<GlobalBlock, NetBlock> blockEntry : netBlocks.entrySet()) {
            BlockCategory category = blockEntry.getKey().getCategory();
            int index = blockEntry.getValue().blockIndex;

            this.blockCategories[index] = category;
        }

        this.nets = nets;
        
        this.initializeAvarageDelay();
    }
    private void initializeAvarageDelay(){
        BlockCategory clb = BlockCategory.CLB;
        BlockCategory io = BlockCategory.IO;
        this.clbtoclb = calculateAverageDelay(clb,clb);
        this.clbtoio = calculateAverageDelay(clb, io);
        this.iotoclb = calculateAverageDelay(io, clb);
        this.iotoio = calculateAverageDelay(io, io);

        System.out.printf("Average delay\n");
        System.out.printf("\tCLB -> CLB\t%2.2e\n", this.clbtoclb);
        System.out.printf("\tCLB -> IO\t%2.2e\n", this.clbtoio);
        System.out.printf("\tIO -> CLB\t%2.2e\n", this.iotoclb);
        System.out.printf("\tIO -> IO\t%2.2e\n\n", this.iotoio);

    }
    private double calculateAverageDelay(BlockCategory sourceCategory, BlockCategory sinkCategory){
        List<Double> elements = new ArrayList<>();

        int width = this.width;
        int height = this.height;

        if(sourceCategory.equals(BlockCategory.IO)){
        	width += 1;
        	height += 1;
        }
        if(sinkCategory.equals(BlockCategory.IO)){
        	width += 1;
        	height += 1;
        }

        for(int deltaX = 0; deltaX < width; deltaX++){
        	for(int deltaY = 0; deltaY < height; deltaY++){
        		if(deltaX + deltaY > 0){
        			double delay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);
        			if(delay > 0){
        				double unitDelay = delay / (deltaX + deltaY);
        				elements.add(unitDelay);
        			}
        		}
        	}
        }
        return average(elements);
    }
    private double average(List<Double> list){
    	double sum = 0.0;
    	for(double element:list){
    		sum += element;
    	}
    	return sum / list.size();
    }

    public void calculate(int[] x, int[] y) {
        this.updateDelays(x, y);
        this.timingGraph.calculateCriticalities(false);
    }

    private void updateDelays(int[] x, int[] y) {
        for(TimingNet net : this.nets) {
            int sourceIndex = net.source.blockIndex;
            BlockCategory sourceCategory = this.blockCategories[sourceIndex];

            int sourceX = x[sourceIndex];
            int sourceY = y[sourceIndex];

            for(TimingNetBlock sink : net.sinks) {
                int sinkIndex = sink.blockIndex;
                BlockCategory sinkCategory = this.blockCategories[sinkIndex];

                int sinkX = x[sinkIndex];
                int sinkY = y[sinkIndex];

                int deltaX = Math.abs(sinkX - sourceX);
                int deltaY = Math.abs(sinkY - sourceY);

                double wireDelay = (deltaX  + deltaY) * this.getUnitDelay(sourceCategory, sinkCategory);
                //double wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);
                sink.timingEdge.setWireDelay(wireDelay);
            }
        }
    }
    private double getUnitDelay(BlockCategory sourceCategory, BlockCategory sinkCategory){
    	if(sourceCategory.equals(BlockCategory.IO)){
    		if(sinkCategory.equals(BlockCategory.IO)){
    			return this.iotoio;
    		}else{
    			return this.iotoclb;
    		}
    	}else{
    		if(sinkCategory.equals(BlockCategory.IO)){
    			return this.clbtoio;
    		}else{
    			return this.clbtoclb;
    		}
    	}
    }
}
