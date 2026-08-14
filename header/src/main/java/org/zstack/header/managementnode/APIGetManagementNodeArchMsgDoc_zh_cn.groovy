package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIGetManagementNodeArchReply

doc {
	title "GetManagementNodeArch"

	category "managementNode"

	desc """获取管理节点系统架构"""

	rest {
		request {
			url "PUT /v1/management-nodes/actions"



			clz APIGetManagementNodeArchMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.2"
				}
			}
		}

		response {
			clz APIGetManagementNodeArchReply.class
		}
	}
}