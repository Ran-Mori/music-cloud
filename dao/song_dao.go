package dao

import (
	"log"
	"musiccloud/database"
	"musiccloud/entity"
	"strings"
)

type SongDao struct{}

func (songDao *SongDao) QueryAll() ([]entity.Song, bool) {
	rows, err := database.DB.Query("select * from song order by title desc ")
	defer rows.Close()

	if err != nil {
		log.Println(err)
		return nil, false
	}
	songs := make([]entity.Song, 0)
	for rows.Next() {
		var song entity.Song
		err := rows.Scan(&song.Id, &song.Artist, &song.Title)
		if err != nil {
			log.Println(err)
			continue
		}
		songs = append(songs, song)
	}
	return songs, true
}

func (songDao *SongDao) SelectOneById(md5Id string) (*entity.Song, bool) {
	//普通不防止SQL注入的方式执行SQL
	//rows, err := basic.DB.Query("select * from song where md5Id = ?", md5Id)

	//使用stmt防止SQL注入的方式
	stmt, err := database.DB.Prepare("select * from song where id = ?")
	if err != nil {
		log.Println(err)
		return nil, false
	}
	rows, err := stmt.Query(md5Id)
	defer rows.Close()

	if err != nil {
		log.Println(err)
		return nil, false
	}

	if rows.Next() {
		var song entity.Song
		err := rows.Scan(&song.Id, &song.Title, &song.Artist)
		if err != nil {
			log.Println(err)
		}
		return &song, true
	} else {
		return nil, false
	}
}

func (songDao *SongDao) DeleteById(md5Id string) int {
	stmt, err := database.DB.Prepare("delete from song where id = ?")
	if err != nil {
		log.Println(err)
		return -1
	}
	rows, err := stmt.Exec(md5Id)
	if err != nil {
		log.Println(err)
		return -1
	}
	rowsAffected, _ := rows.RowsAffected()
	return int(rowsAffected)
}

func (songDao *SongDao) Insert(song *entity.Song) int {
	stmt, err := database.DB.Prepare("insert into song (id, title, artist) values (?,?,?)")
	if err != nil {
		log.Println(err)
		return -1
	}
	result, err := stmt.Exec(song.Id, song.Title, song.Artist)
	if err != nil {
		log.Println(err)
		return -1
	}
	rowsAffected, _ := result.RowsAffected()
	return int(rowsAffected)
}

func (songDao *SongDao) QueryLikeArtistOrTitle(keyWords string) ([]entity.Song, bool) {
	stmt, err := database.DB.Prepare("select * from song where artist like ? or title like ?")
	if err != nil {
		log.Println(err)
		return nil, false
	}

	keyword := strings.Join([]string{"%", keyWords, "%"}, "")
	rows, err := stmt.Query(keyword, keyword)
	defer rows.Close()
	if err != nil {
		log.Println(err)
		return nil, false
	}

	songs := make([]entity.Song, 0)
	for rows.Next() {
		var song entity.Song
		err := rows.Scan(&song.Id, &song.Title, &song.Artist)
		if err != nil {
			log.Println(err)
			continue
		}
		songs = append(songs, song)
	}
	return songs, true
}
