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
package com.n3integration.gradle.aws

import com.n3integration.gradle.aws.models.Cluster
import com.n3integration.gradle.aws.tasks.CreateClusterTask
import com.n3integration.gradle.aws.tasks.DeleteClusterTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class ECSPlugin implements Plugin<Project> {

    public static final String AWS_SDK_VERSION = "1.10.69"
    public static final String AWS_JAVA_CONFIGURATION_NAME = "awsJava"

    @Override
    void apply(Project project) {
        project.tasks.create("createCluster", CreateClusterTask)
        project.tasks.create("deleteCluster", DeleteClusterTask)
        project.extensions.create("aws",
            AWSExtension,
            project.container(Cluster))

        project.configurations.create(AWS_JAVA_CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(true)
                .setDescription('The AWS SDK to be used for this project.')

        Configuration config = project.configurations[AWS_JAVA_CONFIGURATION_NAME]
        config.defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-config:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-core:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-ecs:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-iam:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-sts:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create('org.slf4j:slf4j-simple:1.7.5'))
        }
    }
}
