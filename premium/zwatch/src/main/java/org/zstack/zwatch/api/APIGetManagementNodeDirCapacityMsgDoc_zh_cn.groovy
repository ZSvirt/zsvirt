package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetManagementNodeDirCapacityReply

doc {
	title "GetManagementNodeDirCapacity"

	category "zwatch"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/zwatch/mn"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetManagementNodeDirCapacityMsg.class

			desc """"""

			params {

				column {
					name "managementNodeUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetManagementNodeDirCapacityReply.class
		}
	}
}