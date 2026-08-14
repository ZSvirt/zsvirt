package org.zstack.header.network.service

import org.zstack.header.network.service.APIQueryNetworkServiceL3NetworkRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询网络服务与三层网络引用(QueryNetworkServiceL3NetworkRef)"

	category "三层网络"

	desc """查询网络服务与三层网络引用"""

	rest {
		request {
			url "GET /v1/l3-networks/network-services/refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNetworkServiceL3NetworkRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNetworkServiceL3NetworkRefReply.class
		}
	}
}