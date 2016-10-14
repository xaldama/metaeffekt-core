/**
 * Copyright 2009-2016 the original author or authors.
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
package org.metaeffekt.core.maven.artifact.publisher;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Prepares the artifact creation by copying selected resources to a dedicated
 * folder structure.
 * 
 * @goal publish-artifact-copy
 * @phase prepare-package
 */
public class PublishArtifactCopyMojo extends AbstractArtifactMojo {

    /**
     * The qualifier of the resource to be copied from.
     * @parameter
     */
    private String sourceQualifier = null;

    /**
     * The classifier of the resource to be copied from.
     * @parameter
     */
    private String sourceClassifier = null;
    
    /**
     * The groupId to be used for the created artifact.
     * @parameter
     */
    private String alternateGroupId = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // exit if the project is pom only
        if (isPomPackagingProject()) {
            return;
        }

        File srcArtifactFile = getArtifactFile(getSourceClassifier(), getSourceQualifier());

        if (srcArtifactFile.exists()) {
            attachArtifact(srcArtifactFile, getAlternateGroupId());
        }
    }

    public String getSourceQualifier() {
        return sourceQualifier;
    }

    public void setSourceQualifier(String sourceQualifier) {
        this.sourceQualifier = sourceQualifier;
    }

    public String getSourceClassifier() {
        return sourceClassifier;
    }

    public void setSourceClassifier(String sourceClassifier) {
        this.sourceClassifier = sourceClassifier;
    }

    public String getAlternateGroupId() {
        return alternateGroupId;
    }

    public void setAlternateGroupId(String alternateGroupId) {
        this.alternateGroupId = alternateGroupId;
    }
    
}
