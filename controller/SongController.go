package controller

import (
	"NeteaseMusicCloudDist/basic"
	"NeteaseMusicCloudDist/dao"
	"encoding/json"
	"net/http"
)

type SongController struct {}

var songDao = new(dao.SongDao)

func (s *SongController)Router(handler *basic.RouterHandler){
	handler.Router("/songs",s.QueryAll)
}

func (s *SongController)QueryAll(w http.ResponseWriter,r *http.Request){
	songs := songDao.QueryAll()
	w.Header().Set("Content-Type","application/json")
	json,_ := json.Marshal(songs)
	w.Write(json)
}

