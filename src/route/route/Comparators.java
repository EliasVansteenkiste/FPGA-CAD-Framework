package route.route;

import java.util.Comparator;

public class Comparators {
	public static Comparator<Net> FANOUT = new Comparator<Net>() {
        @Override
        public int compare(Net n1, Net n2) {
            return n2.fanout - n1.fanout;
        }
    };
    public static Comparator<QueueElement> PRIORITY_COMPARATOR = new Comparator<QueueElement>() {
        @Override
        public int compare(QueueElement node1, QueueElement node2) {
            if(node1.cost < node2.cost) {
            	return -1;
            } else {
            	return 1;
            }
        }
    };
    
    public static Comparator<Connection>  BBComparator = new Comparator<Connection>() {
    	@Override
    	public int compare(Connection a, Connection b) {
    		if(a.boundingBox > b.boundingBox){
    			return 1;
    		}else if(a.boundingBox == b.boundingBox){
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
    };
    public static Comparator<Connection>  FanoutComparator = new Comparator<Connection>() {
    	@Override
    	public int compare(Connection a, Connection b) {
    		if(a.net.fanout < b.net.fanout){
    			return 1;
    		}else if(a.net.fanout == b.net.fanout){
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
    };
}