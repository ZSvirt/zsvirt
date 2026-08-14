package org.zstack.header.baremetal.preconfiguration

import org.zstack.header.baremetal.preconfiguration.APIDeletePreconfigurationTemplateEvent

doc {
	title "DeletePreconfigurationTemplate"

	category "baremetal.preconfiguration"

	desc """删除预配置模板"""

	rest {
		request {
			url "DELETE /v1/baremetal/preconfigurations/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeletePreconfigurationTemplateMsg.class

			desc """删除预配置模板"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIDeletePreconfigurationTemplateEvent.class
		}
	}
}