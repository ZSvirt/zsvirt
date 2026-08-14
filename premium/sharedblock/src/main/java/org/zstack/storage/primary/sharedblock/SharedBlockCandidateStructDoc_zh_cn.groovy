package org.zstack.storage.primary.sharedblock

import java.lang.Long

doc {

	title "共享块设备候选清单"

	category "sharedblock"

	field {
		name "wwid"
		desc "设备WWID"
		type "String"
		since "2.5.0"
	}
	field {
		name "vendor"
		desc "设备供应商"
		type "String"
		since "2.5.0"
	}
	field {
		name "model"
		desc "设备型号"
		type "String"
		since "2.5.0"
	}
	field {
		name "wwn"
		desc "设备WWN"
		type "String"
		since "2.5.0"
	}
	field {
		name "serial"
		desc "设备序列号"
		type "String"
		since "2.5.0"
	}
	field {
		name "hctl"
		desc "SCSI设备HCTL"
		type "String"
		since "2.5.0"
	}
	field {
		name "type"
		desc "设备类型"
		type "String"
		since "2.5.0"
	}
	field {
		name "size"
		desc "设备大小"
		type "Long"
		since "2.5.0"
	}
}
