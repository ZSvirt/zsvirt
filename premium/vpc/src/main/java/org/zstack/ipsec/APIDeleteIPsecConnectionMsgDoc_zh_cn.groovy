package org.zstack.ipsec

import org.zstack.ipsec.APIDeleteIPsecConnectionEvent

doc {
	title "删除IPSec连接(DeleteIPsecConnection)"

	category "ipsec"

	desc """删除IPSec连接"""

	rest {
		request {
			url "DELETE /v1/ipsec/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteIPsecConnectionMsg.class

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
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteIPsecConnectionEvent.class
		}
	}
}