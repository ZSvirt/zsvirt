package org.zstack.core.progressbar;

import org.zstack.header.message.APIEvent;

public class InProgressEvent extends APIEvent {
	private String description;
	
	protected InProgressEvent(String apiId, String description) {
		super(apiId);
		this.description = description;
	}
	
	public InProgressEvent() {
	    super(null);
	}

	public String getDescription() {
    	return description;
    }

	public void setDescription(String description) {
    	this.description = description;
    }
}
