/*
 * Copyright 2009-2024 the original author or authors.
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

package org.metaeffekt.core.itest.container;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.DuplicateList;
import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.FolderBasedTestSetup;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Predicate;

import static org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator.evaluateComponentPatterns;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.COMPONENT_SOURCE_TYPE;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.VERSION_ANCHOR;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.container.ContainerDumpSetup.saveContainerFromRegistryByRepositoryAndTag;

public class CentOSTest extends AbstractCompositionAnalysisTest {

    public static final NamedBasePredicate<AssetMetaData> CONTAINER_ASSET_PREDICATE = new NamedBasePredicate<AssetMetaData>() {
        @Override
        public Predicate<AssetMetaData> getPredicate() {
            return a -> "container".equals(a.get(TYPE));
        }

        @Override
        public String getDescription() {
            return "Asset of type container";
        }
    };

    @BeforeClass
    public static void prepare() throws IOException, InterruptedException, NoSuchAlgorithmException {
        final File baseDir = saveContainerFromRegistryByRepositoryAndTag(
                null,
                CentOSTest.class.getSimpleName().toLowerCase(),
                "6.9",
                CentOSTest.class.getName());
        AbstractCompositionAnalysisTest.testSetup = new FolderBasedTestSetup()
                .setSource("file://" + baseDir.getAbsolutePath())
                .setName(CentOSTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.clear());
    }

    @Ignore
    @Test
    public void analyse() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.rebuildInventory());
    }

    @Test
    public void testComponentPatterns() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        final Inventory referenceInventory = AbstractCompositionAnalysisTest.testSetup.readReferenceInventory();
        final File baseDir = new File(AbstractCompositionAnalysisTest.testSetup.getScanFolder());

        // FIXME-KKL: strange explicit/implicit style; missing or imprecise semantics I guess
        final List<FilePatternQualifierMapper> filePatternQualifierMapperList =
                evaluateComponentPatterns(referenceInventory, inventory, baseDir);
        final DuplicateList duplicateList = new DuplicateList(filePatternQualifierMapperList);
        duplicateList.identifyRemainingDuplicatesWithoutArtifact();

        // FIXME-AOE: review expectation; was 0; the duplicates are marked as shared includes
        Assert.assertEquals(4, duplicateList.getRemainingDuplicates().size());
        Assert.assertFalse(duplicateList.getFileWithoutDuplicates().isEmpty());
    }

    @Test
    public void testContainerStructure() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();

        final Analysis analysis = new Analysis(inventory);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "rpm")).hasSizeOf(131);
        analysis.selectComponentPatterns(containsToken(VERSION_ANCHOR, "Packages")).hasSizeGreaterThan(1);

        // there must be only one container asset
        analysis.selectAssets(CONTAINER_ASSET_PREDICATE).hasSizeOf(1);

        // FIXME-AOE: here the merge logic seems not to apply. The container-inspect artifact remains
        // we expect the container being only represented as asset; no artifacts with type container or container-inspect expected
        analysis.selectArtifacts(containsToken(TYPE, "container")).hasSizeOf(1);
    }
}
