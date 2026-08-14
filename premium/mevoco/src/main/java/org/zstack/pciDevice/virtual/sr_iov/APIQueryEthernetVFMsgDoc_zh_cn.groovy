package org.zstack.pciDevice.virtual.sr_iov

import org.zstack.pciDevice.virtual.sr_iov.APIQueryEthernetVFReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询网卡VF"

	category "pciDevice"

	desc """查询网卡VF"""

	rest {
		request {
			url "GET /v1/pci-device/ethernet-vfs"
			url "GET /v1/pci-device/ethernet-vfs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEthernetVFMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryEthernetVFReply.class
		}
	}
}