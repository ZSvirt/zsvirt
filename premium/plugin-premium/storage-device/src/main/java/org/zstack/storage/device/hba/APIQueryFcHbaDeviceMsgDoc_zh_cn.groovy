package org.zstack.storage.device.hba

import org.zstack.storage.device.hba.APIQueryFcHbaDeviceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHBADevice"

	category "storage.device"

	desc """查询HBA卡信息"""

	rest {
		request {
			url "GET /v1/storage-devices/hba"
			url "GET /v1/storage-devices/hba/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryFcHbaDeviceMsg.class

			desc """查询HBA卡信息"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryFcHbaDeviceReply.class
		}
	}
}