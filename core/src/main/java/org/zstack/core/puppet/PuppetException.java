package org.zstack.core.puppet;

public class PuppetException extends RuntimeException {
	public PuppetException(String msg) {
	    super(msg);
	}
	
	public PuppetException(String msg, Throwable t) {
	    super(msg, t);
	}
	
	public PuppetException(Throwable t) {
	    super(t);
	}
}
