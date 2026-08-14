
## Name

```
incrementalSnapshot.maxNum(云盘快照链的最大长度)
```

### Description

```
The length of a volume snapshot chain. When the lenght of a volume snapshot chain reaches this value, the next volume snapshot will be a full snapshot
```

### 含义

```
代表一个云盘快照链的最大长度。当一个云盘的快照链的长度达到这个数字时，下一个云盘快照会是一个完整的快照。
```

### Type

```
java.lang.Integer
```

### Category

```
volumeSnapshot
```

### 取值范围

```
[0, 120]
```

### 取值范围补充说明

```
云盘增量快照数目不应超过120，云盘整个qcow2链长不超过121，当qcow2链长大于121时，qmp query-block无法查询到结果
```

### DefaultValue

```
64
```

### 默认值补充说明

```
无
```

### 支持的资源级配置



### 资源粒度说明

```
##该条目支持的资源粒度-如无需写：无##
```

### 背景信息

```
##触发该条目增删改的背景-如无需写：无##
```

### UI暴露

```
##该条目是否需UI暴露？-必填##
```

### CLI手册暴露

```
##该条目是否需CLI手册暴露？-必填##
```

## 注意事项

```
##该条目有哪些注意事项-如无需写：无##
```
