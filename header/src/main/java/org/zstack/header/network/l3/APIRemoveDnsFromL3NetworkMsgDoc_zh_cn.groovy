package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIRemoveDnsFromL3NetworkEvent

doc {
	title "从三层网络移除DNS(RemoveDnsFromL3Network)"

	category "三层网络"

	desc """从三层网络移除DNS"""

	rest {
		request {
			url "DELETE /v1/l3-networks/{l3NetworkUuid}/dns/{dns}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveDnsFromL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "dns"
					enclosedIn ""
					desc "DNS地址"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIRemoveDnsFromL3NetworkEvent.class
		}
	}
}