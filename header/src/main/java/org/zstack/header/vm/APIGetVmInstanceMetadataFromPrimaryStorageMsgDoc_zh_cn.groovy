package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmInstanceMetadataFromPrimaryStorageEvent

doc {
	title "获取云主机元数据"

	category "主存储"

	desc """从主存储获取指定云主机的元数据内容"""

	rest {
		request {
			url "GET /v1/primary-storage/vm-instances/metadata"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmInstanceMetadataFromPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "query"
					type "String"
					optional false
					since "zsv 5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APIGetVmInstanceMetadataFromPrimaryStorageEvent.class
		}
	}
}