# MySQL 调优

### 1，所有的表都必须有primary key.
因为需要用primary key来表示每一行，最好用自增的整形

### 2， 使用in代替or
SELECT * FROM t WHERE LOC_IN IN (10,20,30);

### 3， 不使用子查询
使用join

### 4. 足够大的 innodb_buffer_pool_size

最大内存数在总的innoDB内存数据160%就行，比如数据大小5G，buffer size设置为8G就行，有个命令来获取应当设置的大小
```
SELECT CEILING(Total_InnoDB_Bytes*1.6/POWER(1024,3)) RIBPS FROM
(SELECT SUM(data_length+index_length) Total_InnoDB_Bytes
FROM information_schema.tables WHERE engine='InnoDB') A;
```

> 需要注意这个系统刚开始允许，无法计算出实际使用量。一般需要系统实际允许几个月，或者设置成机器物理内存的一半。

---
### 5. 配置 innodb_log_file_size
使用足够大的写入缓存 innodb_log_file_size

可是须要注意假设用 1G 的 innodb_log_file_size 。假如server当机。须要 10 分钟来恢复。

推荐 innodb_log_file_size 设置为 0.25 * innodb_buffer_pool_size

---
### 6. 配置 innodb_flush_log_at_trx_commit

innodb_flush_log_at_trx_commit = 1 则每次改动写入磁盘

innodb_flush_log_at_trx_commit = 0/2 每秒写入磁盘

假设你的应用不涉及非常高的安全性 (金融系统)，或者基础架构足够安全，或者 事务都非常小，都能够用 0 或者 2 来减少磁盘操作。

---
### 7. 避免双写入缓冲

innodb_flush_method=O_DIRECT

---

### 8. innodb_write_io_threads

写IO线程，默认4，最大可设置成64.每个线程可以处理256个pending io request.需要根据您的cpu数和读写比例调整，在当前项目中可设置为物理核数的2倍

---

### 9. innodb_read_io_threads

读IO线程，默认4，最大可设置成64.每个线程可以处理256个pending io request.需要根据您的cpu数和读写比例调整，在当前项目中保持4就可以了

---

### 10. innodb_thread_concurrency

线程并发数，0是不限制。如果设置最大是1000，可以设置成cpu核数的8倍

---
### 11. 关闭swap

对于单独的数据库服务器，在内存足够大的情况下，可以关闭swap
```
sync                         # 先执行下同步
swapoff -a                   # 关闭swap分区
swapon -a                    # 开启swap分区
swapoff -a && swapon -a      # 刷新swap空间，即将SWAP里的数据转储回内存，并清空SWAP里的数据。刷新原理就是把swap关闭后再重启。
```

---
### 12. DB连接数
show global variables like "max_connections"; 显示最大连接数
SET GLOBAL max_connections = 2000; 设置最大连接数

### 13. c3p0连接池
修改最大连接数，注意需要确保DB能撑得起集群的最大连接数

### 14. 示例
mysqld.cnf配置
```
[mysqld]
    # Uncomment the following if you are using InnoDB tables
    #innodb_data_home_dir = /usr/local/mysql/data
    #innodb_data_file_path = ibdata1:10M:autoextend
    #innodb_log_group_home_dir = /usr/local/mysql/data
    # You can set .._buffer_pool_size up to 50 - 80 %
    # of RAM but beware of setting memory usage too high
    innodb_buffer_pool_size = 2G
    # Set .._log_file_size to 25 % of buffer pool size
    innodb_log_file_size = 512M
    innodb_log_buffer_size = 64M
    innodb_flush_log_at_trx_commit = 2
    innodb_flush_method=O_DIRECT
    #innodb_lock_wait_timeout = 50
```

> 以上信息来源于

> https://www.cnblogs.com/jxldjsn/p/6010720.html

> https://www.percona.com/blog/2016/10/12/mysql-5-7-performance-tuning-immediately-after-installation/

> https://www.cnblogs.com/claireyuancy/p/7258314.html

> http://www.codingpedia.org/ama/optimizing-mysql-server-settings/
