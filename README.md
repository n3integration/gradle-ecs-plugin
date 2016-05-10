# gradle-ecs-plugin
[ ![Codeship Status for n3integration/gradle-ecs-plugin](https://codeship.com/projects/977c2ec0-f694-0133-1e27-5e1b5517d789/status?branch=master)](https://codeship.com/projects/150599) [ ![Download](https://api.bintray.com/packages/n3integration/maven/gradle-ecs-plugin/images/download.svg) ](https://bintray.com/n3integration/maven/gradle-ecs-plugin/_latestVersion)

Gradle plugin for Elastic Container Service provisioning

### Usage
The AWS credentials are pulled from either environment variables or from a `~/.aws/credentials` file. Refer to Amazon's [official](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-environment) documentation for more information.

#### Project Configuration
The following should be placed at the head of our `build.gradle` file to include the gradle-ecs-plugin dependency into our Gradle project.

```gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "com.n3integration:gradle-ecs-plugin:0.1.0"
    }
}

apply plugin: 'aws-ecs'
```

#### ECS Cluster Definitions
Although the plugin takes care of setting up most of the infrastructure, it is assumed that the security groups and subnets already exist. Auto scaling groups are provisioned. This allows the infrastructure to be more resilient to failure. Although the current implementation does not configure an auto scaling policy, future revisions may. By default, the Amazon ECS-optimized AMI from us-east-1 is configured. Reference the following [documentation](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-optimized_AMI.html) for the AMI for your region.

```gradle
ecs {
    clusters {
        dev {
            region = "us-west-1"
            instanceSettings {
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
              securityGroups = ["sg-1234abcd"]
              iamInstanceProfileArn = "arn:aws:iam::01234567890123:instance-profile/EcsTester"
              autoScaling {
                  min = 2
                  max = 4
                  subnetIds = ["subnet-123abcd", "subnet-234bcde"]
              }
          }
          containers {
              "dockercloud-hello-world" {
                  instances = 2
                  image = "dockercloud/hello-world"
                  ports = ["80:80"]
              }
          }
        }
    }
    defaultCluster = clusters.dev
}
```

### Tasks
The following tasks are available if a `defaultCluster` is specified.

<dl>
<dt>createCluster</dt>
<dd>Creates a new EC2 Container Service cluster along with one or more ec2 instances.</dd>
<dt>up</dt>
<dd>Registers and starts containers</dd>
<dt>down</dt>
<dd>Unregisters and terminates containers</dd>
<dt>deleteCluster</dt>
<dd>Deletes an existing EC2 Container Service cluster</dd>
</dl>
