package org.zstack.network.service.vip

import org.zstack.network.service.vip.APICheckVipPortAvailabilityReply

doc {
	title "CheckVipPortAvailability"

	category "vip"

	desc """检查VIP端口是否空闲"""

	rest {
		request {
			url "GET /v1/vips/{vipUuid}/check-port-availability"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckVipPortAvailabilityMsg.class

			desc """"""

			params {

				column {
					name "vipUuid"
					enclosedIn ""
					desc "VIP UUID"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "port"
					enclosedIn ""
					desc ""
					location "query"
					type "int"
					optional false
					since "3.17.21"
				}
				column {
					name "protocolType"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "3.17.21"
					values ("TCP","UDP")
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.17.21"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APICheckVipPortAvailabilityReply.class
		}
	}
}