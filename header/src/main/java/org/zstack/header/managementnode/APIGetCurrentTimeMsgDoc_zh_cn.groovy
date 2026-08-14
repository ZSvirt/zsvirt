package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIGetCurrentTimeReply

doc {
	title "GetCurrentTime"

	category "managementNode"

	desc """获取当前时间"""

	rest {
		request {
			url "PUT /v1/management-nodes/actions"



			clz APIGetCurrentTimeMsg.class

			desc """获取当前时间"""

			params {

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
			clz APIGetCurrentTimeReply.class
		}
	}
}