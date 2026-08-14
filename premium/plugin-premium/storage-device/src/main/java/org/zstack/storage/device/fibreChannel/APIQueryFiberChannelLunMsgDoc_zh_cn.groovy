package org.zstack.storage.device.fibreChannel

import org.zstack.storage.device.fibreChannel.APIQueryFiberChannelLunReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryFiberChannelLun"

	category "storage.device"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/storage-devices/fiber-channel/luns"
			url "GET /v1/storage-devices/fiber-channel/luns/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryFiberChannelLunMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryFiberChannelLunReply.class
		}
	}
}