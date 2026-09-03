package org.zstack.resourceconfig

import org.zstack.resourceconfig.APIDeleteResourceConfigEvent

doc {
	title "DeleteResourceConfig"

	category "resourceConfig"

	desc """删除资源高级设置"""

	rest {
		request {
			url "DELETE /v1/resource-configurations/{category}/{name}/{resourceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteResourceConfigMsg.class

			desc """"""

			params {

				column {
					name "category"
					enclosedIn ""
					desc "设置类型"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "name"
					enclosedIn ""
					desc "设置名称"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
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
			clz APIDeleteResourceConfigEvent.class
		}
	}
}