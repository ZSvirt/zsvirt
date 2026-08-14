package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIGetManagementNodeOSReply

doc {
	title "GetManagementNodeOS"

	category "managementNode"

	desc """获取管理节点系统"""

	rest {
		request {
			url "PUT /v1/management/actions"



			clz APIGetManagementNodeOSMsg.class

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
			clz APIGetManagementNodeOSReply.class
		}
	}
}