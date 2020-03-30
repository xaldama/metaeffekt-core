/**
 * Copyright 2009-2020 the original author or authors.
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
package org.metaeffekt.core.inventory.scan;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.DirectoryInventoryScan;

import java.io.File;
import java.io.IOException;

public class DirectoryInventoryScanTest {

    @Test
    public void testScanExtractedFiles() throws IOException {
        File inputDir = new File("src/test/resources/test-scan-inputs");
        File scanDir = new File("target/test-scan");
        String[] scanIncludes = new String[] {"**/*"};
        String[] scanExcludes = new String[] {"--none--"};
        File inventoryFile = new File("src/test/resources/test-inventory-01/artifact-inventory.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        final DirectoryInventoryScan scan = new DirectoryInventoryScan(inputDir, scanDir, scanIncludes, scanExcludes, inventory);

        scan.setEnableImplicitUnpack(false);
        final Inventory resultInventory = scan.createScanInventory();

        for (Artifact a : resultInventory.getArtifacts()) {
            System.out.println(a.getId() + " - " + a.getChecksum() + " - " + a.getProjects());
        }

        Assertions.assertThat(resultInventory.findAllWithId("file.txt").size()).isEqualTo(2);
        Assertions.assertThat(resultInventory.findArtifactByIdAndChecksum("file.txt", "6a38dfd8c715a9465f871d776267043e")).isNotNull();
        Assertions.assertThat(resultInventory.findArtifactByIdAndChecksum("file.txt", "8dc71de3cc97ca6d4cd8c9b876252823")).isNotNull();

        // these are covered by component patterns
        Assertions.assertThat(resultInventory.findArtifact("a.txt")).isNull();
        Assertions.assertThat(resultInventory.findArtifact("A Files")).isNotNull();
        Assertions.assertThat(resultInventory.findArtifact("b.txt")).isNull();
        Assertions.assertThat(resultInventory.findArtifact("B Files")).isNotNull();
    }

}
