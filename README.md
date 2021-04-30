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
> * 首先执行自定义初始化数据库函数`basic.InitDataBase()`、
> * 不要忘记最后要关闭数据库连接`defer basic.DB.Close()`
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

## 自定义路由

> ### 流程
>
> * 创建一个服务器`server :=&http.Server{Addr: "127.0.0.1:80", ReadTimeout: 5 * time.Second, Handler: basic.Router,}`
>
> * 服务器server的一个属性是`Handler`，这是一个接口，含有`ServeHTTP(http.ResponseWriter,*http.Request)`方法
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
> * `SongController`主要是写业务代码，然后还要对外暴露`func (s *SongController)Router(handler *basic.RouterHandler){}`方法用于添加路由
> * 最后在主函数中调用`(s *SongController)Router`方法，初始化Map容器，然后http进行监听。
>
> ***

## HTTPRouter路由

> ### 导入依赖
>
> * `import github.com/julienschmidt/httprouter v1.3.0`
>
> ### 路径匹配规则
>
> * 冒号匹配
>
>   ```shell
>   Path: "/blog/:category/:id" 
>   匹配 "/blog/daily/4" 即category = daily，id = 3
>   ```
>
> * *匹配
>
>   ```shell
>   Path: "/files/*filePath"
>   匹配 "/files/opt" 即filePath = /opt
>   ```
>
> ### handler方法格式
>
> * `func(w http.ResponseWriter, r http.Requset, params httprouter.Params)`
> * `httprouter.Params`是指使用`:`和`*`匹配到的参数，当未使用参数匹配时可以使用`_`代替
> * 但必须要有，因为`httpRouter`框架规定handler的格式必须是这个
>
> ***

## 自定义Error类型

> ### 原本Error类型
>
> ```go
> type Error interface{
>     Error() string
> }
> ```
>
> ### 注意
>
> * 在go中接口定义的函数名字没有实际意义
> * 实现接口时只需方法签名类型相同就行
>
> ### 自定义Error
>
> ```go
> type MyError struct{
>     ErrMsg string
> }
> func (err *MyError)ShowErrMsg() string{
>     return err.Errmsg
> }
> ```
>
> ### 返回Error
>
> * `return basic.Error{ErrMsg: "parameter null"}`
>
> ### 理解
>
> * go语言中类即结构的定义只定义成员属性
> * 成员方法是通过函数的接收者来进行定义
>
> ***

## entity实体类json映射

> ### 实体类定义
>
> ```go
> type Song struct {
> 	Id int `json:"id"`
> 	Singer string `json:"singer"`
> 	Name string `json:"name"`
> }
> ```
>
> ### 注意
>
> * 最好在实体类定义前就做好json映射，确保json的key是自己预设的值
>
> ***

## 路由冲突

> ### 原因
>
> * `router.Get("/song/:id",s.SelectOneById)`和`router.Get("/song/download",s.Download)`是冲突的
>
> ### 解决方式
>
> * 增加一层判断
> * router定义`router.Get("/song/:id",s.GetSongHandler)`
> * `GetSongHandler`实现
>
> ```go
> func (s SongController)GetSongHandler(w http.ResponseWriter,r *http.Request,params httprouter.Params){
> 	idValue := params.ByName("id")
> 	if strings.HasPrefix(idValue,"download"){
> 		s.DownLoad(w,r,params)
> 	}else {
> 		s.SelectOneById(w,r,params)
> 	}
> }
> ```
>
> * 即使用`strings.HasPrefix(source string, prefix string) bool`来做一个区分
>
> ***

## 妥协Java的返回json格式定义

> ### java中返回的json如下
>
> ```json
> {
>     "code": 200,
>     "message": "发送成功",
>     "data": {
>         "songs": [
>         	{},
>             {}
>     	]
> 	}
> }
> ```
>
> * 因此go的返回格式尽量得匹配，不然还得修改前端代码
>
> ### 定义一个结构
>
> ```go
> type ResultForceByJava struct {
> 	Code int `json:"code"`
> 	Message string `json:"message"`
> 	Data map[string]interface{} `json:"data"`
> }
> ```
>
> * 其中go的`interface{}`有点类似于java中的`Object`，什么都能装
>
> ### 返回过程
>
> ```go
> songsMap := make(map[string]interface{})
> songsMap["songs"] = songs
> var result = ResultForceByJava{
>     Code: 200,
>     Message: "发送成功",
>     Data: songsMap,
> }
> json,_ := json.Marshal(result)
> w.Write(json)
> ```
>
> ***

## Upload逻辑

