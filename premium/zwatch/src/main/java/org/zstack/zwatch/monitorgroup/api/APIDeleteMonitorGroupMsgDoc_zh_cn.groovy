package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIDeleteMonitorGroupEvent

doc {
	title "DeleteMonitorGroup"

	category "zwatch"

	desc """删除资源分组返回"""

	rest {
		request {
			url "DELETE /v1/zwatch/monitorgroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteMonitorGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源分组UUID"
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
			clz APIDeleteMonitorGroupEvent.class
		}
	}
}