package org.zstack.header.baremetal.preconfiguration

import org.zstack.header.baremetal.preconfiguration.APIAddPreconfigurationTemplateEvent

doc {
	title "AddPreconfigurationTemplate"

	category "baremetal.preconfiguration"

	desc """添加预配置模板"""

	rest {
		request {
			url "POST /v1/baremetal/preconfigurations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddPreconfigurationTemplateMsg.class

			desc """添加预配置模板"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "distribution"
					enclosedIn "params"
					desc "适用操作系统发行版"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "模板类型"
					location "body"
					type "String"
					optional false
					since "3.4.0"
					values ("kickstart","preseed","autoyast")
				}
				column {
					name "content"
					enclosedIn "params"
					desc "模板内容"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddPreconfigurationTemplateEvent.class
		}
	}
}