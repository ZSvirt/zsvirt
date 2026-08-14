package org.zstack.header.vm

import org.zstack.header.vm.APIUpdateVmInstanceMetadataEvent

doc {
	title "更新云主机元数据"

	category "云主机"

	desc """立即触发指定云主机的元数据更新"""

	rest {
		request {
			url "PUT /v1/vm-instances/metadata/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmInstanceMetadataMsg.class

			desc """"""

			params {

				column {
					name "vmUuids"
					enclosedIn "updateVmInstanceMetadata"
					desc "需要更新元数据的云主机UUID列表"
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
			clz APIUpdateVmInstanceMetadataEvent.class
		}
	}
}