package database

import (
	"database/sql"
	_ "github.com/go-sql-driver/mysql"
	"log"
	"musiccloud/basic"
	"strings"
)

var DB = new(sql.DB)

func InitDataBase(config basic.MysqlUserConfig) {
	//数据库连接格式和jdbc不一样
	//格式为"username:password@tcp(ip:port)/database?charset=utf8&parseTime=True&loc=Local"
	path := strings.Join([]string{basic.MysqlUserName, ":", config.MysqlPassword, "@tcp(", config.MysqlServerHost, ":", basic.MysqlPort, ")/", basic.MysqlSchemaName, "?charset=utf8&parseTime=True&loc=Local"}, "")
	db, err := sql.Open(basic.DataBaseType, path)
	if err != nil {
		log.Println(err)
		return
	}
	DB = db
	DB.SetConnMaxLifetime(100)
	DB.SetConnMaxIdleTime(10)
	if DB.Ping() != nil {
		log.Fatalf("fail to ping database : %s\n", basic.MysqlSchemaName)
	}
	log.Printf("success to connect to database : %s\n", basic.MysqlSchemaName)
}
