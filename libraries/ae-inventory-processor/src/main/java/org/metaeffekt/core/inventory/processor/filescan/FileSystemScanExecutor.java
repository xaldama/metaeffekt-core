/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.filescan;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ArtifactUnwrapTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.DirectoryScanTask;
import org.metaeffekt.core.inventory.processor.filescan.tasks.ScanTask;
import org.metaeffekt.core.inventory.processor.inspector.InspectorRunner;
import org.metaeffekt.core.inventory.processor.inspector.JarInspector;
import org.metaeffekt.core.inventory.processor.inspector.NestedJarInspector;
import org.metaeffekt.core.inventory.processor.inspector.param.JarInspectionParam;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.*;
import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_PATH_IN_ASSET;
import static org.metaeffekt.core.inventory.processor.model.Constants.MARKER_CROSS;

public class FileSystemScanExecutor implements FileSystemScanTaskListener {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemScanExecutor.class);

    private final FileSystemScanContext fileSystemScanContext;

    private ExecutorService executor;

    private final Map<ScanTask, Future<?>> monitor;

    public FileSystemScanExecutor(FileSystemScanContext fileSystemScan) {
        this.fileSystemScanContext = fileSystemScan;
        fileSystemScan.setScanTaskListener(this);
        this.monitor = new ConcurrentHashMap<>();
    }

    public void execute() {
        LOG.info("Triggering scan on {}...", fileSystemScanContext.getBaseDir().getPath());

        this.executor = Executors.newFixedThreadPool(4);

        // start with an empty assetIdChain on the root
        final ArrayList<String> assetIdChain = new ArrayList<>();

        // trigger an initial scan from basedir
        fileSystemScanContext.push(new DirectoryScanTask(fileSystemScanContext.getBaseDir(), assetIdChain));

        LOG.info("Awaiting triggered tasks to finish.");
        awaitTasks();

        // the scanner works in sequences
        boolean iteration = true;
        while (iteration) {

            LOG.info("Collecting outstanding tasks.");
            iteration = false;

            // wait for existing scan tasks to finish
            awaitTasks();

            // marks artifacts with scan classifier
            runNestedComponentInspection();

            // TODO: check invariants
            //  * either SCAN_DIRECTIVE=delete OR checksum != null
            //  * ASSET_ID_CHAIN?

            // collect tasks based on the current inventory (process markers)
            final List<ScanTask> scanTasks = collectOutstandingScanTasks();

            // push tasks for being processed and mark for another iteration
            LOG.info("Triggering {} outstanding tasks.", scanTasks.size());
            if (!scanTasks.isEmpty()) {
                scanTasks.forEach(fileSystemScanContext::push);
                iteration = true;
            }
        }

        executor.shutdown();

        if (fileSystemScanContext.getScanParam().isDetectComponentPatterns()) {
            // currently we detect component patterns on the final directory
            final ComponentPatternProducer patternProducer = new ComponentPatternProducer();
            final Inventory implicitRereferenceInventory = new Inventory();
            patternProducer.detectAndApplyComponentPatterns(implicitRereferenceInventory, fileSystemScanContext);
        }

        // run remaining inspection last
        inspectArtifacts();

        setArtifactAssetMarker();


        final Inventory inventory = fileSystemScanContext.getInventory();

        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_INSPECTED, inventory);
        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_SCAN_DIRECTIVE, inventory);
        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_ARTIFACT_PATH, inventory);
        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_ASSET_ID_CHAIN, inventory);
        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_ASSET_PATH, inventory);
        InventoryUtils.removeArtifactAttribute(ATTRIBUTE_KEY_COMPONENT_PATTERN_MARKER, inventory);

        mergeDuplicates(inventory);

        LOG.info("Scan completed.");
    }

    private void setArtifactAssetMarker() {
        for (Artifact artifact : fileSystemScanContext.getInventory().getArtifacts()) {
            final String assetIdChainString = artifact.get(ATTRIBUTE_KEY_ASSET_ID_CHAIN);

            if (StringUtils.isNotBlank(artifact.get(ATTRIBUTE_KEY_COMPONENT_PATTERN_MARKER))) continue;

            if (StringUtils.isNotBlank(assetIdChainString) && !(".").equals(assetIdChainString)) {
                final String[] split = assetIdChainString.split("\\|\n");
                for (String assetPath : split) {
                    String assetId = fileSystemScanContext.getPathToAssetIdMap().get(assetPath);

                    // fallback to asset path; more as a processing failure indication
                    if (StringUtils.isBlank(assetId)) {
                        assetId = "AID-" + assetPath;
                    }

                    if (StringUtils.isNotBlank(assetId)) {
                        artifact.set(assetId, MARKER_CROSS);
                    }
                }
            }
        }
    }

    private void runNestedComponentInspection() {
        final Properties properties = new Properties();
        properties.put(ProjectPathParam.KEY_PROJECT_PATH, fileSystemScanContext.getBaseDir().getPath());

        final NestedJarInspector nestedJarInspector = new NestedJarInspector();

        for (Artifact artifact : fileSystemScanContext.getInventory().getArtifacts()) {
            boolean inspected = StringUtils.isNotBlank(artifact.get(ATTRIBUTE_KEY_INSPECTED));
            if (!inspected) {

                // run executors here
                nestedJarInspector.run(artifact, properties);

                artifact.set(ATTRIBUTE_KEY_INSPECTED, MARKER_CROSS);

                if (artifact.hasClassification(HINT_SCAN)) {
                    artifact.set(ATTRIBUTE_KEY_UNWRAP, MARKER_CROSS);
                }
            }
        }
    }

    private List<ScanTask> collectOutstandingScanTasks() {
        final List<ScanTask> scanTasks = new ArrayList<>();

        final Inventory inventory = fileSystemScanContext.getInventory();
        final Inventory referenceInventory = fileSystemScanContext.getScanParam().getReferenceInventory();

        final ComponentPatternProducer componentPatternProducer = new ComponentPatternProducer();

        // match and apply component patterns before performing unwrapping
        // only anticipates predefined component patterns
        componentPatternProducer.matchAndApplyComponentPatterns(referenceInventory, fileSystemScanContext);

        for (Artifact artifact : inventory.getArtifacts()) {
            if (!StringUtils.isEmpty(artifact.get(ATTRIBUTE_KEY_UNWRAP))) {

                // TODO: exclude artifacts removed (though collection in component)
                scanTasks.add(new ArtifactUnwrapTask(artifact, null));
            }
        }

        return scanTasks;
    }


    public void awaitTasks() {
        Collection<Future<?>> futures = new HashSet<>(monitor.values());
        while (!futures.isEmpty()) {
            boolean allDone = true;
            for (Future<?> future : futures) {
                if (!future.isDone() && !future.isCancelled()) {
                    allDone = false;
                }
            }
            if (!allDone) {
                futures = new HashSet<>(monitor.values());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                break;
            }
        }
    }

    @Override
    public void notifyOnTaskPushed(ScanTask scanTask) {
        final Future<?> future = executor.submit(() -> {
            try {
                scanTask.process(fileSystemScanContext);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            } finally {
                notifyOnTaskPopped(scanTask);
            }
        });
        monitor.put(scanTask, future);
    }

    @Override
    public void notifyOnTaskPopped(ScanTask scanTask) {
        monitor.remove(scanTask);
    }

    private void inspectArtifacts() {
        // attempt to extract artifactId, version, groupId from contained POMs
        final Properties properties = new Properties();
        properties.put(ProjectPathParam.KEY_PROJECT_PATH, fileSystemScanContext.getBaseDir().getPath());
        properties.put(JarInspectionParam.KEY_INCLUDE_EMBEDDED,
                Boolean.toString(fileSystemScanContext.getScanParam().isIncludeEmbedded()));

        // run further inspections on identified artifacts
        final InspectorRunner runner = InspectorRunner.builder()
                .queue(JarInspector.class)
                .build();

        runner.executeAll(fileSystemScanContext.getInventory(), properties);

        final List<AssetMetaData> assetMetaDataList = fileSystemScanContext.getInventory().getAssetMetaData();

        if (assetMetaDataList != null) {
            for (AssetMetaData assetMetaData : assetMetaDataList) {
                final String path = assetMetaData.get(ATTRIBUTE_KEY_ASSET_PATH);
                final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);
                if (StringUtils.isNotBlank(path) && StringUtils.isNotBlank(assetId)) {
                    // FIXME: we may need a map to list; validated; refers to putIfAbsent
                    fileSystemScanContext.getPathToAssetIdMap().putIfAbsent(path, assetId);
                }
            }
        }
    }

    public void mergeDuplicates(Inventory inventory) {

        final Map<String, List<Artifact>> stringListMap = buildQualifierArtifactMap(inventory);

        for (List<Artifact> list : stringListMap.values()) {
            Artifact artifact = list.get(0);

            HashSet<String> paths = new HashSet<>();
            addPath(artifact, paths);
            for (int i = 1; i < list.size(); i++) {
                final Artifact a = list.get(i);
                inventory.getArtifacts().remove(a);
                artifact.merge(a);
                addPath(a, paths);
            }

            if (!paths.isEmpty()) {
                artifact.setProjects(paths);
                artifact.set(KEY_PATH_IN_ASSET, paths.stream().sorted(String::compareToIgnoreCase).collect(Collectors.joining("|\n")));
            }
        }

        InventoryUtils.mergeDuplicateAssets(inventory);

    }

    private static void addPath(Artifact a, HashSet<String> paths) {
        String path = a.get(KEY_PATH_IN_ASSET);
        if (StringUtils.isNotBlank(path)) {
            paths.add(path);
        }
    }

    public Map<String, List<Artifact>> buildQualifierArtifactMap(Inventory inventory) {
        final Map<String, List<Artifact>> qualifierArtifactMap = new LinkedHashMap<>();

        for (final Artifact artifact : inventory.getArtifacts()) {
            final String artifactQualifier = qualifierOf(artifact);
            final List<Artifact> artifacts = qualifierArtifactMap.computeIfAbsent(artifactQualifier, a -> new ArrayList<>());
            artifacts.add(artifact);
        }
        return qualifierArtifactMap;
    }

    public String qualifierOf(Artifact artifact) {
        if (StringUtils.isNotBlank(artifact.getChecksum())) {
            // in case we have a checksum id and checksum are sufficient
            return "[" +
                    artifact.getId() + "-" +
                    artifact.getChecksum() +
                    "]";
        } else {
            // in case there is no checksum use groupId and version as discriminator
            return "[" +
                    artifact.getId() + "-" +
                    artifact.getGroupId() + "-" +
                    artifact.getVersion() +
                    "]";

        }
    }

}
