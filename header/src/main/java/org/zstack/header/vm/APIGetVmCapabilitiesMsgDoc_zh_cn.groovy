package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmCapabilitiesReply

doc {
	title "获取云主机能力(GetVmCapabilities)"

	category "vmInstance"

	desc """获取一个云主机的能力，用于判断云主机是否能做某些特定操作。目前已定义能力包括:
	
|名称|描述|类型|
|---|---|---|
|LiveMigration|是否支持热迁移|Boolean|
|VolumeMigration|是否支持根云盘迁移|Boolean|
"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/capabilities"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmCapabilitiesMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
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
			clz APIGetVmCapabilitiesReply.class
		}
	}
}