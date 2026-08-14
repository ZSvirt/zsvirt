package org.zstack.storage.backup.sftp

import org.zstack.header.storage.backup.APIUpdateBackupStorageEvent

doc {
	title "更新Sftp镜像服务器属性(UpdateSftpBackupStorage)"

	category "storage.backup.sftp"

	desc """更新Sftp镜像服务器属性"""

	rest {
		request {
			url "PUT /v1/backup-storage/sftp/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSftpBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn "updateSftpBackupStorage"
					desc "镜像服务器登录用户名"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "updateSftpBackupStorage"
					desc "镜像服务器登录密码"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "hostname"
					enclosedIn "updateSftpBackupStorage"
					desc "镜像服务器所在的物理机地址"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "updateSftpBackupStorage"
					desc "镜像服务器所在物理机的登录端口"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "uuid"
					enclosedIn "updateSftpBackupStorage"
					desc "镜像服务器的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateSftpBackupStorage"
					desc "镜像服务器名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateSftpBackupStorage"
					desc "镜像服务器的详细描述"
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
			}
		}

		response {
			clz APIUpdateBackupStorageEvent.class
		}
	}
}