package org.zstack.resourceconfig

import org.zstack.resourceconfig.APIGetResourceBindableConfigReply

doc {
	title "GetResourceBindableConfig"

	category "resourceConfig"

	desc """罗列可配置的资源高级设置"""

	rest {
		request {
			url "GET /v1/resource-configurations/bindable"
			url "GET /v1/resource-configurations/bindable/{category}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceBindableConfigMsg.class

			desc """"""

			params {

				column {
					name "category"
					enclosedIn ""
					desc "设置类型"
					location "query"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIGetResourceBindableConfigReply.class
		}
	}
}