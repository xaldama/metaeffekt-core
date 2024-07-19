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
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.COMPONENT_SOURCE_TYPE;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.VERSION_ANCHOR;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.common.predicates.TokenStartsWith.tokenStartsWith;

public class ClamavIcapTest extends AbstractCompositionAnalysisTest {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        AbstractCompositionAnalysisTest.testSetup = new UrlBasedTestSetup()
                .setSource("http://ae-scanner/images/CID-clamav-icap%40891f267a6b2a304616854ad2f013dc5d23f6f6c84d535c8b46e76d124fe39b6a-export.tar")
                .setSha256Hash("51e4d79caa561f31834b8cdd3d19f1ed85e239986f69e33b9e46b9c384eddbee")
                .setName(ClamavIcapTest.class.getName());
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
    public void testContainerStructure() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);
        analysis.selectComponentPatterns(tokenStartsWith(COMPONENT_SOURCE_TYPE, "apk")).hasSizeOf(30);
        analysis.selectComponentPatterns(containsToken(VERSION_ANCHOR, "/installed")).hasSizeGreaterThan(1);
    }
}
