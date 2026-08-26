#!/bin/bash

# usage: ./find_all_tests.sh > testCasePath

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
    module_name="${module#module-}"

    # find ${module}/pom.xml
    pom_file=`find -regex ".*${module_name}/pom.xml" | head -n 1`
    if [ -z "$pom_file" ]; then
        continue
    fi

    # filter the tests in this module
    module_path=`dirname $pom_file`
    tests=`find -regex "${module_path}/.*Test.groovy"`
    if [ -z "$tests" ]; then
        continue
    fi

    for test in $tests; do
        echo $test
    done
done
