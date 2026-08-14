package org.zstack.header.configuration

import org.zstack.header.configuration.APIDeleteDiskOfferingEvent

doc {
	title "DeleteDiskOffering"

	category "configuration"

	desc """删除云盘规格"""

	rest {
		request {
			url "DELETE /v1/disk-offerings/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteDiskOfferingMsg.class

			desc """删除云盘规格"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteDiskOfferingEvent.class
		}
	}
}