package org.zstack.header.storageDevice

import org.zstack.header.storageDevice.APIQueryScsiLunReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryScsiLun"

	category "storageDevice"

	desc """查询 SCSI Lun"""

	rest {
		request {
			url "GET /v1/storage-devices/scsi-lun/luns"
			url "GET /v1/storage-devices/scsi-lun/luns/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryScsiLunMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryScsiLunReply.class
		}
	}
}