> ### 核心源代码
>
> ```go
> //从请求r的FormFile中获取到文件流
> //header中包含此文件流的名字和大小等信息
> //file就是一个二进制流
> file, header, err := r.FormFile("file") 
> defer file.Close()
> 
> //获取文件的名字即'BEYOND - 海阔天空.mp3'
> //然后在做一下拆分获取歌手和歌曲名
> originalFilename := header.Filename
> _index := strings.Index(originalFilename,"-")
> singer := originalFilename[0 : _index - 1]
> name := originalFilename[_index + 2 : strings.Index(originalFilename,".")]
> 
> //执行数据库查询操作
> var song = entity.Song{Name: name,Singer: singer}
> s.Insert(&song)
> 
> //创建云服务硬盘的文件
> dist, _ := os.Create(strings.Join([]string{basic.MusicBasePath, originalFilename}, ""))
> defer dist.Close()
> 
> //做一个流复制操作
> if _, err := io.Copy(dist, file); err != nil{
>     log.Println(err)
> }else {
>     w.Write([]byte("successful to upload"))
> }
> ```
>
> ***

## Download逻辑

> ### 核心代码
>
> ```go
> //从URL中获取歌手和歌名信息
> getParams := r.URL.Query()
> songName := getParams.Get("songname")
> singer := getParams.Get("singer")
> 
> //确定要在硬盘上读取的流文件的路径
> filePath := strings.Join([]string{basic.MusicBasePath,singer," - ",songName,".mp3"},"")
> //获取磁盘上的文件流
> file, err := os.Open(filePath)
> defer file.Close()
> 
> //磁盘文件是完整路径如'/home/music/BEYOND - 海阔天空.mp3'，因此要把前面的路径去掉
> index := strings.Index(file.Name(),singer)
> //设置为强制下载
> w.Header().Set("content-type","application/force-download")
> //设置disk缓存，二次听歌不用拉取
> w.Header().Set("cache-control","max-age=31536000")
> //设置下载的歌曲的名字
> w.Header().Set("content-disposition",strings.Join([]string{"attachment;fileName=",file.Name()[index:len(file.Name())]},""))
> //把磁盘流复制到respond流
> io.Copy(w,file)
> ```
>
> ***

## 错误和经验总结

> ### golang被墙
>
> * `go env -w GOPROXY=https://goproxy.io,direct`
>
> ### golang中的nil
>
> * golang中的`nil`只能代表指针，即nil实际上是`int`类型，代表一个内存地址
> * 即当函数的返回值是一个实体，如返回值为`entity.Song`时，不能使用`reetrun nil`返回
> * 要么使用`return entity.Song{}`退而求其次返回，要不直接把函数返回值声明成指针`*entity.Song`
>
> ### 从get请求URL中获取参数
>
> * `value := http.Request.URL.Query().Get("key")`
> * 获取的参数只指URL中`? &`这种参数
> * 感觉比SpringBoot的框架获取强，这样更直接心里都要更踏实一些
>
> ### 从Post请求的Body体中获取参数
>
> * `value := http.Request.PostFormValue("key")`
>
> ### 防止SQL注入写法
>
> * `stmt, err := basic.DB.Prepare("delete from song where id = ?")`
> * `rows, err := stmt.Exec(id)`
>
> ### Insert和Delete
>
> * `result, err := stmt.Exec(song.Name, song.Singer)`，
> * 用`Exec`而不用`Query`
>
> ### Windows查看端口和杀进程
>
> * 查看进程：`netstat -ano`
> * 杀死进程：`taskkill /F /PID 14156`
>
> ### 部署
>
> * `docker run -itd -v /opt/go:/opt/go -v /home/music:/home/music -p 8001:8001 --name goproject golang`
> * 一定不要忘记`/home/music`目录也要进行映射，不然容易找不到文件而报空指针异常
>
> ### linux非docker部署失败
>
> * 虽然编译过后的文件是一个linux下的可执行文件
> * 但云主机未安装go的环境依然不能执行
> * 因此建议直接使用docker进行部署
>
> ### audio标签的src属性
>
> * src属性的URL路径应该只能是http开头路径，不能使用代理
> * 因为本身src就不受跨域的限制
>
> ### log和fmt的区别
>
> * log底层使用了fmt
> * log是打印在运行中命令行，而fmt是打印在控制台
> * 即在linux部署环境中看不到fmt打印的内容
>
> ### 上传bug
>
> * 不能上传源文件带有`&`符号的歌曲，不然在解析的时候会被认为是URL参数的分隔符
> * 如`Charlie Puth & Selena Gomez - We Don't Talk Anymore.mp3`就会失败
>
> ***

