package dao

import (
	"NeteaseMusicCloudDisk/basic"
	"NeteaseMusicCloudDisk/entity"
	"log"
	"strings"
)

type SongDao struct {}

func (songDao *SongDao)QueryAll() ([]entity.Song,bool){
	rows, err := basic.DB.Query("select * from song order by id desc ")
	defer rows.Close()

	if err != nil {
		 log.Println(err)
		 return nil,false
	}
	songs := make([]entity.Song,0)
	for rows.Next() {
		var song entity.Song
		err := rows.Scan(&song.Id, &song.Singer, &song.Name)
		if err != nil {
			log.Println(err)
			continue
		}
		songs = append(songs,song)
	}
	return songs,true
}

func (songDao *SongDao)SelectOneById(id int) (*entity.Song,bool){
	//普通不防止SQL注入的方式执行SQL
	//rows, err := basic.DB.Query("select * from song where id = ?", id)

	//使用stmt防止SQL注入的方式
	stmt, err := basic.DB.Prepare("select * from song where id = ?")
	if err != nil {
		log.Println(err)
		return nil,false
	}
	rows, err := stmt.Query(id)
	defer rows.Close()

	if err != nil {
		log.Println(err)
		return nil,false
	}

	if rows.Next() {
		var song entity.Song
		err := rows.Scan(&song.Id, &song.Name, &song.Singer)
		if err != nil {
			log.Println(err)
		}
		return &song,true
	}else {
		return nil,false
	}
}

func (songDao *SongDao)DeleteById(id int) int{
	stmt, err := basic.DB.Prepare("delete from song where id = ?")
	if err != nil {
		log.Println(err)
		return -1
	}
	rows, err := stmt.Exec(id)
	if err != nil {
		log.Println(err)
		return -1
	}
	rowsAffected, _ := rows.RowsAffected()
	return int(rowsAffected)
}

func (songDao *SongDao)Insert(song *entity.Song) int{
	stmt, err := basic.DB.Prepare("insert into song (name,singer) values (?,?)")
	if err != nil {
		log.Println(err)
		return -1
	}
	result, err := stmt.Exec(song.Name, song.Singer)
	if err != nil {
		log.Println(err)
		return -1
	}
	lastInsertId, _ := result.LastInsertId()
	return int(lastInsertId)
}

func (songDao *SongDao)LikeSingerOrSongName(str string) ([]entity.Song,bool){
	stmt, err := basic.DB.Prepare("select * from song where name like ? or singer like ?")
	if err != nil {
		log.Println(err)
		return nil,false
	}

	keyword := strings.Join([]string{"%",str,"%"},"")
	rows, err := stmt.Query(keyword,keyword)
	defer rows.Close()
	if err != nil {
		log.Println(err)
		return nil,false
	}

	songs := make([]entity.Song,0)
	for rows.Next() {
		var song entity.Song
		err := rows.Scan(&song.Id, &song.Singer, &song.Name)
		if err != nil {
			log.Println(err)
			continue
		}
		songs = append(songs,song)
	}
	return songs,true
}