#!/bin/bash

whereami ()
{
	src="${BASH_SOURCE[0]}"
	while [ -h "$src" ]
	do
		dir="$(cd -P "$(dirname "$src")" && pwd)"
		src="$(readlink "$src")"
		[[ $src != /* ]] && src="$dir/$src"
	done
	cd -P "$(dirname "$src")" && pwd
}

SCRIPT_DIRNAME="$(whereami)"

. "$SCRIPT_DIRNAME/environment.sh"
export RUNNING_SMARTBOT=1

extra_msg=

#This will be replaced with the correct value by the install script
export SMARTBOT_package_install_path=__PACKAGE_INSTALL_PATH__


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
