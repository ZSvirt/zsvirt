package org.zstack.ipsec

import org.zstack.ipsec.APIGetVpcIPsecLogReply

doc {
	title "GetVpcIPsecLog"

	category "ipsec"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers/ipseclog"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVpcIPsecLogMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "lines"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
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
			clz APIGetVpcIPsecLogReply.class
		}
	}
}