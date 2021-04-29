package main

import (
	"NeteaseMusicCloudDisk/basic"
	"NeteaseMusicCloudDisk/controller"
	"github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"time"
)

var Router = httprouter.New()

func main()  {
	basic.InitDataBase()
	defer basic.DB.Close()
	RouterInit()

	server :=&http.Server{
		Addr: ":8001",
		ReadTimeout: 5 * time.Minute,
		WriteTimeout: 5 * time.Minute,
		Handler: Router,
	}

	err := server.ListenAndServe()
	if err != nil {
		log.Fatalln(err)
	}
}

func RouterInit(){
	new(controller.SongController).Router(Router)
}