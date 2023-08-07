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
package org.metaeffekt.core.inventory.processor.filescan.tasks;

import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Produces an inventory with file names (id), normalized paths (projects), and checksums.
 */
public class DirectoryScanTask extends ScanTask {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryScanTask.class);

    private final FileRef dirRef;

    public DirectoryScanTask(FileRef dirRef, List<String> assetIdChain) {
        super(assetIdChain);
        this.dirRef = dirRef;
    }

    @Override
    public void process(FileSystemScanContext scanContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing " + getClass().getName() + " on: " + dirRef);
        }

        final File[] files = dirRef.getFile().listFiles();
        final FileSystemScanParam scanParam = scanContext.getScanParam();

        for (File file : files) {
            final FileRef fileRef = new FileRef(file);

            if (scanParam.collects(fileRef.getPath())) {

                // dispatch depending on type (file or folder)
                if (file.isFile()) {
                    scanContext.push(new FileCollectTask(fileRef, getAssetIdChain()));
                } else {
                    final String folderName = file.getName();
                    final boolean implicitFolder = folderName.startsWith("[") && folderName.endsWith("]");
                    if (!implicitFolder) {
                        // implicit folders are collected when processing the archives subtree
                        scanContext.push(new DirectoryScanTask(fileRef, getAssetIdChain()));
                    } else {
                        // check whether folder is originating from an archive
                        final String expectedName = folderName.substring(1, folderName.length() - 1);
                        final File expectedFile = new File(file.getParentFile(), expectedName);
                        if (!expectedFile.exists()) {
                            // archive does not exist; scan
                            scanContext.push(new DirectoryScanTask(fileRef, getAssetIdChain()));
                        }
                    }
                }

            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignored {} due to collect include/exclude patterns.", file.getAbsolutePath());
                }
            }
        }
    }

}
