package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIGetSupportAPIsReply

doc {
	title "GetSupportAPIs"

	category "managementNode"

	desc """获取所有支持的API"""

	rest {
		request {
			url "PUT /v1/management-nodes/actions"



			clz APIGetSupportAPIsMsg.class

			desc """"""

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
			clz APIGetSupportAPIsReply.class
		}
	}
}