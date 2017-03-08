#!/bin/bash -eu

Home=$(cd "$(cd $(dirname "$0") && pwd -P)/../../.." && pwd -P)
Prog=$(basename "$0")
CurrentDir=$(pwd -P)

isSubDir() {
  local subDir="$1"
  local root="$2"
  [ "${subDir##$root}" != "$subDir" ]
}

if ! isSubDir "$CurrentDir" "$Home"
then
  echo "Start '$Prog' in subfolder of '$Home'"
  exit 1
fi

(cd "$Home" && sbt universal:stage)
relCurrentDir="${CurrentDir##$Home}"
vagrant ssh -c "cd /vagrant/$relCurrentDir && /vagrant/src/main/shell/start-test.sh $*"