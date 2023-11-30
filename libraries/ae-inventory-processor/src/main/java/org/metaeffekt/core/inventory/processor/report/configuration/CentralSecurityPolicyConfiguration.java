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
package org.metaeffekt.core.inventory.processor.report.configuration;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.configuration.ProcessConfiguration;
import org.metaeffekt.core.inventory.processor.configuration.ProcessMisconfiguration;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaContentIdentifiers;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.metaeffekt.core.security.cvss.processor.CvssSelectionResult.CvssScoreVersionSelectionPolicy;
import org.metaeffekt.core.security.cvss.processor.CvssSelector;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;
import static org.metaeffekt.core.security.cvss.processor.CvssSelector.*;

public class CentralSecurityPolicyConfiguration extends ProcessConfiguration {

    private String cvssSeverityRanges = CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString();
    private CvssSeverityRanges cachedCvssSeverityRanges;

    private JSONObject baseCvssSelector = CVSS_SELECTOR_BASE.toJson();
    private JSONObject effectiveCvssSelector = CVSS_SELECTOR_EFFECTIVE.toJson();
    private CvssSelector cachedBaseCvssSelector;
    private CvssSelector cachedEffectiveCvssSelector;

    private List<CvssScoreVersionSelectionPolicy> cvssVersionSelectionPolicy = new ArrayList<>(Collections.singletonList(CvssScoreVersionSelectionPolicy.LATEST));

    private double insignificantThreshold = 7.0;
    private double includeScoreThreshold = -1.0;

    private final List<String> includeVulnerabilitiesWithAdvisoryProviders = new ArrayList<>(Collections.singletonList("all"));
    private final List<String> includeAdvisoryTypes = new ArrayList<>(Collections.singletonList("all"));

    private String vulnerabilityStatusDisplayMapperName = "unmodified";
    private VulnerabilityStatusMapper vulnerabilityStatusDisplayMapper = VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED;

    public CentralSecurityPolicyConfiguration setCvssSeverityRanges(String cvssSeverityRanges) {
        this.cvssSeverityRanges = cvssSeverityRanges == null ? CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString() : cvssSeverityRanges;
        this.cachedCvssSeverityRanges = new CvssSeverityRanges(this.cvssSeverityRanges);
        return this;
    }

    public CentralSecurityPolicyConfiguration setCvssSeverityRanges(CvssSeverityRanges cvssSeverityRanges) {
        this.cvssSeverityRanges = cvssSeverityRanges.toString();
        cachedCvssSeverityRanges = cvssSeverityRanges;
        return this;
    }

    public CvssSeverityRanges getCvssSeverityRanges() {
        if (this.cachedCvssSeverityRanges == null) {
            this.cvssSeverityRanges = this.cvssSeverityRanges == null ? CvssSeverityRanges.CVSS_3_SEVERITY_RANGES.toString() : this.cvssSeverityRanges;
            this.cachedCvssSeverityRanges = new CvssSeverityRanges(this.cvssSeverityRanges);
        }
        return this.cachedCvssSeverityRanges;
    }

    public CentralSecurityPolicyConfiguration setBaseCvssSelector(JSONObject baseCvssSelector) {
        this.baseCvssSelector = baseCvssSelector;
        this.cachedBaseCvssSelector = fromJson(this.baseCvssSelector);
        return this;
    }

    public CentralSecurityPolicyConfiguration setBaseCvssSelector(CvssSelector baseCvssSelector) {
        this.baseCvssSelector = baseCvssSelector.toJson();
        this.cachedBaseCvssSelector = baseCvssSelector;
        return this;
    }

    public CvssSelector getBaseCvssSelector() {
        if (this.cachedBaseCvssSelector == null) {
            this.baseCvssSelector = this.baseCvssSelector == null ? CVSS_SELECTOR_BASE.toJson() : this.baseCvssSelector;
            this.cachedBaseCvssSelector = fromJson(this.baseCvssSelector);
        }
        return this.cachedBaseCvssSelector;
    }

    public CentralSecurityPolicyConfiguration setEffectiveCvssSelector(JSONObject effectiveCvssSelector) {
        this.effectiveCvssSelector = effectiveCvssSelector;
        this.cachedEffectiveCvssSelector = fromJson(this.effectiveCvssSelector);
        return this;
    }

    public CentralSecurityPolicyConfiguration setEffectiveCvssSelector(CvssSelector effectiveCvssSelector) {
        this.effectiveCvssSelector = effectiveCvssSelector.toJson();
        this.cachedEffectiveCvssSelector = effectiveCvssSelector;
        return this;
    }

