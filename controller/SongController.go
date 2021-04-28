package controller

import (
	"NeteaseMusicCloudDist/dao"
	"NeteaseMusicCloudDist/entity"
	"encoding/json"
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"strconv"
)

type SongController struct {}

var songDao = new(dao.SongDao)

func (s *SongController)Router(router *httprouter.Router){
	router.GET("/songs",s.QueryAll)
	router.GET("/song/:id",s.SelectOneById)
	router.DELETE("/song/:id",s.DeleteOneById)
	router.POST("/song",s.Insert)
}

func (s *SongController)QueryAll(w http.ResponseWriter,r *http.Request,_ httprouter.Params){
	if songs,ok := songDao.QueryAll();ok{
		w.Header().Set("Content-Type","application/json")
		json,_ := json.Marshal(songs)
		w.Write(json)
	}else {
		http.Error(w,"query all error",http.StatusBadGateway)
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
		return
	}
	if song,ok:= songDao.SelectOneById(id); song == nil || !ok{
		http.Error(w,"select one by id error",http.StatusBadRequest)
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
		return
	}
	affectRow := songDao.DeleteById(id)
	if affectRow == -1 {
		http.Error(w,"delete one by id error",http.StatusBadGateway)
		return
	}else {
		w.Header().Set("Content-Type","application/json")
		w.Write([]byte("successful to delete"))
	}
}

func (s *SongController)Insert(w http.ResponseWriter,r *http.Request,_ httprouter.Params){
	name := r.PostFormValue("name")
	singer := r.PostFormValue("singer")

	if len(name) == 0 || len(singer) == 0 {
		http.Error(w,"empty params",http.StatusBadRequest)
		return
	}
	var song = entity.Song{Name: name,Singer: singer}
	if lastInsertId := songDao.Insert(&song);lastInsertId <= 0{
		http.Error(w,"insert error",http.StatusBadRequest)
		return
	}else {
		w.Header().Set("Content-Type","application/json")
		w.Write([]byte("successful to insert"))
	}
}
