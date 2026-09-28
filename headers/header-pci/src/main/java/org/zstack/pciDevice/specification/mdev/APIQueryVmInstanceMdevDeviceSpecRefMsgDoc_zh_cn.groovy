package org.zstack.pciDevice.specification.mdev

import org.zstack.pciDevice.specification.mdev.APIQueryVmInstanceMdevDeviceSpecRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmInstanceMdevDeviceSpecRef"

	category "pciDevice"

	desc """查询云主机与MDEV设备规格的关联关系"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/mdev-device-specs"
			url "GET /v1/vm-instances/{vmInstanceUuid}/mdev-device-specs/{mdevSpecUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmInstanceMdevDeviceSpecRefMsg.class

			desc """查询云主机与MDEV设备规格的关联关系"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmInstanceMdevDeviceSpecRefReply.class
		}
	}
}