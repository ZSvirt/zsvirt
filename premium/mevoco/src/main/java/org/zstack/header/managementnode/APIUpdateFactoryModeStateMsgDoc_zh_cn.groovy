package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIUpdateFactoryModeStateEvent

doc {
	title "UpdateFactoryModeState"

	category "mevoco"

	desc """更新工厂模式启用状态"""

	rest {
		request {
			url "PUT /v1/management-nodes/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateFactoryModeStateMsg.class

			desc """"""

			params {

				column {
					name "factoryModeState"
					enclosedIn "updateFactoryModeState"
					desc "工厂模式启用状态"
					location "body"
					type "Boolean"
					optional false
					since "3.8"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
			}
		}

		response {
			clz APIUpdateFactoryModeStateEvent.class
		}
	}
}