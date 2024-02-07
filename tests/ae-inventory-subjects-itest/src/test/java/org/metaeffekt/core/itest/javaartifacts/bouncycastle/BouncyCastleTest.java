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
package org.metaeffekt.core.itest.javaartifacts.bouncycastle;

import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.javaartifacts.TestBasicInvariants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.inventory.dsl.predicates.IdMissmatchesVersion.ID_MISMATCHING_VERSION;

public class BouncyCastleTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.77/bcprov-jdk18on-1.77.jar")
                .setName(BouncyCastleTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(preparer.clear());

    }

    @Ignore
    @Test
    public void analyse() throws Exception {
        Assert.assertTrue(preparer.rebuildInventory());
    }

    //TODO
    @Ignore
    @Test
    public void versionMismatch() {
        getAnalysisAfterInvariants()
                .selectArtifacts(ID_MISMATCHING_VERSION)
                .logArtifactList("Type")
                .assertEmpty();
    }


}
