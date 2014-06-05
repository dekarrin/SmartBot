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

check_install || { echo "Script not in distribution environment. Exiting." && exit 1; }
	
