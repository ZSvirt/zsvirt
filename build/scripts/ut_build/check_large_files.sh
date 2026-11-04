#!/bin/bash

# check large files in the project (in git repo). All files larger than 10MB will be printed.
# Usage: check_large_files.sh [base_commit]
#   With base_commit: scan objects in base_commit..HEAD
#   Without: scan objects reachable from HEAD
# Output: one line per file as "path size_in_bytes"; empty output and exit 0 if none.
#
# Compatible with git >= 1.8.3: avoids %(rest) in cat-file --batch-check (needs 1.8.5+).

MY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd $MY_DIR
cd ../../../     # under zvirt

# Files in this list are ignored even if larger than the threshold
# (match by full repo-relative path, or by basename).
EXCLUDE_FILES=(
    "tests/testlib-simple/src/main/java/org/zstack/testlib/ApiHelper.groovy"
    "conf/tools/ansible-4.10.0-py2.py3-none-any.whl"
)

MIN_GIT_VERSION="1.8.3"

version_ge() {
    # return 0 if $1 >= $2
    [ "$(printf '%s\n' "$1" "$2" | sort -V | head -n1)" = "$2" ]
}

git_version="$(git --version 2>/dev/null | awk '{print $3}')"
if [ -z "${git_version}" ] || ! version_ge "${git_version}" "${MIN_GIT_VERSION}"; then
    echo "error: git >= ${MIN_GIT_VERSION} is required (found: ${git_version:-none})" >&2
    exit 1
fi

THRESHOLD=10485760

if [ -n "${1:-}" ]; then
    range="${1}..HEAD"
else
    range="HEAD"
fi

exclude_csv="$(IFS=,; echo "${EXCLUDE_FILES[*]}")"

tmp_objects="$(mktemp)"
tmp_info="$(mktemp)"
trap 'rm -f "${tmp_objects}" "${tmp_info}"' EXIT

# Lines with a path: "<sha> <path>"; feed only sha to cat-file (1.8.3 cannot use %(rest)).
git rev-list --objects "${range}" | awk 'NF >= 2 { print }' > "${tmp_objects}"
if [ ! -s "${tmp_objects}" ]; then
    exit 0
fi

awk '{ print $1 }' "${tmp_objects}" | git cat-file --batch-check > "${tmp_info}"

awk -v threshold="${THRESHOLD}" -v exclude_csv="${exclude_csv}" '
    BEGIN {
        n = split(exclude_csv, arr, ",")
        for (i = 1; i <= n; i++) {
            if (arr[i] != "") {
                exclude[arr[i]] = 1
            }
        }
    }
    function basename(p,   n, a) {
        n = split(p, a, "/")
        return a[n]
    }
    # First file: "<sha> <type> <size>"
    FNR == NR {
        if ($2 == "blob" && ($3 + 0) > threshold) {
            size[$1] = $3
        }
        next
    }
    # Second file: "<sha> <path...>"
    {
        sha = $1
        if (!(sha in size)) {
            next
        }
        $1 = ""
        sub(/^ +/, "")
        path = $0
        if (path in exclude || basename(path) in exclude) {
            next
        }
        print path, size[sha]
    }
' "${tmp_info}" "${tmp_objects}"

exit 0