    public CvssSelector getEffectiveCvssSelector() {
        if (this.cachedEffectiveCvssSelector == null) {
            this.effectiveCvssSelector = this.effectiveCvssSelector == null ? CVSS_SELECTOR_EFFECTIVE.toJson() : this.effectiveCvssSelector;
            this.cachedEffectiveCvssSelector = fromJson(this.effectiveCvssSelector);
        }
        return this.cachedEffectiveCvssSelector;
    }

    public void setCvssVersionSelectionPolicy(List<CvssScoreVersionSelectionPolicy> cvssVersionSelectionPolicy) {
        this.cvssVersionSelectionPolicy = cvssVersionSelectionPolicy;
    }

    public List<CvssScoreVersionSelectionPolicy> getCvssVersionSelectionPolicy() {
        return cvssVersionSelectionPolicy;
    }

    public CentralSecurityPolicyConfiguration setInsignificantThreshold(double insignificantThreshold) {
        this.insignificantThreshold = insignificantThreshold;
        return this;
    }

    public double getInsignificantThreshold() {
        return this.insignificantThreshold;
    }

    public CentralSecurityPolicyConfiguration setIncludeScoreThreshold(double includeScoreThreshold) {
        this.includeScoreThreshold = includeScoreThreshold;
        return this;
    }

    public boolean isVulnerabilityInsignificant(AeaaVulnerability vulnerability) {
        if (insignificantThreshold == -1.0) return true;
        final CvssVector<?> vector = vulnerability.getCvssSelectionResult().getSelectedEffectiveIfAvailableOtherwiseBase();
        final double score = vector == null ? 0.0 : vector.getOverallScore();
        return score <= insignificantThreshold;
    }

    public double getIncludeScoreThreshold() {
        return this.includeScoreThreshold;
    }

    public boolean isVulnerabilityAboveIncludeScoreThreshold(AeaaVulnerability vulnerability) {
        if (includeScoreThreshold == -1.0) return true;
        final CvssVector<?> vector = vulnerability.getCvssSelectionResult().getSelectedEffectiveIfAvailableOtherwiseBase();
        final double score = vector == null ? 0.0 : vector.getOverallScore();
        return score >= includeScoreThreshold;
    }

    public CentralSecurityPolicyConfiguration setIncludeVulnerabilitiesWithAdvisoryProviders(List<String> includeVulnerabilitiesWithAdvisoryProviders) {
        this.includeVulnerabilitiesWithAdvisoryProviders.clear();
        this.includeVulnerabilitiesWithAdvisoryProviders.addAll(includeVulnerabilitiesWithAdvisoryProviders);
        return this;
    }

    public List<String> getIncludeVulnerabilitiesWithAdvisoryProviders() {
        return includeVulnerabilitiesWithAdvisoryProviders;
    }

    public boolean isVulnerabilityIncludedRegardingAdvisoryProviders(AeaaVulnerability vulnerability) {
        if (includeVulnerabilitiesWithAdvisoryProviders.contains("all")) {
            return true;
        }

        final List<AeaaContentIdentifiers> filter = includeVulnerabilitiesWithAdvisoryProviders.stream()
                .map(AeaaContentIdentifiers::fromContentIdentifierName)
                .collect(Collectors.toList());

        return isVulnerabilityIncludedRegardingAdvisoryProviders(vulnerability, filter);
    }

    public static List<AeaaVulnerability> filterVulnerabilitiesForAdvisories(Collection<AeaaVulnerability> vulnerabilities, Collection<AeaaContentIdentifiers> filter) {
        return vulnerabilities.stream()
                .filter(v -> isVulnerabilityIncludedRegardingAdvisoryProviders(v, filter))
                .collect(Collectors.toList());
    }

    public static boolean isVulnerabilityIncludedRegardingAdvisoryProviders(AeaaVulnerability vulnerability, Collection<AeaaContentIdentifiers> filter) {
        return vulnerability.getSecurityAdvisories().stream().anyMatch(a -> filter.contains(a.getEntrySource()));
    }

    public CentralSecurityPolicyConfiguration setIncludeAdvisoryTypes(List<String> includeAdvisoryTypes) {
        this.includeAdvisoryTypes.clear();
        this.includeAdvisoryTypes.addAll(includeAdvisoryTypes);
        return this;
    }

    public List<String> getIncludeAdvisoryTypes() {
        return includeAdvisoryTypes;
    }

