/*
 * Copyright (C) 2021 Peter Paul Bakker, Stokpop Software Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.stokpop.event.wiremock;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import nl.stokpop.eventscheduler.EventMessageBusSimple;
import nl.stokpop.eventscheduler.api.CustomEvent;
import nl.stokpop.eventscheduler.api.config.TestConfig;
import nl.stokpop.eventscheduler.api.message.EventMessageBus;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class WiremockEventTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Test
    public void runningSomeEvents() {

        WiremockEventConfig eventConfig = new WiremockEventConfig();
        eventConfig.setName("myWiremockEvent");
        eventConfig.setWiremockFilesDir(new File(".","src/test/resources/wiremock-stubs").getAbsolutePath());
        eventConfig.setWiremockUrl("http://localhost:" + wireMockRule.port() + ",http://localhost:" + wireMockRule.port());
        eventConfig.setTestConfig(TestConfig.builder().testRunId("my-test-run-id").build());

        EventMessageBus messageBus = new EventMessageBusSimple();

        WiremockEvent event = new WiremockEvent(eventConfig.toContext(), messageBus, EventLoggerStdOut.INSTANCE);
        event.beforeTest();
        event.keepAlive();
        event.customEvent(CustomEvent.createFromLine("PT3S|wiremock-change-delay|delay=4000"));
        event.customEvent(CustomEvent.createFromLine("PT1M|wiremock-change-delay|delay=8000"));
        event.afterTest();

        // event.check() can be used to check if all went well with the wiremock calls, todo?

    }

    @Test
    public void parseSettingsZero() {
        Map<String, String> emptyMap = WiremockEvent.parseSettings("");
        assertEquals(0, emptyMap.size());
    }

    @Test
    public void parseSettingsOne() {
        Map<String, String> settings = WiremockEvent.parseSettings("foo=bar");
        assertEquals(1, settings.size());
        assertEquals("bar", settings.get("foo"));
    }

    @Test
    public void parseSettingsTwo() {
        Map<String, String> settings = WiremockEvent.parseSettings("foo=bar;name=stokpop");
        assertEquals(2, settings.size());
        assertEquals("bar", settings.get("foo"));
        assertEquals("stokpop", settings.get("name"));
    }

    @Test
    public void parseSettingsNoValue() {
        Map<String, String> settings = WiremockEvent.parseSettings("foo=bar;name");
        assertEquals(2,settings.size());
        assertEquals("bar", settings.get("foo"));
        assertEquals("", settings.get("name"));
    }

    @Test
    public void parseSettingsNoEntry() {
        Map<String, String> settings = WiremockEvent.parseSettings("foo=bar;");
        assertEquals(1, settings.size());
        assertEquals("bar", settings.get("foo"));
    }

}