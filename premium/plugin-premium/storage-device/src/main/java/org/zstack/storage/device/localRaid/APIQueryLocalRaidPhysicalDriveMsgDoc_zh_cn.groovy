package org.zstack.storage.device.localRaid

import org.zstack.storage.device.localRaid.APIQueryLocalRaidPhysicalDriveReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryLocalRaidPhysicalDrive"

	category "storageDevice"

	desc """查询Raid物理盘"""

	rest {
		request {
			url "GET /v1/storage-devices/local-raid/physical-drives"
			url "GET /v1/storage-devices/local-raid/physical-drives/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLocalRaidPhysicalDriveMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLocalRaidPhysicalDriveReply.class
		}
	}
}