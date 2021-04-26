package main

import (
	"NeteaseMusicCloudDist/basic"
	"NeteaseMusicCloudDist/controller"
	"log"
	"net/http"
	"time"
)

func main()  {
	basic.InitDataBase()
	server :=&http.Server{
		Addr: "127.0.0.1:80",
		ReadTimeout: 5 * time.Second,
		Handler: basic.Router,
	}

	new(controller.SongController).Router(basic.Router)
	err := server.ListenAndServe()
	if err != nil {
		log.Fatalln(err)
	}
}