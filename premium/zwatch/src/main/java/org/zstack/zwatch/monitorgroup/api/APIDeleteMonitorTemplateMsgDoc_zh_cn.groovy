package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIDeleteMonitorTemplateEvent

doc {
	title "DeleteMonitorTemplate"

	category "zwatch"

	desc """删除监控模板"""

	rest {
		request {
			url "DELETE /v1/zwatch/monitortemplates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteMonitorTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "模板UUID"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIDeleteMonitorTemplateEvent.class
		}
	}
}