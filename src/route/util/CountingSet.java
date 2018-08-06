package route.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * MultiSet that counts how many of each object are present. Implemented using a
 * HashMap<N,Integer>. Only objects that are present once or more are stored in
 * the Map.
 */
public class CountingSet<N> implements Collection<N> {
	
	private Map<N,Integer> map;
	private int size;

	public CountingSet() {
		this.map = new HashMap<>();
		this.size = 0;
	}
	
	@Override
	public int size() {
		return this.size;
	}

	@Override
	public boolean isEmpty() {
		return this.size == 0;
	}

	@Override
	public boolean contains(Object o) {
		return this.map.containsKey(o);
	}

	@Override
	public Iterator<N> iterator() {
		throw new RuntimeException();
	}

	@Override
	public Object[] toArray() {
		throw new RuntimeException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new RuntimeException();
	}

	@Override
	public boolean add(N e) {
		Integer count = this.map.get(e);
		if (count != null) {
			this.map.put(e, count + 1);
		} else {
			this.map.put(e, 1);
		}
		this.size++;
		
		return true;
	}

	@Override
	public boolean remove(Object o) {
		@SuppressWarnings("unchecked")
		N key = (N)o;
		
		Integer count = this.map.get(key);
		if (count == null) {
			return false;
		}
		
		this.size--;
		
		if (count == 1) {
			this.map.remove(key);
		} else {
			this.map.put(key, count - 1);
		}
		
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new RuntimeException();
	}

	@Override
	public boolean addAll(Collection<? extends N> c) {
		throw new RuntimeException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new RuntimeException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new RuntimeException();
	}

	@Override
	public void clear() {
		this.map.clear();
		this.size = 0;
	}
	
	public int count(N n) {
		Integer count = this.map.get(n);
		if(count == null) {
			return 0;
		} else {
			return count;
		}
	}

	public int uniqueSize() {
		return this.map.size();
	}
}
