/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.model.common.mapping.builtin.BuiltinMetadataMapping;

import com.evolveum.midpoint.model.common.mapping.builtin.BuiltinMetadataMappingsRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;

import java.util.Collection;

/**
 * Evaluates metadata mappings.
 */
@Component
public class MetadataMappingEvaluator {

    @Autowired MappingFactory mappingFactory;
    @Autowired PrismContext prismContext;
    @Autowired BuiltinMetadataMappingsRegistry builtinMetadataMappingsRegistry;

    Collection<BuiltinMetadataMapping> getBuiltinMappings() {
        return builtinMetadataMappingsRegistry.getMappings();
    }
}