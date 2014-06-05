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
bin_dir="$SMARTBOT_package_install_path/bin_$SMARTBOT_package_version"

while :
do
	cd "$bin_dir"
	java -jar smartbot.jar "$extra_msg" "$@"
	botstatus=$?
	cd ..
	extra_msg=
	if [ "$botstatus" = 100 ]
	then
		if [ -f restart_hook.sh ]
		then	
			./restart_hook.sh
			extra_msg="Restart hook: status $?"
		fi
		if [ -n "$SMARTBOT_integration_enabled" ]
		then
			cd "$SMARTBOT_local_repo"
			if git pull && ant dist
			then
				success=1
			else
				success=
			fi
			cd "$SMARTBOT_package_install_path"
			if [ -n "$success" ]
			then
				chmod 755 "$SMARTBOT_local_repo/dist/*.sh"
				extra_msg="$extra_msg, $("$SMARTBOT_local_repo/dist/install.sh" "$bin_dir")"
				. "$SCRIPT_DIRNAME/environment.sh"
				bin_dir="$SMARTBOT_package_install_path/bin_$SMARTBOT_package_version"
			else
				extra_msg="$extra_msg, Warning: could not integrate changes"
			fi	
		fi
	else
		break
	fi
done
