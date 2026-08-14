package org.zstack.utils.data;

public interface FieldPrinter {
    String print(Object obj);
	
	String print(Object obj, boolean recursive);
}
