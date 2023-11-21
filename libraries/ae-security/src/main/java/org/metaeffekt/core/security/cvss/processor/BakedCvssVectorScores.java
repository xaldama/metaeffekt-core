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
package org.metaeffekt.core.security.cvss.processor;


import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

public class BakedCvssVectorScores<T extends CvssVector<T>> {

    private final CvssVector<T> vector;

    private final double base;
    private final double impact;
    private final double exploitability;
    private final double temporal;
    private final double environmental;
    private final double adjustedImpact;
    private final double overall;

    public BakedCvssVectorScores(CvssVector<T> vector) {
        this.vector = vector;

        this.base = vector.getBaseScore();
        this.overall = vector.getOverallScore();

        if (vector instanceof MultiScoreCvssVector) {
            final MultiScoreCvssVector<T> cast = ((MultiScoreCvssVector<T>) vector);
            this.impact = cast.getImpactScore();
            this.exploitability = cast.getExploitabilityScore();
            this.temporal = cast.getTemporalScore();
            this.environmental = cast.getEnvironmentalScore();
            this.adjustedImpact = cast.getAdjustedImpactScore();
        } else {
            this.impact = Double.NaN;
            this.exploitability = Double.NaN;
            this.temporal = Double.NaN;
            this.environmental = Double.NaN;
            this.adjustedImpact = Double.NaN;
        }
    }

    public static <T extends CvssVector<T>> BakedCvssVectorScores<T> fromNullableCvss(CvssVector<T> cvss) {
        if (cvss == null) {
            return null;
        }
        return new BakedCvssVectorScores<>(cvss);
    }

    public double getBaseScore() {
        return base;
    }

    public double getImpactScore() {
        return impact;
    }

    public double getExploitabilityScore() {
        return exploitability;
    }

    public double getTemporalScore() {
        return temporal;
    }

    public double getEnvironmentalScore() {
        return environmental;
    }

    public double getAdjustedImpactScore() {
        return adjustedImpact;
    }

    public double getOverallScore() {
        return overall;
    }

    public boolean isBaseScoreAvailable() {
        return !Double.isNaN(base);
    }

    public boolean isImpactScoreAvailable() {
        return !Double.isNaN(impact);
    }

    public boolean isExploitabilityScoreAvailable() {
        return !Double.isNaN(exploitability);
    }

    public boolean isTemporalScoreAvailable() {
        return !Double.isNaN(temporal);
    }

    public boolean isEnvironmentalScoreAvailable() {
        return !Double.isNaN(environmental);
    }

    public boolean isAdjustedImpactScoreAvailable() {
        return !Double.isNaN(adjustedImpact);
    }

    public boolean isOverallScoreAvailable() {
        return !Double.isNaN(overall);
    }

    public boolean hasNormalizedBaseScore() {
        return this.getUnNormalizedBaseScoreMax() != 10.0;
    }

    public boolean hasNormalizedImpactScore() {
        return this.getUnNormalizedImpactScoreMax() != 10.0;
    }

    public boolean hasNormalizedExploitabilityScore() {
        return this.getUnNormalizedExploitabilityScoreMax() != 10.0;
    }

    public boolean hasNormalizedTemporalScore() {
        return this.getUnNormalizedTemporalScoreMax() != 10.0;
    }

    public boolean hasNormalizedEnvironmentalScore() {
        return this.getUnNormalizedEnvironmentalScoreMax() != 10.0;
    }

    public boolean hasNormalizedAdjustedImpactScore() {
        return this.getUnNormalizedAdjustedImpactScoreMax() != 10.0;
    }

    public boolean hasNormalizedOverallScore() {
        return this.getUnNormalizedOverallScoreMax() != 10.0;
    }

    public double getUnNormalizedBaseScoreMax() {
        return 10.0;
    }

    public double getUnNormalizedImpactScoreMax() {
        if (vector instanceof Cvss3P1) {
            return 6.0;
        } else {
            return 10.0;
        }
    }

    public double getUnNormalizedExploitabilityScoreMax() {
        if (vector instanceof Cvss3P1) {
            return 3.9;
        } else {
            return 10.0;
        }
    }

    public double getUnNormalizedTemporalScoreMax() {
        return 10.0;
    }

    public double getUnNormalizedEnvironmentalScoreMax() {
        return 10.0;
    }

    public double getUnNormalizedAdjustedImpactScoreMax() {
        if (vector instanceof Cvss3P1) {
            return 6.1;
        } else {
            return 10.0;
        }
    }

    public double getUnNormalizedOverallScoreMax() {
        return 10.0;
    }

    public double getNormalizedBaseScore() {
        return normalizeScore(base, this.getUnNormalizedBaseScoreMax());
    }

    public double getNormalizedImpactScore() {
        return normalizeScore(impact, this.getUnNormalizedImpactScoreMax());
    }

    public double getNormalizedExploitabilityScore() {
        return normalizeScore(exploitability, this.getUnNormalizedExploitabilityScoreMax());
    }

    public double getNormalizedTemporalScore() {
        return normalizeScore(temporal, this.getUnNormalizedTemporalScoreMax());
    }

    public double getNormalizedEnvironmentalScore() {
        return normalizeScore(environmental, this.getUnNormalizedEnvironmentalScoreMax());
    }

    public double getNormalizedAdjustedImpactScore() {
        return normalizeScore(adjustedImpact, this.getUnNormalizedAdjustedImpactScoreMax());
    }

    public double getNormalizedOverallScore() {
        return normalizeScore(overall, this.getUnNormalizedOverallScoreMax());
    }

    @Override
    public String toString() {
        return vector.toString();
    }

    /**
     * Maps the range (0 - max) of the score to the range of the normalized score (0 - 10).
     *
     * @param score the score to normalize
     * @param max   the maximum value of the score
     * @return the normalized score
     */
    public double normalizeScore(double score, double max) {
        if (max == 10.0) {
            return score;
        }
        return round(mapRange(score, 0, max, 0, 10), 1);
    }

    private double mapRange(double value, double min, double max, double newMin, double newMax) {
        return (value - min) / (max - min) * (newMax - newMin) + newMin;
    }

    private double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
}
