package org.zstack.core.progressbar;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

public class ProgressBarFacadeImpl implements ProgressBarFacade {
	public static final String API_ID = "ProgressBar.API_ID";
	
	@Autowired
	private CloudBus bus;
	
	public void report(Message msg, String description) {
		String apiId = msg.getHeaderEntry(API_ID);
		if (apiId == null) {
			return;
		}
		
		InProgressEvent inevt = new InProgressEvent(apiId, description);
		bus.publish(inevt);
	}

	@Override
    public void report(Message msg, String description, long total, long current) {
		String apiId = msg.getHeaderEntry(API_ID);
		if (apiId == null) {
			return;
		}
		
		FixedInProgressEvent fevt = new FixedInProgressEvent(apiId, description, total, current);
		bus.publish(fevt);
    }

	@Override
    public void progagateContext(Message src, Message dest) {
		String apiId = src.getHeaderEntry(API_ID);
		if (apiId == null) {
			return;
		}
		
		dest.putHeaderEntry(API_ID, apiId);
    }

	@Override
    public void setContextToApiMessage(APIMessage msg) {
		msg.putHeaderEntry(API_ID, msg.getId());
    }
}
