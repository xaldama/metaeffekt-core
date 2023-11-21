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
package org.metaeffekt.core.security.cvss.v3;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.metaeffekt.core.security.cvss.processor.BakedCvssVectorScores;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Model a CvssV3.1 vector and calculate the corresponding scores.
 */
public class Cvss3P1 extends MultiScoreCvssVector<Cvss3P1> {

    // base
    private AttackVector attackVector = AttackVector.NULL;
    private AttackComplexity attackComplexity = AttackComplexity.NULL;
    private PrivilegesRequired privilegesRequired = PrivilegesRequired.NULL;
    private UserInteraction userInteraction = UserInteraction.NULL;
    private Scope scope = Scope.NULL;
    private CIAImpact confidentialityImpact = CIAImpact.NULL;
    private CIAImpact integrityImpact = CIAImpact.NULL;
    private CIAImpact availabilityImpact = CIAImpact.NULL;

    // temporal
    private ExploitCodeMaturity exploitCodeMaturity = ExploitCodeMaturity.NULL;
    private RemediationLevel remediationLevel = RemediationLevel.NULL;
    private ReportConfidence reportConfidence = ReportConfidence.NULL;

    // environmental
    private AttackVector modifiedAttackVector = AttackVector.NULL;
    private AttackComplexity modifiedAttackComplexity = AttackComplexity.NULL;
    private PrivilegesRequired modifiedPrivilegesRequired = PrivilegesRequired.NULL;
    private UserInteraction modifiedUserInteraction = UserInteraction.NULL;
    private Scope modifiedScope = Scope.NULL;
    private CIAImpact modifiedConfidentialityImpact = CIAImpact.NULL;
    private CIAImpact modifiedIntegrityImpact = CIAImpact.NULL;
    private CIAImpact modifiedAvailabilityImpact = CIAImpact.NULL;
    private CIARequirement confidentialityRequirement = CIARequirement.NULL;
    private CIARequirement integrityRequirement = CIARequirement.NULL;
    private CIARequirement availabilityRequirement = CIARequirement.NULL;

    public Cvss3P1() {
        super();
    }

    public Cvss3P1(String vector) {
        super();
        super.applyVector(vector);
    }

    public Cvss3P1(String vector, CvssSource<Cvss3P1> source) {
        super(source);
        super.applyVector(vector);
    }

    public Cvss3P1(String vector, CvssSource<Cvss3P1> source, JSONObject applicabilityCondition) {
        super(source, applicabilityCondition);
        super.applyVector(vector);
    }

    public Cvss3P1(String vector, Collection<CvssSource<Cvss3P1>> sources, JSONObject applicabilityCondition) {
        super(sources, applicabilityCondition);
        super.applyVector(vector);
    }

