package main

import (
	"github.com/julienschmidt/httprouter"
	"log"
	"musiccloud/basic"
	"musiccloud/controller"
	"musiccloud/database"
	"net/http"
	"time"
)

var Router = httprouter.New()

func main() {
	config := basic.GetUserGlobalConfig()

	database.InitDataBase(config)
	defer database.DB.Close()
	RouterInit()

	server := &http.Server{
		Addr:         basic.ServerAddress,
		ReadTimeout:  5 * time.Minute,
		WriteTimeout: 5 * time.Minute,
		Handler:      Router,
	}

	err := server.ListenAndServe()
	if err != nil {
		log.Fatalln(err)
	}
}

func RouterInit() {
	new(controller.SongController).Router(Router)
}
