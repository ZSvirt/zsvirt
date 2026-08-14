package org.zstack.header.vm.devices

import org.zstack.header.vm.devices.APIQueryVmInstanceResourceMetadataArchiveReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmInstanceResourceMetadataArchive"

	category "snapshot.volume"

	desc """查询云主机设备地址归档"""

	rest {
		request {
			url "GET /v1/vmInstance/resource/metadata/archive"
			url "GET /v1/vmInstance/resource/metadata/archive/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmInstanceResourceMetadataArchiveMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmInstanceResourceMetadataArchiveReply.class
		}
	}
}