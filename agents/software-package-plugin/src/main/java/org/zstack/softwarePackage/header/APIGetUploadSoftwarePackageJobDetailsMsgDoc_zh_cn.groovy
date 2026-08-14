package org.zstack.softwarePackage.header

doc {
	title "GetUploadSoftwarePackageJobDetails"

	category "softwarePackage"

	desc """获取上传软件包任务详情"""

	rest {
		request {
			url "GET /v1/software-package/upload-jobs/details/{softwarePackageId}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetUploadSoftwarePackageJobDetailsMsg.class

			desc """"""

			params {

				column {
					name "softwarePackageId"
					enclosedIn ""
					desc "软件包Id(默认为hash值)"
					location "url"
					type "String"
					optional false
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
			clz APIGetUploadSoftwarePackageJobDetailsReply.class
		}
	}
}