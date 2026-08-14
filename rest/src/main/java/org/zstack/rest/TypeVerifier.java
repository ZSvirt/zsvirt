package org.zstack.rest;

import org.zstack.core.Platform;
import org.zstack.utils.TypeUtils;

import java.lang.reflect.Field;
import java.util.function.BiFunction;

/**
 * @Author: fubang
 * @Date: 2018/4/2
 */
public class TypeVerifier {
	private static final BiFunction<Field, String, String> verifyInt = (Field f, String source) -> {
		if (TypeUtils.isTypeOf(f.getType(), int.class, Integer.class, long.class, Long.class, Short.class, short.class)) {
			try {
				double value = Double.parseDouble(source);
				if (value != Math.floor(value)) {
					throw new RuntimeException("error");
				}
			} catch (Exception e) {
				 return Platform.i18n("[%s] field is excepted an int or long, but was [%s].", f.getName(), source);
			}
		}
		return null;
	};

	private static final BiFunction<Field, String, String> verifyBoolean = (Field f, String source) -> {
	    if (TypeUtils.isTypeOf(f.getType(), boolean.class, Boolean.class)) {
			if (!(source.equalsIgnoreCase("true") || source.equalsIgnoreCase("false"))) {
				return Platform.i18n("Invalid value for boolean field [%s],"
						+ " [%s] is not a valid boolean string[true, false].", f.getName(), source);
			}
		}
		return null;
	};

	private static final BiFunction<Field, String, String>[] functions = new BiFunction[] {
			verifyBoolean, verifyInt };

	public static String verify(Field f, String source) {
		for (BiFunction<Field, String, String> bi : functions) {
			String result = bi.apply(f, source);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
}
