package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIApplyMonitorTemplateToMonitorGroupEvent

doc {
	title "ApplyMonitorTemplateToMonitorGroup"

	category "zwatch"

	desc """应用报警模板到资源分组"""

	rest {
		request {
			url "POST /v1/zwatch/monitortemplates/{templateUuid}/monitorgroups/{groupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIApplyMonitorTemplateToMonitorGroupMsg.class

			desc """"""

			params {

				column {
					name "templateUuid"
					enclosedIn "params"
					desc "模板UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "groupUuid"
					enclosedIn "params"
					desc "资源分组UUID"
					location "url"
					type "String"
					optional false
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
			clz APIApplyMonitorTemplateToMonitorGroupEvent.class
		}
	}
}