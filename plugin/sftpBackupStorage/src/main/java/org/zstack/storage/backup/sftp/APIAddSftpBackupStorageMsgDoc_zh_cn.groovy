package org.zstack.storage.backup.sftp

import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageEvent

doc {
	title "添加sftp镜像服务器(AddSftpBackupStorage)"

	category "storage.backup.sftp"

	desc """添加sftp镜像服务器"""

	rest {
		request {
			url "POST /v1/backup-storage/sftp"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSftpBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "hostname"
					enclosedIn "params"
					desc "服务器主机地址"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "username"
					enclosedIn "params"
					desc "服务器 SSH 用户名 (用于 Ansible 部署)"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "服务器 SSH 用户密码"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "params"
					desc "服务器 SSH 端口"
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "sftp数据存放地址"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "sftp镜像服务器名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "sftp镜像服务器的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "这里是 SftpBackupStorage"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "importImages"
					enclosedIn "params"
					desc "是否导入镜像"
					location "body"
					type "boolean"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，镜像服务器会使用该字段值作为UUID"
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
			clz APIAddSftpBackupStorageEvent.class
		}
	}
}