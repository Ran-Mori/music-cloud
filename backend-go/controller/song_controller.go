package controller

import (
	"encoding/json"
	"errors"
	"github.com/julienschmidt/httprouter"
	"io"
	"log"
	"musiccloud/basic"
	"musiccloud/dao"
	"musiccloud/entity"
	"net/http"
	"os"
	"strings"
)

type SongController struct{}

var songDao = new(dao.SongDao)

func (s *SongController) Router(router *httprouter.Router) {
	router.GET("/songs", s.queryAll)
	router.DELETE("/song/:id", s.deleteOneById)
	router.GET("/song/download/:id/", s.downLoadMusic)
	router.GET("/song/picture/download/:id/", s.downloadPicture)
	router.GET("/song/query/:id", s.queryOneById)
	router.POST("/song/upload", s.upload)
	router.GET("/songs/like/:keyword", s.queryLike)
}

func (s *SongController) queryAll(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	if songs, ok := songDao.QueryAll(); ok {
		w.Header().Set("Content-Type", "application/json")
		json, _ := json.Marshal(songs)
		w.Write(json)
	} else {
		w.Write([]byte("query all error"))
		log.Println("fail to query all")
	}
}

func (s *SongController) queryOneById(w http.ResponseWriter, r *http.Request, params httprouter.Params) {
	md5IdStr := params.ByName("id")

	if song, ok := songDao.SelectOneById(md5IdStr); song == nil || !ok {
		w.Write([]byte("fail to select one by id"))
		log.Println("fail to select one by id")
	} else {
		w.Header().Set("Content-Type", "application/json")
		marshal, _ := json.Marshal(song)
		w.Write(marshal)
	}
}

func (s *SongController) deleteOneById(w http.ResponseWriter, r *http.Request, params httprouter.Params) {
	md5IdStr := params.ByName("id")

	affectRow := songDao.DeleteById(md5IdStr)
	if affectRow != 1 {
		w.Write([]byte("fail to delete one by id"))
		log.Println("fail to delete one by id")
	} else {
		w.Write([]byte("successful to delete one by id"))
	}

	songPath := strings.Join([]string{basic.GetUserHomeDir(), basic.MusicStorePath, md5IdStr, ".mp3"}, "")
	picPaht := strings.Join([]string{basic.GetUserHomeDir(), basic.MusicStorePath, md5IdStr, ".png"}, "")
	if _, err := os.Stat(songPath); !errors.Is(err, os.ErrNotExist) {
		os.Remove(songPath)
	}
	if _, err := os.Stat(picPaht); !errors.Is(err, os.ErrNotExist) {
		os.Remove(picPaht)
	}
}

func (s *SongController) upload(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	w.Header().Set("Content-Type", "application/json")
	r.Body = http.MaxBytesReader(w, r.Body, 32<<20+512)
	//从请求r的FormFile中获取到文件流
	//header中包含此文件流的名字和大小等信息
	//file就是一个二进制流
	file, header, err := r.FormFile("file")
	if err != nil {
		log.Println(err)
		w.Write([]byte("can't get a music file from http from-data"))
		return
	}
	defer file.Close()

	//获取文件的名字类似于'BEYOND - 海阔天空.mp3'
	//然后在做一下拆分获取歌手和歌曲名
	md5 := basic.GetMd5(file)
	fileName := strings.ReplaceAll(header.Filename, " ", "")
	title, artist, picture := basic.GetTitleArtistAndPicture(file)
	log.Print("size picture = ")
	log.Println(len(picture))
	index := strings.Index(fileName, "-")
	if len(title) == 0 {
		title = fileName[index+1 : strings.Index(fileName, ".")]
	}
	if len(artist) == 0 {
		artist = fileName[0:index]
	}

	//执行数据库Insert操作
	var song = entity.Song{Id: md5, Title: title, Artist: artist}
	if rowsAffected := songDao.Insert(&song); rowsAffected != 1 {
		log.Println("insert song error")
		w.Write([]byte("insert song error"))
		return
	}

	//创建云服务硬盘的文件
	mp3Dist, _ := os.Create(strings.Join([]string{basic.GetUserHomeDir(), basic.MusicStorePath, md5, ".mp3"}, ""))
	defer mp3Dist.Close()
	_, mp3Err := io.Copy(mp3Dist, file)

	var picErr = errors.New("")
	if len(picture) != 0 {
		picErr = os.WriteFile(strings.Join([]string{basic.GetUserHomeDir(), basic.MusicStorePath, md5, ".png"}, ""), picture, 0644)
	}

	if mp3Err == nil && picErr == nil {
		w.Write([]byte("successful to upload"))
	} else if mp3Err == nil && picErr != nil {
		w.Write([]byte("save picture file to disk error or no picture"))
	} else if mp3Err != nil && picErr == nil {
		w.Write([]byte("save music file to disk error"))
	} else {
		w.Write([]byte("music fail and picture fail"))
	}
}

func (s *SongController) downLoadMusic(w http.ResponseWriter, r *http.Request, params httprouter.Params) {
	md5IdStr := params.ByName("id")

	//确定要在硬盘上读取的流文件的路径
	filePath := strings.Join([]string{basic.GetUserHomeDir(), basic.MusicStorePath, md5IdStr, ".mp3"}, "")
	//获取磁盘上的文件流
	file, err := os.Open(filePath)
	if err != nil {
		w.Write([]byte("file not exist"))
		log.Println(err)
		return
	}
	defer file.Close()

	//磁盘文件是完整路径如'/home/music/43a0298b5039248ed09324f.mp3'
	//设置为强制下载
	w.Header().Set("content-type", "application/force-download")
	//设置disk缓存，二次听歌不用拉取
	w.Header().Set("cache-control", "max-age=31536000")
	//设置下载的歌曲的名字
	length := len(file.Name())
	w.Header().Set("content-disposition", strings.Join([]string{"attachment;fileName=", file.Name()[length-36 : len(file.Name())]}, ""))
	//把磁盘流复制到respond流
	io.Copy(w, file)
}

func (s *SongController) downloadPicture(w http.ResponseWriter, r *http.Request, params httprouter.Params) {
	md5IdStr := params.ByName("id")

	filePath := strings.Join([]string{basic.GetUserHomeDir(), basic.MusicStorePath, md5IdStr, ".png"}, "")
	//获取磁盘上的文件流
	file, err := os.Open(filePath)
	if err != nil {
		w.Write([]byte("file not exist"))
		log.Println(err)
		return
	}
	defer file.Close()

	//磁盘文件是完整路径如'/home/music/43a0298b5039248ed09324f.png'
	w.Header().Set("content-type", "application/force-download")
	w.Header().Set("cache-control", "max-age=31536000")
	length := len(file.Name())
	w.Header().Set("content-disposition", strings.Join([]string{"attachment;fileName=", file.Name()[length-36 : len(file.Name())]}, ""))
	io.Copy(w, file)
}

func (s *SongController) queryLike(w http.ResponseWriter, r *http.Request, params httprouter.Params) {
	keyword := params.ByName("keyword")

	if songs, ok := songDao.QueryLikeArtistOrTitle(keyword); ok {
		w.Header().Set("Content-Type", "application/json")
		json, _ := json.Marshal(songs)
		w.Write(json)
	} else {
		w.Write([]byte("like query error"))
		log.Println("like query error")
	}
}
