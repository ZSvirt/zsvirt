package org.zstack.network.service.virtualrouter;

public class VirtualRouterException extends Exception {
	public VirtualRouterException(String msg, Throwable t) {
		super(msg, t);
	}
	
	public VirtualRouterException(String msg) {
		super(msg);
	}
	
	public VirtualRouterException(Throwable t) {
		super(t);
	}
}
