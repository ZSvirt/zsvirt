package org.zstack.softwarePackage.header

doc {
	title "UploadAndExecuteSoftwareUpgradePackage"

	category "softwarePackage"

	desc """上传并更新软件包"""

	rest {
		request {
			url "POST /v1/software-packages/backup-storage/{uuid}/upgrade"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadAndExecuteSoftwareUpgradePackageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "软件包资源的UUID"
					location "url"
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
					desc ""
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "installPath"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "upgradeType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.0.0"
					values ("Normal","Reexecute")
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
			clz APIUploadAndExecuteSoftwareUpgradePackageEvent.class
		}
	}
}