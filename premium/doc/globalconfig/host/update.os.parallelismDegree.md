
## Name

```
update.os.parallelismDegree(升级操作系统或云平台依赖包的并发度)
```

### Description

```
The maximum count of host that can update operating system at the same time
```

### 含义

```
当前同时运行升级操作系统或云平台依赖包的物理机的最大数量
```

### Type

```
java.lang.Integer
```

### Category

```
host
```

### 取值范围

```
[-2147483648, 2147483647]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
10
```

### 默认值补充说明

```
目前考虑到升级时间和资源消耗，设置默认值为10
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
修改背景：ZSTAC-66472 / ZSV-9716 集群规模很大的时候升级效率过低，需要提高并发度
```

### UI暴露

```
是否暴露给用户：否
```

### CLI手册暴露

```
是否暴露给用户：否
```

## 注意事项

```
无
```
