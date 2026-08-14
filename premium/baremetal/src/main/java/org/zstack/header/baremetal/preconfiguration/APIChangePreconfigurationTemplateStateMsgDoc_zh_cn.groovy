package org.zstack.header.baremetal.preconfiguration

import org.zstack.header.baremetal.preconfiguration.APIChangePreconfigurationTemplateStateEvent

doc {
	title "ChangePreconfigurationTemplateState"

	category "baremetal.preconfiguration"

	desc """修改预配置模板状态"""

	rest {
		request {
			url "PUT /v1/baremetal/preconfigurations/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangePreconfigurationTemplateStateMsg.class

			desc """修改预配置模板状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changePreconfigurationTemplateState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "stateEvent"
					enclosedIn "changePreconfigurationTemplateState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "3.4.0"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIChangePreconfigurationTemplateStateEvent.class
		}
	}
}