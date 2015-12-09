#!/bin/sh

DOCKER_IFACE=`ip a s docker0 | grep 'inet ' | cut -d ' ' -f6 | cut -d/ -f1`

if [ -z "$DOCKER_IFACE" ]; then
    echo unable to determine docker network interface
    exit 1
fi

echo "using docker interface: $DOCKER_IFACE"

DOCKER_SOCKET=${DOCKER_SOCKET:-/run/docker.sock}

if [ ! -S "$DOCKER_SOCKET" ]; then
    echo "did not find docker socket, please specify \$DOCKER_SOCKET"
    exit 1
fi

# update resolv.conf if necessary
if ! grep "$DOCKER_IFACE" /etc/resolv.conf >/dev/null; then
    if [ "$1" = "-f" ]; then
        sed -i "1i nameserver $DOCKER_IFACE" /etc/resolv.conf
    else
        echo "you may want to add 'nameserver $DOCKER_IFACE' to your resolv.conf"
    fi
fi

if ! docker top dnsdock >/dev/null 2>/dev/null; then
    echo "dnsdock not running yet"
    if docker ps -a | grep dnsdock >/dev/null 2>&1; then
        echo "restarting dnsdock..."
        docker start dnsdock >/dev/null
    else
        echo "starting dnsdock..."
        docker run -d --name dnsdock -p "${DOCKER_IFACE}:53:53/udp" -v $DOCKER_SOCKET:$DOCKER_SOCKET tonistiigi/dnsdock >/dev/null
    fi
else
    echo dnsdock is already running
fi
