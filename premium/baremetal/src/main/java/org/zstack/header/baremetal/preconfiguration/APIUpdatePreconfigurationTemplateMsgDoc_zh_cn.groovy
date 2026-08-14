package org.zstack.header.baremetal.preconfiguration

import org.zstack.header.baremetal.preconfiguration.APIUpdatePreconfigurationTemplateEvent

doc {
	title "UpdatePreconfigurationTemplate"

	category "baremetal.preconfiguration"

	desc """更新预配置模板"""

	rest {
		request {
			url "PUT /v1/baremetal/preconfigurations/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePreconfigurationTemplateMsg.class

			desc """更新预配置模板"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePreconfigurationTemplate"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "name"
					enclosedIn "updatePreconfigurationTemplate"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "description"
					enclosedIn "updatePreconfigurationTemplate"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "distribution"
					enclosedIn "updatePreconfigurationTemplate"
					desc "适用操作系统发行版"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "type"
					enclosedIn "updatePreconfigurationTemplate"
					desc "模板类型"
					location "body"
					type "String"
					optional true
					since "3.4.0"
					values ("kickstart","preseed","autoyast")
				}
				column {
					name "content"
					enclosedIn "updatePreconfigurationTemplate"
					desc "模板内容"
					location "body"
					type "String"
					optional true
					since "3.4.0"
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
			clz APIUpdatePreconfigurationTemplateEvent.class
		}
	}
}