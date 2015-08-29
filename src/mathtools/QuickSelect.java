package mathtools;

import java.util.Random;

/**
 * A quickselect implementation taken from Rosetta Code.
 * Quickselect is an algorithm that finds the kth smallest
 * element in an unordered list. Average performance is O(n),
 * worst case performance is O(n^2).
 * 
 * @author seppe
 *
 */

public class QuickSelect {
	
	public static int select(int[] arr, int n) {
		int left = 0;
		int right = arr.length - 1;
		Random rand = new Random();
		while (right >= left) {
			int pivotIndex = partition(arr, left, right, rand.nextInt(right - left + 1) + left);
			if (pivotIndex == n) {
				return arr[pivotIndex];
			} else if (pivotIndex < n) {
				left = pivotIndex + 1;
			} else {
				right = pivotIndex - 1;
			}
		}
		
		return -1;
	}
	 
	private static int partition(int[] arr, int left, int right, int pivot) {
		int pivotVal = arr[pivot];
		swap(arr, pivot, right);
		int storeIndex = left;
		for (int i = left; i < right; i++) {
			if (arr[i] < pivotVal) {
				swap(arr, i, storeIndex);
				storeIndex++;
			}
		}
		swap(arr, right, storeIndex);
		return storeIndex;
	}
 
	private static void swap(int[] arr, int i1, int i2) {
		if (i1 != i2) {
			int temp = arr[i1];
			arr[i1] = arr[i2];
			arr[i2] = temp;
		}
	}
}
