package org.zstack.pciDevice.gpu

import org.zstack.pciDevice.gpu.APIQueryGpuDeviceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryGpuDevice"

	category "pciDevice"

	desc """查询GPU信息"""

	rest {
		request {
			url "GET /v1/gpu-device/gpu-devices"
			url "GET /v1/gpu-device/gpu-devices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryGpuDeviceMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryGpuDeviceReply.class
		}
	}
}