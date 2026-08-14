package org.zstack.utils.test;

import org.junit.Before;
import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.data.ArraySpliter;

import java.util.List;

public class TestArraySpliter {
	ArraySpliter spliter = Utils.getArraySpliter();

	@Before
	public void setUp() throws Exception {
	}

	void print(List<Integer[]> arr) {
		System.out.println("=======Enter Array ========");
		if (arr.isEmpty()) {
			System.out.println("Nothing");
			return;
		}
		
		for (Integer[] a : arr) {
			for (int i=0; i<a.length; i++) {
				System.out.print(a[i]);
				System.out.print(",");
			}
			System.out.println();
		}
		System.out.println("=======Exit Array ========");
	}
	
	Integer[] getArray(int num) {
		Integer[] arr = new Integer[num];
		for (int i=0; i<num; i++) {
			arr[i] = i;
		}
		return arr;
	}
	
	void test1() {
		Integer[] arr = getArray(3);
		List<Integer[]> ret = spliter.split(arr, 5);
		print(ret);
	}
	
	void test2() {
		Integer[] arr = getArray(10);
		List<Integer[]> ret = spliter.split(arr, 2);
		print(ret);
	}
	
	void test3() {
		Integer[] arr = getArray(0);
		List<Integer[]> ret = spliter.split(arr, 2);
		print(ret);
	}
	
	void test4() {
		Integer[] arr = getArray(13);
		List<Integer[]> ret = spliter.split(arr, 3);
		print(ret);
	}
	
	@Test
	public void test() {
		test1();
		test2();
		test3();
		test4();
	}

}
