package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIAddDnsToL3NetworkEvent

doc {
	title "向三层网络添加DNS(AddDnsToL3Network)"

	category "三层网络"

	desc """向三层网络添加DNS"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/dns"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddDnsToL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "dns"
					enclosedIn "params"
					desc "DNS地址"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIAddDnsToL3NetworkEvent.class
		}
	}
}