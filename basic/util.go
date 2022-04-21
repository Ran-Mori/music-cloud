package basic

import (
	"crypto/md5"
	"flag"
	"fmt"
	"github.com/dhowden/tag"
	"io"
	"log"
	"os"
)

func GetUserGlobalConfig() MysqlUserConfig {
	mysqlServerHost := flag.String("MysqlServerHost", "", "mysql server host")
	mysqlPassword := flag.String("MysqlPassword", "", "mysql password")
	flag.Parse()

	if len(*mysqlServerHost) == 0 || len(*mysqlPassword) == 0 {
		log.Fatal("mysql host address or password is null.")
	}

	return MysqlUserConfig{
		MysqlServerHost: *mysqlServerHost,
		MysqlPassword:   *mysqlPassword,
	}

}

func GetMd5(rs io.ReadSeeker) string {
	hash := md5.New()

	result := ""
	if _, err := io.Copy(hash, rs); err != nil {
		fmt.Errorf("%x", err)
	} else {
		result = fmt.Sprintf("%x", hash.Sum(nil))
		fmt.Printf("%x", result)
	}
	rs.Seek(0, io.SeekStart)
	log.Println(rs)
	return result
}

func GetTitleArtistAndPicture(rs io.ReadSeeker) (string, string, []byte) {
	metaInfo, err := tag.ReadFrom(rs)

	title, artist := "", ""
	picture := []byte("")
	if err != nil {
		log.Fatal(err)
	} else {
		title, artist = metaInfo.Title(), metaInfo.Artist()
		if metaInfo.Picture() != nil {
			picture = metaInfo.Picture().Data
		}
	}

	rs.Seek(0, io.SeekStart)
	return title, artist, picture
}

func GetUserHomeDir() string {
	dir, _ := os.UserHomeDir()
	return dir
}
