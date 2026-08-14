import os
import os.path
import sys
import subprocess
import argparse
import re
import collections
import tempfile
import json
import traceback


DEBUG = False

class ShellCmd(object):
    def __init__(self, cmd, workdir=None, pipe=True):
        self.cmd = cmd
        if pipe:
            self.process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE, cwd=workdir)
        else:
            self.process = subprocess.Popen(cmd, shell=True, cwd=workdir)

        self.return_code = None
        self.stdout = None
        self.stderr = None

    def raise_error(self):
        err = []
        err.append('failed to execute shell command: %s' % self.cmd)
        err.append('return code: %s' % self.process.returncode)
        err.append('stdout: %s' % self.stdout)
        err.append('stderr: %s' % self.stderr)
        raise Error('\n'.join(err))

    def __call__(self, is_exception=True):
        if DEBUG:
            debug('executing shell command[%s]:' % self.cmd)

        (self.stdout, self.stderr) = self.process.communicate()
        if is_exception and self.process.returncode != 0:
            self.raise_error()

        self.return_code = self.process.returncode

        if DEBUG:
            debug(json.dumps({
                "shell" : self.cmd,
                "return_code" : self.return_code,
                "stdout": self.stdout,
                "stderr": self.stderr
            }, ensure_ascii=True, sort_keys=True, indent=4))

        return self.stdout

def shell(cmd, is_exception=True):
    return ShellCmd(cmd)(is_exception)

def shell_no_pipe(cmd, is_exception=True):
    return ShellCmd(cmd, pipe=False)(is_exception)

def shell_return(cmd):
    scmd = ShellCmd(cmd)
    scmd(False)
    return scmd.return_code

class Parser(argparse.ArgumentParser):
    def error(self, message):
        sys.stderr.write('error:%s\n' % message)
        self.print_help()
        sys.exit(1)

class Error(Exception):
    pass

class Keyspace(object):
    def __init__(self):
        self.name = None
        self.schema = []
        self.path = None

def info(msg):
    sys.stdout.write('[INFO] %s\n' % msg)

def warn(msg):
    sys.stdout.write('[WARNING] %s\n' % msg)

def debug(msg):
    if DEBUG:
        sys.stdout.write('[DEBUG] %s\n' % msg)

