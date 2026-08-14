package org.zstack.header.vm

import org.zstack.header.core.APIGetChainTaskReply

doc {
	title "GetVmTask"

	category "vmInstance"

	desc """获取虚拟机上的任务信息"""

	rest {
		request {
			url "GET /v1/vm-instances/task-details"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmTaskMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional false
					since "zsv 3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "syncSignatures"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
			}
		}

		response {
			clz APIGetChainTaskReply.class
		}
	}
}