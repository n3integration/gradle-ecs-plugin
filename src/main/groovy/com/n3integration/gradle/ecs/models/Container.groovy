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

import com.amazonaws.services.ecs.model.ContainerDefinition
import com.amazonaws.services.ecs.model.KeyValuePair
import com.amazonaws.services.ecs.model.PortMapping
import com.google.common.base.Strings
import org.gradle.api.GradleException

/**
 * Docker container definition. Uses docker-compose style port mappings.
 *
 * @author n3integration
 */
class Container {

    public static final int MINIMUM_MIB_SIZE = 64
    public static final int DEFAULT_MIB_SIZE = 128

    final String name

    String image
    String group
    String hostname
    int instances = 1
    Integer cpu = 10
    int memory = DEFAULT_MIB_SIZE
    List<String> ports = []
    String entryPoint
    String cmd
    List<String> links = []
    String workdir
    Map<String, String> environment = [:]

    Container(name) {
        this.name = name
    }

    @Override
    def String toString() {
        name
    }

    def String familySuffix() {
        if(Strings.isNullOrEmpty(group)) {
            name
        }
        else {
            group
        }
    }

    def toDefinition() {
        def definition = new ContainerDefinition().withName(name)
        if(image) {
            definition.image = image
        }
        else {
            throw new GradleException("${name} container is missing image")
        }
        if(!Strings.isNullOrEmpty(hostname)) {
            definition.hostname = hostname
        }
        if(cpu) {
            definition.cpu = cpu
        }
        if(memory >= MINIMUM_MIB_SIZE) {
            definition.memory = memory
        }
        if(!ports.empty) {
            definition.portMappings = ports.collect { port ->
                toPortMapping(port)
            }
        }
        if(entryPoint) {
            definition.entryPoint = entryPoint.split("\\s+")
        }
        if(cmd) {
            definition.command = cmd.split("\\s+")
        }
        if(!links.empty) {
            definition.links = links
        }
        if(workdir) {
            definition.workingDirectory = workdir
        }
        if(!environment.isEmpty()) {
            definition.environment = environment.collect { key, value ->
                new KeyValuePair().withName(key).withValue(value)
            }
        }
        definition
    }

    private PortMapping toPortMapping(port) {
        def ports = port.split(":")
        if(ports.size() > 1) {
            new PortMapping().withContainerPort(ports[0].toInteger())
                .withHostPort(ports[1].toInteger())
        }
        else {
            new PortMapping().withContainerPort(ports[0].toInteger())
        }
    }
}
