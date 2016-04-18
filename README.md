# gradle-ecs-plugin
Gradle plugin for EC2 Container Service orchestration

### Usage
The AWS credentials are pulled from either environment variables or from a `~/.aws/credentials` file. Refer to Amazon's [official](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-environment) documentation for more information.
```gradle
ecs {
    region = "us-east-1"
    clusters {
        dev {
            region = "us-west-1"
            containers {
                wordpress {
                    image = "wordpress"
                }
            }
        }
        test {
            containers {
                wordpress {
                    image = "wordpress:4.5.0"
                    instances = 2
                }
            }
        }
        prod {
            containers {
                wordpress {
                    image = "wordpress:4"
                    instances = 2
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
<dd>Creates a new EC2 Container Service cluster</dd>
<dt>deleteCluster</dt>
<dd>Deletes an existing EC2 Container Service cluster</dd>
</dl>
