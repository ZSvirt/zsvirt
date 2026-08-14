package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIUpdateMonitorTemplateEvent

doc {
	title "UpdateMonitorTemplate"

	category "zwatch"

	desc """更新监控模板"""

	rest {
		request {
			url "PUT /v1/zwatch/monitortemplates/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateMonitorTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateMonitorTemplate"
					desc "监控模板UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "name"
					enclosedIn "updateMonitorTemplate"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "description"
					enclosedIn "updateMonitorTemplate"
					desc "资源的详细描述"
					location "body"
					type "String"
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
			clz APIUpdateMonitorTemplateEvent.class
		}
	}
}