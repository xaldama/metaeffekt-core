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
package org.metaeffekt.core.inventory.processor.writer;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class InventoryWriter extends AbstractXlsInventoryWriter {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    public static final String VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX = "Assessment-";

    public static final String SINGLE_VULNERABILITY_ASSESSMENT_WORKSHEET = "Vulnerabilities";

    public void writeInventory(Inventory inventory, File file) throws IOException {
        if (file.getName().endsWith(".xls")) {
            new XlsInventoryWriter().writeInventory(inventory, file);
        } else {
            new XlsxInventoryWriter().writeInventory(inventory, file);
        }
    }

}
