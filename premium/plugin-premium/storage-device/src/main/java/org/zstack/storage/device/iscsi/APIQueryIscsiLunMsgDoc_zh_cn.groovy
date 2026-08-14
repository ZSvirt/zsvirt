package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.APIQueryIscsiLunReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryIscsiLun"

	category "storage.device"

	desc """查询iSCSI磁盘"""

	rest {
		request {
			url "GET /v1/storage-devices/iscsi/luns"
			url "GET /v1/storage-devices/iscsi/luns/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryIscsiLunMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryIscsiLunReply.class
		}
	}
}