package org.zstack.ipsec

import org.zstack.ipsec.APIGetCandidateL3NetworksForIpSecConnectionReply

doc {
	title "GetCandidateL3NetworksForIpSecConnection"

	category "ipsec"

	desc """获取ipsec可用的l3列表"""

	rest {
		request {
			url "GET /v1/ipsec/candidatesL3Networks"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateL3NetworksForIpSecConnectionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "publicL3Uuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.10"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "vipUuid"
					enclosedIn ""
					desc "VIP UUID"
					location "query"
					type "String"
					optional true
					since "3.10"
				}
			}
		}

		response {
			clz APIGetCandidateL3NetworksForIpSecConnectionReply.class
		}
	}
}