package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIAddInstanceToMonitorGroupEvent

doc {
	title "AddInstanceToMonitorGroup"

	category "zwatch"

	desc """添加资源到资源分组"""

	rest {
		request {
			url "POST /v1/zwatch/monitorgroups/{groupUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddInstanceToMonitorGroupMsg.class

			desc """"""

			params {

				column {
					name "instanceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "groupUuid"
					enclosedIn "params"
					desc "资源分组"
					location "url"
					type "String"
					optional false
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
			clz APIAddInstanceToMonitorGroupEvent.class
		}
	}
}