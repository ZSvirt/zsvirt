package org.zstack.softwarePackage.header

doc {
	title "CleanSoftwarePackage"

	category "softwarePackage"

	desc """清理软件包"""

	rest {
		request {
			url "DELETE /v1/software-package/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanSoftwarePackageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "软件包UUID"
					location "url"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APICleanSoftwarePackageEvent.class
		}
	}
}