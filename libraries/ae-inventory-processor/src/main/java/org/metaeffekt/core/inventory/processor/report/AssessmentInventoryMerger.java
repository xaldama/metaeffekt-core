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
package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssessmentInventoryMerger {

    private final static Logger LOG = LoggerFactory.getLogger(AssessmentInventoryMerger.class);

    private List<File> inputInventoryFiles;
    private List<Inventory> inputInventories;

    public AssessmentInventoryMerger(List<File> inputInventoryFiles, List<Inventory> inputInventories) {
        this.inputInventoryFiles = inputInventoryFiles;
        this.inputInventories = inputInventories;
    }

    public AssessmentInventoryMerger() {
        this.inputInventoryFiles = new ArrayList<>();
        this.inputInventories = new ArrayList<>();
    }

    public Inventory mergeInventories() throws IOException {
        final Inventory outputInventory = new Inventory();

        final List<Inventory> collectedInventories = new ArrayList<>(inputInventories);
        final List<File> inventoryFiles = collectInventoryFiles();
        LOG.info("Processing [{}] inventories", collectedInventories.size() + inventoryFiles.size());

        {
            final InventoryReader reader = new InventoryReader();
            for (File inventoryFile : inventoryFiles) {
                collectedInventories.add(reader.readInventory(inventoryFile));
            }
        }

        final Map<String, List<Inventory>> assessmentContextInventoryMap = new HashMap<>();
        final Map<String, Inventory> assessmentInventoryMap = new HashMap<>();

        for (Inventory inputInventory : collectedInventories) {
            final AssetMetaData assetMetaData = inputInventory.getAssetMetaData().get(0);

            // in this case the assessment context is the asset (assessment by asset case)
            final String assetName = assetMetaData.get("Name");
            final String assessmentContext = formatNormalizedAssessmentContextName(assetName);
            LOG.info("Processing inventory with asset [{}] and assessment context [{}]", assetName, assessmentContext);

            final List<Inventory> commonInventories = assessmentContextInventoryMap.computeIfAbsent(assessmentContext, a -> new ArrayList<>());
            final int inventoryDisplayIndex = commonInventories.size() + 1;
            final String localUniqueAssessmentId = String.format("%s-%03d", assessmentContext, inventoryDisplayIndex);

            assetMetaData.set("Assessment", localUniqueAssessmentId);
            assetMetaData.set("Name", assetName.toUpperCase());

            commonInventories.add(inputInventory);
            assessmentInventoryMap.put(localUniqueAssessmentId, inputInventory);

            // contribute asset metadata to output inventory
            outputInventory.getAssetMetaData().add(assetMetaData);
        }

        // iterate over each asset in the output inventory and add its respective vulnerabilities
        for (AssetMetaData assetMetaData : outputInventory.getAssetMetaData()) {
            final String localUniqueAssessmentId = assetMetaData.get(AssetMetaData.Attribute.ASSESSMENT);
            if (StringUtils.isNotEmpty(localUniqueAssessmentId)) {
                final Inventory singleInventory = assessmentInventoryMap.get(localUniqueAssessmentId);
                if (singleInventory != null) {
                    outputInventory.getVulnerabilityMetaData(localUniqueAssessmentId).addAll(singleInventory.getVulnerabilityMetaData());
                }
            }
        }

        return outputInventory;
    }

    protected String formatNormalizedAssessmentContextName(String assetName) {
        String assessmentContext = assetName.toUpperCase().replace(" ", "_");
        final int maxAllowedChars = 25;
        assessmentContext = assessmentContext.substring(0, Math.min(assessmentContext.length(), maxAllowedChars - InventoryWriter.VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX.length()));

        // remove trailing "_-"
        while (assessmentContext.endsWith("_") || assessmentContext.endsWith("-")) {
            assessmentContext = assessmentContext.substring(0, assessmentContext.length() - 1);
        }
        return assessmentContext;
    }

    private List<File> collectInventoryFiles() {
        final List<File> inventoryFiles = new ArrayList<>();

        for (File inputInventory : inputInventoryFiles) {
            if (inputInventory.isDirectory()) {
                final String[] files = FileUtils.scanDirectoryForFiles(inputInventory, "*.xls");
                for (String file : files) {
                    inventoryFiles.add(new File(inputInventory, file));
                }
            } else {
                inventoryFiles.add(inputInventory);
            }
        }

        return inventoryFiles;
    }

    public void addInputInventoryFile(File inputInventory) {
        this.inputInventoryFiles.add(inputInventory);
    }

    public void setInputInventoryFiles(List<File> inputInventoryFiles) {
        this.inputInventoryFiles = inputInventoryFiles;
    }

    public List<File> getInputInventoryFiles() {
        return inputInventoryFiles;
    }

    public void addInputInventory(Inventory inputInventory) {
        this.inputInventories.add(inputInventory);
    }

    public void setInputInventories(List<Inventory> inputInventories) {
        this.inputInventories = inputInventories;
    }

    public List<Inventory> getInputInventories() {
        return inputInventories;
    }
}
