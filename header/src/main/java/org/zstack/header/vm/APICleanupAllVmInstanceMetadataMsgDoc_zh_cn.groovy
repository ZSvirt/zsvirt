package org.zstack.header.vm

import org.zstack.header.vm.APICleanupAllVmInstanceMetadataEvent

doc {
	title "清理全部云主机元数据"

	category "云主机"

	desc """清理一个或多个主存储上保存的全部云主机元数据文件，仅管理员可调用。当 primaryStorageUuids 为空（未传或传空列表）时，将清理系统中所有 Enabled+Connected 且支持云主机元数据的主存储；否则仅清理列表中指定的主存储。"""

	rest {
		request {
			url "DELETE /v1/vm-instances/metadata"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanupAllVmInstanceMetadataMsg.class

			desc """"""

			params {

				column {
					name "primaryStorageUuids"
					enclosedIn ""
					desc "需要清理云主机元数据的主存储UUID列表；为空时清理所有 Enabled+Connected 主存储上的元数据"
					location "query"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
			}
		}

		response {
			clz APICleanupAllVmInstanceMetadataEvent.class
		}
	}
}