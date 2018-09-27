package route.route;

import java.util.Comparator;
import java.util.Map;


public class FanoutComparator implements Comparator<Connection> {
	Map<Connection, Integer> base;
	
	public FanoutComparator(Map<Connection, Integer> base) {
		this.base = base;
	}
	
	public int compare(Connection a, Connection b) {
		if(this.base.get(a).intValue() < this.base.get(b).intValue()){
			return 1;
		}else if(this.base.get(a).intValue() == this.base.get(b).intValue()){
			if(a.boundingBox > b.boundingBox){
				return 1;
			}else if(a.boundingBox == b.boundingBox){
				if(a.hashCode() > b.hashCode()){
					return 1;
				}else if(a.hashCode() < b.hashCode()){
					return -1;
				}else{
					if(a != b) System.out.println("Failure: Error while comparing 2 connections. HashCode of Two Connections was identical");
					return 0;
				}
			}else{
				return -1;
			}
		}else{
			return -1;
		}
	}
}
