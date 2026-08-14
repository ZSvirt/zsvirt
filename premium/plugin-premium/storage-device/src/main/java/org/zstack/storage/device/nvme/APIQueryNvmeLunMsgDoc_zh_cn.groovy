package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIQueryNvmeLunReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryNvmeLun"

	category "storageDevice"

	desc """查询NVMe磁盘"""

	rest {
		request {
			url "GET /v1/storage-devices/nvme/luns"
			url "GET /v1/storage-devices/nvme/luns/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNvmeLunMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNvmeLunReply.class
		}
	}
}