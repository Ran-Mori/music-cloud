package basic

import (
	"fmt"
	"net/http"
)

type RouterHandler struct {}

var Router = new(RouterHandler)
var RouterMap = make(map[string]func(http.ResponseWriter,*http.Request))

func (p *RouterHandler)ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Println(r.URL.Path)
	if fun,ok := RouterMap[r.URL.Path];ok {
		fun(w,r)
		return
	}
	http.Error(w,"error URL:" + r.URL.Path,http.StatusBadRequest)
}

func (p *RouterHandler)Router(relativePath string,handler func(http.ResponseWriter,*http.Request)){
	RouterMap[relativePath] = handler
}