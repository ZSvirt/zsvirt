package org.zstack.softwarePackage.header

doc {
	title "UploadSoftwarePackageToBackupStorage"

	category "softwarePackage"

	desc """上传软件包到镜像存储"""

	rest {
		request {
			url "POST /v1/software-packages/backup-storage/upload"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadSoftwarePackageToBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "软件包类型"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "软件包URL"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "installPath"
					enclosedIn "params"
					desc "安装路径"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APIUploadSoftwarePackageToBackupStorageEvent.class
		}
	}
}