package org.zstack.storage.ceph.backup

import org.zstack.header.storage.backup.APIAddBackupStorageEvent

doc {
	title "添加Ceph镜像服务器(AddCephBackupStorage)"

	category "storage.ceph.backup"

	desc """添加Ceph镜像服务器"""

	rest {
		request {
			url "POST /v1/backup-storage/ceph"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddCephBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "monUrls"
					enclosedIn "params"
					desc "Ceph mon 的地址列表"
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "poolName"
					enclosedIn "params"
					desc "用于存放镜像的 Ceph pool 的名字"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "未使用"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "镜像服务器名字"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "镜像服务器的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "镜像服务器的类型，此处为 Ceph"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "importImages"
					enclosedIn "params"
					desc "添加后是否导入镜像"
					location "body"
					type "boolean"
					optional true
					since "1.9"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，镜像服务器会使用该字段值作为UUID。"
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
			clz APIAddBackupStorageEvent.class
		}
	}
}