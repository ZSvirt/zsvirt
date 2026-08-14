
## Name

```
vm.metadata.maintenance.orphanCheckInterval(元数据孤儿脏行检测间隔)
```

### Description

```
Orphan dirty-row detector interval in seconds. Releases rows claimed by dead management nodes.
```

### 含义

```
孤儿脏行检测任务的执行间隔，单位为秒。查找认领者管理节点已不存在的脏行并释放
```

### Type

```
java.lang.Long
```

### Category

```
vm
```

### 取值范围

```
[0, 86400]
```

### 取值范围补充说明

```
间隔过短增加DB查询开销，过长则dead MN遗留的脏行无法及时被重新处理
```

### DefaultValue

```
3600
```

### 默认值补充说明

```
默认3600秒（1小时）
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
管理节点正常下线时FK SET_NULL自动释放认领。该任务处理异常退出的情况，不使用Hash Ring，所有管理节点均参与
```

### UI暴露

```
暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
该任务不使用Hash Ring，所有管理节点均独立执行，操作是幂等的
```
