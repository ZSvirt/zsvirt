package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.query.QueryFacade;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;

import java.util.List;

public class SftpBackupStorageManagerImpl extends AbstractService implements SftpBackupStorageManager {
	@Autowired
	private CloudBus bus;
	@Autowired
	private QueryFacade qf;

	@Override
    @MessageSafe
	public void handleMessage(Message msg) {
		bus.dealWithUnknownMessage(msg);
	}



	@Override
	public String getId() {
		return bus.makeLocalServiceId(SftpBackupStorageConstant.SERVICE_ID);
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

}
