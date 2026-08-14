package org.zstack.header.vm

import org.zstack.header.vm.APICleanupVmInstanceMetadataEvent

doc {
	title "清理云主机元数据"

	category "云主机"

	desc """清理指定云主机在主存储上的元数据文件"""

	rest {
		request {
			url "PUT /v1/vm-instances/metadata/cleanup"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanupVmInstanceMetadataMsg.class

			desc """"""

			params {

				column {
					name "vmUuids"
					enclosedIn "cleanupVmInstanceMetadata"
					desc "需要清理元数据的云主机UUID列表"
					location "body"
					type "List"
					optional false
					since "zsv 5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APICleanupVmInstanceMetadataEvent.class
		}
	}
}