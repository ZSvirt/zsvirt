package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIGetVSwitchTypesReply

doc {
	title "获取虚拟交换机类型(GetVSwitchTypes)"

	category "二层网络"

	desc """获取虚拟交换机类型"""

	rest {
		request {
			url "GET /v1/l2-networks/vSwitchTypes"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVSwitchTypesMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
			}
		}

		response {
			clz APIGetVSwitchTypesReply.class
		}
	}
}