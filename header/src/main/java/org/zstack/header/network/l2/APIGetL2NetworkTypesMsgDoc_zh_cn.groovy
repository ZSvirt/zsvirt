package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIGetL2NetworkTypesReply

doc {
	title "获取二层网络类型(GetL2NetworkTypes)"

	category "二层网络"

	desc """获取二层网络类型"""

	rest {
		request {
			url "GET /v1/l2-networks/types"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetL2NetworkTypesMsg.class

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
			clz APIGetL2NetworkTypesReply.class
		}
	}
}