    public boolean isSecurityAdvisoryIncludedRegardingEntrySourceType(AeaaAdvisoryEntry advisory) {
        if (includeAdvisoryTypes.contains("all")) {
            return true;
        }
        return includeAdvisoryTypes.contains(advisory.getType());
    }

    public CentralSecurityPolicyConfiguration setVulnerabilityStatusDisplayMapper(String vulnerabilityStatusDisplayMapper) {
        this.vulnerabilityStatusDisplayMapperName = vulnerabilityStatusDisplayMapper;
        this.vulnerabilityStatusDisplayMapper = CentralSecurityPolicyConfiguration.getStatusMapperByName(vulnerabilityStatusDisplayMapper);
        return this;
    }

    public CentralSecurityPolicyConfiguration setVulnerabilityStatusDisplayMapper(VulnerabilityStatusMapper vulnerabilityStatusDisplayMapper) {
        this.vulnerabilityStatusDisplayMapperName = vulnerabilityStatusDisplayMapper.getName();
        this.vulnerabilityStatusDisplayMapper = vulnerabilityStatusDisplayMapper;
        return this;
    }

    public VulnerabilityStatusMapper getVulnerabilityStatusDisplayMapper() {
        if (this.vulnerabilityStatusDisplayMapper == null
                || !this.vulnerabilityStatusDisplayMapperName.equals(this.vulnerabilityStatusDisplayMapper.getName())) {
            this.setVulnerabilityStatusDisplayMapper(this.vulnerabilityStatusDisplayMapperName);
        }
        return this.vulnerabilityStatusDisplayMapper;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        final LinkedHashMap<String, Object> configuration = new LinkedHashMap<>();

        configuration.put("cvssSeverityRanges", cvssSeverityRanges);
        configuration.put("baseCvssSelector", baseCvssSelector);
        configuration.put("effectiveCvssSelector", effectiveCvssSelector);
        configuration.put("insignificantThreshold", insignificantThreshold);
        configuration.put("includeScoreThreshold", includeScoreThreshold);
        configuration.put("includeVulnerabilitiesWithAdvisoryProviders", includeVulnerabilitiesWithAdvisoryProviders);
        configuration.put("includeAdvisoryTypes", includeAdvisoryTypes);
        configuration.put("vulnerabilityStatusDisplayMapperName", vulnerabilityStatusDisplayMapperName);

        return configuration;
    }

    @Override
    public void setProperties(LinkedHashMap<String, Object> properties) {
        super.loadStringProperty(properties, "cvssSeverityRanges", this::setCvssSeverityRanges);
        super.loadStringProperty(properties, "baseCvssSelector", selector -> setBaseCvssSelector(fromJson(new JSONObject(selector))));
        super.loadStringProperty(properties, "effectiveCvssSelector", selector -> setEffectiveCvssSelector(fromJson(new JSONObject(selector))));
        super.loadDoubleProperty(properties, "insignificantThreshold", this::setInsignificantThreshold);
        super.loadDoubleProperty(properties, "includeScoreThreshold", this::setIncludeScoreThreshold);
        super.loadListProperty(properties, "includeVulnerabilitiesWithAdvisoryProviders", String::valueOf, this::setIncludeVulnerabilitiesWithAdvisoryProviders);
        super.loadListProperty(properties, "includeAdvisoryTypes", String::valueOf, this::setIncludeAdvisoryTypes);
        super.loadStringProperty(properties, "vulnerabilityStatusDisplayMapperName", this::setVulnerabilityStatusDisplayMapper);
    }

    @Override
    protected void collectMisconfigurations(List<ProcessMisconfiguration> misconfigurations) {
        if (cvssSeverityRanges == null) {
            misconfigurations.add(new ProcessMisconfiguration("cvssSeverityRanges", "CVSS severity ranges must not be null"));
        }
        if (baseCvssSelector == null) {
            misconfigurations.add(new ProcessMisconfiguration("baseCvssSelector", "Base CVSS selector must not be null"));
        }
        if (effectiveCvssSelector == null) {
            misconfigurations.add(new ProcessMisconfiguration("effectiveCvssSelector", "Effective CVSS selector must not be null"));
        }
        if ((insignificantThreshold < 0.0 && insignificantThreshold != -1) || insignificantThreshold > 10.0) {
            misconfigurations.add(new ProcessMisconfiguration("insignificantThreshold", "Insignificant threshold must be between 0.0 and 10.0 or be -1.0"));
        }
        if ((includeScoreThreshold < 0.0 && includeScoreThreshold != -1) || includeScoreThreshold > 10.0) {
            misconfigurations.add(new ProcessMisconfiguration("includeScoreThreshold", "Include score threshold must be between 0.0 and 10.0 or be -1.0"));
        }
        try {
            this.getVulnerabilityStatusDisplayMapper();
        } catch (IllegalArgumentException e) {
            misconfigurations.add(new ProcessMisconfiguration("vulnerabilityStatusDisplayMapper", "Unknown status mapper: " + vulnerabilityStatusDisplayMapper));
        }
    }

