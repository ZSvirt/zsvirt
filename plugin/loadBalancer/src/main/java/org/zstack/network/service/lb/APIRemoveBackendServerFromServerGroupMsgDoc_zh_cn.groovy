package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRemoveBackendServerFromServerGroupEvent

doc {
	title "RemoveBackendServerFromServerGroup"

	category "loadBalancer"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/load-balancers/servergroups/{serverGroupUuid}/backendservers/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveBackendServerFromServerGroupMsg.class

			desc """"""

			params {

				column {
					name "serverGroupUuid"
					enclosedIn "removeBackendServerFromServerGroup"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "vmNicUuids"
					enclosedIn "removeBackendServerFromServerGroup"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "serverIps"
					enclosedIn "removeBackendServerFromServerGroup"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIRemoveBackendServerFromServerGroupEvent.class
		}
	}
}