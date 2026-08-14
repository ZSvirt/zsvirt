package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIQueryNvmeTargetReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryNvmeTarget"

	category "storageDevice"

	desc """查询NVMe设备"""

	rest {
		request {
			url "GET /v1/storage-devices/nvme/controllers"
			url "GET /v1/storage-devices/nvme"
			url "GET /v1/storage-devices/nvme/controllers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNvmeTargetMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNvmeTargetReply.class
		}
	}
}