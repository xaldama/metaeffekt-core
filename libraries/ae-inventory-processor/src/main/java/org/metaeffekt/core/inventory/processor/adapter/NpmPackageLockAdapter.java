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
package org.metaeffekt.core.inventory.processor.adapter;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Extracts an inventory for production npm modules based on a package-lock.json file.
 */
public class NpmPackageLockAdapter {

    /**
     *
     * @param packageLockJsonFile The package-lock.json file to parse.
     * @param relPath The relative path to the file from the relevant basedir.
     *
     * @return An inventory popoulated with the runtime modules defined in the package json file.
     *
     * @throws IOException May throw {@link IOException} when accessing and parsing the packageLockJsonFile.
     */
    public Inventory createInventoryFromPackageLock(File packageLockJsonFile, String relPath) throws IOException {
        final Inventory inventory = new Inventory();

        populateInventory(packageLockJsonFile, inventory, relPath);

        return inventory;
    }

    private void populateInventory(File packageLockJsonFile, Inventory inventory, String path) throws IOException {
        final String json = FileUtils.readFileToString(packageLockJsonFile, FileUtils.ENCODING_UTF_8);
        final JSONObject obj = new JSONObject(json);
        addDependencies(obj, inventory, path, "dependencies");
        addDependencies(obj, inventory, path, "peerDependencies");
        addDependencies(obj, inventory, path, "packages");
    }

    private void addDependencies(JSONObject obj, Inventory inventory, String path, String dependencyTag) {
        final String prefix = "node_modules/";

        if (obj.has(dependencyTag)) {
            final JSONObject dependencies = obj.getJSONObject(dependencyTag);
            for (String key : dependencies.keySet()) {
                if (StringUtils.isBlank(key)) continue;

                final JSONObject dep = dependencies.getJSONObject(key);

                String version = dep.getString("version");
                String url = dep.has("resolved") ? dep.getString("resolved") : null;

                Artifact artifact = new Artifact();

                String module = key;
                int index = module.lastIndexOf(prefix);
                if (index != -1) {
                    module = module.substring(index + prefix.length());
                }

                artifact.setId(module + "-" + version);
                artifact.setComponent(module);
                artifact.setVersion(version);
                artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_NODEJS_MODULE);
                artifact.setUrl(url);
                artifact.set(Constants.KEY_PATH_IN_ASSET, path + "[" + key + "]");

                boolean production = !dep.has("dev") || !dep.getBoolean("dev");

                // only consider production artifacts
                if (production) {
                    inventory.getArtifacts().add(artifact);

                    // validate whether this is still required
                    addDependencies(dep, inventory, path, dependencyTag);
                }
            }
        }
    }

}
