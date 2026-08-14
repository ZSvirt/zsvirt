
## Name

```
vm.metadata.flush.batchSize(元数据脏行批量认领数)
```

### Description

```
Max dirty rows claimed per poll cycle
```

### 含义

```
Poller每次轮询周期中认领的最大脏行数量，认领后的每一行提交独立的刷盘任务
```

### Type

```
java.lang.Integer
```

### Category

```
vm
```

### 取值范围

```
[0, 1000]
```

### 取值范围补充说明

```
实际并发刷盘数还受flush.concurrency限制，超出并发上限的任务会被节流释放
```

### DefaultValue

```
20
```

### 默认值补充说明

```
默认20，配合默认concurrency=10使用
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
认领采用原子化UPDATE...LIMIT操作，避免多管理节点竞争
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
该值建议 >= flush.concurrency，否则每轮Poller认领的行数不足以填满并发槽位
```
