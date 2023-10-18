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
package org.metaeffekt.core.security.cvss.v4_0;

public interface EqOperations {
    String[] getHighestSeverityVectors(Cvss4_0MacroVector thisMacroVector);

    String[] getRelevantAttributes();

    Cvss4_0MacroVector[] deriveNextLowerMacro(Cvss4_0MacroVector thisMacroVector);

    double lookupScoresForNextLowerMacro(Cvss4_0MacroVector[] nextLowerMacro);

    /**
     * Max hamming distance (vector depth) within the equivalence set that the macro vector represents.
     *
     * @param thisMacroVector the macro vector to look up the depth for
     * @return the depth of the equivalence set in integers.
     */
    int lookupMacroVectorDepth(Cvss4_0MacroVector thisMacroVector);

    static EqOperations[] getEqImplementations() {
        return new EqOperations[]{
                EqOperations1_2_4_5.EqOperations1.getInstanceEq1(),
                EqOperations1_2_4_5.EqOperations2.getInstanceEq2(),
                EqOperations1_2_4_5.EqOperations4.getInstanceEq4(),
                EqOperations1_2_4_5.EqOperations5.getInstanceEq5(),
                EqOperations36.getInstance()
        };
    }
}
