package basic

import (
	"database/sql"
	_ "github.com/go-sql-driver/mysql"
	"log"
	"strings"
)

var DB = new(sql.DB)

const (
	Host = "47.108.63.126"
	Port = "3306"
	Username = "root"
	Password = "542270191MSzyl"
	Database = "netease"
	DriverName = "mysql"
)

func InitDataBase() {
	//数据库连接格式和jdbc不一样
	//格式为"username:password@tcp(ip:port)/database?charset=utf8&parseTime=True&loc=Local"
	path := strings.Join([]string{Username,":",Password,"@tcp(",Host,":",Port,")/",Database,"?charset=utf8&parseTime=True&loc=Local"},"")
	db, err := sql.Open(DriverName, path)
	if  err != nil{
		log.Println(err)
		return
	}
	DB = db
	DB.SetConnMaxLifetime(100)
	DB.SetConnMaxIdleTime(10)
	if DB.Ping() != nil{
		log.Fatalf("fail to ping database : %s\n",Database)
	}
	log.Printf("success to connect to database : %s\n",Database)
}