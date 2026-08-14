#!/usr/bin/env python
# coding=utf-8
#
# This script is expected to be placed under ${basedir}

TargetClasses = [
    "org/zstack/mevoco/MevocoManagerImpl\\$LicenseeHlper.class",
]

def updateClassFile(fname):
    with open(fname, "r+") as f:
        buf = f.read()
        f.seek(0)               # rewind
        f.write(buf.replace('LicenseeHlper', 'LicenseHelper', 1))

def obfusticate(jar):
    import os, string, shutil
    argstr = "./target/%s %s" % (jar, string.join(TargetClasses, ' '))
    os.system("/usr/bin/unzip -o " + argstr)

    for f in TargetClasses:
        updateClassFile(f.replace("\\", ""))

    os.system("/usr/bin/jar uf " + argstr)

    for d in set([classfile.split('/')[0] for classfile in TargetClasses]):
        shutil.rmtree(d)

if __name__ == '__main__':
    import sys
    # python this-script.py jarfile
    obfusticate(sys.argv[1])

# vim: set ts=4 sw=4 ai et:
