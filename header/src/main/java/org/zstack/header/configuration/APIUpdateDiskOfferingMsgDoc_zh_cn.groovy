package org.zstack.header.configuration

import org.zstack.header.configuration.APIUpdateDiskOfferingEvent

doc {
	title "UpdateDiskOffering"

	category "configuration"

	desc """更新云盘规格"""

	rest {
		request {
			url "PUT /v1/disk-offerings/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateDiskOfferingMsg.class

			desc """更新云盘规格"""

			params {

				column {
					name "uuid"
					enclosedIn "updateDiskOffering"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateDiskOffering"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateDiskOffering"
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
			}
		}

		response {
			clz APIUpdateDiskOfferingEvent.class
		}
	}
}