# music-cloud
A cloud music player that user can upload music to and listen from.

# Still in progress!!!



## File structure

```sh
├── music-cloud
│   ├── backend-go  //a go project
│   ├── frontend-android //an Android project
│   ├── frontend-web //a web project
```



## Requirements

1. a cloud server based on linux (In fact it's OK on your computer locally if you just want to have a test. )



## Quick start

I would recommend you just download release executable targets from this page, also it is not a bad choice to build it  from source code.

### Build backend executable

1. clone this project 

   ```shell
   cd ~
   git clone https://github.com/IzumiSakai-zy/music-cloud.git
   ```

2. build backend executable target

   ```shell
   cd ~/music-cloud/backend-go
   go get . #make sure dependency of this project is right
   env GOOS=linux GOARCH=amd64 go build -o music-cloud-linux-amd64
   ```

3. transfer executable target to your remote server based on linux. you can try scp command below or use some ftp client such as XFtp.

   ```shell
   cd ~/music-cloud/backend-go
   scp music-cloud-linux-amd64 'username'@'ip address':~
   ```

4. setup Mysql database.  just execute sql commands below.

   ```sql
   create table `music-cloud`.song
   (
       id     varchar(64)  not null
           primary key,
       title  varchar(512) null,
       artist varchar(512) null
   );
   ```

5. login in your remote linux server and deploy backend service. you can just do it on your server itself, or you can deploy it in a docker container, whatever is OK.

   ```shell
   ssh 'username'@'ip address'
   cd ~
   mkdir music-cloud
   nohup ./music-cloud-linux-amd64 --MysqlServerHost 'mysql server address' --MysqlPassword 'mysql password' > log.txt &
   ```

6. Over
