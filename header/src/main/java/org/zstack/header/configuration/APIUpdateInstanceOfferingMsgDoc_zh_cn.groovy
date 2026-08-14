package org.zstack.header.configuration

import org.zstack.header.configuration.APIUpdateInstanceOfferingEvent

doc {
	title "UpdateInstanceOffering"

	category "configuration"

	desc """更新云主机规格"""

	rest {
		request {
			url "PUT /v1/instance-offerings/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateInstanceOfferingMsg.class

			desc """更新云主机规格"""

			params {

				column {
					name "uuid"
					enclosedIn "updateInstanceOffering"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateInstanceOffering"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateInstanceOffering"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "allocatorStrategy"
					enclosedIn "updateInstanceOffering"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 2.3.1"
				}
			}
		}

		response {
			clz APIUpdateInstanceOfferingEvent.class
		}
	}
}