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
package org.metaeffekt.core.inventory.processor.model;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

/**
 * Class representing an inventory of artifact and license meta data. The implementation
 * offers various methods to analyze, optimize and utilize the meta data.
 *
 * @author Karsten Klein
 */
public class Inventory {

    private static final Logger LOG = LoggerFactory.getLogger(Inventory.class);

    public static final String CLASSIFICATION_CURRENT = "current";

    // Components are structured by context. This is the package context.
    public static final String COMPONENT_CONTEXT_PACKAGE = "package";

    // Components are structured by context. This is the artifact context.
    public static final String COMPONENT_CONTEXT_ARTIFACT = "artifact";

    // Components are structured by context. This is the web module context.
    public static final String COMPONENT_CONTEXT_WEBMODULE = "web-module";

    private List<Artifact> artifacts = new ArrayList<>();

    private List<LicenseMetaData> licenseMetaData = new ArrayList<>();

    private List<ComponentPatternData> componentPatternData = new ArrayList<>();

    private List<LicenseData> licenseData = new ArrayList<>();

    private Map<String, List<VulnerabilityMetaData>> vulnerabilityMetaData = new LinkedHashMap<>(1);

    private List<CertMetaData> certMetaData = new ArrayList<>();

    private List<InventoryInfo> inventoryInfo = new ArrayList<>();

    private List<ReportData> reportData = new ArrayList<>();

    private List<AssetMetaData> assetMetaData = new ArrayList<>();

    private Map<String, String> licenseNameMap = new HashMap<>();

    private Map<String, String> componentNameMap = new HashMap<>();

    /**
     * Enables to store serialization-related data with the inventory.
     */
    private final InventorySerializationContext serializationContext = new InventorySerializationContext();

    public boolean hasInformationOtherThanArtifacts() {
        if (!licenseMetaData.isEmpty()) return true;
        if (!componentPatternData.isEmpty()) return true;
        if (!licenseData.isEmpty()) return true;
        if (!certMetaData.isEmpty()) return true;
        if (!inventoryInfo.isEmpty()) return true;
        if (!reportData.isEmpty()) return true;
        if (!assetMetaData.isEmpty()) return true;
        if (vulnerabilityMetaData.values().stream().anyMatch(l -> !l.isEmpty())) return true;
        return false;
    }

