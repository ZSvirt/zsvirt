package org.zstack.storage.device.fibreChannel

import org.zstack.storage.device.fibreChannel.APIQueryFiberChannelStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryFiberChannelStorage"

	category "storage.device"

	desc """查询FC SAN存储"""

	rest {
		request {
			url "GET /v1/storage-devices/fiber-channel/controllers"
			url "GET /v1/storage-devices/fiber-channel"
			url "GET /v1/storage-devices/fiber-channel/controllers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryFiberChannelStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryFiberChannelStorageReply.class
		}
	}
}