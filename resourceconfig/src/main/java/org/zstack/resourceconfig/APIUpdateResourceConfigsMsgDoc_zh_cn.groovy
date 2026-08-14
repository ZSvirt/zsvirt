package org.zstack.resourceconfig

import org.zstack.resourceconfig.APIUpdateResourceConfigsEvent

doc {
	title "UpdateResourceConfigs"

	category "resourceConfig"

	desc """批量更新资源高级设置"""

	rest {
		request {
			url "POST /v1/resource-configurations/{resourceUuid}/resource-configs/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateResourceConfigsMsg.class

			desc """"""

			params {

				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "设置的资源UUID"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "resourceConfigs"
					enclosedIn "params"
					desc "资源的高级配置列表"
					location "body"
					type "List"
					optional false
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APIUpdateResourceConfigsEvent.class
		}
	}
}