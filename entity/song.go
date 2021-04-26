package entity

type Song struct {
	Id int `json:"id"`
	Singer string `json:"singer"`
	Name string `json:"name"`
}