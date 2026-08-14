package org.zstack.storage.backup.imagestore

import org.zstack.header.storage.backup.APIUpdateBackupStorageEvent

doc {
	title "更新镜像服务器信息(UpdateImageStoreBackupStorage)"

	category "storage.backup.imagestore"

	desc """更新镜像服务器信息"""

	rest {
		request {
			url "PUT /v1/backup-storage/image-store/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn "updateImageStoreBackupStorage"
					desc "SSH 用户名 (用于 Ansible 部署)"
					location "body"
					type "String"
					optional true
					since "1.6"
				}
				column {
					name "password"
					enclosedIn "updateImageStoreBackupStorage"
					desc "SSH 用户密码"
					location "body"
					type "String"
					optional true
					since "1.6"
				}
				column {
					name "hostname"
					enclosedIn "updateImageStoreBackupStorage"
					desc "镜像服务器主机名"
					location "body"
					type "String"
					optional true
					since "1.6"
				}
				column {
					name "sshPort"
					enclosedIn "updateImageStoreBackupStorage"
					desc "SSH 端口号"
					location "body"
					type "Integer"
					optional true
					since "1.6"
				}
				column {
					name "uuid"
					enclosedIn "updateImageStoreBackupStorage"
					desc "镜像服务器的UUID"
					location "url"
					type "String"
					optional false
					since "1.6"
				}
				column {
					name "name"
					enclosedIn "updateImageStoreBackupStorage"
					desc "镜像服务器的新名称"
					location "body"
					type "String"
					optional true
					since "1.6"
				}
				column {
					name "description"
					enclosedIn "updateImageStoreBackupStorage"
					desc "镜像服务器的更新详细描述"
					location "body"
					type "String"
					optional true
					since "1.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.6"
				}
			}
		}

		response {
			clz APIUpdateBackupStorageEvent.class
		}
	}
}