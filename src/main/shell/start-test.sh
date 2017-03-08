#!/bin/bash -eu

Home=$(readlink -e $(dirname $(readlink -e "$0"))/../../..)

TestProg="$Home/src/main/python/crdt-counter-partitions.py"

startDns() {
  sudo "$Home/src/main/shell/start-dns.sh" -f
}

runTest() {
  local scenarioDir="$1"
  cd "$scenarioDir"
  shift
  echo "Start blockade in '$scenarioDir'"
  sudo blockade up
  if sudo $TestProg "$@"
  then
    :
  else
    ret=$?
    sudo blockade status
    echo "containers are kept running (to ease analysis) and can be stopped with 'cd \"$scenarioDir\" && sudo blockade destroy'"
    exit $ret
  fi
  sudo blockade destroy
}

help() {
  echo "$0 <scenario-folder> <test-prog-args>..."
  echo "<test-prog-args>:"
  $TestProg -h
}

if [ $# -lt 1 ] || [ "$1" = -h -o "$1" = --help ]
then
  help
  exit 0
fi

startDns
runTest "$@"
