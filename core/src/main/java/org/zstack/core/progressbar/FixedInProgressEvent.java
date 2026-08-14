package org.zstack.core.progressbar;

public class FixedInProgressEvent extends InProgressEvent {
	private long total;
	private long current;
	
	protected FixedInProgressEvent(String apiId, String description, long total, long current) {
		super(apiId, description);
		this.total = total;
		this.current = current;
	}
	
	public FixedInProgressEvent() {
	}

	public long getTotal() {
    	return total;
    }

	public void setTotal(long total) {
    	this.total = total;
    }

	public long getCurrent() {
    	return current;
    }

	public void setCurrent(long current) {
    	this.current = current;
    }
}
