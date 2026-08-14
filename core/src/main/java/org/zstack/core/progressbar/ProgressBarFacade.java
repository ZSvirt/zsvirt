package org.zstack.core.progressbar;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

public interface ProgressBarFacade {
	void report(Message msg, String description);
	
	void report(Message msg, String description, long total, long current);
	
	void progagateContext(Message src, Message dest);
	
	void setContextToApiMessage(APIMessage msg);
}
