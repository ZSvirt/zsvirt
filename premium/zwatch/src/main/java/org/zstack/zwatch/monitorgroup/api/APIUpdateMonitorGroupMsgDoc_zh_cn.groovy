package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIUpdateMonitorGroupEvent

doc {
	title "UpdateMonitorGroup"

	category "zwatch"

	desc """更新资源分组"""

	rest {
		request {
			url "PUT /v1/zwatch/monitorgroups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateMonitorGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateMonitorGroup"
					desc "资源分组UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "name"
					enclosedIn "updateMonitorGroup"
					desc "资源分组名称"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "description"
					enclosedIn "updateMonitorGroup"
					desc "资源分组详细描述"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "actions"
					enclosedIn "updateMonitorGroup"
					desc "报警行为"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "stateEvent"
					enclosedIn "updateMonitorGroup"
					desc "状态"
					location "body"
					type "String"
					optional true
					since "3.10.0"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIUpdateMonitorGroupEvent.class
		}
	}
}