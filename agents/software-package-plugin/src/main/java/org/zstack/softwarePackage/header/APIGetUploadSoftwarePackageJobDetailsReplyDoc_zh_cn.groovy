package org.zstack.softwarePackage.header


import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取上传软件包任务详情返回"

	ref {
		name "existingJobDetails"
		path "org.zstack.softwarePackage.header.APIGetUploadSoftwarePackageJobDetailsReply.existingJobDetails"
		desc "现有的上传任务详情列表"
		type "List"
		since "4.10.20"
		clz JobDetails.class
	}
	field {
		name "success"
		desc "操作成功时为true，否则为false"
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.softwarePackage.header.APIGetUploadSoftwarePackageJobDetailsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
