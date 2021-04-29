package basic

type Error struct {
	ErrMsg string
}
func (e Error)Error() string{
	return e.ErrMsg
}