    @Override
    protected boolean applyVectorArgument(String identifier, String value) {
        switch (identifier) {
            case "AV": // base
                attackVector = AttackVector.fromString(value);
                break;
            case "AC":
                attackComplexity = AttackComplexity.fromString(value);
                break;
            case "PR":
                privilegesRequired = PrivilegesRequired.fromString(value);
                break;
            case "UI":
                userInteraction = UserInteraction.fromString(value);
                break;
            case "S":
                scope = Scope.fromString(value);
                break;
            case "C":
                confidentialityImpact = CIAImpact.fromString(value);
                break;
            case "I":
                integrityImpact = CIAImpact.fromString(value);
                break;
            case "A":
                availabilityImpact = CIAImpact.fromString(value);
                break;
            case "E": // temporal
                exploitCodeMaturity = ExploitCodeMaturity.fromString(value);
                break;
            case "RL":
                remediationLevel = RemediationLevel.fromString(value);
                break;
            case "RC":
                reportConfidence = ReportConfidence.fromString(value);
                break;
            case "MAV": // environmental
                modifiedAttackVector = AttackVector.fromString(value);
                break;
            case "MAC":
                modifiedAttackComplexity = AttackComplexity.fromString(value);
                break;
            case "MPR":
                modifiedPrivilegesRequired = PrivilegesRequired.fromString(value);
                break;
            case "MUI":
                modifiedUserInteraction = UserInteraction.fromString(value);
                break;
            case "MS":
                modifiedScope = Scope.fromString(value);
                break;
            case "MC":
                modifiedConfidentialityImpact = CIAImpact.fromString(value);
                break;
            case "MI":
                modifiedIntegrityImpact = CIAImpact.fromString(value);
                break;
            case "MA":
                modifiedAvailabilityImpact = CIAImpact.fromString(value);
                break;
            case "CR":
                confidentialityRequirement = CIARequirement.fromString(value);
                break;
            case "IR":
                integrityRequirement = CIARequirement.fromString(value);
                break;
            case "AR":
                availabilityRequirement = CIARequirement.fromString(value);
                break;

            default:
                return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cvss3P1)) return false;
        Cvss3P1 cvss3 = (Cvss3P1) o;
        return attackVector == cvss3.attackVector &&
                attackComplexity == cvss3.attackComplexity &&
                privilegesRequired == cvss3.privilegesRequired &&
                userInteraction == cvss3.userInteraction &&
                scope == cvss3.scope &&
                confidentialityImpact == cvss3.confidentialityImpact &&
                integrityImpact == cvss3.integrityImpact &&
                availabilityImpact == cvss3.availabilityImpact &&
                exploitCodeMaturity == cvss3.exploitCodeMaturity &&
                remediationLevel == cvss3.remediationLevel &&
                reportConfidence == cvss3.reportConfidence &&
                modifiedAttackVector == cvss3.modifiedAttackVector &&
                modifiedAttackComplexity == cvss3.modifiedAttackComplexity &&
                modifiedPrivilegesRequired == cvss3.modifiedPrivilegesRequired &&
                modifiedUserInteraction == cvss3.modifiedUserInteraction &&
                modifiedScope == cvss3.modifiedScope &&
                modifiedConfidentialityImpact == cvss3.modifiedConfidentialityImpact &&
                modifiedIntegrityImpact == cvss3.modifiedIntegrityImpact &&
                modifiedAvailabilityImpact == cvss3.modifiedAvailabilityImpact &&
                confidentialityRequirement == cvss3.confidentialityRequirement &&
                integrityRequirement == cvss3.integrityRequirement &&
                availabilityRequirement == cvss3.availabilityRequirement;
    }

    private final static double EXPLOITABILITY_COEFFICIENT = 8.22;
    private final static double SCOPE_COEFFICIENT = 1.08;

    /**
     * If Impact &lt;= 0:         0, else<br>
     * If Scope is Unchanged:   Roundup (Minimum [(Impact + Exploitability), 10])
     * If Scope is Changed:     Roundup (Minimum [1.08 × (Impact + Exploitability), 10])
     *
     * @return The Cvss Base Score.
     */
    @Override
    public double getBaseScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        double impact = calculateImpactScore();
        if (impact <= 0) return 0.0;
        if (!scope.changed)
            return roundUp(Math.min(impact + calculateExploitabilityScore(), 10));
        else return roundUp(Math.min(SCOPE_COEFFICIENT * (impact + calculateExploitabilityScore()), 10));
    }

    /**
     * If Scope is Unchanged: 6.42 × ISS<br>
     * If Scope is Changed: 7.52 × (ISS - 0.029) - 3.25 × (ISS - 0.02)<sup>15</sup>
     *
     * @return The Cvss Impact Score.
     */
    private double calculateImpactScore() {
        double iss = calculateISS();
        if (scope.changed) return Scope.SCOPE_CHANGED_FACTOR * (iss - 0.029) - 3.25 * Math.pow(iss - 0.02, 15);
        else return Scope.SCOPE_UNCHANGED_FACTOR * iss;
    }

    @Override
    public double getImpactScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        return round(calculateImpactScore(), 1);
    }

    /**
     * 1 - [ (1 - Confidentiality) × (1 - Integrity) × (1 - Availability) ]
     *
     * @return The ISS score.
     */
    private double calculateISS() {
        return 1 - ((1 - confidentialityImpact.factor) * (1 - integrityImpact.factor) * (1 - availabilityImpact.factor));
    }

    /**
     * 8.22 × AttackVector × AttackComplexity × PrivilegesRequired × UserInteraction
     *
     * @return The Cvss Exploitability Score.
     */
    private double calculateExploitabilityScore() {
        if (scope.changed)
            return EXPLOITABILITY_COEFFICIENT * attackVector.factor * attackComplexity.factor * privilegesRequired.factorChanged * userInteraction.factor;
        else
            return EXPLOITABILITY_COEFFICIENT * attackVector.factor * attackComplexity.factor * privilegesRequired.factorUnchanged * userInteraction.factor;
    }

    @Override
    public double getExploitabilityScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        return round(calculateExploitabilityScore(), 1);
    }

    /**
     * Roundup (BaseScore × ExploitCodeMaturity × RemediationLevel × ReportConfidence)
     *
     * @return The Cvss Temporal Score.
     */
    private double calculateTemporalScore() {
        if (!isAnyTemporalDefined()) return 0;
        double exploitCodeMaturityFactor = exploitCodeMaturity == ExploitCodeMaturity.NULL ? ExploitCodeMaturity.NOT_DEFINED.factor : exploitCodeMaturity.factor;
        double remediationLevelFactor = remediationLevel == RemediationLevel.NULL ? RemediationLevel.NOT_DEFINED.factor : remediationLevel.factor;
        double reportConfidenceFactor = reportConfidence == ReportConfidence.NULL ? ReportConfidence.NOT_DEFINED.factor : reportConfidence.factor;
        double baseScore = getBaseScore();
        return baseScore * exploitCodeMaturityFactor * remediationLevelFactor * reportConfidenceFactor;
    }

    @Override
    public double getTemporalScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyTemporalDefined()) return Double.NaN;
        return roundUp(calculateTemporalScore());
    }

    /**
     * If ModifiedImpact &lt;= 0:         0, else<br>
     * If ModifiedScope is Unchanged:   Roundup ( Roundup [Minimum ([ModifiedImpact + ModifiedExploitability], 10) ] × ExploitCodeMaturity × RemediationLevel × ReportConfidence)<br>
     * If ModifiedScope is Changed:     Roundup ( Roundup [Minimum (1.08 × [ModifiedImpact + ModifiedExploitability], 10) ] × ExploitCodeMaturity × RemediationLevel × ReportConfidence)
     *
     * @return The Cvss Environmental Score.
     */
    @Override
    public double getEnvironmentalScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyEnvironmentalDefined()) return Double.NaN;

        double modifiedImpact = calculateAdjustedImpact();
        if (modifiedImpact <= 0) return 0;

        double modifiedExploitability = calculateAdjustedExploitability();
        double exploitCodeMaturityFactor = exploitCodeMaturity == ExploitCodeMaturity.NULL ? ExploitCodeMaturity.NOT_DEFINED.factor : exploitCodeMaturity.factor;
        double remediationLevelFactor = remediationLevel == RemediationLevel.NULL ? RemediationLevel.NOT_DEFINED.factor : remediationLevel.factor;
        double reportConfidenceFactor = reportConfidence == ReportConfidence.NULL ? ReportConfidence.NOT_DEFINED.factor : reportConfidence.factor;

        if (isModifiedScope())
            return roundUp(roundUp(Math.min((modifiedImpact + modifiedExploitability), 10)) * exploitCodeMaturityFactor * remediationLevelFactor * reportConfidenceFactor);
        else
            return roundUp(roundUp(Math.min(1.08 * (modifiedImpact + modifiedExploitability), 10)) * exploitCodeMaturityFactor * remediationLevelFactor * reportConfidenceFactor);
    }

    /**
     * Minimum ( 1 - [ (1 - ConfidentialityRequirement × ModifiedConfidentiality) × (1 - IntegrityRequirement × ModifiedIntegrity) × (1 - AvailabilityRequirement × ModifiedAvailability) ], 0.915)
     *
     * @return The MISS Score.
     */
    private double calculateMISS() {
        double mci = (modifiedConfidentialityImpact == CIAImpact.NULL || modifiedConfidentialityImpact == CIAImpact.NOT_DEFINED ? (confidentialityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED.factor : confidentialityImpact.factor) : modifiedConfidentialityImpact.factor);
        double mii = (modifiedIntegrityImpact == CIAImpact.NULL || modifiedIntegrityImpact == CIAImpact.NOT_DEFINED ? (integrityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED.factor : integrityImpact.factor) : modifiedIntegrityImpact.factor);
        double mai = (modifiedAvailabilityImpact == CIAImpact.NULL || modifiedAvailabilityImpact == CIAImpact.NOT_DEFINED ? (availabilityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED.factor : availabilityImpact.factor) : modifiedAvailabilityImpact.factor);

        double crFactor = confidentialityRequirement == CIARequirement.NULL && (integrityRequirement != CIARequirement.NULL || availabilityRequirement != CIARequirement.NULL) ? CIARequirement.NOT_DEFINED.factor : confidentialityRequirement.factor;
        double irFactor = integrityRequirement == CIARequirement.NULL && (confidentialityRequirement != CIARequirement.NULL || availabilityRequirement != CIARequirement.NULL) ? CIARequirement.NOT_DEFINED.factor : integrityRequirement.factor;
        double arFactor = availabilityRequirement == CIARequirement.NULL && (confidentialityRequirement != CIARequirement.NULL || integrityRequirement != CIARequirement.NULL) ? CIARequirement.NOT_DEFINED.factor : availabilityRequirement.factor;

        return Math.min(1 - (
                (1 - crFactor * mci) *
                        (1 - irFactor * mii) *
                        (1 - arFactor * mai)
        ), 0.915);
    }

    /**
     * If ModifiedScope is Unchanged: 6.42 × MISS<br>
     * If ModifiedScope is Changed: 7.52 × (MISS - 0.029) - 3.25 × (MISS × 0.9731 - 0.02)<sup>13</sup>
     *
     * @return The Cvss Adjusted Impact Score.
     */
    private double calculateAdjustedImpact() {
        double miss = calculateMISS();
        if (isModifiedScope())
            return Scope.SCOPE_UNCHANGED_FACTOR * miss;
        else return Scope.SCOPE_CHANGED_FACTOR * (miss - 0.029) - 3.25 * Math.pow(miss * 0.9731 - 0.02, 13);
    }

    @Override
    public double getAdjustedImpactScore() {
        if (!isBaseFullyDefined()) return Double.NaN;
        if (!isAnyEnvironmentalDefined()) return Double.NaN;

        return Math.max(0, round(calculateAdjustedImpact(), 1));
    }

    /**
     * 8.22 × ModifiedAttackVector × ModifiedAttackComplexity × ModifiedPrivilegesRequired × ModifiedUserInteraction<br>
     * Replace the modified version with the base when not defined
     *
     * @return The Cvss Adjusted Exploitability Score.
     */
    private double calculateAdjustedExploitability() {
        double mav = (modifiedAttackVector == AttackVector.NULL || modifiedAttackVector == AttackVector.NOT_DEFINED ? attackVector.factor : modifiedAttackVector.factor);
        double mac = (modifiedAttackComplexity == AttackComplexity.NULL || modifiedAttackComplexity == AttackComplexity.NOT_DEFINED ? attackComplexity.factor : modifiedAttackComplexity.factor);
        double mui = (modifiedUserInteraction == UserInteraction.NULL || modifiedUserInteraction == UserInteraction.NOT_DEFINED ? userInteraction.factor : modifiedUserInteraction.factor);
        double mpr;
        if (isModifiedScope())
            mpr = (modifiedPrivilegesRequired == PrivilegesRequired.NULL || modifiedPrivilegesRequired == PrivilegesRequired.NOT_DEFINED ? privilegesRequired.factorUnchanged : modifiedPrivilegesRequired.factorUnchanged);
        else
            mpr = (modifiedPrivilegesRequired == PrivilegesRequired.NULL || modifiedPrivilegesRequired == PrivilegesRequired.NOT_DEFINED ? privilegesRequired.factorChanged : modifiedPrivilegesRequired.factorChanged);
        return 8.22 * mav * mac * mpr * mui;
    }

    private boolean isModifiedScope() {
        return ((modifiedScope != Scope.NULL && modifiedScope != Scope.NOT_DEFINED) && !modifiedScope.changed) || ((modifiedScope == Scope.NULL || modifiedScope == Scope.NOT_DEFINED) && !scope.changed);
    }

    @Override
    public double getOverallScore() {
        if (isAnyEnvironmentalDefined()) return getEnvironmentalScore();
        else if (isAnyTemporalDefined()) return getTemporalScore();
        return getBaseScore();
    }

    @Override
    public CvssSeverityRanges.SeverityRange getDefaultSeverityCategory() {
        return getSeverityCategory(CvssSeverityRanges.CVSS_3_SEVERITY_RANGES);
    }

    @Override
    public boolean isBaseFullyDefined() {
        return attackVector != AttackVector.NULL
                && attackComplexity != AttackComplexity.NULL
                && privilegesRequired != PrivilegesRequired.NULL
                && userInteraction != UserInteraction.NULL
                && scope != Scope.NULL
                && confidentialityImpact != CIAImpact.NULL
                && integrityImpact != CIAImpact.NULL
                && availabilityImpact != CIAImpact.NULL;
    }

    @Override
    public boolean isAnyBaseDefined() {
        return attackVector != AttackVector.NULL
                || attackComplexity != AttackComplexity.NULL
                || privilegesRequired != PrivilegesRequired.NULL
                || userInteraction != UserInteraction.NULL
                || scope != Scope.NULL
                || confidentialityImpact != CIAImpact.NULL
                || integrityImpact != CIAImpact.NULL
                || availabilityImpact != CIAImpact.NULL;
    }

    @Override
    public boolean isAnyTemporalDefined() {
        return exploitCodeMaturity != ExploitCodeMaturity.NULL
                || remediationLevel != RemediationLevel.NULL
                || reportConfidence != ReportConfidence.NULL;
    }

    @Override
    public boolean isTemporalFullyDefined() {
        return exploitCodeMaturity != ExploitCodeMaturity.NULL
                && remediationLevel != RemediationLevel.NULL
                && reportConfidence != ReportConfidence.NULL;
    }

    @Override
    public boolean isAnyEnvironmentalDefined() {
        return modifiedAttackVector != AttackVector.NULL
                || modifiedAttackComplexity != AttackComplexity.NULL
                || modifiedPrivilegesRequired != PrivilegesRequired.NULL
                || modifiedUserInteraction != UserInteraction.NULL
                || modifiedScope != Scope.NULL
                || modifiedConfidentialityImpact != CIAImpact.NULL
                || modifiedIntegrityImpact != CIAImpact.NULL
                || modifiedAvailabilityImpact != CIAImpact.NULL
                || confidentialityRequirement != CIARequirement.NULL
                || integrityRequirement != CIARequirement.NULL
                || availabilityRequirement != CIARequirement.NULL;
    }

    @Override
    public boolean isEnvironmentalFullyDefined() {
        return modifiedAttackVector != AttackVector.NULL
                && modifiedAttackComplexity != AttackComplexity.NULL
                && modifiedPrivilegesRequired != PrivilegesRequired.NULL
                && modifiedUserInteraction != UserInteraction.NULL
                && modifiedScope != Scope.NULL
                && modifiedConfidentialityImpact != CIAImpact.NULL
                && modifiedIntegrityImpact != CIAImpact.NULL
                && modifiedAvailabilityImpact != CIAImpact.NULL
                && confidentialityRequirement != CIARequirement.NULL
                && integrityRequirement != CIARequirement.NULL
                && availabilityRequirement != CIARequirement.NULL;
    }

    @Override
    public void clearTemporal() {
        exploitCodeMaturity = ExploitCodeMaturity.NULL;
        remediationLevel = RemediationLevel.NULL;
        reportConfidence = ReportConfidence.NULL;
    }

    @Override
    public void clearEnvironmental() {
        modifiedAttackVector = AttackVector.NULL;
        modifiedAttackComplexity = AttackComplexity.NULL;
        modifiedPrivilegesRequired = PrivilegesRequired.NULL;
        modifiedUserInteraction = UserInteraction.NULL;
        modifiedScope = Scope.NULL;
        modifiedConfidentialityImpact = CIAImpact.NULL;
        modifiedIntegrityImpact = CIAImpact.NULL;
        modifiedAvailabilityImpact = CIAImpact.NULL;
        confidentialityRequirement = CIARequirement.NULL;
        integrityRequirement = CIARequirement.NULL;
        availabilityRequirement = CIARequirement.NULL;
    }

    @Override
    public BakedCvssVectorScores<Cvss3P1> bakeScores() {
        return new BakedCvssVectorScores<>(this);
    }

    public String getAttackComplexity() {
        return attackComplexity.identifier;
    }

    public String getAttackVector() {
        return attackVector.identifier;
    }

    public String getAvailabilityImpact() {
        return availabilityImpact.identifier;
    }

    public String getConfidentialityImpact() {
        return confidentialityImpact.identifier;
    }

    public String getIntegrityImpact() {
        return integrityImpact.identifier;
    }

    public String getModifiedAttackComplexity() {
        return modifiedAttackComplexity.identifier;
    }

    public String getExploitCodeMaturity() {
        return exploitCodeMaturity.identifier;
    }

    public String getModifiedAttackVector() {
        return modifiedAttackVector.identifier;
    }

    public String getModifiedAvailabilityImpact() {
        return modifiedAvailabilityImpact.identifier;
    }

    public String getModifiedConfidentialityImpact() {
        return modifiedConfidentialityImpact.identifier;
    }

    public String getModifiedIntegrityImpact() {
        return modifiedIntegrityImpact.identifier;
    }

    public String getPrivilegesRequired() {
        return privilegesRequired.identifier;
    }

    public String getAvailabilityRequirement() {
        return availabilityRequirement.identifier;
    }

    public String getConfidentialityRequirement() {
        return confidentialityRequirement.identifier;
    }

    public String getModifiedPrivilegesRequired() {
        return modifiedPrivilegesRequired.identifier;
    }

    public String getIntegrityRequirement() {
        return integrityRequirement.identifier;
    }

    public String getRemediationLevel() {
        return remediationLevel.identifier;
    }

    public String getReportConfidence() {
        return reportConfidence.identifier;
    }

    public String getModifiedScope() {
        return modifiedScope.identifier;
    }

    public String getScope() {
        return scope.identifier;
    }

    public String getUserInteraction() {
        return userInteraction.identifier;
    }

    public String getModifiedUserInteraction() {
        return modifiedUserInteraction.identifier;
    }

    public void setAttackComplexity(AttackComplexity attackComplexity) {
        this.attackComplexity = attackComplexity;
    }

    public void setAttackVector(AttackVector attackVector) {
        this.attackVector = attackVector;
    }

    public void setAvailabilityImpact(CIAImpact availabilityImpact) {
        this.availabilityImpact = availabilityImpact;
    }

    public void setAvailabilityRequirement(CIARequirement availabilityRequirement) {
        this.availabilityRequirement = availabilityRequirement;
    }

    public void setConfidentialityImpact(CIAImpact confidentialityImpact) {
        this.confidentialityImpact = confidentialityImpact;
    }

    public void setConfidentialityRequirement(CIARequirement confidentialityRequirement) {
        this.confidentialityRequirement = confidentialityRequirement;
    }

    public void setExploitCodeMaturity(ExploitCodeMaturity exploitCodeMaturity) {
        this.exploitCodeMaturity = exploitCodeMaturity;
    }

    public void setIntegrityImpact(CIAImpact integrityImpact) {
        this.integrityImpact = integrityImpact;
    }

    public void setIntegrityRequirement(CIARequirement integrityRequirement) {
        this.integrityRequirement = integrityRequirement;
    }

    public void setModifiedAttackComplexity(AttackComplexity modifiedAttackComplexity) {
        this.modifiedAttackComplexity = modifiedAttackComplexity;
    }

    public void setModifiedAttackVector(AttackVector modifiedAttackVector) {
        this.modifiedAttackVector = modifiedAttackVector;
    }

    public void setModifiedAvailabilityImpact(CIAImpact modifiedAvailabilityImpact) {
        this.modifiedAvailabilityImpact = modifiedAvailabilityImpact;
    }

    public void setModifiedConfidentialityImpact(CIAImpact modifiedConfidentialityImpact) {
        this.modifiedConfidentialityImpact = modifiedConfidentialityImpact;
    }

    public void setModifiedIntegrityImpact(CIAImpact modifiedIntegrityImpact) {
        this.modifiedIntegrityImpact = modifiedIntegrityImpact;
    }

    public void setModifiedPrivilegesRequired(PrivilegesRequired modifiedPrivilegesRequired) {
        this.modifiedPrivilegesRequired = modifiedPrivilegesRequired;
    }

    public void setModifiedScope(Scope modifiedScope) {
        this.modifiedScope = modifiedScope;
    }

    public void setModifiedUserInteraction(UserInteraction modifiedUserInteraction) {
        this.modifiedUserInteraction = modifiedUserInteraction;
    }

    public void setPrivilegesRequired(PrivilegesRequired privilegesRequired) {
        this.privilegesRequired = privilegesRequired;
    }

    public void setRemediationLevel(RemediationLevel remediationLevel) {
        this.remediationLevel = remediationLevel;
    }

    public void setReportConfidence(ReportConfidence reportConfidence) {
        this.reportConfidence = reportConfidence;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setUserInteraction(UserInteraction userInteraction) {
        this.userInteraction = userInteraction;
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    /**
     * Returns the smallest number, specified to 1 decimal place, that is equal to or higher than its input.
     *
     * @param value The value to round.
     * @return The rounded value.
     */
    public static double roundUp(double value) {
        int input = (int) Math.round(value * 100000);
        if ((input % 10000) == 0)
            return input / 100000.0;
        else
            return (Math.floor(Double.parseDouble(input + "") / 10000d) + 1) / 10.0;
    }

    public static String getVersionName() {
        return "CVSS:3.1";
    }

    @Override
    public String getName() {
        return getVersionName();
    }

    /**
     * Depending on whether the environmental or temporal attributes are defined, one of the following two URLs is generated:
     * <pre>
     * 1. https://www.first.org/cvss/calculator/3.1#%s
     * 2. https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=%s&amp;version=3.1
     * </pre>
     * Where <code>%s</code> is replaced with the current vector string. If the vector is to be used with the second URL, and it contains the <code>CVSS:3.1/</code> prefix, this prefix is removed.
     * Examples:
     * <ul>
     *     <li>https://www.first.org/cvss/calculator/3.1#CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H</li>
     *     <li>https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H&amp;version=3.1</li>
     * </ul>
     */
    @Override
    public String getWebEditorLink() {
        final String vectorString = this.toString(!isAnyEnvironmentalDefined());
        if (this.isAnyEnvironmentalDefined() || this.isAnyTemporalDefined()) {
            return String.format("https://www.first.org/cvss/calculator/3.1#%s", vectorString);
        } else {
            return String.format("https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=%s&version=3.1", vectorString.replace(this.getName() + "/", ""));
        }
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean filterUndefinedProperties) {
        final StringBuilder vector = new StringBuilder();
        vector.append(getName()).append("/");

        vector.append("AV:").append(attackVector.shortIdentifier).append("/");
        vector.append("AC:").append(attackComplexity.shortIdentifier).append("/");
        vector.append("PR:").append(privilegesRequired.shortIdentifier).append("/");
        vector.append("UI:").append(userInteraction.shortIdentifier).append("/");
        vector.append("S:").append(scope.shortIdentifier).append("/");
        vector.append("C:").append(confidentialityImpact.shortIdentifier).append("/");
        vector.append("I:").append(integrityImpact.shortIdentifier).append("/");
        vector.append("A:").append(availabilityImpact.shortIdentifier).append("/");
        vector.append("E:").append(exploitCodeMaturity.shortIdentifier).append("/");
        vector.append("RL:").append(remediationLevel.shortIdentifier).append("/");
        vector.append("RC:").append(reportConfidence.shortIdentifier).append("/");
        vector.append("MAV:").append(modifiedAttackVector.shortIdentifier).append("/");
        vector.append("MAC:").append(modifiedAttackComplexity.shortIdentifier).append("/");
        vector.append("MPR:").append(modifiedPrivilegesRequired.shortIdentifier).append("/");
        vector.append("MUI:").append(modifiedUserInteraction.shortIdentifier).append("/");
        vector.append("MS:").append(modifiedScope.shortIdentifier).append("/");
        vector.append("MC:").append(modifiedConfidentialityImpact.shortIdentifier).append("/");
        vector.append("MI:").append(modifiedIntegrityImpact.shortIdentifier).append("/");
        vector.append("MA:").append(modifiedAvailabilityImpact.shortIdentifier).append("/");
        vector.append("CR:").append(confidentialityRequirement.shortIdentifier).append("/");
        vector.append("IR:").append(integrityRequirement.shortIdentifier).append("/");
        vector.append("AR:").append(availabilityRequirement.shortIdentifier).append("/");

        if (filterUndefinedProperties) {
            return vector.toString().replaceAll("[^:/]+:X", "").replaceAll("/{2,}", "/").replaceAll("/$", "");
        } else {
            return vector.toString().replaceAll("/$", "");
        }
    }

    @Override
    public int size() {
        int size = 0;

        if (attackVector != AttackVector.NOT_DEFINED && attackVector != AttackVector.NULL) size++;
        if (attackComplexity != AttackComplexity.NOT_DEFINED && attackComplexity != AttackComplexity.NULL) size++;
        if (privilegesRequired != PrivilegesRequired.NOT_DEFINED && privilegesRequired != PrivilegesRequired.NULL)
            size++;
        if (userInteraction != UserInteraction.NOT_DEFINED && userInteraction != UserInteraction.NULL) size++;
        if (scope != Scope.NOT_DEFINED && scope != Scope.NULL) size++;
        if (confidentialityImpact != CIAImpact.NOT_DEFINED && confidentialityImpact != CIAImpact.NULL) size++;
        if (integrityImpact != CIAImpact.NOT_DEFINED && integrityImpact != CIAImpact.NULL) size++;
        if (availabilityImpact != CIAImpact.NOT_DEFINED && availabilityImpact != CIAImpact.NULL) size++;
        if (exploitCodeMaturity != ExploitCodeMaturity.NOT_DEFINED && exploitCodeMaturity != ExploitCodeMaturity.NULL)
            size++;
        if (remediationLevel != RemediationLevel.NOT_DEFINED && remediationLevel != RemediationLevel.NULL) size++;
        if (reportConfidence != ReportConfidence.NOT_DEFINED && reportConfidence != ReportConfidence.NULL) size++;
        if (modifiedAttackVector != AttackVector.NOT_DEFINED && modifiedAttackVector != AttackVector.NULL) size++;
        if (modifiedAttackComplexity != AttackComplexity.NOT_DEFINED && modifiedAttackComplexity != AttackComplexity.NULL)
            size++;
        if (modifiedPrivilegesRequired != PrivilegesRequired.NOT_DEFINED && modifiedPrivilegesRequired != PrivilegesRequired.NULL)
            size++;
        if (modifiedUserInteraction != UserInteraction.NOT_DEFINED && modifiedUserInteraction != UserInteraction.NULL)
            size++;
        if (modifiedScope != Scope.NOT_DEFINED && modifiedScope != Scope.NULL) size++;
        if (modifiedConfidentialityImpact != CIAImpact.NOT_DEFINED && modifiedConfidentialityImpact != CIAImpact.NULL)
            size++;
        if (modifiedIntegrityImpact != CIAImpact.NOT_DEFINED && modifiedIntegrityImpact != CIAImpact.NULL) size++;
        if (modifiedAvailabilityImpact != CIAImpact.NOT_DEFINED && modifiedAvailabilityImpact != CIAImpact.NULL) size++;
        if (confidentialityRequirement != CIARequirement.NOT_DEFINED && confidentialityRequirement != CIARequirement.NULL)
            size++;
        if (integrityRequirement != CIARequirement.NOT_DEFINED && integrityRequirement != CIARequirement.NULL) size++;
        if (availabilityRequirement != CIARequirement.NOT_DEFINED && availabilityRequirement != CIARequirement.NULL)
            size++;

        return size;
    }

    public enum AttackVector implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        NETWORK("NETWORK", "N", 0.85),
        ADJACENT_NETWORK("ADJACENT_NETWORK", "A", 0.62),
        LOCAL("LOCAL", "L", 0.55),
        PHYSICAL("PHYSICAL", "P", 0.2);

        public final String identifier, shortIdentifier;
        public final double factor;

        AttackVector(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static AttackVector fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum AttackComplexity implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        LOW("LOW", "L", 0.77),
        HIGH("HIGH", "H", 0.44);

        public final String identifier, shortIdentifier;
        public final double factor;

        AttackComplexity(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static AttackComplexity fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum PrivilegesRequired implements Cvss3Attribute {
        NULL("NULL", "X", 0.0, 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0, 1.0),
        HIGH("HIGH", "H", 0.27, 0.5),
        LOW("LOW", "L", 0.62, 0.68),
        NONE("NONE", "N", 0.85, 0.85);

        public final String identifier, shortIdentifier;
        public final double factorUnchanged, factorChanged;

        PrivilegesRequired(String identifier, String shortIdentifier, double factorUnchanged, double factorChanged) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factorUnchanged = factorUnchanged;
            this.factorChanged = factorChanged;
        }

        public static PrivilegesRequired fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum UserInteraction implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        REQUIRED("REQUIRED", "R", 0.62),
        NONE("NONE", "N", 0.85);

        public final String identifier, shortIdentifier;
        public final double factor;

        UserInteraction(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static UserInteraction fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum Scope implements Cvss3Attribute {
        NULL("NULL", "X", false),
        NOT_DEFINED("NOT_DEFINED", "X", false),
        CHANGED("CHANGED", "C", true),
        UNCHANGED("UNCHANGED", "U", false);

        public final String identifier, shortIdentifier;
        public final boolean changed;

        Scope(String identifier, String shortIdentifier, boolean changed) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.changed = changed;
        }

        public static Scope fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }

        public final static double SCOPE_CHANGED_FACTOR = 7.52;
        public final static double SCOPE_UNCHANGED_FACTOR = 6.42;
    }

    public enum CIAImpact implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        NONE("NONE", "N", 0.0),
        LOW("LOW", "L", 0.22),
        HIGH("HIGH", "H", 0.56);

        public final String identifier, shortIdentifier;
        public final double factor;

        CIAImpact(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static CIAImpact fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum ExploitCodeMaturity implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        UNPROVEN("UNPROVEN", "U", 0.91),
        PROOF_OF_CONCEPT("PROOF_OF_CONCEPT", "P", 0.94),
        FUNCTIONAL("FUNCTIONAL", "F", 0.97),
        HIGH("HIGH", "H", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        ExploitCodeMaturity(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static ExploitCodeMaturity fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum RemediationLevel implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        OFFICIAL_FIX("OFFICIAL_FIX", "O", 0.95),
        TEMPORARY_FIX("TEMPORARY_FIX", "T", 0.96),
        WORKAROUND("WORKAROUND", "W", 0.97),
        UNAVAILABLE("UNAVAILABLE", "U", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        RemediationLevel(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static RemediationLevel fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum ReportConfidence implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        UNKNOWN("UNKNOWN", "U", 0.92),
        REASONABLE("REASONABLE", "R", 0.96),
        CONFIRMED("CONFIRMED", "C", 1.0);

        public final String identifier, shortIdentifier;
        public final double factor;

        ReportConfidence(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static ReportConfidence fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    public enum CIARequirement implements Cvss3Attribute {
        NULL("NULL", "X", 0.0),
        NOT_DEFINED("NOT_DEFINED", "X", 1.0),
        LOW("LOW", "L", 0.5),
        MEDIUM("MEDIUM", "M", 1.0),
        HIGH("HIGH", "H", 1.5);

        public final String identifier, shortIdentifier;
        public final double factor;

        CIARequirement(String identifier, String shortIdentifier, double factor) {
            this.identifier = identifier;
            this.shortIdentifier = shortIdentifier;
            this.factor = factor;
        }

        public static CIARequirement fromString(String part) {
            return Arrays.stream(values()).filter(value -> value.identifier.equals(part) || value.shortIdentifier.equals(part)).findFirst().orElse(NULL);
        }

        @Override
        public String getShortIdentifier() {
            return shortIdentifier;
        }
    }

    @Override
    public Cvss3P1 clone() {
        return new Cvss3P1(toString(), super.sources, super.applicabilityCondition);
    }

    @Override
    public void completeVector() {
        cleanupTemporalVectorParts();
        cleanupEnvironmentalVectorParts();
    }

    private void cleanupTemporalVectorParts() {
        if (isAnyTemporalDefined()) {
            exploitCodeMaturity = exploitCodeMaturity == ExploitCodeMaturity.NULL ? ExploitCodeMaturity.NOT_DEFINED : exploitCodeMaturity;
            remediationLevel = remediationLevel == RemediationLevel.NULL ? RemediationLevel.NOT_DEFINED : remediationLevel;
            reportConfidence = reportConfidence == ReportConfidence.NULL ? ReportConfidence.NOT_DEFINED : reportConfidence;
        }

        if (isTemporalAllPartsNotDefined()) {
            clearTemporal();
        }
    }

    private void cleanupEnvironmentalVectorParts() {
        if (isAnyEnvironmentalDefined()) {
            modifiedAttackVector = modifiedAttackVector == AttackVector.NULL ? AttackVector.NOT_DEFINED : modifiedAttackVector;
            modifiedAttackComplexity = modifiedAttackComplexity == AttackComplexity.NULL ? AttackComplexity.NOT_DEFINED : modifiedAttackComplexity;
            modifiedPrivilegesRequired = modifiedPrivilegesRequired == PrivilegesRequired.NULL ? PrivilegesRequired.NOT_DEFINED : modifiedPrivilegesRequired;
            modifiedUserInteraction = modifiedUserInteraction == UserInteraction.NULL ? UserInteraction.NOT_DEFINED : modifiedUserInteraction;
            modifiedScope = modifiedScope == Scope.NULL ? Scope.NOT_DEFINED : modifiedScope;
            modifiedConfidentialityImpact = modifiedConfidentialityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED : modifiedConfidentialityImpact;
            modifiedIntegrityImpact = modifiedIntegrityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED : modifiedIntegrityImpact;
            modifiedAvailabilityImpact = modifiedAvailabilityImpact == CIAImpact.NULL ? CIAImpact.NOT_DEFINED : modifiedAvailabilityImpact;
            confidentialityRequirement = confidentialityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : confidentialityRequirement;
            integrityRequirement = integrityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : integrityRequirement;
            availabilityRequirement = availabilityRequirement == CIARequirement.NULL ? CIARequirement.NOT_DEFINED : availabilityRequirement;
        }

        if (isEnvironmentalAllPartsNotDefined()) {
            clearEnvironmental();
        }
    }

    private boolean isTemporalAllPartsNotDefined() {
        return exploitCodeMaturity == ExploitCodeMaturity.NOT_DEFINED &&
                remediationLevel == RemediationLevel.NOT_DEFINED &&
                reportConfidence == ReportConfidence.NOT_DEFINED;
    }

    private boolean isEnvironmentalAllPartsNotDefined() {
        return modifiedAttackVector == AttackVector.NOT_DEFINED &&
                modifiedAttackComplexity == AttackComplexity.NOT_DEFINED &&
                modifiedPrivilegesRequired == PrivilegesRequired.NOT_DEFINED &&
                modifiedUserInteraction == UserInteraction.NOT_DEFINED &&
                modifiedScope == Scope.NOT_DEFINED &&
                modifiedConfidentialityImpact == CIAImpact.NOT_DEFINED &&
                modifiedIntegrityImpact == CIAImpact.NOT_DEFINED &&
                modifiedAvailabilityImpact == CIAImpact.NOT_DEFINED &&
                confidentialityRequirement == CIARequirement.NOT_DEFINED &&
                integrityRequirement == CIARequirement.NOT_DEFINED &&
                availabilityRequirement == CIARequirement.NOT_DEFINED;
    }

    public static Optional<Cvss3P1> optionalParse(String vector) {
        if (vector == null || StringUtils.isEmpty(MultiScoreCvssVector.normalizeVector(vector))) {
            return Optional.empty();
        }

        return Optional.of(new Cvss3P1(vector));
    }

    public interface Cvss3Attribute {
        String getShortIdentifier();
    }
}