    public static void sortArtifacts(List<Artifact> artifacts) {
        final Comparator<Artifact> comparator = new Comparator<Artifact>() {

            @Override
            public int compare(Artifact o1, Artifact o2) {
                return createRepresentation(o1).compareTo(createRepresentation(o2));
            }

            private String createRepresentation(Artifact o1) {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtils.isNotBlank(o1.getGroupId()) ? o1.getGroupId() : STRING_EMPTY);
                sb.append(DELIMITER_COLON);
                sb.append(StringUtils.isNotBlank(o1.getArtifactId()) ? o1.getArtifactId() : STRING_EMPTY);
                sb.append(DELIMITER_COLON);
                sb.append(StringUtils.isNotBlank(o1.getVersion()) ? o1.getVersion() : STRING_EMPTY);
                return sb.toString();
            }
        };
        artifacts.sort(comparator);
    }

    public void mergeDuplicates() {
        final Map<String, Set<Artifact>> artifactMap = new HashMap<>();

        for (Artifact artifact : artifacts) {
            artifact.deriveArtifactId();

            // in case no id is provided (legacy case) we use the component name as id.
            String key = artifact.getId();
            if (!StringUtils.isNotBlank(key)) {
                key = artifact.getComponent();
            }

            // append checksum (may be null)
            if (StringUtils.isNotBlank(artifact.getChecksum())) {
                key += "^" + artifact.getChecksum();
            }

            // append version and groupid
            if (StringUtils.isNotBlank(key)) {
                key += "^" + artifact.getVersion() + "^" + artifact.getGroupId();
                Set<Artifact> set = artifactMap.get(key);
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(artifact);
                artifactMap.put(key, set);
            }
        }

        for (Set<Artifact> set : artifactMap.values()) {
            // check whether there are multiple
            if (set.size() > 1) {
                final Iterator<Artifact> it = set.iterator();

                // skip first
                final Artifact ref = it.next();

                while (it.hasNext()) {
                    Artifact a = it.next();

                    // merge content before removing
                    ref.merge(a);

                    // remove
                    artifacts.remove(a);
                }
            }
        }
    }

    public Artifact findArtifact(Artifact artifact) {
        return findArtifact(artifact, false);
    }

    public Artifact findArtifact(Artifact artifact, boolean fuzzy) {
        for (Artifact candidate : getArtifacts()) {
            candidate.deriveArtifactId();
            if (matchesOnMavenProperties(artifact, candidate)) {
                if (matchesChecksumOrChecksumsIncomplete(artifact, candidate)) {
                    return candidate;
                }
            }
        }

        // fuzzy check allows wildcards
        if (fuzzy) {

            // the pure match on id is required to support filesystem scans (no maven metadata available)
            for (Artifact candidate : getArtifacts()) {
                if (matchesOnId(artifact.getId(), candidate, false)) {
                    if (matchesChecksumOrChecksumsIncomplete(artifact, candidate)) {
                        return candidate;
                    }
                }
            }

            // pass with wildcards enabled
            return findArtifactMatchingId(artifact.getId());
        }

        return null;
    }

    public InventoryInfo findOrCreateInventoryInfo(String id) {
        final InventoryInfo info = findInventoryInfo(id);
        if (info != null) {
            return info;
        }

        final InventoryInfo inventoryInfo = new InventoryInfo();
        inventoryInfo.set(InventoryInfo.Attribute.ID, id);
        getInventoryInfo().add(inventoryInfo);

        return inventoryInfo;
    }

    public InventoryInfo findInventoryInfo(String id) {
        for (InventoryInfo inventoryInfo : getInventoryInfo()) {
            if (id.equals(inventoryInfo.getId())) {
                return inventoryInfo;
            }
        }

        return null;
    }

    private boolean isVariableVersion(String version) {
        if (isWildcardVersion(version)) return true;
        if (version == null) return true;
        if (StringUtils.isEmpty(version)) return true;
        if (version.startsWith("$")) return true;
        return false;
    }

    private boolean matchesChecksumOrChecksumsIncomplete(Artifact artifact, Artifact candidate) {
        String leftChecksum = artifact.getChecksum();
        String rightChecksum = candidate.getChecksum();

        String leftVersion = artifact.getVersion();
        String rightVersion = candidate.getVersion();

        // check version constellation
        if (!Objects.equals(rightVersion, leftVersion)) {
            // if the versions are not equal they are either variable
            if (!isVariableVersion(leftVersion) && !isVariableVersion(rightVersion)) {
                return false;
            }
            if (isVariableVersion(rightVersion) && !isVariableVersion(leftVersion)) {
                if (!isWildcardVersion(rightVersion)) return false;
            }
        }

        // now compare checksums
        if (!StringUtils.isNotBlank(leftChecksum)) {
            return true;
        }
        if (!StringUtils.isNotBlank(rightChecksum)) {
            return true;
        }
        if (leftChecksum.equals(rightChecksum)) return true;
        return false;
    }

    private boolean isWildcardVersion(String rightVersion) {
        return "*".equals(rightVersion);
    }

    public Artifact findArtifactByIdAndChecksum(String id, String checksum) {
        if (!StringUtils.isNotBlank(id) || !StringUtils.isNotBlank(checksum)) {
            return null;
        }
        String trimmedId = id.trim();
        String trimmedChecksum = checksum.trim();
        for (Artifact candidate : getArtifacts()) {
            if (candidate.getId() == null || candidate.getChecksum() == null) {
                continue;
            }
            if (trimmedId.equalsIgnoreCase(candidate.getId().trim())) {
                if (trimmedChecksum.equalsIgnoreCase(candidate.getChecksum().trim())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public List<Artifact> findAllWithId(String id) {
        List<Artifact> artifacts = new ArrayList<>();
        if (id == null) throw new IllegalStateException("Artifact id must not be null.");
        for (Artifact artifact : getArtifacts()) {
            if (id.equals(artifact.getId())) {
                artifacts.add(artifact);
            }
        }
        return artifacts;
    }

    public Artifact findArtifact(String id) {
        return findArtifact(id, false);
    }

    public Artifact findArtifact(String id, boolean matchWildcards) {
        if (id == null) return null;

        // 1st pass: check for exact match
        for (Artifact candidate : getArtifacts()) {
            if (id.equals(candidate.getId())) {
                return candidate;
            }
        }

        // 2nd pass: allow wildcard matches
        if (matchWildcards) {
            for (Artifact candidate : getArtifacts()) {
                if (matchesOnId(id, candidate, true)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Anticipates only wildcard matches.
     *
     * @param artifactId The artifact id to match.
     * @return The best matched artifact or <code>null</code>.
     */
    public Artifact findArtifactMatchingId(String artifactId) {
        int maximumMatchLength = -1;
        Artifact longestIdMatchCandidate = null;
        for (Artifact candidate : getArtifacts()) {
            if (matchesOnId(artifactId, candidate, true)) {
                int idLength = candidate.getId().length();
                if (candidate.getId().contains("*") && StringUtils.isEmpty(candidate.getChecksum())) {
                    if (idLength > maximumMatchLength) {
                        longestIdMatchCandidate = candidate;
                        maximumMatchLength = idLength;
                    }
                }
            }
        }
        return longestIdMatchCandidate;
    }

    private boolean matchesOnMavenProperties(Artifact artifact, Artifact candidate) {
        if (!matchesOnId(artifact.getId(), candidate, false)) {
            return false;
        }
        if (!matchesOnType(artifact, candidate)) {
            return false;
        }
        if (candidate.getGroupId() == null)
            return false;
        if (candidate.getArtifactId() == null)
            return false;
        if (candidate.getVersion() == null)
            return false;
        if (artifact.getGroupId() == null)
            return false;
        if (artifact.getArtifactId() == null)
            return false;
        if (artifact.getVersion() == null)
            return false;
        if (!candidate.getGroupId().trim().equalsIgnoreCase(artifact.getGroupId().trim()))
            return false;
        if (!candidate.getArtifactId().trim().equalsIgnoreCase(artifact.getArtifactId().trim()))
            return false;
        if (!candidate.getVersion().trim().equalsIgnoreCase(artifact.getVersion().trim()))
            return false;
        return true;
    }

    protected boolean matchesOnId(String id, Artifact candidate, boolean allowWildcards) {
        final String candidateId = candidate.getId();

        if (id == null && candidateId == null) {
            return true;
        }

        if (candidateId == null) {
            return false;
        }

        // check the ids match (exact)
        if (id != null) {
            if (id.equals(candidateId)) {
                return true;
            }
        }

        // check the ids match (allow wildcard)
        if (allowWildcards && ASTERISK.equals(candidate.getVersion())) {
            // check the wildcard is really used in the candidate id
            final int index = candidateId.indexOf(ASTERISK);
            if (index != -1) {
                String prefix = candidateId.substring(0, index);
                String suffix = candidateId.substring(index + 1);
                if (id.startsWith(prefix) && id.endsWith(suffix)) {
                    return true;
                }
            }
        }

        // NOTE: in this case, no VERSION_PLACEHOLDER agnostic match is appropriate

        return false;
    }

    protected boolean matchesOnType(Artifact artifact, Artifact candidate) {
        final String artifactType = artifact.getType();
        final String candidateType = candidate.getType();
        if (artifactType == null && candidateType == null) {
            return true;
        }
        if (artifactType != null) {
            return artifactType.equals(candidateType);
        }
        return false;
    }

    public Artifact findArtifact(String groupId, String artifactId) {
        for (Artifact candidate : getArtifacts()) {
            candidate.deriveArtifactId();
            if (candidate.getArtifactId() == null)
                continue;
            if (candidate.getGroupId() == null)
                continue;
            if (!candidate.getArtifactId().trim().equals(artifactId.trim()))
                continue;
            if (!candidate.getGroupId().trim().equals(groupId.trim()))
                continue;
            return candidate;
        }
        return null;
    }

    public Set<Artifact> findArtifacts(String groupId, String artifactId) {
        Set<Artifact> matchingArtifacts = new HashSet<>();
        for (Artifact candidate : getArtifacts()) {
            candidate.deriveArtifactId();
            if (candidate.getArtifactId() == null)
                continue;
            if (candidate.getGroupId() == null)
                continue;
            if (!candidate.getArtifactId().trim().equals(artifactId.trim()))
                continue;
            if (!candidate.getGroupId().trim().equals(groupId.trim()))
                continue;

            matchingArtifacts.add(candidate);
        }
        return matchingArtifacts;
    }

    public void sortArtifacts() {
        sortArtifacts(artifacts);
    }

    public List<String> evaluateLicenses(boolean includeLicensesWithArtifactsOnly) {
        return evaluateLicenses(includeLicensesWithArtifactsOnly, false);
    }

    /**
     * Returns a sorted list of licenses that is covered by this inventory. Please note that this
     * method only produces the license names, which are assumed to be non-redundant and unique.
     *
     * @param includeLicensesWithArtifactsOnly Result will cover licenses of artifacts without artifactId when true.
     * @param includeManagedArtifactsOnly      Results will only cover license of managed artifacts when true.
     * @return List of license names covered by this inventory.
     */
    public List<String> evaluateLicenses(boolean includeLicensesWithArtifactsOnly, boolean includeManagedArtifactsOnly) {
        final Set<String> licenses = new HashSet<>();

        for (Artifact artifact : getArtifacts()) {
            // not relevant artifact licenses must not be included
            if (!artifact.isRelevant()) {
                continue;
            }

            // skip license in case only managed artifacts are to be included
            if (includeManagedArtifactsOnly && !artifact.isManaged()) {
                continue;
            }

            String artifactLicense = artifact.getLicense();

            if (!StringUtils.isNotBlank(artifact.getArtifactId())) {
                if (includeLicensesWithArtifactsOnly) {
                    continue;
                }
            }

            // check whether there is an effective license (set of licenses)

            final LicenseMetaData matchingLicenseMetaData = findMatchingLicenseMetaData(artifact);
            if (matchingLicenseMetaData != null) {
                artifactLicense = matchingLicenseMetaData.deriveLicenseInEffect();
            }

            if (artifactLicense != null) {
                String[] splitLicense = artifactLicense.split("\\|");
                for (int i = 0; i < splitLicense.length; i++) {
                    if (StringUtils.isNotBlank(splitLicense[i])) {
                        licenses.add(splitLicense[i].trim());
                    }
                }
            }
        }

        List<String> sortedByLicense = new ArrayList<>(licenses);
        Collections.sort(sortedByLicense);
        return sortedByLicense;
    }

    // FIXME: remove
    public List<String> evaluateAssetAssociatedLicenses() {
        final Set<String> licenses = new HashSet();

        for (AssetMetaData assetMetaData : getAssetMetaData()) {

            final String assetId = assetMetaData.get(AssetMetaData.Attribute.ASSET_ID);

            for (Artifact artifact : getArtifacts()) {

                // skip all artifacts that do not belong to an asset
                if (!StringUtils.isNotBlank(artifact.get(assetId))) {
                    continue;
                }

                // not relevant artifact licenses must not be included
                if (!artifact.isRelevant()) {
                    continue;
                }

                List<String> artifactLicense = artifact.getLicenses();

                // check whether there is an effective license (set of licenses)
                licenses.addAll(artifactLicense);
            }
        }

        List<String> sortedByLicense = new ArrayList<>(licenses);
        Collections.sort(sortedByLicense);
        return sortedByLicense;
    }


    /**
     * Returns all relevant notices for a given effective license.
     *
     * @param effectiveLicense The effective license.
     * @return List of {@link ArtifactLicenseData} instances.
     */
    public List<ArtifactLicenseData> evaluateNotices(String effectiveLicense) {
        final Map<String, ArtifactLicenseData> map = new LinkedHashMap<>();
        for (final Artifact artifact : artifacts) {
            String artifactLicense = artifact.getLicense();
            if (artifactLicense != null) {
                artifactLicense = artifactLicense.trim();
                // find a matching LMD instance
                LicenseMetaData match = findMatchingLicenseMetaData(
                        artifact.getComponent(), artifactLicense, artifact.getVersion());
                if (match != null && matches(effectiveLicense, match)) {
                    String qualifier = new StringBuilder(artifactLicense).append("-").
                            append(artifact.getVersion()).append("-").append(artifact.getComponent()).toString();
                    ArtifactLicenseData artifactLicenseData = map.get(qualifier);
                    if (artifactLicenseData == null) {
                        artifactLicenseData = new ArtifactLicenseData(artifact.getComponent(), artifact.getVersion(), match);
                        map.put(qualifier, artifactLicenseData);
                    }
                    artifactLicenseData.add(artifact);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Used by tpc_inventory-notices.dita.vt.
     *
     * @return List of component notices for this inventory.
     */
    public List<ComponentNotice> evaluateComponentNotices() {
        List<ComponentNotice> componentNotices = new ArrayList<>();

        Map<String, ComponentNotice> componentNameComponentNoticeMap = new HashMap<>();

        // evaluate all artifacts
        for (Artifact artifact : getArtifacts()) {
            final LicenseMetaData matchingLicenseMetaData = findMatchingLicenseMetaData(artifact);
            if (matchingLicenseMetaData != null) {
                final String componentName = matchingLicenseMetaData.getComponent();
                if (componentName == null) continue;
                ComponentNotice componentNotice = componentNameComponentNoticeMap.get(componentName);
                if (componentNotice == null) {
                    componentNotice = new ComponentNotice(componentName, artifact.getLicense());
                    componentNameComponentNoticeMap.put(componentName, componentNotice);
                    componentNotices.add(componentNotice);
                }
                componentNotice.add(artifact, matchingLicenseMetaData);
            } else {
                // with this branch we generate default notices to cover all components
                final String componentName = artifact.getComponent();
                if (componentName == null) continue;
                ComponentNotice componentNotice = componentNameComponentNoticeMap.get(componentName);
                if (componentNotice == null) {
                    componentNotice = new ComponentNotice(componentName, artifact.getLicense());
                    componentNameComponentNoticeMap.put(componentName, componentNotice);
                    componentNotices.add(componentNotice);
                }
                componentNotice.add(artifact);

            }
        }

        componentNotices.sort(((cn1, cn2) -> cn1.getComponentName().compareToIgnoreCase(cn2.getComponentName())));

        return componentNotices;
    }

    public List<ArtifactLicenseData> evaluateComponents(String effectiveLicense) {
        final Map<String, ArtifactLicenseData> map = new LinkedHashMap<>();
        for (final Artifact artifact : artifacts) {
            String artifactLicense = artifact.getLicense();
            if (StringUtils.isNotBlank(artifact.getLicense()) &&
                    StringUtils.isNotBlank(artifact.getVersion()) &&
                    StringUtils.isNotBlank(artifact.getComponent())) {
                artifactLicense = artifactLicense.trim();
                // find a matching LMD instance
                LicenseMetaData match = findMatchingLicenseMetaData(
                        artifact.getComponent(), artifactLicense, artifact.getVersion());

                if (match == null) {
                    match = new LicenseMetaData();
                    match.setLicense(artifactLicense);
                }

                if (matches(effectiveLicense, match)) {
                    // only version and name must be used here
                    // there may be multiple entries (if validation for the component is disabled), but that
                    // is not of interest here (we need just representatives for documentation).
                    String qualifier = new StringBuilder(artifact.getVersion()).append("-").append(artifact.getComponent()).toString();
                    ArtifactLicenseData artifactLicenseData = map.get(qualifier);
                    if (artifactLicenseData == null) {
                        artifactLicenseData = new ArtifactLicenseData(artifact.getComponent(), artifact.getVersion(), match);
                        map.put(qualifier, artifactLicenseData);
                    }
                    artifactLicenseData.add(artifact);
                }
            }
        }
        final ArrayList<ArtifactLicenseData> artifactLicenseData = new ArrayList<>(map.values());
        Collections.sort(artifactLicenseData, (o1, o2) ->
                Objects.compare(artifactSortString(o1), artifactSortString(o2), String::compareToIgnoreCase));
        return artifactLicenseData;
    }


    private String artifactSortString(ArtifactLicenseData o1) {
        return o1.getComponentName() + "-" + o1.getComponentVersion();
    }

    private boolean matches(String effectiveLicense, LicenseMetaData match) {
        List<String> licenses = Arrays.asList(match.deriveLicenseInEffect().split("\\|"));
        return licenses.contains(effectiveLicense);
    }

    /**
     * Iterates through the license metadata to find a match for the given component and license parameters.
     *
     * @param component The component.
     * @param license   The license name.
     * @param version   The version.
     * @return A matching {@link LicenseMetaData} instance if available. In case multiple
     * can be matched an {@link IllegalStateException} is thrown.
     */
    public LicenseMetaData findMatchingLicenseMetaData(String component, String license, String version) {
        LicenseMetaData match = null;
        for (LicenseMetaData lmd : licenseMetaData) {

            // NOTE: for artifacts with VERSION_PLACEHOLDER we expect that the license metadata is provided with ASTERISK.

            if (lmd.getLicense().equals(license) &&
                    (lmd.getVersion().equals(version) || lmd.getVersion().equals(ASTERISK)) &&
                    (lmd.getComponent().equals(component) || lmd.getComponent().equals(ASTERISK))) {
                if (match != null) {
                    // in case match is not null we have found two matching license meta data elements
                    // this means that the license data is inconsistent and has overlaps. This must
                    // be resolved in the underlying meta data.
                    throw new IllegalStateException(String.format("Multiple matches for component:version:license: %s|%s|%s." +
                            " Meta data inconsistent. Please correct license meta data to resolve inconsistencies.", component, version, license));
                }
                match = lmd;
            }
        }
        return match;
    }

    /**
     * Tries to find the matching {@link LicenseMetaData} details for the specified artifact.
     *
     * @param artifact The artifact to look for {@link LicenseMetaData}.
     * @return The found {@link LicenseMetaData} or <code>null</code> when no matching {@link LicenseMetaData} could be
     * found.
     */
    public LicenseMetaData findMatchingLicenseMetaData(Artifact artifact) {
        return findMatchingLicenseMetaData(artifact.getComponent(), artifact.getLicense(), artifact.getVersion());
    }

    /**
     * Tries to find the {@link LicenseData} with the given canonicalName.
     *
     * @param canonicalName The canonical name.
     * @return The found {@link LicenseData} instance or <code>null</code> in case no matching {@link LicenseData}
     * instance was identified.
     */
    public LicenseData findMatchingLicenseData(String canonicalName) {
        if (StringUtils.isEmpty(canonicalName)) return null;

        for (LicenseData ld : licenseData) {
            if (canonicalName.trim().equals(ld.get(LicenseData.Attribute.CANONICAL_NAME))) {
                return ld;
            }
        }
        return null;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public List<Artifact> getArtifacts(String context) {
        if (COMPONENT_CONTEXT_PACKAGE.equalsIgnoreCase(context)) {
            return getArtifacts().stream().filter(a -> isPackageType(a))
                    .collect(Collectors.toList());
        }
        if (COMPONENT_CONTEXT_ARTIFACT.equalsIgnoreCase(context)) {
            return getArtifacts().stream().filter(
                            a -> isArtifactType(a))
                    .collect(Collectors.toList());
        }
        if (COMPONENT_CONTEXT_WEBMODULE.equalsIgnoreCase(context)) {
            return getArtifacts().stream().filter(
                            a -> isWebModuleType(a))
                    .collect(Collectors.toList());
        }
        throw new IllegalStateException("Artifact context '" + context + "' not supported.");
    }

    private boolean isPackageType(Artifact a) {
        String type = a.get(KEY_TYPE);
        return ARTIFACT_TYPE_PACKAGE.equalsIgnoreCase(type);
    }

    private boolean isArtifactType(Artifact a) {
        String type = a.get(KEY_TYPE);
        if (StringUtils.isEmpty(type)) return true;
        if (ARTIFACT_TYPE_PACKAGE.equalsIgnoreCase(type)) return false;
        if (ARTIFACT_TYPE_NODEJS_MODULE.equalsIgnoreCase(type)) return false;
        return true;
    }

    private boolean isWebModuleType(Artifact a) {
        String type = a.get(KEY_TYPE);
        if (StringUtils.isEmpty(type)) return false;
        if (ARTIFACT_TYPE_NODEJS_MODULE.equalsIgnoreCase(type)) return true;
        return false;
    }

    public List<LicenseMetaData> getLicenseMetaData() {
        return licenseMetaData;
    }

    public void setLicenseMetaData(List<LicenseMetaData> licenses) {
        this.licenseMetaData = licenses;
    }

    public String getLicenseFolder(String license) {
        if (StringUtils.isNotBlank(license)) {
            return LicenseMetaData.deriveLicenseFolderName(license.trim());
        }
        return null;
    }

    public void dumpAsFile(File file) {
        file.delete();
        List<String> strings = new ArrayList<String>();
        try {
            for (Artifact artifact : artifacts) {
                artifact.deriveArtifactId();
                strings.add(artifact.createCompareStringRepresentation());
            }
            Collections.sort(strings);
            FileUtils.writeLines(file, strings, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String deriveLicenseId(String license) {
        if (license == null)
            return null;
        return removeSpecialCharacters(license);
    }

    public String deriveLicenseId(LicenseMetaData licenseMetaData) {
        if (licenseMetaData == null)
            return null;
        return removeSpecialCharacters(licenseMetaData.getComponent() + "-" + licenseMetaData.getVersion());
    }


    private String removeSpecialCharacters(String license) {
        String licenseId = LicenseMetaData.deriveLicenseFolderName(license);
        licenseId = licenseId.replace("(", "_");
        licenseId = licenseId.replace(")", "_");
        licenseId = licenseId.replace(" ", "-");
        licenseId = licenseId.replace("\"", "");
        licenseId = licenseId.replace("'", "");
        int length = -1;
        while (length != licenseId.length()) {
            length = licenseId.length();
            licenseId = licenseId.replace("__", "_");
            licenseId = licenseId.replace("--", "-");
        }
        return licenseId.toLowerCase();
    }

    public Artifact findMatchingId(Artifact artifact) {
        final String id = normalize(artifact.getId());
        // find current only works for artifacts, which have a proper artifact id
        if (id.isEmpty()) {
            return null;
        }

        for (Artifact candidate : artifacts) {
            if (candidate != artifact) {
                final String candidateId = normalize(candidate.getId());
                if (id.equals(candidateId)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public Artifact findCurrent(Artifact artifact) {
        final String id = normalize(artifact.getId());
        final String artifactId = normalize(artifact.getArtifactId());
        final String groupId = normalize(artifact.getGroupId());
        final String classifier = normalize(artifact.getClassifier());
        final String type = normalize(artifact.getType());

        // find current only works for artifacts, which have a proper artifact id
        if (id.isEmpty()) {
            return null;
        }

        for (Artifact candidate : artifacts) {
            if (candidate != artifact) {
                final String candidateClassification = normalize(candidate.getClassification());
                if (candidateClassification.contains(CLASSIFICATION_CURRENT)) {
                    final String candidateGroupId = normalize(candidate.getGroupId());
                    final String candidateArtifactId = normalize(candidate.getArtifactId());
                    final String candidateClassifier = normalize(candidate.getClassifier());
                    final String candidateType = normalize(candidate.getType());
                    if (groupId.equals(candidateGroupId)) {
                        if (artifactId.equals(candidateArtifactId)) {
                            if (classifier.equals(candidateClassifier)) {
                                if (type.equals(candidateType)) {
                                    return candidate;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public Artifact findArtifactClassificationAgnostic(Artifact artifact) {
        final String id = normalize(artifact.getId());
        final String artifactId = normalize(artifact.getArtifactId());
        final String groupId = normalize(artifact.getGroupId());
        final String classifier = normalize(artifact.getClassifier());
        final String type = normalize(artifact.getType());

        // find current only works for artifacts, which have a proper artifact id
        if (id.isEmpty()) {
            return null;
        }

        for (Artifact candidate : artifacts) {
            if (candidate != artifact) {
                final String candidateGroupId = normalize(candidate.getGroupId());
                final String candidateArtifactId = normalize(candidate.getArtifactId());
                final String candidateClassifier = normalize(candidate.getClassifier());
                final String candidateType = normalize(candidate.getType());
                if (groupId.equals(candidateGroupId)) {
                    if (artifactId.equals(candidateArtifactId)) {
                        if (classifier.equals(candidateClassifier)) {
                            if (type.equals(candidateType)) {
                                return candidate;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public String normalize(String s) {
        if (StringUtils.isNotBlank(s))
            return s.trim();
        return STRING_EMPTY;
    }

    public List<Component> evaluateComponentsInContext(String context) {
        Map<String, Component> nameComponentMap = new HashMap<>();
        // only evaluate artifacts for the given context
        for (Artifact artifact : getArtifacts(context)) {
            final Component component = createComponent(artifact);
            Component alreadyExistingComponent = nameComponentMap.get(component.getQualifier());
            if (alreadyExistingComponent != null) {
                alreadyExistingComponent.add(artifact);
            } else {
                nameComponentMap.put(component.getQualifier(), component);
                component.add(artifact);
            }
        }

        List<Component> sortedByComponent = new ArrayList<>(nameComponentMap.values());
        Collections.sort(sortedByComponent, new Comparator<Component>() {
            @Override
            public int compare(Component o1, Component o2) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        });
        return sortedByComponent;
    }

    public List<List<Artifact>> evaluateComponent(Component component) {
        List<Artifact> artifactsForComponent = new ArrayList<>(component.getArtifacts());
        sortArtifacts(artifactsForComponent);

        // rearrange components into groups of max 60
        List<List<Artifact>> groups = new ArrayList<>();
        List<Artifact> currentGroup = new ArrayList<>();
        groups.add(currentGroup);
        for (Artifact artifact : artifactsForComponent) {
            currentGroup.add(artifact);
            if (currentGroup.size() >= 60) {
                currentGroup = new ArrayList<>();
                groups.add(currentGroup);
            }
        }

        // if the last group did not receive any artifacts we remove it
        if (currentGroup.isEmpty()) {
            groups.remove(currentGroup);
        }

        return groups;
    }

    /**
     * Used by templates to check whether a notice for the given component is available.
     *
     * @param component The component to check.
     * @return Indicated whether a notice is available or not.
     */
    public boolean hasNotice(Component component) {
        // FIXME: remove aspect from templates and remove method; currently all components have a notice.
        return true;
    }

    /**
     * Used by templates to check whether a notice for the given component is available.
     *
     * @param ald The {@link ArtifactLicenseData} instance to check.
     * @return Indicated whether a notice is available or not.
     */
    public boolean hasNotice(ArtifactLicenseData ald) {
        // FIXME: remove aspect from templates and remove method; currently all components have a notice.
        return true;
    }

    public List<Artifact> evaluateLicense(String licenseName, boolean includeLicensesWithArtifactsOnly) {
        List<Artifact> artifactsForComponent = new ArrayList<>();
        for (Artifact artifact : getArtifacts()) {
            if (licenseName.equals(artifact.getLicense())) {
                if (!StringUtils.isNotBlank(artifact.getArtifactId())) {
                    if (includeLicensesWithArtifactsOnly) {
                        continue;
                    }
                }
                artifactsForComponent.add(artifact);
            }
        }
        sortArtifacts(artifactsForComponent);
        return artifactsForComponent;
    }

    public boolean removeInconsistencies() {
        Set<String> uniqueSet = new HashSet<String>();
        Set<String> qualifiedSet = new HashSet<String>();
        Map<String, Artifact> map = new HashMap<String, Artifact>();
        sortArtifacts();
        boolean b = true;
        int index = 1;

        List<Artifact> removeList = new ArrayList<Artifact>();

        for (Artifact a : getArtifacts()) {
            if (StringUtils.isNotBlank(a.getComponent())) {
                String key = a.getComponent() + "/" + a.getVersion();
                String qualifier = key + "/" + a.getLicense() + "/" + a.getVersion();
                if (uniqueSet.contains(key) && !qualifiedSet.contains(qualifier)) {
                    Artifact duplicate = map.get(key);
                    LOG.warn("Detected inconsistency #{}: {} {}",
                            index++, a.createCompareStringRepresentation(),
                            duplicate.createCompareStringRepresentation());
                    b = false;

                    removeList.add(a);
                    removeList.add(duplicate);
                }
                uniqueSet.add(key);
                qualifiedSet.add(qualifier);
                map.put(key, a);
            }
        }

        getArtifacts().removeAll(removeList);
        return b;
    }

    public void mapComponentNames() {
        if (getArtifacts() != null) {
            for (Artifact a : getArtifacts()) {
                String mappedName = mapComponentName(a.getComponent());
                if (mappedName != null) {
                    a.setComponent(mappedName);
                }
            }
        }
        if (getLicenseMetaData() != null) {
            for (LicenseMetaData licenseMetaData : getLicenseMetaData()) {
                String mappedName = mapComponentName(licenseMetaData.getComponent());
                if (mappedName != null) {
                    licenseMetaData.setComponent(mappedName);
                }
            }
        }
    }

    private String mapComponentName(String componentName) {
        String mappedName = null;
        if (componentName != null) {
            componentName = componentName.trim();
            mappedName = componentNameMap.get(componentName);
        }
        return mappedName;
    }

    public void mapLicenseNames() {
        if (getArtifacts() != null) {
            for (Artifact a : getArtifacts()) {
                String mappedName = mapLicenseName(a.getLicense());
                if (mappedName != null) {
                    a.setLicense(mappedName);
                }
            }
        }
        if (getLicenseMetaData() != null) {
            for (LicenseMetaData licenseMetaData : getLicenseMetaData()) {
                String mappedName = mapLicenseName(licenseMetaData.getName());
                if (mappedName != null) {
                    licenseMetaData.setName(mappedName);
                }
            }
        }
    }

    private String mapLicenseName(String licenseName) {
        if (licenseName != null) {
            licenseName = licenseName.trim();
            return licenseNameMap.get(licenseName);
        }
        return null;
    }

    /**
     * Remove all artifacts that do not contain or valid data
     */
    public void cleanup() {
        Iterator<Artifact> iterator = getArtifacts().iterator();
        while (iterator.hasNext()) {
            final Artifact artifact = iterator.next();

            // an artifact requires at least an id or a component (name)
            if (!artifact.isValid()) {
                iterator.remove();
            }
        }
    }

    @Deprecated
    public Map<String, Object> getContextMap() {
        return serializationContext.getContextMap();
    }

    @Deprecated
    public void setContextMap(Map<String, Object> contextMap) {
        this.serializationContext.setContextMap(contextMap);
    }

    public InventorySerializationContext getSerializationContext() {
        return this.serializationContext;
    }

    public Map<String, String> getLicenseNameMap() {
        return licenseNameMap;
    }

    public void setLicenseNameMap(Map<String, String> licenseNameMap) {
        this.licenseNameMap = licenseNameMap;
    }

    public Map<String, String> getComponentNameMap() {
        return componentNameMap;
    }

    public void setComponentNameMap(Map<String, String> componentNameMap) {
        this.componentNameMap = componentNameMap;
    }

    public boolean hasConcreteArtifacts(Artifact artifact) {
        if (artifact != null) {
            String id = artifact.getId();
            return StringUtils.isNotBlank(id);
        }
        return false;
    }

    public boolean hasConcreteArtifacts(Collection<Artifact> artifacts) {
        if (artifacts != null && !artifacts.isEmpty()) {
            for (Artifact artifact : artifacts) {
                if (hasConcreteArtifacts(artifact)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a shallow copy of the inventory. The artifact list is filtered such that only
     * artifacts evaluating isRelevant() to <code>true</code> are taken over into the new inventory.
     *
     * @return A shallow copy of the inventory containing only report-relevant artifacts.
     */
    public Inventory getFilteredInventory() {
        Inventory filteredInventory = new Inventory();
        for (Artifact artifact : getArtifacts()) {
            if (artifact.isRelevant()) {
                filteredInventory.getArtifacts().add(artifact);
            }
        }
        filteredInventory.setComponentNameMap(getComponentNameMap());
        filteredInventory.setContextMap(getContextMap());
        filteredInventory.setLicenseMetaData(getLicenseMetaData());
        filteredInventory.setLicenseData(getLicenseData());
        filteredInventory.setLicenseNameMap(getLicenseNameMap());

        if (getVulnerabilityMetaDataContexts().size() > 1) {
            getVulnerabilityMetaDataContexts().forEach(
                    context -> filteredInventory.setVulnerabilityMetaData(getVulnerabilityMetaData(context), context)
            );
        } else {
            filteredInventory.setVulnerabilityMetaData(getVulnerabilityMetaData());
        }

        filteredInventory.setCertMetaData(getCertMetaData());
        filteredInventory.setAssetMetaData(getAssetMetaData());
        filteredInventory.setInventoryInfo(getInventoryInfo());
        filteredInventory.setReportData(getReportData());
        return filteredInventory;
    }

    public Component createComponent(Artifact artifact) {
        String componentName = artifact.getComponent();
        if (StringUtils.isEmpty(componentName)) {
            componentName = artifact.getId();
            String version = artifact.getVersion();
            if (!StringUtils.isEmpty(version)) {
                if (componentName.contains("-" + version)) {
                    componentName = componentName.substring(0, componentName.indexOf("-" + version));
                }
            }
        }
        return new Component(componentName, artifact.getComponent(), artifact.getLicense());
    }

    /**
     * Takes over missing license metadata from the provided inputInventory. If the local inventory already has matching
     * license metadata the local data has priority.
     *
     * @param inputInventory  The input inventory. From this inventory license metadata will be taken over.
     * @param infoOnOverwrite Methods provides information on overwrites when true.
     */
    public void inheritLicenseMetaData(Inventory inputInventory, boolean infoOnOverwrite) {
        // Iterate through all license meta data. Generate qualifier based on component name, version and license.
        // Test whether the qualifier is present in current. If yes skip; otherwise add.
        final Map<String, LicenseMetaData> currentLicenseMetaDataMap = new HashMap<>();
        for (LicenseMetaData licenseMetaData : getLicenseMetaData()) {
            final String qualifier = licenseMetaData.deriveQualifier();
            currentLicenseMetaDataMap.put(qualifier, licenseMetaData);
        }

        for (LicenseMetaData licenseMetaData : inputInventory.getLicenseMetaData()) {
            final String qualifier = licenseMetaData.deriveQualifier();
            if (currentLicenseMetaDataMap.containsKey(qualifier)) {
                // overwrite; the current inventory contains the artifact.
                if (infoOnOverwrite) {
                    LicenseMetaData currentLicenseMetaData = currentLicenseMetaDataMap.get(qualifier);
                    if (currentLicenseMetaData.createCompareStringRepresentation().equals(
                            licenseMetaData.createCompareStringRepresentation())) {
                        LOG.info("License meta data {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info("License meta data {} overwritten.", qualifier);
                    }
                }
            } else {
                // add the license meta data
                getLicenseMetaData().add(licenseMetaData);
            }
        }
    }

    public void filterLicenseMetaData() {
        Set<LicenseMetaData> filteredSet = new HashSet<>();
        for (Artifact artifact : getArtifacts()) {
            LicenseMetaData lmd = findMatchingLicenseMetaData(artifact);
            if (lmd != null) {
                filteredSet.add(lmd);
            }
        }
        getLicenseMetaData().retainAll(filteredSet);
    }

    /**
     * Inherits the artifacts from the specified inputInventory. Local artifacts with the same qualifier have
     * priority.
     *
     * @param inputInventory  Input inventory with artifact information.
     * @param infoOnOverwrite Logs information on overwrites when active.
     */
    public void inheritArtifacts(Inventory inputInventory, boolean infoOnOverwrite) {
        // Iterate through all artifacts in the input repository. If the artifact is present in the current repository
        // then skip (but log some information); otherwise add the artifact to current.
        final Map<String, Artifact> currentArtifactMap = new HashMap<>();
        for (Artifact artifact : getArtifacts()) {
            artifact.deriveArtifactId();
            String artifactQualifier = artifact.deriveQualifier();
            currentArtifactMap.put(artifactQualifier, artifact);
        }
        for (Artifact artifact : inputInventory.getArtifacts()) {
            artifact.deriveArtifactId();
            String qualifier = artifact.deriveQualifier();
            if (currentArtifactMap.containsKey(qualifier)) {
                // overwrite; the current inventory contains the artifact.
                if (infoOnOverwrite) {
                    Artifact currentArtifact = currentArtifactMap.get(qualifier);
                    if (artifact.createCompareStringRepresentation().equals(
                            currentArtifact.createCompareStringRepresentation())) {
                        LOG.info("Artifact {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Artifact %s overwritten. %n  %s%n  %s", qualifier,
                                artifact.createCompareStringRepresentation(),
                                currentArtifact.createCompareStringRepresentation()));
                    }
                }
            } else {
                // add the artifact
                getArtifacts().add(artifact);
            }
        }
    }

    public void inheritComponentPatterns(Inventory inputInventory, boolean infoOnOverwrite) {
        final Map<String, ComponentPatternData> localCpds = new HashMap<>();
        for (ComponentPatternData cpd : getComponentPatternData()) {
            String artifactQualifier = cpd.deriveQualifier();
            localCpds.put(artifactQualifier, cpd);
        }
        for (ComponentPatternData cpd : inputInventory.getComponentPatternData()) {
            String qualifier = cpd.deriveQualifier();
            if (localCpds.containsKey(qualifier)) {
                // overwrite; the localCpds inventory contains the artifact.
                if (infoOnOverwrite) {
                    ComponentPatternData localCpd = localCpds.get(qualifier);
                    if (cpd.createCompareStringRepresentation().equals(
                            localCpd.createCompareStringRepresentation())) {
                        LOG.info("Component pattern {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Component pattern %s overwritten. %n  %s%n  %s", qualifier,
                                cpd.createCompareStringRepresentation(),
                                localCpd.createCompareStringRepresentation()));
                    }
                }
            } else {
                // add the artifact
                getComponentPatternData().add(cpd);
            }
        }
    }

    public void inheritLicenseData(Inventory inputInventory, boolean infoOnOverwrite) {
        final Map<String, LicenseData> localLds = new HashMap<>();
        for (LicenseData ld : getLicenseData()) {
            String artifactQualifier = ld.deriveQualifier();
            localLds.put(artifactQualifier, ld);
        }
        for (LicenseData ld : inputInventory.getLicenseData()) {
            String qualifier = ld.deriveQualifier();
            if (localLds.containsKey(qualifier)) {
                // overwrite; the localLds inventory contains the artifact.
                if (infoOnOverwrite) {
                    LicenseData localLd = localLds.get(qualifier);
                    if (ld.createCompareStringRepresentation().equals(
                            localLd.createCompareStringRepresentation())) {
                        LOG.info("License data {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("License data %s overwritten. %n  %s%n  %s", qualifier,
                                ld.createCompareStringRepresentation(),
                                localLd.createCompareStringRepresentation()));
                    }
                }
            } else {
                // add the artifact
                getLicenseData().add(ld);
            }
        }
    }

    public void inheritVulnerabilityMetaData(Inventory inputInventory, boolean infoOnOverwrite) {
        for (String context : inputInventory.getVulnerabilityMetaDataContexts()) {
            final Map<String, VulnerabilityMetaData> localVmds = new HashMap<>();

            for (VulnerabilityMetaData vmd : getVulnerabilityMetaData(context)) {
                final String artifactQualifier = vmd.deriveQualifier();
                localVmds.put(artifactQualifier, vmd);
            }

            for (VulnerabilityMetaData vmd : inputInventory.getVulnerabilityMetaData(context)) {
                final String qualifier = vmd.deriveQualifier();

                if (localVmds.containsKey(qualifier)) {
                    // overwrite; the localVmds inventory contains the artifact.
                    if (infoOnOverwrite) {
                        VulnerabilityMetaData localVmd = localVmds.get(qualifier);
                        if (vmd.createCompareStringRepresentation().equals(
                                localVmd.createCompareStringRepresentation())) {
                            LOG.info("Vulnerability metadata {} overwritten. Relevant content nevertheless matches. " +
                                    "Consider removing the overwrite.", qualifier);
                        } else {
                            LOG.info(String.format("Vulnerability metadata %s overwritten. %n  %s%n  %s", qualifier,
                                    vmd.createCompareStringRepresentation(),
                                    localVmd.createCompareStringRepresentation()));
                        }
                    }

                } else {
                    // add the artifact
                    getVulnerabilityMetaData(context).add(vmd);
                }
            }
        }
    }

    public void inheritCertMetaData(Inventory inputInventory, boolean infoOnOverwrite) {
        final Map<String, CertMetaData> localCerts = new HashMap<>();
        for (CertMetaData cert : getCertMetaData()) {
            String artifactQualifier = cert.deriveQualifier();
            localCerts.put(artifactQualifier, cert);
        }
        for (CertMetaData cert : inputInventory.getCertMetaData()) {
            String qualifier = cert.deriveQualifier();
            if (localCerts.containsKey(qualifier)) {
                // overwrite; the localCerts inventory contains the artifact.
                if (infoOnOverwrite) {
                    CertMetaData localCert = localCerts.get(qualifier);
                    if (cert.createCompareStringRepresentation().equals(
                            localCert.createCompareStringRepresentation())) {
                        LOG.info("Cert metadata {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Cert metadata %s overwritten. %n  %s%n  %s", qualifier,
                                cert.createCompareStringRepresentation(),
                                localCert.createCompareStringRepresentation()));
                    }
                }
            } else {
                // add the cert
                getCertMetaData().add(cert);
            }
        }
    }

    public void inheritAssetMetaData(Inventory inputInventory, boolean infoOnOverwrite) {
        final Map<String, AssetMetaData> localAssets = new HashMap<>();
        for (AssetMetaData assetMetaData : getAssetMetaData()) {
            localAssets.put(assetMetaData.deriveQualifier(), assetMetaData);
        }
        for (AssetMetaData assetMetaData : inputInventory.getAssetMetaData()) {
            final String qualifier = assetMetaData.deriveQualifier();
            if (localAssets.containsKey(qualifier)) {
                // overwrite; the localCerts inventory contains the artifact.
                if (infoOnOverwrite) {
                    AssetMetaData localAssetMetadata = localAssets.get(qualifier);
                    if (assetMetaData.createCompareStringRepresentation().equals(
                            localAssetMetadata.createCompareStringRepresentation())) {
                        LOG.info("Asset metadata {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Asset metadata %s overwritten. %n  %s%n  %s", qualifier,
                                assetMetaData.createCompareStringRepresentation(),
                                localAssetMetadata.createCompareStringRepresentation()));
                    }
                }
            } else {
                getAssetMetaData().add(assetMetaData);
            }
        }
    }

    public void inheritInventoryInfo(Inventory inputInventory, boolean infoOnOverwrite) {
        final Map<String, InventoryInfo> localInfo = new HashMap<>();
        for (InventoryInfo inventoryInfo : getInventoryInfo()) {
            localInfo.put(inventoryInfo.deriveQualifier(), inventoryInfo);
        }
        for (InventoryInfo inventoryInfo : inputInventory.getInventoryInfo()) {
            final String qualifier = inventoryInfo.deriveQualifier();
            if (localInfo.containsKey(qualifier)) {
                // overwrite; the localCerts inventory contains the artifact.
                if (infoOnOverwrite) {
                    InventoryInfo localInventoryInfo = localInfo.get(qualifier);
                    if (inventoryInfo.createCompareStringRepresentation().equals(
                            localInventoryInfo.createCompareStringRepresentation())) {
                        LOG.info("Inventory info {} overwritten. Relevant content nevertheless matches. " +
                                "Consider removing the overwrite.", qualifier);
                    } else {
                        LOG.info(String.format("Inventory info %s overwritten. %n  %s%n  %s", qualifier,
                                inventoryInfo.createCompareStringRepresentation(),
                                localInventoryInfo.createCompareStringRepresentation()));
                    }
                }
            } else {
                getInventoryInfo().add(inventoryInfo);
            }
        }
    }

    /**
     * Removes all VulnerabilityMetaData entries from the inventory that do not fulfill at least one of these conditions:
     * <ul>
     *     <li>referenced by at least one artifact</li>
     *     <li>has the status classification <code>void</code></li>
     * </ul>
     */
    public void filterVulnerabilityMetaData() {
        // collect vulnerability ids referenced by artifacts
        final Set<String> coveredVulnerabilityIds = new HashSet<>();
        for (Artifact artifact : artifacts) {
            final String v = artifact.getVulnerability();
            splitCommaSeparated(v).stream().
                    map(this::toPlainCVE).
                    filter(Objects::nonNull).
                    forEach(coveredVulnerabilityIds::add);
        }
        LOG.debug("Covered vulnerabilities: {}", coveredVulnerabilityIds);

        final List<VulnerabilityMetaData> forDeletion = new ArrayList<>();

        // FIXME-YWI: do we want to filter all contexts or only the default context?
        for (String context : this.getVulnerabilityMetaDataContexts()) {
            for (VulnerabilityMetaData vmd : this.getVulnerabilityMetaData(context)) {
                // retain void vulnerabilities
                if (vmd.isStatus(VulnerabilityMetaData.STATUS_VALUE_VOID)) continue;

                // retain vulnerabilities referenced by artifacts
                if (coveredVulnerabilityIds.contains(vmd.get(VulnerabilityMetaData.Attribute.NAME))) continue;

                // collect others for deletion
                forDeletion.add(vmd);
            }
        }

        // log vulnerabilities deleted
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing vulnerability metadata for: {}",
                    forDeletion.stream().map(v -> v.get(VulnerabilityMetaData.Attribute.NAME)).collect(Collectors.joining(", "))
            );
        }

        // remove non-relevant vulnerabilities
        for (String context : this.getVulnerabilityMetaDataContexts()) {
            getVulnerabilityMetaData(context).removeAll(forDeletion);
        }
    }

    public List<ComponentPatternData> getComponentPatternData() {
        return this.componentPatternData;
    }

    public void setComponentPatternData(List<ComponentPatternData> componentPatternData) {
        this.componentPatternData = componentPatternData;
    }

    /**
     * Access the default assessment context.
     *
     * @return The default assessment contexts' vulnerability metadata.
     */
    public List<VulnerabilityMetaData> getVulnerabilityMetaData() {
        return getVulnerabilityMetaData(VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT);
    }

    public List<VulnerabilityMetaData> getVulnerabilityMetaData(String context) {
        return vulnerabilityMetaData.computeIfAbsent(context, e -> new ArrayList<>());
    }

    public void setVulnerabilityMetaData(List<VulnerabilityMetaData> vulnerabilityMetaData) {
        setVulnerabilityMetaData(vulnerabilityMetaData, VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT);
    }

    public void setVulnerabilityMetaData(List<VulnerabilityMetaData> vulnerabilityMetaData, String context) {
        this.vulnerabilityMetaData.put(context, vulnerabilityMetaData);
    }

    public List<String> getVulnerabilityMetaDataContexts() {
        return new ArrayList<>(vulnerabilityMetaData.keySet());
    }

    @Deprecated // is still used be the german translation; preserve until translation is completely revised
    public List<VulnerabilityMetaData> getApplicableVulnerabilityMetaData(float threshold) {
        List<VulnerabilityMetaData> vmd = VulnerabilityMetaData.filterApplicableVulnerabilities(getVulnerabilityMetaData(), threshold);
        vmd.sort(VulnerabilityMetaData.VULNERABILITY_COMPARATOR_OVERALL_SCORE);
        return vmd;
    }

    @Deprecated // is still used be the german translation; preserve until translation is completely revised
    public List<VulnerabilityMetaData> getNotApplicableVulnerabilityMetaData(float threshold) {
        List<VulnerabilityMetaData> vmd = VulnerabilityMetaData.filterNotApplicableVulnerabilities(getVulnerabilityMetaData(), threshold);
        vmd.sort(VulnerabilityMetaData.VULNERABILITY_COMPARATOR_OVERALL_SCORE);
        return vmd;
    }

    @Deprecated // is still used be the german translation; preserve until translation is completely revised
    public List<VulnerabilityMetaData> getInsignificantVulnerabilities(float threshold) {
        List<VulnerabilityMetaData> vmd = VulnerabilityMetaData.filterInsignificantVulnerabilities(getVulnerabilityMetaData(), threshold);
        vmd.sort(VulnerabilityMetaData.VULNERABILITY_COMPARATOR_OVERALL_SCORE);
        return vmd;
    }

    public List<CertMetaData> getCertMetaData() {
        return certMetaData;
    }

    public void setCertMetaData(List<CertMetaData> certMetaData) {
        this.certMetaData = certMetaData;
    }

    public List<InventoryInfo> getInventoryInfo() {
        return inventoryInfo;
    }

    public List<ReportData> getReportData() {
        return reportData;
    }

    public void setInventoryInfo(List<InventoryInfo> inventoryInfo) {
        this.inventoryInfo = inventoryInfo;
    }

    public void setReportData(List<ReportData> reportData) {
        this.reportData = reportData;
    }

    public List<AssetMetaData> getAssetMetaData() {
        return assetMetaData;
    }

    public void setAssetMetaData(List<AssetMetaData> assetMetaData) {
        this.assetMetaData = assetMetaData;
    }

    private Set<String> splitCommaSeparated(String string) {
        if (string == null) return Collections.emptySet();
        return Arrays.stream(string.split(",")).map(String::trim).collect(Collectors.toSet());
    }


    // helpers

    private String toPlainCVE(String cve) {
        if (StringUtils.isEmpty(cve)) return null;
        int index = cve.indexOf(" ");
        return index == -1 ? cve : cve.substring(0, index);
    }

    public String getEffectiveLicense(Artifact artifact) {
        if (artifact == null) return null;
        String effectiveLicense = artifact.getLicense();
        if (StringUtils.isEmpty(artifact.getComponent())) return effectiveLicense;
        if (StringUtils.isEmpty(artifact.getVersion())) return effectiveLicense;
        if (StringUtils.isEmpty(artifact.getLicense())) return effectiveLicense;
        LicenseMetaData licenseMetaData = findMatchingLicenseMetaData(artifact);
        if (licenseMetaData == null) return effectiveLicense;
        effectiveLicense = licenseMetaData.deriveLicenseInEffect();
        if (StringUtils.isEmpty(effectiveLicense)) return null;
        effectiveLicense = effectiveLicense.replace("|", ", ");
        return effectiveLicense;
    }

    public List<String> getEffectiveLicenses(Artifact artifact) {
        String effectiveLicense = getEffectiveLicense(artifact);
        return InventoryUtils.tokenizeLicense(effectiveLicense, true, true);
    }

    public List<LicenseData> getLicenseData() {
        return licenseData;
    }

    public void setLicenseData(List<LicenseData> licenseData) {
        this.licenseData = licenseData;
    }

    public String getRepresentedLicenseName(String license) {
        // this code operates on the metadata in the inventory, only
        for (LicenseData ld : getLicenseData()) {
            if (license.equals(ld.get(LicenseData.Attribute.CANONICAL_NAME))) {
                final String representedAs = ld.get(LicenseData.Attribute.REPRESENTED_AS);
                if (representedAs != null) {
                    return representedAs;
                } else {
                    break;
                }
            }
        }

        // return original license
        return license;
    }

    public List<String> getRepresentedLicenses(List<String> effectiveLicenses) {
        return effectiveLicenses.stream()
                .map(this::getRepresentedLicenseName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getLicensesRepresentedBy(String representedLicenseName) {
        Set<String> representedEffectiveLicenses = new HashSet<>();

        // add represented license name itself
        representedEffectiveLicenses.add(representedLicenseName);

        for (LicenseData ld : getLicenseData()) {
            final String representedAs = ld.get(LicenseData.Attribute.REPRESENTED_AS);
            if (representedAs != null && representedLicenseName.equals(representedAs)) {
                representedEffectiveLicenses.add(ld.get(LicenseData.Attribute.CANONICAL_NAME));
            }
        }
        return representedEffectiveLicenses.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }

    /**
     * The substructure is required when:
     * <ol>
     *     <li>more than 2 licenses are represented by a single license.</li>
     *     <li>the represented name and variant license name deviates</li>
     * </ol>
     *
     * @param license                      The license for which the request needs to ne answered.
     * @param representedEffectiveLicenses List of represented effective licenses
     * @return Returns <code>true</code> when a substructure to represent the licenses is required.
     */
    public boolean isSubstructureRequired(String license, List<String> representedEffectiveLicenses) {
        if (!representedEffectiveLicenses.contains(license)) {
            return true;
        } else {
            return getLicensesRepresentedBy(license).size() > 1;
        }
    }

    public Set<String> evaluateComponentsRepresentedLicense(String representedNameLicense) {
        final Set<String> componentNames = new HashSet<>();
        for (String effectiveLicense : getLicensesRepresentedBy(representedNameLicense)) {
            evaluateComponents(effectiveLicense).forEach(ald -> componentNames.add(ald.getComponentName()));
        }
        return componentNames;
    }

    public boolean isFootnoteRequired(List<String> effectiveLicenses, List<String> representedEffectiveLicenses) {
        for (String license : effectiveLicenses) {
            if (isSubstructureRequired(license, representedEffectiveLicenses)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Component data. Please note that the component per-se does not have a version, but a license.
     * Artifacts (with various versions) can be added to the component. When evaluating the
     * LicenseMetaData for a component the version (on artifact level) is still important.
     * FIXME:
     * - add an optional component version to the artifacts or identify the component with version
     * - alternatively add a component version on license meta data level; rationale: an artifact can
     * belong in the same version can belong to several components.
     */
    public class Component {
        private String name;
        private String license;
        private String originalComponentName;

        private List<Artifact> artifacts = new ArrayList<>();

        public Component(String name, String originalComponentName, String license) {
            this.name = name;
            this.license = license;
            this.originalComponentName = originalComponentName;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof Component) {
                return toString().equals(o.toString());
            }
            return false;
        }

        public List<Artifact> getArtifacts() {
            return artifacts;
        }

        public void add(Artifact artifact) {
            artifacts.add(artifact);
        }

        @Override
        public String toString() {
            return STRING_EMPTY + name + "/" + license;
        }

        public String getName() {
            return name;
        }

        public String getLicense() {
            return license;
        }

        public String getOriginalComponentName() {
            return originalComponentName;
        }

        public String getQualifier() {
            return toString();
        }
    }
}
