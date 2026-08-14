package org.zstack.ipsec

import org.zstack.ipsec.APIAttachL3NetworksToIPsecConnectionEvent

doc {
	title "AttachL3NetworksToIPsecConnection"

	category "ipsec"

	desc """添加三层网络到IPsec连接"""

	rest {
		request {
			url "POST /v1/ipsec/{uuid}/l3networks"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachL3NetworksToIPsecConnectionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "2.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAttachL3NetworksToIPsecConnectionEvent.class
		}
	}
}