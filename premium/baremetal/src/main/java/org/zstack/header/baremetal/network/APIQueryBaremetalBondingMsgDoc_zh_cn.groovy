package org.zstack.header.baremetal.network

import org.zstack.header.baremetal.network.APIQueryBaremetalBondingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryBaremetalBonding"

	category "baremetal.network"

	desc """查询裸金属网卡绑定"""

	rest {
		request {
			url "GET /v1/baremetal/network/bondings"
			url "GET /v1/baremetal/network/bondings/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryBaremetalBondingMsg.class

			desc """查询裸金属网卡绑定"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBaremetalBondingReply.class
		}
	}
}