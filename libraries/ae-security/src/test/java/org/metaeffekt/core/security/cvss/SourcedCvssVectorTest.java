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
package org.metaeffekt.core.security.cvss;

import org.junit.Test;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

import static org.junit.Assert.assertEquals;

public class SourcedCvssVectorTest {

    @Test
    public void cloneSourceTest() {
        SourcedCvssVector<Cvss3P1> sourcedVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, Cvss3P1.class),
                "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N"
        );

        SourcedCvssVector<Cvss3P1> clone = sourcedVector.clone();
        clone.equals(sourcedVector);
        assertEquals(sourcedVector, clone);
    }
}
