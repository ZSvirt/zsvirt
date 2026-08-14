package org.zstack.header.vm.devices

import org.zstack.header.vm.devices.APIQueryVmInstanceResourceMetadataGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmInstanceResourceMetadataGroup"

	category "snapshot.volume"

	desc """查询云主机设备地址组"""

	rest {
		request {
			url "GET /v1/vmInstance/resource/metadata/group"
			url "GET /v1/vmInstance/resource/metadata/group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmInstanceResourceMetadataGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmInstanceResourceMetadataGroupReply.class
		}
	}
}