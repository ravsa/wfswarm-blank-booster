/*
 *
 *  Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.openshift.booster;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;
import io.restassured.RestAssured;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;

/**
 * @author Ravindra Ratnawat
 */
@RunWith(Arquillian.class)
public class OpenshiftIT {


    private String project;

    private final String applicationName = System.getProperty("app.name");

    @ArquillianResource
    private OpenShiftClient client;

    @ArquillianResource
    private Session session;

    @RouteURL(value = "${app.name}", path = "/")
    @AwaitRoute
    private String url;

    @Before
    public void setup() {
        RestAssured.baseURI = url;
        project = this.client.getNamespace();
    }

    @Test
    public void testServiceInvocation() {
        when()
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    public void testThatWeAreReady() {
        await().atMost(5, TimeUnit.MINUTES).until(() -> {
                    List<Pod> list = client.pods().inNamespace(project).list().getItems();
                    return list.stream()
                            .filter(pod -> pod.getMetadata().getName().startsWith(applicationName))
                            .filter(this::isRunning)
                            .collect(Collectors.toList()).size() >= 1;
                }
        );
        // Check that the route is served.
        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> get().getStatusCode() < 500);
        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> get("/")
                .getStatusCode() < 500);
    }

    private boolean isRunning(Pod pod) {
        return "running".equalsIgnoreCase(pod.getStatus().getPhase());
    }
}
