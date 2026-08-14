package org.zstack.portal.managementnode;

import org.zstack.header.Service;

public interface ManagementNodeManager extends Service {
	void startNode();

	void quit(String reason);
}
