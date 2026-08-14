package org.zstack.header.host

import org.zstack.header.host.APIGetCandidateNetworkBondingsReply

doc {
	title "GetCandidateNetworkBondings"

	category "host"

	desc """获取物理机交集绑定信息"""

	rest {
		request {
			url "GET /v1/cluster/hosts-network-bondings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateNetworkBondingsMsg.class

			desc """"""

			params {

				column {
					name "hostUuids"
					enclosedIn ""
					desc "物理机UUIDs"
					location "query"
					type "List"
					optional false
					since "3.18.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.18.0"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.18.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.18.0"
				}
			}
		}

		response {
			clz APIGetCandidateNetworkBondingsReply.class
		}
	}
}