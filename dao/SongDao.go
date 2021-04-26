package dao

import (
	"NeteaseMusicCloudDist/basic"
	"NeteaseMusicCloudDist/entity"
	"log"
)

type SongDao struct {}

func (songDao *SongDao)QueryAll() []entity.Song{
	rows, err := basic.DB.Query("select * from song")
	if err != nil {
		 log.Println(err)
		 return nil
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
	rows.Close()
	return songs
}