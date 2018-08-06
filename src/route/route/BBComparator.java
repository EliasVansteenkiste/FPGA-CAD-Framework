package route.route;

import java.util.Comparator;
import java.util.Map;


public class BBComparator implements Comparator<Connection> {
	Map<Connection, Integer> base;
	
	public BBComparator(Map<Connection, Integer> base) {
		this.base = base;
	}
	
	public int compare(Connection a, Connection b) {
		if(this.base.get(a) > this.base.get(b)){
			return 1;
		}else if(this.base.get(a) == this.base.get(b)){
			if(a.net.fanout > b.net.fanout){
				return 1;
			}else if(a.net.fanout == b.net.fanout){
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
