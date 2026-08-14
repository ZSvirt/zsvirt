package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIGetVipUsedPortsReply

doc {
	title "GetVipUsedPorts"

	category "virtualRouter"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vips/{uuid}/usedports"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVipUsedPortsMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "protocol"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
					values ("tcp","udp")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVipUsedPortsReply.class
		}
	}
}