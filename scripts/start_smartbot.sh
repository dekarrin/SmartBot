#!/bin/bash

extra_msg=

while :
do
	java -jar smartbot.jar "$extra_msg" "$@"
	botstatus=$?
	extra_msg=
	if [ "$botstatus" = 100 ]
	then
		if [ -f restart_hook.sh ]
		then	
			./restart_hook.sh
			extra_msg="Restart script exited with status $?"
		else
			extra_msg="Restart script does not exist"
		fi
	else
		break
	fi
done
