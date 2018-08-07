package place.placers.analytical;

public class CostAndIndexList {

	double cost;
	int index;
	int[] list;
	CostAndIndexList(double cost, int index, int[] list){
		this.cost = cost;
		this.index = index;
		this.list = list.clone();
	}
}
