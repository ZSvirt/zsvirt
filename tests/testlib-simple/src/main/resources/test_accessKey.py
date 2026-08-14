#/usr/bin/python
from zssdk import *
from inventory import *
import argparse

parser = argparse.ArgumentParser(description='test vm operation by accessKey')
parser.add_argument('host')
parser.add_argument('accessKeyId')
parser.add_argument('accessKeySecret')
 
args = parser.parse_args()

configure(hostname=args.host, context_path="/zstack")

action = CreateVmInstanceAction()
action.name = "vm1"
action.instanceOfferingUuid = "ab60e633db5c44f0aaf8de39aa265f05"
action.imageUuid = "b154382c04bc3f09af6152c6c39e25aa"
action.l3NetworkUuids = ["2ede66f8669c4f71976ce8efee79edf7"]
action.accessKeyId = args.accessKeyId
action.accessKeySecret = args.accessKeySecret
res = action.call()
vm = res.value.inventory

print "create a new vm [uuid:%s], name:%s" % (vm.uuid, vm.name)

updateAction = UpdateVmInstanceAction()
updateAction.uuid = vm.uuid
updateAction.description = "updated by accessKey"
updateAction.accessKeyId = args.accessKeyId
updateAction.accessKeySecret = args.accessKeySecret
res = updateAction.call()
vm = res.value.inventory

print "after updated, new description is %s" % vm.description

qaction = QueryVmInstanceAction()
qaction.conditions=["uuid=%s" % vm.uuid]
qaction.accessKeyId = args.accessKeyId
qaction.accessKeySecret = args.accessKeySecret
res = qaction.call()
vm = res.value.inventories

print "there are %d vm" % len(vm)

daction = DestroyVmInstanceAction()
daction.uuid = vm[0].uuid
daction.deleteMode = "Enforcing"
daction.accessKeyId = args.accessKeyId
daction.accessKeySecret = args.accessKeySecret
res = daction.call()

qaction = QueryVmInstanceAction()
qaction.conditions=["uuid=%s" % vm[0].uuid]
qaction.accessKeyId = args.accessKeyId
qaction.accessKeySecret = args.accessKeySecret
res = qaction.call()
vm = res.value.inventories

