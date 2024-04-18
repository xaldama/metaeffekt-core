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
package org.metaeffekt.core.inventory.processor.model;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Artifact extends AbstractModelBase {

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

    private static final String DELIMITER_DASH = "-";
    private static final char DELIMITER_DOT = '.';
    private static final char DELIMITER_COLON = ':';
    private static final String DELIMITER_UNDERSCORE = "_";

    // FIXME: revise attribute name projects; should be paths (in asset)
    public static final String PROJECT_DELIMITER_REGEXP = "\\|\n";

    public static final String PROJECT_DELIMITER = "|\n";

    /**
     * Core attributes to support component patterns.
     */
    public enum Attribute implements AbstractModelBase.Attribute {
        ID("Id"),
        COMPONENT("Component"),
        CHECKSUM("Checksum"),
        VERSION("Version"),

        // latest available version
        LATEST_VERSION("Latest Version"),
        CLASSIFICATION("Classification"),
        LICENSE("License"),
        GROUPID("Group Id"),

        // artifact type information
        TYPE("Type"),

        // comments (and hints)
        COMMENT("Comment"),

        // url of the project pages
        URL("URL"),

        // indicates whether the artifact is security relevant and needs to be upgraded asap
        SECURITY_RELEVANT("Security Relevance"),

        // if the artifact is security relevant it is classified into a security category
        SECURITY_CATEGORY("Security Relevance"),

        // vulnerability information
        VULNERABILITY("Vulnerability"),

        // FIXME: rename to locations
        // project locations
        PROJECTS("Projects"),

        VERIFIED("Verified"),
        ERRORS("Errors"),
        HASH_SHA256("Hash (SHA-256)"),
        PATH_IN_ASSET("Path in Asset"),
        VIRTUAL_ROOT_PATH("Virtual Root Path"),
        PURL("PURL");

        private String key;

        Attribute(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }


    // artifact id (derived from id and version)
    private transient String artifactId;

    /**
     * Relevant means "reportRelevant" meaning that the artifact and its metadata needs to be included in the build.
     */
    private transient boolean relevant = true;

    /**
     * Managed means that the artifact may fail the build, when the meta data reflects issues.
     */
    private transient boolean managed = true;

    public Artifact() {
    }

    /**
     * Copy constructor.
     *
     * @param artifact The artifact to copy from.
     */
    public Artifact(Artifact artifact) {
        super(artifact);

        // copy transient attributes
        this.artifactId = artifact.getArtifactId();
        this.relevant = artifact.isRelevant();
        this.managed = artifact.isManaged();
    }

    public Set<String> getProjects() {
        final String projectsString = get(Attribute.PROJECTS);
        if (StringUtils.isEmpty(projectsString)) {
            return Collections.emptySet();
        }
        return Arrays.stream(projectsString.split(PROJECT_DELIMITER_REGEXP)).
                map(String::trim).collect(Collectors.toSet());
    }

    public void setProjects(Set<String> project) {
        set(Attribute.PROJECTS, project.stream().collect(Collectors.joining(PROJECT_DELIMITER)));
    }

    public String getComponent() {
        return get(Attribute.COMPONENT);
    }

    public void setComponent(String component) {
        set(Attribute.COMPONENT, component);
    }

    public String getGroupId() {
        return get(Attribute.GROUPID);
    }

    public void setGroupId(String groupId) {
        set(Attribute.GROUPID, groupId);
    }

    public String getId() {
        return get(Attribute.ID);
    }

    public void setId(String id) {
        set(Attribute.ID, id);
    }

    public String getVersion() {
        return get(Attribute.VERSION);
    }

    public void setVersion(String version) {
        set(Attribute.VERSION, version);
    }

    public String getLicense() {
        return get(Attribute.LICENSE);
    }

    public void setLicense(String license) {
        set(Attribute.LICENSE, license);
    }

    public String getUrl() {
        return get(Attribute.URL);
    }

    public void setUrl(String url) {
        set(Attribute.URL, url);
    }

    public String getClassification() {
        return get(Attribute.CLASSIFICATION);
    }

    public void setClassification(String classification) {
        set(Attribute.CLASSIFICATION, classification);
    }

    @Deprecated
    public boolean isVerified() {
        // FIXME: remove this concept
        return "X".equalsIgnoreCase(get(Attribute.VERIFIED));
    }

    @Deprecated
    public void setVerified(boolean verified) {
        set(Attribute.VERIFIED, verified ? "X" : null);
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getComment() {
        return get(Attribute.COMMENT);
    }

    public void setComment(String comment) {
        set(Attribute.COMMENT, comment);
    }

    public String getLatestVersion() {
        return get(Attribute.LATEST_VERSION);
    }

    public void setLatestVersion(String latestAvailableVersion) {
        set(Attribute.LATEST_VERSION, latestAvailableVersion);
    }

    public String toString() {
        return "Artifact id: " + getId() + ", component: " + getComponent() + ", version: " + getVersion();
    }

    public void addProject(String project) {
        final Set<String> projects = getProjects();
        if (projects.contains(project)) return;

        // use append to derive new value
        append(Attribute.PROJECTS.getKey(), project, PROJECT_DELIMITER);
    }

    public void merge(Artifact a) {

        // projects merge differently
        mergeProjects(a);

        // merge attributes
        super.merge(a);

        deriveArtifactId();
    }

    private void mergeProjects(Artifact a) {
        Set<String> projects = new HashSet<>(getProjects());
        projects.addAll(a.getProjects());
        setProjects(projects);
    }

    /**
     * Derive a qualifier that uniquely represents an artifact.
     *
     * @return The derived artifact qualifier.
     */
    public String deriveQualifier() {
        final String id = getId();

        final StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(id)) {
            sb.append(id.trim());
        }
        sb.append(":");
        if (StringUtils.isNotBlank(getComponent())) {
            sb.append(getComponent().trim());
        }
        sb.append(":");
        if (StringUtils.isNotBlank(getGroupId())) {
            sb.append(getGroupId().trim());
        }
        sb.append(":");
        if (StringUtils.isNotBlank(getChecksum())) {
            sb.append(getChecksum().trim());
        }
        sb.append(":");
        if (StringUtils.isNotBlank(getVersion())) {
            sb.append(getVersion().trim());
        }

        return sb.toString();
    }

    public void deriveArtifactId() {
        if (artifactId == null) {
            String artifactId = extractArtifactId(getId(), getVersion());
            if (artifactId == null) {
                artifactId = getId();
            }
            this.setArtifactId(artifactId);
        }
    }

    /**
     * Extracts a derived artifactId. The artifactId is derived from the artifact file component. The extraction here is
     * based on the knowledge of the version. This is particularly the case, when using maven as repository manager.
     * Where the file component is constructed as artifactId-version[-classifier].type. The version therefore can be used to
     * separate the artifactId from the remaining pieces of the file component.
     *
     * @param id      The artifact id.
     * @param version The version of the artifact.
     * @return The derived artifact id or null, in case the version is not part of the file component.
     */
    public String extractArtifactId(String id, String version) {
        if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(version)) {
            int index = id.lastIndexOf(version);
            if (index != -1) {
                id = id.substring(0, index);
                if (id.endsWith(DELIMITER_DASH)) {
                    id = id.substring(0, id.length() - 1);
                } else if (id.endsWith(DELIMITER_UNDERSCORE)) {
                    // underscore are the delimiter e.g. when dealing with osgi bundles
                    id = id.substring(0, id.length() - 1);
                }
                if (StringUtils.isNotBlank(id)) {
                    return id;
                }
            }
        }
        return null;
    }


    public String createStringRepresentation() {
        StringBuffer artifactRepresentation = new StringBuffer();
        if (getGroupId() != null) {
            artifactRepresentation.append(getGroupId());
        }
        artifactRepresentation.append(DELIMITER_COLON);
        if (artifactId != null) {
            artifactRepresentation.append(getArtifactId());
        }
        artifactRepresentation.append(DELIMITER_COLON);
        if (getVersion() != null) {
            artifactRepresentation.append(getVersion());
        }
        if (getClassifier() != null) {
            artifactRepresentation.append(DELIMITER_COLON);
            artifactRepresentation.append(getClassifier());
        }
        artifactRepresentation.append(DELIMITER_COLON);
        // skip type if no information was derived
        if (getId() != null && !getId().equals(artifactId)) {
            artifactRepresentation.append(getType());
        }
        return artifactRepresentation.toString();
    }

    private String inferTypeFromId() {
        String type = null;
        String id = getId();
        if (id != null) {
            String classifier = inferClassifierFromId();
            String version = inferVersionFromId();

            final String versionClassifierPart;
            if (classifier == null) {
                versionClassifierPart = version + DELIMITER_DOT;
            } else {
                versionClassifierPart = version + DELIMITER_DASH + classifier + DELIMITER_DOT;
            }

            final int index = id.lastIndexOf(versionClassifierPart);
            if (index != -1) {
                type = id.substring(index + versionClassifierPart.length());
            }

            if (type == null) {
                final int i = id.lastIndexOf(DELIMITER_DOT);
                if (i != -1) {
                    type = id.substring(i + 1);
                }
            }
        }
        return type;
    }

    private String inferVersionFromId() {
        String version = getVersion();
        if (!StringUtils.isNotBlank(version)) {
            version = deriveVersionFromId();
        }
        return version;
    }

    public String deriveVersionFromId() {
        String version = getId();
        if (version != null) {
            if (version.indexOf('.') > 0) {
                version = version.substring(0, version.lastIndexOf('.'));
            }

            if (version.endsWith("-tests") ||
                    version.endsWith("-api") ||
                    version.endsWith("-config") ||
                    version.endsWith("-source") ||
                    version.endsWith("-sources") ||
                    version.endsWith("-bootstrap") ||
                    version.endsWith("-mock") ||
                    version.endsWith("-doc") ||
                    version.endsWith("-runtime")) {
                version = version.substring(0, version.lastIndexOf('-'));
            }

            if (version.endsWith("-api") ||
                    version.endsWith("-runtime")) {
                version = version.substring(0, version.lastIndexOf('-'));
            }

            while (version.length() > 0 && version.substring(0, 1).matches("[a-zA-Z]")) {
                int index = version.indexOf('-');
                if (index > -1) {
                    version = version.substring(index + 1);
                } else
                    break;
            }

            int index = version.indexOf('/');
            if (index > -1) {
                version = version.substring(0, index);
            }
        }
        return version;
    }

    /**
     * Anticipated a string in the shape "artifactId-version-classifier.extension". The part between
     * "-version-" and the first "." are regarded the classifier.
     *
     * @return
     */
    private String inferClassifierFromId() {
        final String id = getId();
        final String version = getVersion();
        if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(version)) {
            // get rid of anything right to version
            final String queryString = DELIMITER_DASH + version + DELIMITER_DASH;
            final int versionIndex = id.indexOf(queryString);
            if (versionIndex < 0) {
                // no '-<version>-' part, no classifier
                return null;
            }
            final int beginIndex = versionIndex + queryString.length();
            final String classifierAndType = id.substring(beginIndex);
            // get rid of trailing .{type}
            final int index = classifierAndType.indexOf(DELIMITER_DOT);
            if (index != -1) {
                final String classifier = classifierAndType.substring(0, index).trim();
                if (StringUtils.isNotBlank(classifier)) {
                    return classifier;
                }
            }
        }
        return null;
    }

    public String createCompareStringRepresentation() {
        StringBuffer artifactRepresentation = new StringBuffer();
        artifactRepresentation.append(normalize(getId()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getChecksum()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getGroupId()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getArtifactId()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getComponent()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getVersion()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getType()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getClassification()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getLicense()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getLatestVersion()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getComment()));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(get(Attribute.SECURITY_CATEGORY)));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(get(Attribute.SECURITY_RELEVANT)));
        artifactRepresentation.append(DELIMITER_COLON);
        artifactRepresentation.append(normalize(getVulnerability()));
        return artifactRepresentation.toString();
    }

    private String normalize(String s) {
        if (StringUtils.isNotBlank(s))
            return s.trim();
        return "";
    }

    private String normalize(Boolean b) {
        return Boolean.toString(b);
    }

    public String getDerivedLicenseFolder() {
        return LicenseMetaData.deriveLicenseFolderName(getLicense());
    }

    public String getType() {
        return inferTypeFromId();
    }


    public String getClassifier() {
        return inferClassifierFromId();
    }

    public boolean isEnabledForDistribution() {
        String classification = getClassification();
        if (!StringUtils.isEmpty(classification)) {
            if (classification.contains("internal") || classification.contains("banned")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the artifact is internal. Internal artifacts require a license association, but
     * no component folder or license notice. Nevertheless, a component folder and/or license notice
     * can already be provided.
     * <p>
     * The internal flag may be used to mark artifacts that are identified for associated licenses.
     *
     * @return Boolean indicating whether the artifacts is classified as internal.
     */
    public boolean isInternal() {
        String classification = getClassification();
        if (!StringUtils.isEmpty(classification)) {
            if (classification.contains("internal")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the artifact is banned. Banned artifacts are allowed to have incomplete meta data.
     *
     * @return Boolean indicating whether the artifact is banned.
     */
    public boolean isBanned() {
        String classification = getClassification();
        if (!StringUtils.isEmpty(classification)) {
            if (classification.contains("banned")) {
                return true;
            }
        }
        return false;
    }

    public String getChecksum() {
        return get(Attribute.CHECKSUM);
    }

    public void setChecksum(String checksum) {
        set(Attribute.CHECKSUM, checksum);
    }

    public boolean isRelevant() {
        return relevant;
    }

    public void setRelevant(boolean relevant) {
        this.relevant = relevant;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public String getVulnerability() {
        return get(Attribute.VULNERABILITY);
    }

    public void setVulnerability(String vulnerability) {
        set(Attribute.VULNERABILITY, vulnerability);
    }

    /**
     * @return the complete vulnerability string
     * @deprecated use {@link #getVulnerability()} instead
     */
    @Deprecated
    public String getCompleteVulnerability() {
        return get(Attribute.VULNERABILITY);
    }

    /**
     * @param vulnerability a comma-separated vulnerability string
     * @deprecated use {@link #setVulnerability(String)} instead
     */
    @Deprecated
    public void setCompleteVulnerability(String vulnerability) {
        set(Attribute.VULNERABILITY, vulnerability);
    }

    /**
     * Uses the information in the {@link Attribute#TYPE} column to determine whether the artifact is a hardware
     * component via {@link ArtifactType#CATEGORY_HARDWARE}.
     *
     * @return true if the artifact is a hardware component, false otherwise.
     */
    public boolean isHardware() {
        return getArtifactType().map(ArtifactType::isHardware).orElse(false);
    }

    /**
     * Uses the information in the {@link Attribute#TYPE} column to determine whether the artifact is a driver via
     * {@link ArtifactType#DRIVER}.
     *
     * @return true if the artifact is a driver, false otherwise.
     */
    public boolean isDriver() {
        return getArtifactType().map(ArtifactType::isDriver).orElse(false);
    }

    /**
     * Uses the information in the {@link Attribute#TYPE} column to find the artifact type.<br>
     * Currently only supports hardware types.
     *
     * @return the artifact type or empty if no type was found / type is not yet registered.
     */
    public Optional<ArtifactType> getArtifactType() {
        return ArtifactType.findType(get(Attribute.TYPE));
    }

    public boolean isValid() {
        // an artifact requires at least an id or component
        return StringUtils.isNotBlank(getId()) || StringUtils.isNotBlank(getComponent());
    }

    public String get(Attribute attribute, String defaultValue) {
        return get(attribute.getKey(), defaultValue);
    }

    public String get(Attribute attribute) {
        return get(attribute.getKey());
    }

    public void set(Attribute attribute, String value) {
        set(attribute.getKey(), value);
    }

    public void append(Attribute attribute, String value, String delimiter) {
        append(attribute.getKey(), value, delimiter);
    }

    /**
     * Return the tokenized license string.
     *
     * @return List of individual licenses.
     */
    public List<String> getLicenses() {
        if (!StringUtils.isNotBlank(getLicense())) return Collections.EMPTY_LIST;
        return InventoryUtils.tokenizeLicense(getLicense(), true, true);
    }

    public boolean hasClassification(String classification) {
        if (StringUtils.isNotBlank(getClassification())) {
            return getClassification().contains(classification);
        }
        return false;
    }
}
