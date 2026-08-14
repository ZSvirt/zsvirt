package org.zstack.softwarePackage.header

doc {
	title "InstallSoftwarePackage"

	category "softwarePackage"

	desc """安装软件包"""

	rest {
		request {
			url "PUT /v1/software-package/install/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIInstallSoftwarePackageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "installSoftwarePackage"
					desc "软件包UUID"
					location "url"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "config"
					enclosedIn "installSoftwarePackage"
					desc "安装软件包配置"
					location "body"
					type "String"
					optional true
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIInstallSoftwarePackageEvent.class
		}
	}
}