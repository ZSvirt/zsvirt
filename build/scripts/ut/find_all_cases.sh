#!/bin/bash

# usage: ./find_all_cases.sh > testCasePath

MY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd $MY_DIR
cd ../../../     # under zvirt

# in zsvirt, we has more test module than cloud: (under zsvirt)
#
# test\src\test\resources\UTCapabilities\
# ├── module-test            ==>  test module has cases
# │   └── black.list             ==>  UT black list in test module
# ├── module-test-premium    ==>  test-premium module has cases
# │   └── black.list (optional)  ==>  UT black list in test-premium module
# └── module-test-xxx        ==>  test-xxx module has cases
#     └── black.list (optional)  ==>  UT black list in test-xxx module
# ...

CASELIST=""
for module in `ls test/src/test/resources/UTCapabilities`; do
    module_name=`echo $module | awk -F'-' '{print $2}'`

    # find ${module}/pom.xml
    pom_file=`find -regex ".*${module_name}/pom.xml" | head -n 1`
    if [ -z "$pom_file" ]; then
        continue
    fi

    # filter the cases in this module
    module_path=`dirname $pom_file`
    cases=`find -regex "${module_path}/.*Case.groovy"`
    if [ -z "$cases" ]; then
        continue
    fi

    # filter the cases in the black list
    black_file="test/src/test/resources/UTCapabilities/${module}/black.list"
    black_list=""
    if [ -f "$black_file" ]; then
        black_list=`cat "$black_file"`
    fi

    for case in $cases; do
        skip=0
        case_base=`basename "$case" .groovy`
        for black in $black_list; do
            if [ "$case_base" = "$black" ]; then
                skip=1
                break
            fi
        done
        if [ $skip -eq 0 ]; then
            echo $case
        fi
    done
done