class Main(object):
    SCHEMA_NAME_REGX = "^V\\d+(\\.\\d+)+__[-_a-zA-Z0-9]+\\.cql$"

    def __init__(self):
        parser = Parser(prog="deploycdb", description='a tool for deploying/upgrading the ZStack Cassandra database', formatter_class=argparse.RawTextHelpFormatter)
        parser.add_argument('-v', help='verbose, print execution details', dest='verbose', action='store_true', default=False)
        parser.add_argument('--schema-folder', help='the root folder where the database schemas are stored', required=True)
        parser.add_argument('--cqlsh', help='the path to the cqlsh command', required=True)
        parser.add_argument('--ip', help='the IP of cassandra server', required=True)
        parser.add_argument('--port', help='the port of cassandra server', type=int, default=9042)
        args, _ = parser.parse_known_args(sys.argv[1:])

        if not os.path.isdir(args.schema_folder):
            raise Error('cannot find the schema folder[%s], or it is not a folder' % args.schema_folder)

        if not os.path.isfile(args.cqlsh):
            raise Error('cannot find the cqlsh[%s], or it is not a file' % args.cqlsh)

        global DEBUG
        DEBUG = args.verbose

        self.args = args
        self.schema_folder = args.schema_folder
        self.cqlsh = args.cqlsh

        self.keyspaces = {}

    def _cqlsh(self, cql):
        return shell("%s %s %s -e '%s'" % (self.cqlsh, self.args.ip, self.args.port, cql))

    def _cqlsh_return_code(self, cql):
        return shell_return("%s %s %s -e '%s'" % (self.cqlsh, self.args.ip, self.args.port, cql))

    def _cqlsh_file(self, file_path):
        return shell("%s %s %s -f %s" % (self.cqlsh, self.args.ip, self.args.port, file_path))

    def _collect_schema(self):
        for keyspace in os.listdir(self.schema_folder):
            kpath = os.path.join(self.schema_folder, keyspace)
            if not os.path.isdir(kpath):
                warn('%s is not a folder, skip it' % kpath)
                continue

            k = Keyspace()
            k.name = keyspace
            k.path = kpath

            for f in os.listdir(kpath):
                if not re.match(self.SCHEMA_NAME_REGX, f):
                    warn('%s is matching schema name pattern, skip it, in %s' % (f, kpath))
                    continue

                k.schema.append(os.path.join(kpath, f))

            self.keyspaces[keyspace] = k
            debug('arranged schemas in %s' % kpath)

    def _test(self):
        try:
            self._cqlsh('DESC KEYSPACES')
        except Error as e:
            raise Error('cannot connect to cassandra, is it running? %s' % str(e))

    def sort_schemas(self, schemas):
        tmp = {}
        for s in schemas:
            sname = os.path.basename(s)
            pair = sname.split('__')
            vname = pair[0]
            vname = vname.lstrip('V')
            tmp[vname] = s

        od = collections.OrderedDict(sorted(tmp.items()))
        return od

    def get_schemas_for_upgrade(self, kspace):
        current_version = shell('%s %s %s -e "use %s; select version from versionco limit 1;" | grep -o "[[:digit:]]\+\(\.[[:digit:]]\+\)\+"' %
                    (self.cqlsh, self.args.ip, self.args.port, kspace.name))

        current_version = current_version.strip(' \t\n\r')
        info('the current version of keyspace[%s] is %s' % (kspace.name, current_version))
        od = self.sort_schemas(kspace.schema)
        schemas_to_apply = []
        for version, schema in od.items():
            if version > current_version:
                schemas_to_apply.append(schema)

        return schemas_to_apply

    def _apply_schemas(self, schemas):
        for s in schemas:
            info('apply schema %s' % s)
            if DEBUG:
                with open(s, 'r') as fd:
                    debug(fd.read())

            self._cqlsh_file(s)

    def _dry_run(self):
        for name, kspace in self.keyspaces.items():
            exist_keyspace = False
            info('dry-run schemas of the keyspace[%s] ...' % name)
            if self._cqlsh_return_code('DESC KEYSPACE %s' % name) != 0:
                schemas = self.sort_schemas(kspace.schema).values()
            else:
                schemas = self.get_schemas_for_upgrade(kspace)
                exist_keyspace = True

            if not schemas:
                continue

            dryrun_keyspace_name = '%s_dryrun' % name
            self._cqlsh('DROP KEYSPACE IF EXISTS %s' % dryrun_keyspace_name)
            _, current_schema_path = tempfile.mkstemp()
            tmp_files = [current_schema_path]
            try:
                if exist_keyspace:
                    shell('%s %s %s -e "DESC KEYSPACE %s" >> %s' % (self.cqlsh, self.args.ip, self.args.port, name, current_schema_path))
                    with open(current_schema_path, 'r') as fd:
                        content = fd.read()
                        content = content.replace(name, dryrun_keyspace_name)

                    with open(current_schema_path, 'w') as fd:
                        fd.write(content)

                    self._cqlsh_file(current_schema_path)

                tmp_schemas = []
                for s in schemas:
                    _, tf_path = tempfile.mkstemp()
                    tmp_files.append(tf_path)
                    with open(s, 'r') as fd:
                        content = fd.read()
                        content = content.replace(name, dryrun_keyspace_name)
                        with open(tf_path, 'w') as fd1:
                            fd1.write(content)

                    tmp_schemas.append(tf_path)

                self._apply_schemas(tmp_schemas)
                info('the keyspace[%s] passed dry-run upgrade' % name)
            finally:
                # ignore error here
                self._cqlsh_return_code('DROP KEYSPACE %s' % dryrun_keyspace_name)
                for f in tmp_files:
                    os.remove(f)

    def _apply(self):
        for name, kspace in self.keyspaces.items():
            info('apply schemas of the keyspace[%s] ...' % name)
            if self._cqlsh_return_code('DESC KEYSPACE %s' % name) != 0:
                od = self.sort_schemas(kspace.schema)
                schemas = od.values()
            else:
                schemas = self.get_schemas_for_upgrade(kspace)

            if not schemas:
                info('no schemas needed to be applied for the keyspace[%s]' % name)
            else:
                self._apply_schemas(schemas)
                info('successfully upgraded the keyspace[%s]' % name)

    def run(self):
        self._test()
        self._collect_schema()
        self._dry_run()
        self._apply()

if __name__ == '__main__':
    try:
        Main().run()
        sys.exit(0)
    except Error as e:
        if DEBUG:
            sys.stderr.write(traceback.format_exc())
        else:
            sys.stderr.write(str(e))
        sys.exit(1)
