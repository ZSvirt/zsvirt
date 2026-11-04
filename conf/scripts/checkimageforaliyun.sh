#!/bin/bash
# usage tools: ssh ${guestip}; sudo bash checkaliyunimage -p [path-for-logs]
# usage this scripts: sudo bash checkimageforaliyun [path-for-image] [path-for-scripts]
set -e
if test -z "$1"; then
  echo "must input image"
  exit 1
fi
image=$1
path=`pwd`"/checkaliyunimage"
if [ "$#" -eq 2 ];then
  path=$2
fi
tmpfile=`mktemp /tmp/checkfile.XXXXXX`
ret=`sudo bash ${path} -p /tmp $image`
echo $ret > $tmpfile
reports=`awk -F "The report is generated:" '{print $2}' $tmpfile|awk -F "Please read the report to check the details" '{print $1}'`
rm -f $tmpfile

return_code=0
check_image() {
  ret=`cat $1`
  if [[ $ret =~ "Failed:" ]];then
      eval cat $1
      if [[ $2 =~ "FAILED" ]];then
          return_code=2
      else
          return_code=3
      fi
  fi
}


test `eval echo $reports`
check_image `eval echo $reports` $ret
rm -f `eval echo $reports`
exit $return_code
