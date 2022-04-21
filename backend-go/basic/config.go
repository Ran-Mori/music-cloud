package basic

const (
	ServerAddress   = ":8001"
	MysqlPort       = "3306"
	MysqlUserName   = "root"
	MysqlSchemaName = "music-cloud"
	DataBaseType    = "mysql"
	MusicStorePath  = "/music-cloud/"
)

type MysqlUserConfig struct {
	MysqlServerHost string
	MysqlPassword   string
}
