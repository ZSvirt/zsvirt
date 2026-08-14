package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIScanVmInstanceMetadataFromPrimaryStorageReply

doc {
	title "扫描主存储上的云主机元数据"

	category "主存储"

	desc """扫描指定主存储上所有云主机元数据文件，返回元数据摘要列表"""

	rest {
		request {
			url "GET /v1/primary-storage/vm-instances/metadata/scan"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIScanVmInstanceMetadataFromPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "主存储UUID"
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
			clz APIScanVmInstanceMetadataFromPrimaryStorageReply.class
		}
	}
}