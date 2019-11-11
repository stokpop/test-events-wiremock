/*
 * Copyright (C) 2019 Peter Paul Bakker, Stokpop Software Solutions
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

import nl.stokpop.eventscheduler.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;

public class WiremockEvent extends EventAdapter {

    private static final String WIREMOCK_FILES_DIR = "wiremockFilesDir";
    private static final String WIREMOCK_URL = "wiremockUrl";
    private static final String CLASS_NAME = WiremockEvent.class.getName();

    static boolean isDebugEnabled = false;

    private List<WiremockClient> clients;
    private File rootDir;

    static {
        sayStatic("class loaded");
    }

    public WiremockEvent(String eventName, TestContext testContext, EventProperties eventProperties, EventLogger logger) {
        super(eventName, testContext, eventProperties, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("Hello before test [" + testContext.getTestRunId() + "]");

        String filesDir = eventProperties.getProperty(WIREMOCK_FILES_DIR);
        if (filesDir == null) {
            throw new WiremockEventException(String.format("property %s is not set", WIREMOCK_FILES_DIR));
        }
        rootDir = new File(filesDir);
        if (!rootDir.exists()) {
            throw new WiremockEventException(String.format("directory not found: %s", rootDir));
        }

        String wiremockUrl = eventProperties.getProperty(WIREMOCK_URL);
        if (wiremockUrl == null) {
            throw new WiremockEventException(String.format("property %s is not set", WIREMOCK_URL));
        }
        clients = Arrays.stream(wiremockUrl.split(","))
                .map(WiremockClient::new)
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private void importAllWiremockFiles(WiremockClient client, File[] files, Map<String, String> replacements) {
        Arrays.stream(files)
                .peek(file -> logger.info("check " + file))
                .filter(file -> !file.isDirectory())
                .filter(File::canRead)
                .peek(file -> logger.info("import " + file))
                .map(this::readContents)
                .filter(Objects::nonNull)
                .forEach(fileContents -> client.uploadFileWithReplacements(fileContents, replacements));
    }

    private String readContents(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("reading file: " + file);
            return null;
        }
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {

        String eventName = scheduleEvent.getName();
        
        if ("wiremock-change-delay".equalsIgnoreCase(eventName)) {
            injectDelayFromSettings(scheduleEvent);
        }
        else {
            logger.debug("ignoring unknown event [" + eventName + "]");
        }
    }

    private void injectDelayFromSettings(CustomEvent scheduleEvent) {
        Map<String, String> replacements = parseSettings(scheduleEvent.getSettings());
        if (rootDir != null && clients != null) {
            clients.forEach(client -> importAllWiremockFiles(client, rootDir.listFiles(), replacements));
        }
    }

    static Map<String, String> parseSettings(String eventSettings) {
        if (eventSettings == null || eventSettings.trim().length() == 0) {
            return Collections.emptyMap();
        }
        return Arrays.stream(eventSettings.split(";"))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(k -> k[0], v -> v.length == 2 ? v[1] : ""));
    }

    private static void sayStatic(String something) {
        System.out.println(String.format("[INFO] [%s] %s", CLASS_NAME , something));
    }
}