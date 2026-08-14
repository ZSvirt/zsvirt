package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIQueryIscsiServerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryIscsiServer"

	category "storage.device"

	desc """查询iSCSI服务器结果"""

	rest {
		request {
			url "GET /v1/storage-devices/iscsi/servers"
			url "GET /v1/storage-devices/iscsi"
			url "GET /v1/storage-devices/iscsi/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryIscsiServerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryIscsiServerReply.class
		}
	}
}