    public final static CvssSelector CVSS_SELECTOR_BASE = new CvssSelector(Collections.singletonList(
            new CvssRule(MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),
                    // other NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // CERT-SEI
                    new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY)
            )
    ));

    public final static CvssSelector CVSS_SELECTOR_EFFECTIVE = new CvssSelector(Arrays.asList(
            new CvssRule(MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),
                    // other NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // CERT-SEI
                    new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY)
            ),
            // assessment
            new CvssRule(MergingMethod.ALL,
                    Collections.singletonList(new SelectorStatsCollector("assessment", StatsCollectorProvider.PRESENCE, StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL)),
            new CvssRule(MergingMethod.LOWER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", StatsCollectorProvider.PRESENCE, StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)),
            new CvssRule(MergingMethod.HIGHER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", StatsCollectorProvider.PRESENCE, StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER))
    ), Collections.singletonList(
            new SelectorStatsEvaluator("assessment", StatsEvaluatorOperation.EQUAL, EvaluatorAction.RETURN_NULL, 0)
    ), Collections.singletonList(
            new SelectorVectorEvaluator(VectorEvaluatorOperation.IS_BASE_FULLY_DEFINED, true, EvaluatorAction.RETURN_NULL)
    ));

    public static class VulnerabilityStatusMapper {
        private final String name;
        private final Function<String, String> mapper;
        private final List<String> statusNames;
        private final List<String> assessedStatusNames;

        public VulnerabilityStatusMapper(String name, List<String> statusNames, List<String> assessedStatusNames, Function<String, String> mapper) {
            this.name = name;
            this.mapper = mapper;
            this.statusNames = statusNames;
            this.assessedStatusNames = assessedStatusNames;
        }

        public String getName() {
            return name;
        }

        public Function<String, String> getMapper() {
            return mapper;
        }

        public List<String> getStatusNames() {
            return statusNames;
        }

        public List<String> getAssessedStatusNames() {
            return assessedStatusNames;
        }
    }

    public final static VulnerabilityStatusMapper VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED = new VulnerabilityStatusMapper(
            "unmodified",
            Arrays.asList("applicable", "in review", "not applicable", "insignificant", "void"),
            Arrays.asList("applicable", "not applicable", "void"),
            name -> {
                if (StringUtils.isEmpty(name)) {
                    return VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW;
                }

                if (VulnerabilityMetaData.STATUS_VALUE_APPLICABLE.equalsIgnoreCase(name)) {
                    return VulnerabilityMetaData.STATUS_VALUE_APPLICABLE;
                }

                // FIXME: when would the status be "Potential Vulnerability"?
                if ("potential vulnerability".equalsIgnoreCase(name)) {
                    return VulnerabilityMetaData.STATUS_VALUE_APPLICABLE;
                }

                return name;
            }
    );

    public final static VulnerabilityStatusMapper VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED = new VulnerabilityStatusMapper(
            "abstracted",
            Arrays.asList("affected", "potentially affected", "not affected"),
            Arrays.asList("affected", "not affected"),
            name -> {
                if (StringUtils.isEmpty(name)) { // in review (implicit)
                    return "potentially affected";
                }

                switch (name) {
                    case VulnerabilityMetaData.STATUS_VALUE_APPLICABLE:
                        return "affected";

                    case VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE:
                    case VulnerabilityMetaData.STATUS_VALUE_VOID:
                        return "not affected";

                    case VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT:
                    case VulnerabilityMetaData.STATUS_VALUE_IN_REVIEW:
                        return "potentially affected";

                    default:
                        return name;
                }
            }
    );

    private final static List<VulnerabilityStatusMapper> REGISTERED_VULNERABILITY_STATUS_DISPLAY_MAPPERS = Arrays.asList(
            VULNERABILITY_STATUS_DISPLAY_MAPPER_UNMODIFIED,
            VULNERABILITY_STATUS_DISPLAY_MAPPER_ABSTRACTED
    );

    public static VulnerabilityStatusMapper getStatusMapperByName(String name) {
        return REGISTERED_VULNERABILITY_STATUS_DISPLAY_MAPPERS.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status mapper: " + name));
    }
}
