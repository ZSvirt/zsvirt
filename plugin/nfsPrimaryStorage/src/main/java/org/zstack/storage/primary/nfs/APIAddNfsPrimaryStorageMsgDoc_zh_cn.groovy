package org.zstack.storage.primary.nfs

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "添加 NFS 主存储(AddNfsPrimaryStorage)"

	category "storage.primary"

	desc """添加 NFS 主存储"""

	rest {
		request {
			url "POST /v1/primary-storage/nfs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddNfsPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "NFS 公开地址，格式为 nfs-host:/path/to/export"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "NFS 主存储名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "NFS 主存储的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "类型为 NFS"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，NFS 主存储会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
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
			clz APIAddPrimaryStorageEvent.class
		}
	}
}