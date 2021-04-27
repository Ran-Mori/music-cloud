# NeteaseMusicCloudDisk
用go语言写的一个云音乐云盘

## 概述

> * 之前用Java写了一个云音乐云盘的播放器项目
> * 现在初学go语言，打算把云音乐云盘的后端代码用go全部重构一遍，因此有了这个项目
> * 即此项目是本人第一个go语言项目，也十分希望能够重构成功。
>
> ***

## 数据库连接部分

> ### 数据库原理
>
> * 个人感觉go语言连接数据库比java轻松很多，或许说GDBC比JDBC优秀许多
> * go语言毕竟是新时代下产物，之前老语言的很多糟粕都可以进行舍弃而不必考虑向后的兼容性。这点是十分不错的
>
> ### 连接代码
>
> ```go
> package basic
> 
> import (
> 	"database/sql"
> 	_ "github.com/go-sql-driver/mysql"
> 	"log"
> 	"strings"
> )
> 
> var DB = new(sql.DB)
> 
> const (
> 	Host = "47.108.63.126"
> 	Port = "3306"
> 	Username = "root"
> 	Password = "542270191MSzyl"
> 	Database = "netease"
> 	DriverName = "mysql"
> )
> 
> func InitDataBase() {
> 	//数据库连接格式和jdbc不一样
> 	//格式为"username:password@tcp(ip:port)/database?charset=utf8&parseTime=True&loc=Local"
> 	path := strings.Join([]string{Username,":",Password,"@tcp(",Host,":",Port,")/",Database,"?charset=utf8&parseTime=True&loc=Local"},"")
> 	db, err := sql.Open(DriverName, path)
> 	if  err != nil{
> 		log.Println(err)
> 		return
> 	}
> 	DB = db
> 	DB.SetConnMaxLifetime(100)
> 	DB.SetConnMaxIdleTime(10)
> 	if DB.Ping() != nil{
> 		log.Fatalf("fail to ping database : %s\n",Database)
> 	}
> 	log.Printf("success to connect to database : %s\n",Database)
> }
> ```
>
> ### 数据库使用
>
> * 首先执行自定义初始化数据库函数`basic.InitDataBase()`
> * 接着就可以调用`rows, err := basic.DB.Query("select * from song")`进行查询，注意返回的结果是`rows`而不是传统认为的数组。还要自己做一遍ORM映射
>
> ### 查询使用
>
> ```go
> func (songDao *SongDao)QueryAll() []entity.Song{
> 	rows, err := basic.DB.Query("select * from song")
> 	if err != nil {
> 		 log.Println(err)
> 		 return nil
> 	}
> 	songs := make([]entity.Song,0)
> 	for rows.Next() {
> 		var song entity.Song
> 		err := rows.Scan(&song.Id, &song.Singer, &song.Name)
> 		if err != nil {
> 			log.Println(err)
> 			continue
> 		}
> 		songs = append(songs,song)
> 	}
> 	rows.Close()
> 	return songs
> }
> ```
>
> ### 整理
>
> * 首先是MySQL驱动，未直接使用到使用 **_** 代替 `import ( _ "github.com/go-sql-driver/mysql" )`，不然基于go语言引入必须使用的原则会报错
> * const常量即连接数据库那几个最基本的参数，不必多说
> * 数据库连接路径还是有必要记录一下，因为和java不一样。`username:password@tcp(ip:port)/database?charset=utf8&parseTime=True&loc=Local`
> * 核心连接操作也是巨傻瓜，巨无脑。直接`db, err := sql.Open(DriverName, path)`就搞定，真的是简单到离谱
>
> ***

## Controller

> ### 流程
>
> * 创建一个服务器`server :=&http.Server{Addr: "127.0.0.1:80", ReadTimeout: 5 * time.Second, Handler: basic.Router,}`
>
> * 服务器server的一个属性是`Handler`，这是一个接口，此有`ServeHTTP(http.ResponseWriter,*http.Request)`方法
>
> * 即服务器是通过`Handler`来进行不同的URL的映射controller处理
>
> * 自定义实现的是一个`RouterHandler`，属性代码如下
>
>   ```go
>   type RouterHandler struct {}
>   
>   var Router = new(RouterHandler)//对外暴露的一个对象
>   var RouterMap = make(map[string]func(http.ResponseWriter,*http.Request))//存储URL到处理controller方法的映射
>   
>   func (p *RouterHandler)ServeHTTP(w http.ResponseWriter, r *http.Request) {}//Handler接口必须实现的一个方法
>   
>   func (p *RouterHandler)Router(relativePath string,handler func(http.ResponseWriter,*http.Request)){}//向RouterMap中添加路径和处理的映射
>   ```
>
> * SongController实现
>
>   ```go
>   type SongController struct {}
>     
>   var songDao = new(dao.SongDao)
>     
>   func (s *SongController)Router(handler *basic.RouterHandler){}//用于向RouterHandler添加URL到方法的映射
>     
>   func (s *SongController)QueryAll(w http.ResponseWriter,r *http.Request){}//实际的
>   ```

> ### 总结
>
> * 首先得有一个自定义实现的`Handler`，这个`Handler`主要是实现了`ServeHTTP(http.ReponseWriter, *http.Request)`接口，将不同的URL调用不同的方法进行处理
> * `SongController`主要是写业务代码，然后还要对外暴露`func (s *SongController)Router(handler *basic.RouterHandler){}`方法
> * 最后在主函数中调用`(s *SongController)Router`方法，初始化Map容器，然后http进行监听。
>
> ***