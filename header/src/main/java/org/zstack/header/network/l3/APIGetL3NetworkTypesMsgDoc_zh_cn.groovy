package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIGetL3NetworkTypesReply

doc {
	title "获取三层网络类型(GetL3NetworkTypes)"

	category "三层网络"

	desc """获取三层网络类型"""

	rest {
		request {
			url "GET /v1/l3-networks/types"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetL3NetworkTypesMsg.class

			desc """"""

			params {

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
			clz APIGetL3NetworkTypesReply.class
		}
	}
}