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
package org.metaeffekt.core.itest.common.predicates;

import java.util.function.Predicate;

import static org.metaeffekt.core.itest.common.predicates.Not.not;


public class AttributeExists<T, E extends Enum<E>> implements NamedBasePredicate<T> {

    @FunctionalInterface
    public interface AttributeGetter<T, E extends Enum<E>> {
        String getAttribute(T instance, E attributeKey);
    }

    private final AttributeGetter<T, E> attributeGetter;
    private final E attributeKey;

    public AttributeExists(AttributeGetter<T, E> attributeGetter, E attributeKey) {
        this.attributeGetter = attributeGetter;
        this.attributeKey = attributeKey;
    }

    /**
     * Only include Artifacts in the collection where attribute is not null.
     */
    public static <T, E extends Enum<E>>  NamedBasePredicate<T> withAttribute(AttributeGetter<T, E> attributeGetter, E attributeKey) {
        return new AttributeExists<>(attributeGetter, attributeKey);
    }


    /**
     * Only include Artifacts in the collection where attribute is null.
     */
    public static <T, E extends Enum<E>>  NamedBasePredicate<T> withoutAttribute(AttributeGetter<T, E> attributeGetter, E attributeKey) {
        return not(new AttributeExists<>(attributeGetter, attributeKey));
    }

    @Override
    public Predicate<T> getPredicate() {
        return instance -> attributeGetter.getAttribute(instance, attributeKey) != null;
    }

    @Override
    public String getDescription() {
        return "'" + attributeKey.name() + "' exists";
    }
}
