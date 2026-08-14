package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APICreateMonitorGroupEvent

doc {
	title "CreateMonitorGroup"

	category "zwatch"

	desc """创建资源分组"""

	rest {
		request {
			url "POST /v1/zwatch/monitorgroups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateMonitorGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "actions"
					enclosedIn "params"
					desc "报警动作列表"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.10.0"
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
			clz APICreateMonitorGroupEvent.class
		}
	}
}