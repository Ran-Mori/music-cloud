package controller

import (
	"NeteaseMusicCloudDisk/basic"
	"NeteaseMusicCloudDisk/dao"
	"NeteaseMusicCloudDisk/entity"
	"encoding/json"
	"github.com/julienschmidt/httprouter"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
)

type SongController struct {}

var songDao = new(dao.SongDao)

func (s *SongController)Router(router *httprouter.Router){
	router.GET("/songs",s.QueryAll)
	router.GET("/song/:id",s.GetSongHandler)
	router.DELETE("/song/:id",s.DeleteOneById)
	router.POST("/song/upload",s.Upload)
}

type ResultForceByJava struct {
	Code int `json:"code"`
	Message string `json:"message"`
	Data map[string]interface{} `json:"data"`
}

func (s *SongController)QueryAll(w http.ResponseWriter,r *http.Request,_ httprouter.Params){
	if songs,ok := songDao.QueryAll();ok{
		w.Header().Set("Content-Type","application/json")
		songsMap := make(map[string]interface{})
		songsMap["songs"] = songs
		var result = ResultForceByJava{
			Code: 200,
			Message: "发送成功",
			Data: songsMap,
		}
		json,_ := json.Marshal(result)
		w.Write(json)
	}else {
		http.Error(w,"query all error",http.StatusBadGateway)
		log.Println("查询所有出错")
	}
}

func (s *SongController)SelectOneById(w http.ResponseWriter, r *http.Request,params httprouter.Params){
	idStr := params.ByName("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w,"sorry, I haven't found the target song,please check your input",http.StatusBadRequest)
		log.Println(err)
		return
	}else if id <= 0 {
		http.Error(w,"sorry, you hava inputed a wrong id, please send request after checking",http.StatusBadRequest)
		log.Println("查询单个输入的id小于0")
		return
	}
	if song,ok:= songDao.SelectOneById(id); song == nil || !ok{
		http.Error(w,"select one by id error",http.StatusBadRequest)
		log.Println("根据id查询失败")
		return
	}else {
		w.Header().Set("Content-Type","application/json")
		marshal, _ := json.Marshal(song)
		w.Write(marshal)
	}
}

func (s *SongController)DeleteOneById(w http.ResponseWriter,r *http.Request,params httprouter.Params){
	idStr := params.ByName("id")
	id, err := strconv.Atoi(idStr)
	if err != nil || id <= 0{
		http.Error(w,"wrong id",http.StatusBadRequest)
		log.Println("根据id删除方法输入的id有误")
		return
	}
	affectRow := songDao.DeleteById(id)
	if affectRow != 1 {
		http.Error(w,"delete one by id error",http.StatusBadGateway)
		log.Println("删除失败,影响的行数不等于1")
		return
	}else {
		w.Header().Set("Content-Type","application/json")
		w.Write([]byte("successful to delete"))
	}
}

func (s *SongController)Insert(song *entity.Song) error {
	if len(song.Name) == 0 || len(song.Singer) == 0 {
		log.Println("歌名为空或者歌手为空")
		return basic.Error{ErrMsg: "parameter null"}
	}
	if lastInsertId := songDao.Insert(song);lastInsertId <= 0{
		log.Println("插入歌曲失败")
		return basic.Error{ErrMsg: "insert error"}
	}else {
		return nil
	}
}

func (s *SongController)Upload(w http.ResponseWriter,r *http.Request,_ httprouter.Params){
	w.Header().Set("Content-Type","application/json")

	//从请求r的FormFile中获取到文件流
	//header中包含此文件流的名字和大小等信息
	//file就是一个二进制流
	file, header, err := r.FormFile("file")
	if err != nil {
		log.Println(err)
		return
	}
	defer file.Close()

	//获取文件的名字即'BEYOND - 海阔天空.mp3'
	//然后在做一下拆分获取歌手和歌曲名
	originalFilename := header.Filename
	_index := strings.Index(originalFilename,"-")
	singer := originalFilename[0 : _index - 1]
	name := originalFilename[_index + 2 : strings.Index(originalFilename,".")]

	//执行数据库查询操作
	var song = entity.Song{Name: name,Singer: singer}
	if err := s.Insert(&song);err != nil{
		log.Println(err)
		w.Write([]byte("insert error"))
		return
	}

	//创建云服务硬盘的文件
	dist, _ := os.Create(strings.Join([]string{basic.MusicBasePath, originalFilename}, ""))
	defer dist.Close()

	//做一个流复制操作
	if _, err := io.Copy(dist, file); err != nil{
		log.Println(err)
	}else {
		w.Write([]byte("successful to upload"))
	}
}

func (s *SongController)DownLoad(w http.ResponseWriter,r *http.Request,_ httprouter.Params){
	//从URL中获取歌手和歌名信息
	getParams := r.URL.Query()
	songName := getParams.Get("songname")
	singer := getParams.Get("singer")

	if len(songName) == 0 || len(singer) == 0 {
		log.Println("歌名或者歌曲参数为空")
		http.Error(w,"param null",http.StatusBadRequest)
		return
	}

	//确定要在硬盘上读取的流文件的路径
	filePath := strings.Join([]string{basic.MusicBasePath,singer," - ",songName,".mp3"},"")
	//获取磁盘上的文件流
	file, err := os.Open(filePath)
	if err != nil {
		log.Println(err)
		http.Error(w,"file not exist",http.StatusBadGateway)
		return
	}
	defer file.Close()

	//磁盘文件是完整路径如'/home/music/BEYOND - 海阔天空.mp3'，因此要把前面的路径去掉
	index := strings.Index(file.Name(),singer)
	//设置为强制下载
	w.Header().Set("content-type","application/force-download")
	//设置disk缓存，二次听歌不用拉取
	w.Header().Set("cache-control","max-age=31536000")
	//设置下载的歌曲的名字
	w.Header().Set("content-disposition",strings.Join([]string{"attachment;fileName=",file.Name()[index:len(file.Name())]},""))
	//把磁盘流复制到respond流
	io.Copy(w,file)
}

func (s SongController)GetSongHandler(w http.ResponseWriter,r *http.Request,params httprouter.Params){
	idValue := params.ByName("id")
	if strings.HasPrefix(idValue,"download"){
		s.DownLoad(w,r,params)
	}else {
		s.SelectOneById(w,r,params)
	}
}