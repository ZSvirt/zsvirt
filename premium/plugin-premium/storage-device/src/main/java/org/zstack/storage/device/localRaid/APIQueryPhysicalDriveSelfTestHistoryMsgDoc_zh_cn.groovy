package org.zstack.storage.device.localRaid

import org.zstack.storage.device.localRaid.APIQueryPhysicalDriveSelfTestHistoryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryPhysicalDriveSelfTestHistory"

	category "storageDevice"

	desc """查询Raid物理盘自检历史"""

	rest {
		request {
			url "GET /v1/storage-devices/local-raid/physical-drives/self-test"
			url "GET /v1/storage-devices/local-raid/physical-drives/{raidPhysicalDriveUuid}/self-test"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPhysicalDriveSelfTestHistoryMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPhysicalDriveSelfTestHistoryReply.class
		}
	}
}