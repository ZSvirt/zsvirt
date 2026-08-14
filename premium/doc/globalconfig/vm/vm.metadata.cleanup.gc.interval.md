
## Name

```
vm.metadata.cleanup.gc.interval(元数据清理GC重试间隔)
```

### Description

```
Metadata cleanup GC retry interval in hours
```

### 含义

```
云主机销毁后清理主存储上元数据文件失败时，GC队列定期重试的间隔，单位为小时
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
[0, 168]
```

### 取值范围补充说明

```
最大167小时（约7天）
```

### DefaultValue

```
8
```

### 默认值补充说明

```
默认8小时重试一次
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
VM彻底删除时执行元数据清理，清理失败后通过GC机制定期重试
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
无
```
