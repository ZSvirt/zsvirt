package org.zstack.storage.primary.smp

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "添加一个共享挂载点的主存储(AddSharedMountPointPrimaryStorage)"

	category "storage.primary"

	desc """添加一个共享挂载点的主存储"""

	rest {
		request {
			url "POST /v1/primary-storage/smp"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSharedMountPointPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "共享挂载主存储所在的本地路径"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "共享挂载主存储的名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "共享挂载主存储的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "主存储的类型，应为SharedMountPoint"
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
					desc "共享挂载主存储的uuid"
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