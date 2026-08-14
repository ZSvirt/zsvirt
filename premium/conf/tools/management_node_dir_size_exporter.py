import os
import sys
import shutil

if len(sys.argv) < 4:
    print "need directory lists"
    sys.exit(1)

mgmt_node_ip = sys.argv[1]
prom_file_path = sys.argv[2]
dir_to_monitor = sys.argv[3:]
output = os.popen('df -B 1 %s | tail -%s' % (' '.join(dir_to_monitor), len(dir_to_monitor))).read()

metrics = []
for idx, line in enumerate(output.split('\n')):
    if line.strip(' \t\r\n') == "":
        continue

    _, total, used, free, _, _ = line.split()
    dir = dir_to_monitor[idx]
    metrics.append('management_node_directory_used_bytes{ip="%s", dir="%s"} %s' % (mgmt_node_ip, dir, used))
    metrics.append('management_node_directory_free_bytes{ip="%s", dir="%s"} %s' % (mgmt_node_ip, dir, free))
    metrics.append('management_node_directory_used_percent{ip="%s", dir="%s"} %s' % (mgmt_node_ip, dir, (float(used) / float(total)) * 100))
    metrics.append('management_node_directory_free_percent{ip="%s", dir="%s"} %s' % (mgmt_node_ip, dir, (float(free) / float(total)) * 100))

metrics.append("")

tmp_file_path = '%s.$$' % prom_file_path

with open(tmp_file_path, 'w') as fd:
    fd.writelines('\n'.join(metrics))

shutil.move(tmp_file_path, prom_file_path)
