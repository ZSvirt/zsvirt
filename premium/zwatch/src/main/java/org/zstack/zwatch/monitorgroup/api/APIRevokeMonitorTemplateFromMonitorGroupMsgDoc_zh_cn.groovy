package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIRevokeMonitorTemplateFromMonitorGroupEvent

doc {
	title "RevokeMonitorTemplateFromMonitorGroup"

	category "zwatch"

	desc """资源分组取消监控模板"""

	rest {
		request {
			url "DELETE /v1/zwatch/monitortemplates/{templateUuid}/monitorgroups/{groupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRevokeMonitorTemplateFromMonitorGroupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn ""
					desc "资源分组UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "templateUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "3.10.0"
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
			clz APIRevokeMonitorTemplateFromMonitorGroupEvent.class
		}
	}
}