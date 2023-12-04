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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.base.DataSourceIndicator</code> 
 * until separation of inventory report generation from ae core inventory processor.
 * <p>
 * A data structure to represent the matching source of a vulnerability or advisory.
 */
public class AeaaDataSourceIndicator {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaDataSourceIndicator.class);

    private final AeaaContentIdentifiers dataSource;
    private final Reason matchReason;

    public AeaaDataSourceIndicator(AeaaContentIdentifiers dataSource, Reason matchReason) {
        this.dataSource = dataSource;
        this.matchReason = matchReason;
    }

    public Reason getMatchReason() {
        return matchReason;
    }

    public AeaaContentIdentifiers getDataSource() {
        return dataSource;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("source", dataSource.name())
                .put("matches", matchReason.toJson());
    }

    public static AeaaDataSourceIndicator fromJson(JSONObject json) {
        return new AeaaDataSourceIndicator(
                AeaaContentIdentifiers.valueOf(json.getString("source")),
                Reason.fromJson(json.getJSONObject("matches"))
        );
    }

    public static List<AeaaDataSourceIndicator> fromJson(JSONArray json) {
        final List<AeaaDataSourceIndicator> result = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            final Object o = json.get(i);
            if (o instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) o;
                AeaaDataSourceIndicator fromJson = fromJson(jsonObject);
                result.add(fromJson);
            } else {
                LOG.warn("Unexpected JSON object in array on [{}#fromJson(JSONArray)]: {}", AeaaDataSourceIndicator.class, o);
            }
        }
        return result;
    }

    public static JSONArray toJson(Collection<AeaaDataSourceIndicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return new JSONArray();
        }
        return new JSONArray(
                indicators.stream()
                        .map(AeaaDataSourceIndicator::toJson)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return "DataSourceIndicator[" + dataSource + " --> " + (matchReason == null ? "unspecified" : matchReason.toJson()) + "]";
    }

    public static class AssessmentStatusReason extends Reason {
        public final static String TYPE = "assessment-status";

        private final String originFile;

        public AssessmentStatusReason(String originFile) {
            super(TYPE);
            this.originFile = originFile;
        }

        public String getOriginFile() {
            return originFile;
        }

        public String getOriginFileName() {
            if (originFile == null || originFile.isEmpty() || originFile.equals("no-file")) {
                return "no-file";
            }
            return new File(originFile).getName();
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("originFile", originFile);
        }
    }

    public static class ArtifactGhsaReason extends ArtifactReason {
        public final static String TYPE = "artifact-ghsa";

        private final String coordinates;

        public ArtifactGhsaReason(Artifact artifact, String coordinates) {
            super(TYPE, artifact);
            this.coordinates = coordinates;
        }

        protected ArtifactGhsaReason(JSONObject artifactData, String coordinates) {
            super(TYPE, artifactData);
            this.coordinates = coordinates;
        }

        public String getCoordinates() {
            return coordinates;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("coordinates", coordinates);
        }
    }

    public static class VulnerabilityReason extends Reason {
        public final static String TYPE = "vulnerability";

        private final String id;

        public VulnerabilityReason(String id) {
            super(TYPE);
            this.id = id;
        }

        public VulnerabilityReason(AeaaVulnerability vulnerability) {
            super(TYPE);
            this.id = vulnerability.getId();
        }

        public String getId() {
            return id;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("id", id);
        }
    }

    public static class ArtifactCpeReason extends ArtifactReason {
        public final static String TYPE = "artifact-cpe";

        private final String cpe;

        public ArtifactCpeReason(Artifact artifact, String cpe) {
            super(TYPE, artifact);
            this.cpe = cpe;
        }

        protected ArtifactCpeReason(JSONObject artifactData, String cpe) {
            super(TYPE, artifactData);
            this.cpe = cpe;
        }

        public String getCpe() {
            return cpe;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("cpe", cpe);
        }
    }

    public static class MsrcProductReason extends ArtifactReason {
        public final static String TYPE = "msrc-product";

        private final String msrcProductId;
        private final String[] kbIds;

        public MsrcProductReason(Artifact artifact, String msrcProductId, String[] kbIds) {
            super(TYPE, artifact);
            this.msrcProductId = msrcProductId;
            this.kbIds = kbIds;
        }

        protected MsrcProductReason(JSONObject artifactData, String msrcProductId, String[] kbIds) {
            super(TYPE, artifactData);
            this.msrcProductId = msrcProductId;
            this.kbIds = kbIds;
        }

        public String getMsrcProductId() {
            return msrcProductId;
        }

        public String[] getKbIds() {
            return kbIds;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("msrcProductId", msrcProductId)
                    .put("kbIds", kbIds);
        }
    }

    public static class AnyReason extends Reason {
        public final static String TYPE = "any";

        private final String description;

        public AnyReason(String description) {
            super(TYPE);
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("description", description);
        }
    }

    public static class AnyArtifactReason extends ArtifactReason {
        public final static String TYPE = "any-artifact";

        public AnyArtifactReason(Artifact artifact) {
            super(TYPE, artifact);
        }

        protected AnyArtifactReason(JSONObject artifactData) {
            super(TYPE, artifactData);
        }

        @Override
        public JSONObject toJson() {
            return super.toJson();
        }
    }

    public abstract static class ArtifactReason extends Reason {
        protected final Artifact artifact;

        protected final String artifactId;
        protected final String artifactComponent;
        protected final String artifactVersion;

        protected ArtifactReason(String type, Artifact artifact) {
            super(type);
            this.artifact = artifact;
            this.artifactId = artifact.getId();
            this.artifactComponent = artifact.getComponent();
            this.artifactVersion = artifact.getVersion();
        }

        protected ArtifactReason(String type, JSONObject artifactData) {
            super(type);
            this.artifact = null;
            this.artifactId = artifactData.optString("artifactId", null);
            this.artifactComponent = artifactData.optString("artifactComponent", null);
            this.artifactVersion = artifactData.optString("artifactVersion", null);
        }

        public Artifact getArtifact() {
            return artifact;
        }

        public boolean hasArtifact() {
            return artifact != null;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getArtifactComponent() {
            return artifactComponent;
        }

        public String getArtifactVersion() {
            return artifactVersion;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("artifactId", artifactId)
                    .put("artifactComponent", artifactComponent)
                    .put("artifactVersion", artifactVersion);
        }

        public Artifact findArtifact(Set<Artifact> artifacts) {
            if (artifact != null) {
                return artifact;
            }
            return artifacts.stream()
                    .filter(a -> Objects.equals(a.getId(), artifactId))
                    .filter(a -> Objects.equals(a.getComponent(), artifactComponent))
                    .filter(a -> Objects.equals(a.getVersion(), artifactVersion))
                    .findFirst()
                    .orElse(null);
        }
    }

    public abstract static class Reason {
        protected final String type;

        protected Reason(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public JSONObject toJson() {
            return new JSONObject().put("type", type);
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        public static Reason fromJson(JSONObject json) {
            if (!json.has("type")) throw new IllegalArgumentException("Missing type attribute in reason JSON: " + json);
            final String type = json.getString("type");
            switch (type) {
                case VulnerabilityReason.TYPE:
                    return new VulnerabilityReason(json.getString("id"));
                case ArtifactCpeReason.TYPE:
                    return new ArtifactCpeReason(
                            json,
                            json.optString("cpe", null)
                    );
                case MsrcProductReason.TYPE:
                    return new MsrcProductReason(
                            json,
                            json.optString("msrcProductId", null),
                            json.getJSONArray("kbIds").toList().stream().map(Object::toString).toArray(String[]::new)
                    );
                case ArtifactGhsaReason.TYPE:
                    return new ArtifactGhsaReason(
                            json,
                            json.optString("coordinates", null)
                    );
                case AnyReason.TYPE:
                    return new AnyReason(json.optString("description", null));
                case AnyArtifactReason.TYPE:
                    return new AnyArtifactReason(json);
                case AssessmentStatusReason.TYPE:
                    return new AssessmentStatusReason(json.optString("originFile", null));
                default:
                    throw new IllegalArgumentException("Unknown reason type: " + type + "\nIn reason JSON:" + json);
            }
        }
    }
}
