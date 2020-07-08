# netcrazy
springboot的网络性能压榨

## 最大TCP ESTABLISHED连接数
CentOS Linux release 7.6.1810 (Core)
Linux version 3.10.0-957.21.3.e17.x86_64

![tcp_max_conn](image/tcp_max_conn.png)

结论：

​	accpetCount对应是socket listen函数的backlog参数，可以通过ss命令查看，如下其中的Send-Q的值为backlog。tcp维护着两个队列，分别为未完成连接队列（状态为SYN_RECV）和已完成连接队列（状态为ESTABLISHED）。已完成队列是完成三次握手等待服务端accpet，backlog可以控制已完成队列的大小，不同系统的backlog参数实现会有差异，根据上面的测试数据可以得出CentOS Linux release 7.6.1810 (Core)完成队列的实际大小等于backlog+1。同时backlog受/proc/sys/net/core/somaxconn内核参数限制（默认128），可以修改somaxconn以增大backlog，也就是tomcat的accpetCount参数。

```
ss -antl |awk 'NR==1 ||$4~/8080/'
State      Recv-Q Send-Q Local Address:Port               Peer Address:Port              
LISTEN     0      2         [::]:8080                  [::]:*
```

性能测试

服务器主机参数 2C8G 千兆带宽 openjdk-1.8

测试主机参数 2C8G

socket 参数 backlog 10000 (tomcat[acceptAcount=10000,MaxConnections=10000]、jetty[server.jetty.acceptQueueSize=10000]、undertow[server.undertow.backlog=10000])

虚拟机参数  -Xmx2G -Xms2G 

预热 三次1B  wrk -c 1000 -d 30s --timeout=30s 

数据量1B 压测30s

10000连接

undertow 30403、30824、31571

tomcat 20317、20864、20502

jetty 20188、20911、20591

1000连接

undertow 33253、34083、33393

tomcat      22597、22399、22274

jetty           21381、22250、22269

100连接

undertow 23246、23690、25973

tomcat      15008、15089、15017

jetty      17174、17628、18162



数据量 2560B (2.5K)

10000连接

undertow 9286、9088、10012

tomcat 10048、10159、10048

jetty    4307、6420、6358

1000连接 

undertow  9349、9576、8976

tomcat  9860、10550、10629

jetty 6665、6597、6795

100连接

undertow 8334、7974、8014

tomcat 8927、8861、9169

jetty     6622、6877、6736



数据量 256000B (250K)

10000连接

undertow 201、204、199.38

tomcat 197、196、200

jetty   8.68、8.12、8.73

1000连接 

undertow 196/48、205/50、211/51 

tomcat 225、222、217

jetty  8.62、8.29、8.48

100连接

undertow 197/48、194/47、196/48

tomcat 228、227、223

jetty 9.92、9.96、10.09



tomcat占用内存高，连接多的时候time_wait多，undertow内存占用大概是tomcat的一半，jetty消耗内存最大，gc频繁