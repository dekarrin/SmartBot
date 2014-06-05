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

read_required ()
{
	prompt="$1: "
	value=
	while [ -z "$value" ]
	do
		read -p "$prompt" value
	done
	echo $value
}

read_default ()
{
	prompt="$1 (default $2): "
	read -p "$prompt" value
	if [ "$value" = "" ]
	then
		value="$2"
	fi
	echo $value
}

confirm ()
{
	val=
	val_set=
	prompt="$1 [Y/N]: "
	while :
	do
		read -rp "$prompt" value
		case $value in
			[Yy][Ee][Ss]|[Yy])
				val=1
				val_set=1
				;;
	
			[Nn][Oo]|[Nn])
				val=
				val_set=1
				;;
	
			*)
				;;
		esac
		[ -n "$val_set" ] && break
	done
	echo $val
}

# ensures that we are in a distribution environment
check_install ()
{
	rstatus=1
	if [ -f "$SCRIPT_DIRNAME/README.md" -a -f "$SCRIPT_DIRNAME/LICENSE.txt" -a -d "$SCRIPT_DIRNAME/lib" ]
	then
		rstatus=0
	fi
	return $rstatus
}

SCRIPT_DIRNAME="$(whereami)"
VAR_SCRIPT="environment.sh"
INST_PACKAGE_VERSION="@VERSION@"

if [ -z "$RUNNING_SMARTBOT" ]
then
	progs_found=0
	check_install || { echo "Script not in distribution environment. Exiting." && exit 1; }

	{ [ -n "$(git --version 2>&1)" ] && progs_found=$(expr $progs_found + 1); } || echo "Warning: couldn't find git installation"
	{ [ -n "$(ant -version 2>&1)" ] && progs_found=$(expr $progs_found + 1); } || echo "Warning: couldn't find Apache Ant installation"
	{ [ -n "$(javac -version 2>&1)" ] && progs_found=$(expr $progs_found + 1); } || echo "Warning: couldn't find sufficient JDK"

	inst_prefix=$(read_default "Installation prefix" $SCRIPT_DIRNAME)
	echo "Proper functionality for the SmartBot command RELOAD depends on git, ant, and the JDK."
	echo "Without this functionality, RELOAD will simply cause restart_hook.sh to be executed,"
	echo "with no automatic integration."
	if [ "$progs_found" = 3 ]
	then
		integration_enabled=$(confirm "Enable automatic integration on RELOAD?" "1")
		if [ -n "$integration_enabled" ]
		then
			local_repo=$(read_required "Absolute path to local clone of repository")
		else
			local_repo=
		fi
	else
		echo "Could not find dependencies. RELOAD command will not cause automatic change integration."
		integration_enabled=
		local_repo=
	fi

	if [ "$inst_prefix" != "$SCRIPT_DIRNAME" ]
	then
		SMARTBOT_package_install_path="$inst_prefix"
		mkdir -p "$inst_prefix"
		cp -R "$SCRIPT_DIRNAME/*" "$inst_prefix"
	else
		SMARTBOT_package_install_path="$SCRIPT_DIRNAME"
	fi

	bin_dir="bin_$INST_PACKAGE_VERSION"

	cd "$SMARTBOT_package_install_path"
	mkdir "$bin_dir"
	for f in *
	do
		if [ "$f" != "$bin_dir" ]
		then
			mv "$f" "$bin_dir"
		fi
	done
	mv "$bin_dir/run_smartbot.sh" .
	chmod 755 "run_smartbot.sh"

	sed -i -e "s:__PACKAGE_INSTALL_PATH__:$SMARTBOT_package_install_path:" run_smartbot.sh

	## Now make the script to set up our system variables
	echo "#!/bin/bash" > "$VAR_SCRIPT"
	echo >> "$VAR_SCRIPT"
	echo "export SMARTBOT_package_version=\"$INST_PACKAGE_VERSION\"" >> "$VAR_SCRIPT"
	echo "export SMARTBOT_integration_enabled=\"$integration_enabled\"" >> "$VAR_SCRIPT"
	echo "export SMARTBOT_local_repo=\"$local_repo\"" >> "$VAR_SCRIPT"
else
	bin_dir="$1"
	cd "$bin_dir/.."
	rm -rf "$bin_dir"
	bin_dir="bin_$INST_PACKAGE_VERSION"
	mkdir "$bin_dir"
	cp -R "$SCRIPT_DIRNAME/*" "$bin_dir"
	msg=
	if [ -n "$(diff "$SMARTBOT_package_install_path/run_smartbot.sh" "$bin_dir/run_smartbot.sh")" ]
	then
		rm -rf "$bin_dir/run_smartbot.sh"
	else
		msg="Notice: run_smartbot.sh cannot be automatically updated"
	fi
	sed -i -e 's/SMARTBOT_package_version=.*/SMARTBOT_package_version="'"$INST_PACKAGE_VERSION"'"/' "$VAR_SCRIPT"
	echo $msg
fi
