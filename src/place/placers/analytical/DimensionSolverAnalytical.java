package place.placers.analytical;

import java.util.HashMap;
import java.util.Map;

import place.mathtools.CGSolver;
import place.mathtools.Csr;

class DimensionSolverAnalytical {

    private final double[] coordinates;
    private final Csr matrix;
    private final double[] vector;

    private final double pseudoWeight;
    private final double epsilon;

    private boolean[] fixed;
    private Map<Integer,Integer> forwardIndexMap;
    private Map<Integer,Integer> backwardIndexMap;

    DimensionSolverAnalytical(double[] coordinates, double pseudoWeight, double epsilon, boolean[] fixed) {
        this.coordinates = coordinates;

        this.pseudoWeight = pseudoWeight;
        this.epsilon = epsilon;

        this.fixed = fixed;

        this.forwardIndexMap = new HashMap<Integer,Integer>();
        this.backwardIndexMap = new HashMap<Integer,Integer>();

        int numMovableBlocks = 0;
        for(int i = 0; i < this.fixed.length; i++){
        	if(!this.fixed[i]){
        		this.forwardIndexMap.put(i, numMovableBlocks);
        		this.backwardIndexMap.put(numMovableBlocks, i);
        		numMovableBlocks++;
        	}
        }

        this.matrix = new Csr(numMovableBlocks);
        this.vector = new double[numMovableBlocks];
    }


    void addPseudoConnection(int blockIndex, double legalCoordinate) {
        double weight = this.pseudoWeight;
        int relativeIndex = this.forwardIndexMap.get(blockIndex);

        this.matrix.addElement(relativeIndex, relativeIndex, weight);
        this.vector[relativeIndex] += weight * legalCoordinate;
    }


    void addConnection(
            boolean fixed1, int index1, double coordinate1, double offset1,
            boolean fixed2, int index2, double coordinate2, double offset2,
            double weightMultiplier) {

        double weight = weightMultiplier / Math.max(Math.abs(coordinate1 - coordinate2), 0.005);

        if(fixed1) {
            if(!fixed2) {
            	int relativeIndex2 = this.forwardIndexMap.get(index2);
            	
                this.matrix.addElement(relativeIndex2, relativeIndex2, weight);
                this.vector[relativeIndex2] += weight * (coordinate1 - offset2);
            }

        } else if(fixed2) {
        	int relativeIndex1 = this.forwardIndexMap.get(index1);

            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.vector[relativeIndex1] += weight * (coordinate2 - offset1);

        } else {
        	int relativeIndex1 = this.forwardIndexMap.get(index1);
        	int relativeIndex2 = this.forwardIndexMap.get(index2);

            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.matrix.addElement(relativeIndex1, relativeIndex2, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex1, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex2, weight);
        }
    }

    void solve() {
        this.matrix.prepareArrays();
        CGSolver solver = new CGSolver(this.matrix, this.vector);
        double[] solution = solver.solve(this.epsilon);

        for(int index:this.backwardIndexMap.keySet()){
        	this.coordinates[this.backwardIndexMap.get(index)] = solution[index];
        }
    }
}
