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

import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification
import com.google.common.io.BaseEncoding
import groovy.text.SimpleTemplateEngine

import java.nio.charset.Charset

class Ec2InstanceSettings {

    static final int DEFAULT_MIN        = 1
    static final int DEFAULT_MAX        = DEFAULT_MIN
    static final String DEFAULT_AMI     = "ami-6d1c2007"    // centos 7
    static final String DEFAULT_TYPE    = "t2.micro"        // free tier

    int min = 1
    int max = min
    String ami  = DEFAULT_AMI
    String type = DEFAULT_TYPE
    String userData
    String subnet
    String iamInstanceProfileArn
    List<String> securityGroups

    Ec2InstanceSettings(String name) {
        this.userData = loadUserDataTemplate(name)
    }

    def IamInstanceProfileSpecification instanceProfileSpecification() {
        if(iamInstanceProfileArn) {
            return new IamInstanceProfileSpecification()
                .withArn(iamInstanceProfileArn)
        }
        return null
    }

    static String loadUserDataTemplate(String name) {
        def binding = [name:name]
        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(getClass().getResource("/userdata.sh").text)
                .make(binding)
        BaseEncoding.base64().encode(template.toString().getBytes(Charset.defaultCharset()))
    }
}
