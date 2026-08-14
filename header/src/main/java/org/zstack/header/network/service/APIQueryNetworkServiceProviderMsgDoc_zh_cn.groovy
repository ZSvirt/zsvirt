package org.zstack.header.network.service

import org.zstack.header.network.service.APIQueryNetworkServiceProviderReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询网络服务模块(QueryNetworkServiceProvider)"

	category "network.service"

	desc """查询网络服务模块"""

	rest {
		request {
			url "GET /v1/network-services/providers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNetworkServiceProviderMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNetworkServiceProviderReply.class
		}
	}
}