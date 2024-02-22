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
package org.metaeffekt.core.itest.common.fluent;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.fluent.base.*;
import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;

import java.util.List;
import java.util.stream.Collectors;

public class ArtifactList extends BaseList<Artifact>
        implements BaseListAsserts<Artifact>, BaseListLogger<Artifact, ArtifactList>,
        BaseListFilter<Artifact, ArtifactList>,
        BaseListSize<Artifact, ArtifactList> {

    public ArtifactList(List<Artifact> artifacts, String description) {
        super(artifacts, description);
    }

    public ArtifactList() {
        super();
    }

    @Override
    public ArtifactList filter(NamedBasePredicate<Artifact> namedPredicate) {
        List<Artifact> filteredItems = this.getItemList().stream()
                .filter(namedPredicate.getPredicate())
                .collect(Collectors.toList());

        this.description = this.getDescription() + ", filtered by: " + namedPredicate.getDescription();

        return this.createNewInstance(filteredItems);
    }


    @Override
    public ArtifactList createNewInstance(List<Artifact> filteredList) {
        return new ArtifactList(filteredList, this.description);
    }

    @Override
    public ArtifactList logListWithAllAttributes() {
        LOG.info("LIST " + getDescription());
        getItemList().forEach(artifact -> {
                    String[] attributes = artifact.getAttributes().toArray(new String[0]);
                    LOG.info(withAttributes(artifact, attributes));
                }
        );
        return this;
    }

    static String withAttributes(Artifact artifact, String[] additionalAttributes) {
        StringBuilder sb = new StringBuilder(artifact.toString());
        for (String additionalAttribute : additionalAttributes) {
            sb.append(", ")
                    .append(additionalAttribute)
                    .append(": ")
                    .append(artifact.get(additionalAttribute));
        }
        return sb.toString();
    }
}
