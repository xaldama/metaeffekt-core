/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public abstract class AbstractInventoryExtractionMojo extends AbstractProjectAwareConfiguredMojo {

    @Parameter(defaultValue="${project.artifactId}")
    protected String artifactInventoryId;

    @Parameter(defaultValue="${project.build.directory}/inventory/${project.artifactId}-container-inventory.xls")
    protected File targetInventoryFile;

}
