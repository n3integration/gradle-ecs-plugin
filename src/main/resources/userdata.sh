#!/bin/bash -x

exec > /tmp/part-001.log 2>&1

if [ -e /etc/redhat-release ]; then
    # install docker
    echo "Enabling 'extras' repository..."
    yum-config-manager --enable rhui-REGION-rhel-server-extras

    echo "Installing docker..."
    yum update -y
    yum install -y docker
    service docker start
    chkconfig docker on

    # install ecs agent
    echo "Setting up ecs agent..."
    mkdir -p /var/log/ecs
    mkdir -p /var/lib/ecs/data
    echo "Installing ecs agent..."
    docker run --name ecs-agent \
        --detach=true \
        --restart=on-failure:10 \
        --volume=/var/run/docker.sock:/var/run/docker.sock \
        --volume=/var/log/ecs/:/log \
        --volume=/var/lib/ecs/data:/data \
        --volume=/sys/fs/cgroup:/sys/fs/cgroup:ro \
        --volume=/var/run/docker/execdriver/native:/var/lib/docker/execdriver/native:ro \
        --privileged \
        --publish=127.0.0.1:51678:51678 \
        --env=ECS_LOGFILE=/log/ecs-agent.log \
        --env=ECS_LOGLEVEL=info \
        --env=ECS_DATADIR=/data \
        --env=ECS_CLUSTER=${name} \
        amazon/amazon-ecs-agent:latest
else
    apt-get update -y
    echo "Installing docker..."
    apt-get -y install docker.io
    [ -e /usr/local/bin/docker ] || ln -sf /usr/bin/docker.io /usr/local/bin/docker
    update-rc.d docker.io defaults

    echo "Setting up ecs agent..."
    mkdir -p /var/log/ecs
    mkdir -p /var/lib/ecs/data
    echo "Installing ecs agent..."
    docker run --name ecs-agent \
        --detach=true \
        --restart=on-failure:10 \
        --volume=/var/run/docker.sock:/var/run/docker.sock \
        --volume=/var/log/ecs/:/log \
        --volume=/var/lib/ecs/data:/data \
        --volume=/sys/fs/cgroup:/sys/fs/cgroup:ro \
        --volume=/var/run/docker/execdriver/native:/var/lib/docker/execdriver/native:ro \
        --publish=127.0.0.1:51678:51678 \
        --env=ECS_LOGFILE=/log/ecs-agent.log \
        --env=ECS_LOGLEVEL=info \
        --env=ECS_DATADIR=/data \
        --env=ECS_CLUSTER=${name} \
        amazon/amazon-ecs-agent:latest
fi