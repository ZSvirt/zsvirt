package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIGetTrashOnPrimaryStorageReply

doc {
	title "GetTrashOnPrimaryStorage"

	category "storage.primary"

	desc """获取主存储站上的回收数据列表"""

	rest {
		request {
			url "GET /v1/primary-storage/trash"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetTrashOnPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "主存储UUID"
					location "query"
					type "String"
					optional false
					since "zsv 3.2.0"
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc "回收数据所属的资源的UUID，必须与resourceType搭配使用"
					location "query"
					type "String"
					optional true
					since "zsv 3.5.0"
				}
				column {
					name "resourceType"
					enclosedIn ""
					desc "回收数据所属的资源的类型，必须与resourceUuid搭配使用"
					location "query"
					type "String"
					optional true
					since "zsv 3.5.0"
				}
				column {
					name "trashType"
					enclosedIn ""
					desc "发生回收的行为"
					location "query"
					type "String"
					optional true
					since "zsv 3.5.0"
					values ("MigrateVolume","MigrateVolumeSnapshot","RevertVolume","VolumeSnapshot")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.2.0"
				}
			}
		}

		response {
			clz APIGetTrashOnPrimaryStorageReply.class
		}
	}
}