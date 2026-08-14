package org.zstack.ipsec

import org.zstack.ipsec.APIAddRemoteCidrsToIPsecConnectionEvent

doc {
	title "AddRemoteCidrsToIPsecConnection"

	category "ipsec"

	desc """添加远端网段到IPsec连接"""

	rest {
		request {
			url "POST /v1/ipsec/{uuid}/remote-cidrs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddRemoteCidrsToIPsecConnectionMsg.class

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
					name "peerCidrs"
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
			clz APIAddRemoteCidrsToIPsecConnectionEvent.class
		}
	}
}