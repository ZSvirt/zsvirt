package org.zstack.storage.device.localRaid

import org.zstack.storage.device.localRaid.APIQueryLocalRaidPhysicalDriveReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryLocalRaidController"

	category "storageDevice"

	desc """查询Raid控制器"""

	rest {
		request {
			url "GET /v1/storage-devices/local-raid/controllers"
			url "GET /v1/storage-devices/local-raid/controllers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLocalRaidControllerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLocalRaidPhysicalDriveReply.class
		}
	}
}