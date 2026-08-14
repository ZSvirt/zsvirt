package org.zstack.header.apimediator

import org.zstack.header.apimediator.APIIsReadyToGoReply

doc {
	title "IsReadyToGo"

	category "other"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/management-nodes/ready"



			clz APIIsReadyToGoMsg.class

			desc """"""

			params {

				column {
					name "managementNodeId"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
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
			clz APIIsReadyToGoReply.class
		}
	}
}