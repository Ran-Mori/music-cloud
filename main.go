package main

import (
	"NeteaseMusicCloudDist/basic"
	"NeteaseMusicCloudDist/controller"
	"github.com/julienschmidt/httprouter"
	_ "github.com/julienschmidt/httprouter"
	"log"
	"net/http"
	"time"
)

func main()  {
	basic.InitDataBase()
	defer basic.DB.Close()

	router := httprouter.New()

	server :=&http.Server{
		Addr: "127.0.0.1:80",
		ReadTimeout: 5 * time.Second,
		Handler: router,
	}

	new(controller.SongController).Router(router)
	err := server.ListenAndServe()
	if err != nil {
		log.Fatalln(err)
	}
}