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
				echo "Please enter Y or N"
				;;
		esac
		[ -n "$val_set" ] && break
	done
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
	export progs_found=0
	check_install || { echo "Script not in distribution environment. Exiting." && exit 1; }

	{ [ -n "$(git --version)" ] && progs_found=$(expr $progs_found++) } || echo "Warning: couldn't find git installation"
	{ [ -n "$(ant -version)" ] && progs_found=$(expr $progs_found++) } || echo "Warning: couldn't find Apache Ant installation"
	{ [ -n "$(javac -version)" ] && progs_found=$(expr $progs_found++) } || echo "Warning: couldn't find sufficient JDK"

	inst_prefix=$(read_default "Installation prefix" $SCRIPT_DIRNAME)
	echo "Proper functionality for the SmartBot command RELOAD depends on git, ant, and the JDK."
	echo "Without this functionality, RELOAD will simply cause restart_hook.sh to be executed,"
	echo "with no automatic integration."
	if [ "$progs_found" = 3 ]
	then
		integration_enabled=$(confirm "Enable automatic integration on RELOAD?" "1")
		local_repo=$(read_required "Absolute path to local clone of repository")
	else
		echo "Could not find dependencies. RELOAD command will not cause automatic change integration."
		integration_enabled=
		local_repo=

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
	mv * "$bin_dir"
	mv "$bin_dir/run_smartbot.sh" .

	sed -i -e "s/__PACKAGE_INSTALL_PATH__/$SMARTBOT_package_install_path/" run_smartbot.sh

	## Now make the script to set up our system variables
	echo >> "$VAR_SCRIPT" <<EOF
#!/bin/bash

export SMARTBOT_package_version="$package_version"
export SMARTBOT_integration_enabled="$integration_enabled"
export SMARTBOT_local_repo="$local_repo"
EOF

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
