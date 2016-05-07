/*
 *  Copyright 2016 n3integration
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.n3integration.gradle.ecs.models

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ClusterSpecification extends Specification {

    String clusterName = "dev"
    String containerName = "dockercloud-hello-world"

    def "can define a cluster"() {
        when:
        def project = ProjectBuilder.builder().build()
        def containers = project.container(Container)
        def cluster = new Cluster(clusterName, containers)
        cluster.instanceSettings {
            securityGroups = ["sg-1"]
            autoScaling {
                min = 1
                max = 2
            }
            tag {
                key = "owner"
                value = "n3integration"
            }
            tag {
                key = "project"
                value = "gradle-ecs-plugin"
            }
        }
        cluster.containers {
            "${containerName}" {
                instances = 1
                image = "dockercloud/hello-world"
                ports = ["80"]
            }
        }

        then:
        cluster.name == clusterName

        def instanceSettings = cluster.instanceSettings
        instanceSettings.tags.size() == 2
        instanceSettings.autoScaling?.min < instanceSettings.autoScaling?.max

        cluster.containers.size() == 1
        cluster.containers.each { container ->
            container.name == containerName
            container.instances == 1
            container.ports = ["80"]
        }
    }
}
