#!/bin/bash -x

exec > /tmp/part-001.log 2>&1

if [[ \$(uname -r) == *"amzn"* ]]; then
    cat > /etc/ecs/ecs.config <<-EOF
ECS_CLUSTER=${name}
ECS_DISABLE_METRICS=true
EOF
elif [ -e /etc/redhat-release ]; then
    # install docker
    echo "Enabling 'extras' repository..."
    yum-config-manager --enable rhui-REGION-rhel-server-extras

    echo "Installing docker..."
    yum update -y
    yum install -y docker
    #echo "VG=docker"        >> /etc/sysconfig/docker-storage-setup
    # assumes that you have a secondary ebs volume @/sdd
    #echo "DEVS=/dev/xvdd"   >> /etc/sysconfig/docker-storage-setup

    # install ecs agent
    echo "Setting up ecs agent..."
    mkdir -p /var/log/ecs
    mkdir -p /var/lib/ecs/data
    echo "Installing ecs agent..."
    cat > /etc/systemd/system/ecs-agent.service <<-EOF
[Unit]
Description=ecs-agent
Requires=docker.service
After=docker.service
[Service]
Restart=on-failure
TimeoutStartSec=0
ExecStartPre=-/usr/bin/docker stop ecs-agent
ExecStartPre=-/usr/bin/docker rm ecs-agent
ExecStartPre=/usr/bin/docker pull amazon/amazon-ecs-agent:latest
ExecStart=/bin/sh -c '/usr/bin/docker run --name ecs-agent \
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
    --env=ECS_DISABLE_METRICS=true \
    --env=ECS_CLUSTER=${name} \
    amazon/amazon-ecs-agent:latest'
ExecStop=/usr/bin/docker stop ecs-agent
[Install]
WantedBy=multi-user.target
EOF

    cat > /etc/systemd/system/ecs-agent.timer <<-EOF
[Unit]
[Timer]
OnStartupSec=2min
[Install]
WantedBy=multi-user.target
EOF

    semanage permissive -a init_t
    semanage permissive -a cloud_init_t
    /bin/systemctl --system daemon-reload
    /bin/systemctl enable lvm2-monitor.service
    /bin/systemctl enable lvm2-lvmetad.service
    /bin/systemctl enable docker.service
    /bin/systemctl enable ecs-agent.service
    /bin/systemctl start ecs-agent.timer

    echo "Complete."
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
        --env=NO_PROXY=s3.amazon.com \
        amazon/amazon-ecs-agent:latest
fi