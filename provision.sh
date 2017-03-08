#!/bin/bash -eu

projectRoot="${1:-$(dirname "$0")}"

if [ ! -f /etc/default/docker ]; then
  echo "/etc/default/docker not found -- is docker installed?" >&2
  exit 1
fi

if [ -f /home/vagrant/.profile ] && ! fgrep -q "cd /vagrant" /home/vagrant/.profile
then
  echo "cd /vagrant" >> /home/vagrant/.profile
fi

# get setuptools
apt-get update
apt-get install -y python-setuptools

# get blockade
if [ -d "/blockade" ]; then
    cd /blockade
    git fetch origin
    git reset --hard origin/master
    cd -
else
    git clone https://github.com/kongo2002/blockade.git /blockade
fi

# build blockade
cd /blockade
python setup.py develop
cd -

# pull required docker images
docker pull cassandra:2.2.3
docker pull cassandra:3.7
docker pull tonistiigi/dnsdock

docker build -t eventuate-chaos "$projectRoot/src/docker/eventuate-chaos"
