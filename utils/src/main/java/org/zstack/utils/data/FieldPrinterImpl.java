package org.zstack.utils.data;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldPrinterImpl implements FieldPrinter {
	private List<String> doPrint(Object obj, Class<?> clazz) {
		try {
			Field[] fields = clazz.getDeclaredFields();
			List<String> ret = new ArrayList<String>(fields.length);
			for (Field f : fields) {
				f.setAccessible(true);
				String name = f.getName();
				Object value = f.get(obj);
				if (f.getType().isArray()) {
					if (value != null) {
						List<Object> lst = new ArrayList<Object>(Array.getLength(value));
						Collections.addAll(lst, (Object[])value);
						value = lst;
					}
				}
				ret.add(String.format("%s=%s", name, value));
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<String>(0);
		}
	}

	@Override
	public String print(Object obj) {
		if (obj == null) {
			return new ArrayList<String>(0).toString();
		}
		
		return doPrint(obj, obj.getClass()).toString();
	}

	@Override
	public String print(Object obj, boolean recursive) {
		if (obj == null) {
			return new ArrayList<String>(0).toString();
		}
		
		Class<?> clazz = obj.getClass();
		List<String> ret = new ArrayList<String>();
		do {
			ret.addAll(doPrint(obj, clazz));
			clazz = clazz.getSuperclass();
		} while (clazz != null && clazz != Object.class);
		return ret.toString();
	}
}
