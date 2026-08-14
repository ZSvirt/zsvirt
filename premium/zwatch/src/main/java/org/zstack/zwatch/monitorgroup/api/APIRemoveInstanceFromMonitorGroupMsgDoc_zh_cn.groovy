package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIRemoveInstanceFromMonitorGroupEvent

doc {
	title "RemoveInstanceFromMonitorGroup"

	category "zwatch"

	desc """删除资源分组资源"""

	rest {
		request {
			url "DELETE /v1/zwatch/monitorgroups/{groupUuid}/actions/{instanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveInstanceFromMonitorGroupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "instanceUuid"
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
			clz APIRemoveInstanceFromMonitorGroupEvent.class
		}
	}
}