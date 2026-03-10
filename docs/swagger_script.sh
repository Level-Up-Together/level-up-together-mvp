#!/bin/sh

echo ">> swagger_script start"

#exit 0


container_stop() {
        echo "> stop ing..."
        docker container stop swagger
}

container_rm() {
        echo "> rm ing..."
        docker rm swagger
}

container_start(){
        echo "> start ing..."
	cd /home/pinkspider/infra/swagger
        bash $RUN_SWAGGER_SH
}

#exit 0

# 실행 유저확인
#EXECUTE_USER=$(whoami)
#if [ ${EXECUTE_USER} = "pinkspider" ]
#then
#	echo ${EXECUTE_USER}
#else
#	echo "실행 권한유저가 아닙니다."
#	echo ${EXECUTE_USER}
#	exit 100
#fi



# 스크립트실행 인자확인 stop, start
ACTION=$1

if [ -z "$ACTION" ]
then
	echo "명령어 인자를 넣어주세요(stop or start or restart)"
	exit 101
fi


SWAGGER_STATUS=`docker ps -a | grep swagger | awk '{print $7}'`
RUN_SWAGGER_SH=/home/pinkspider/infra/swagger/run_swagger.sh

if [ ${ACTION} = "stop" ]
then
       	if [ -z $SWAGGER_STATUS ]
        then
       	        echo "> 현재 컨테이너가 존재하지않습니다."
        elif [ ${SWAGGER_STATUS} = "Exited" ]
       	then
               	echo "> 컨테이너 삭제 진행중..."
       	        container_rm
        else
                echo "> 컨테이너 삭제 진행중... "
                container_stop
                container_rm
       	fi
elif [ ${ACTION} = "start" ]
then
        if [ ${SWAGGER_STATUS} = "Up" ]
        then
                echo "> 현재 실행중입니다. stop후 진행해주세요. "
        else
                echo "> 실행시작"
                container_start
        fi
elif [ ${ACTION} = "restart" ]
then
	echo "> Swagger 재시작 진행중 ..."
	if [ -z $SWAGGER_STATUS ]
	then
	        container_start
	elif [ ${SWAGGER_STATUS} = "Exited" ]
	then
	        container_rm
	        container_start
	else
	        container_stop
	        container_rm
	        container_start
	fi
fi