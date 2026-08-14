package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIQueryNvmeServerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryNvmeServer"

	category "storageDevice"

	desc """查询NVMe服务器"""

	rest {
		request {
			url "GET /v1/storage-devices/nvme/servers"
			url "GET /v1/storage-devices/nvme/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNvmeServerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNvmeServerReply.class
		}
	}
}