package org.zstack.utils.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArraySpliterImpl implements ArraySpliter {

	@Override
	public <T> List<T[]> split(T[] source, int lengthOfSubArray) {
		int slices = source.length / lengthOfSubArray;
		int residue = source.length % lengthOfSubArray;
		
		List<T[]> ret = new ArrayList<T[]>(slices + (residue > 0 ? 1 : 0));
		int offset = 0;
		for (int i=0; i<slices; i++) {
			T[] na = Arrays.copyOfRange(source, offset, offset + lengthOfSubArray);
			ret.add(na);
			offset += lengthOfSubArray;
		}
		
		if (residue != 0) {
			T[] na = Arrays.copyOfRange(source, offset, offset + residue);
			ret.add(na);
		}
		
		return ret;
	}

}
