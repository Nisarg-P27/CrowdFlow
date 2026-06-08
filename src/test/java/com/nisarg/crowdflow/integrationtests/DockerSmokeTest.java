package com.nisarg.crowdflow.integrationtests;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

class DockerSmokeTest {

    @Test
    void dockerAvailable() {
        System.out.println(DockerClientFactory.instance().client().infoCmd().exec());
        System.out.println(DockerClientFactory.instance().client().pingCmd().exec());
    }
}