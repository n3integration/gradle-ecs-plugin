# gradle-ecs-plugin
[ ![Codeship Status for n3integration/gradle-ecs-plugin](https://codeship.com/projects/977c2ec0-f694-0133-1e27-5e1b5517d789/status?branch=master)](https://codeship.com/projects/150599) [ ![Download](https://api.bintray.com/packages/n3integration/maven/gradle-ecs-plugin/images/download.svg) ](https://bintray.com/n3integration/maven/gradle-ecs-plugin/_latestVersion)

Gradle plugin for Elastic Container Service (ECS) provisioning.

- [Usage](#usage)
	- [Project Configuration](#project-configuration)
	- [ECS Cluster Definitions](#ecs-cluster-definitions)
	- [Container Definitions](#container-definitions)
- [Task Types](#task-types)
	- [CreateCluster](#createcluster)
	- [Up](#up)
	- [Down](#down)
	- [DeleteCluster](#deletecluster)

### Usage
The AWS credentials are pulled from either environment variables or from a `~/.aws/credentials` file. Refer to Amazon's [official](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-environment) documentation for more information.

#### Project Configuration
The following should be placed at the head of your `build.gradle` file to include the plugin dependency into your Gradle project.

```gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "com.n3integration:gradle-ecs-plugin:0.5.0"
    }
}

apply plugin: 'aws-ecs'
```

#### ECS Cluster Definitions
Although the plugin takes care of setting up most of the infrastructure, it is assumed that the security groups and subnets already exist. When provisioning the ec2 instances, auto scaling groups are leveraged. This allows the infrastructure to be more resilient to failure, but requires additional infrastructure to respond to scaling events (e.g. [CloudWatch Alarms](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/AlarmThatSendsEmail.html) bound to a [Lambda](https://aws.amazon.com/lambda/) service).

Although the current implementation does not configure an [auto scaling policy](http://docs.aws.amazon.com/autoscaling/latest/userguide/policy_creating.html), future revisions may. By default, the Amazon ECS-optimized [AMI](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html) for the **us-east-1** region is configured for new ec2 instances. If you are running in a region other than **us-east-1**, reference the following [documentation](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-optimized_AMI.html) to identify which optimized AMI is available for your region. Although you are not limited to the Amazon Linux AMIs, other **Linux** operating systems are supported on a best effort basis. RedHat variants (e.g. CentOS/RHEL) configurations have been tested against v7.x.

Additionally, `t2.micro` ec2 instances are provisioned if an `instanceType` is not specified. For environments that have policies in place to track ec2 instances, one or more `tag` blocks can be defined.

```gradle
ecs {
    clusters {
        dev {
            region = "us-west-1"
            instanceSettings {
                ami = "ami-68106908"
                securityGroups = ["sg-abcd1234"]
                iamInstanceProfileArn = "arn:aws:iam::01234567890123:instance-profile/EcsDeveloper"
                autoScaling {
                    min = 1
                    max = 1
                    subnetIds = ["subnet-abcd123"]
                }
            }
            containers {
                "dockercloud-hello-world" {
                    instances = 1
                    image = "dockercloud/hello-world"
                    ports = ["80:8080"]
                }
            }
        }
        test {
          region = "us-east-1"
          instanceSettings {
              instanceType = "t2.small"
              securityGroups = ["sg-1234abcd"]
              iamInstanceProfileArn = "arn:aws:iam::01234567890123:instance-profile/EcsTester"
              autoScaling {
                  min = 2
                  max = 4
                  subnetIds = ["subnet-123abcd", "subnet-234bcde"]
              }
              tag {
                key = "meta"
                value = "DO NOT DELETE"
              }
          }
          containers {
              "dockercloud-hello-world" {
                  cpu = 10
                  memory = 64
                  instances = 2
                  image = "dockercloud/hello-world"
                  ports = ["80:80"]
              }
          }
        }
    }
}
```

#### Container Definitions
<dl>
  <dt>image</dt>
  <dd>The docker image. Required</dd>
  <dt>group</dt>
  <dd>Creates a logical group of containers. Optional</dd>
  <dt>hostname</dt>
  <dd>The container hostname. Optional</dd>
  <dt>instances</dt>
  <dd>The number of container instances to launch. Required</dd>
  <dt>cpu</dt>
  <dd>The amount of CPU to reserve for the container. Optional</dd>
  <dt>memory</dt>
  <dd>The amount of memory (in mb) to reserve for the container. Optional</dd>
  <dt>ports</dt>
  <dd>A list of port mappings (i.e. containerPort:hostPort). Optional</dd>
  <dt>entryPoint</dt>
  <dd>The container entry point. Optional</dd>
  <dt>cmd</dt>
  <dd>The container's default command. Optional</dd>
  <dt>links</dt>
  <dd>A list to zero or more container instances. Must be linked through a logical group. Optional</dd>
  <dt>environment</dt>
  <dd>A mapping of environment variables available to the container. Optional</dd>
</dl>

### Task Types
The following task types are available.

1. CreateCluster
1. Up
1. Down
1. DeleteCluster

#### CreateCluster
Creates a new Elastic Container Service cluster. If an `autoScaling` group is provided for the cluster, one or more ec2 instances are provisioned using the specified `image` (or the default ECS-optimized AMI for the us-east-1 region) according to the `autoScaling` definition. Additionally, a private key file – `~/.ecs/<cluster name>.pem` – is created for the cluster's ec2 instances. This key is reused when recreating a cluster, even after the cluster has been deleted. It must be manually [removed](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html#delete-key-pair) from AWS.

```gradle
task createCluster(type: CreateCluster) {
    cluster = "dev"
}
```

#### Up
Registers and starts the containers associated with the task's `cluster`.

```gradle
task up(type: Up) {
    cluster = "dev"
}
```

#### Down
Unregisters and terminates the containers associated with the task's `cluster`.

```gradle
task down(type: Down) {
    cluster = "dev"
}
```

#### DeleteCluster
Deletes an existing EC2 Container Service cluster. If a `autoScaling` group is defined for the cluster, the associated ec2 instances will be terminated.

```gradle
task deleteCluster(type: DeleteCluster) {
    cluster = "dev"
}
```
