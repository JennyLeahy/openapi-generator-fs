/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.examples;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExampleGenerator {
    private final Logger LOGGER = LoggerFactory.getLogger(ExampleGenerator.class);

    // TODO: move constants to more appropriate location
    private static final String MIME_TYPE_JSON = "application/json";
    private static final String MIME_TYPE_XML = "application/xml";

    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private static final String EXAMPLE = "example";
    private static final String CONTENT_TYPE = "contentType";
    private static final String GENERATED_CONTENT_TYPE = "generatedContentType";
    private static final String OUTPUT = "output";
    private static final String NONE = "none";
    private static final String URL = "url";
    private static final String URI = "uri";
    private static final String STATUS_CODE = "statusCode";

    protected Map<String, Schema> examples;
    private OpenAPI openAPI;
    private Random random;

    public ExampleGenerator(Map<String, Schema> examples, OpenAPI openAPI) {
        this.examples = examples;
        this.openAPI = openAPI;
        // use a fixed seed to make the "random" numbers reproducible.
        this.random = new Random("ExampleGenerator".hashCode());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public List<Map<String, String>> generateFromResponseSchema(String statusCode, Schema responseSchema, Set<String> producesInfo) {
        List<Map<String, String>> examples = generateFromResponseSchema(responseSchema, producesInfo);
        if (examples == null) {
            return null;
        }

        for (Map<String, String> example : examples) {
            example.put(STATUS_CODE, statusCode);
        }

        return examples;
    }

    private List<Map<String, String>> generateFromResponseSchema(Schema responseSchema, Set<String> producesInfo) {
        if (responseSchema.getExample() == null && StringUtils.isEmpty(responseSchema.get$ref()) && !ModelUtils.isArraySchema(responseSchema)) {
            // no example provided
            return null;
        }

        if (responseSchema.getExample() != null && !(responseSchema.getExample() instanceof Map)) {
            return generate(responseSchema.getExample(), new ArrayList<>(producesInfo));
        }

        if (ModelUtils.isArraySchema(responseSchema)) { // array of schema
            if (ModelUtils.getSchemaItems(responseSchema) != null) { // array of primitive types
                return generate((Map<String, Object>) responseSchema.getExample(),
                        new ArrayList<>(producesInfo), responseSchema);
            } else {
                // TODO log warning message as such case is not handled at the moment
                return null;
            }
        } else if (StringUtils.isEmpty(responseSchema.get$ref())) { // primitive type (e.g. integer, string)
            return generate((Map<String, Object>) responseSchema.getExample(),
                    new ArrayList<String>(producesInfo), responseSchema);
        } else { // model
            return generate((Map<String, Object>) responseSchema.getExample(),
                    new ArrayList<String>(producesInfo), ModelUtils.getSimpleRef(responseSchema.get$ref()));
        }
    }

    public List<Map<String, String>> generate(Map<String, Object> examples, List<String> mediaTypes, Schema property) {
        LOGGER.debug("debugging generate in ExampleGenerator");
        List<Map<String, String>> output = new ArrayList<>();
        Set<String> processedModels = new HashSet<>();
        if (examples == null) {
            if (mediaTypes == null) {
                // assume application/json for this
                mediaTypes = Collections.singletonList(MIME_TYPE_JSON); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
            }
            for (String mediaType : mediaTypes) {
                Map<String, String> kv = new HashMap<>();
                kv.put(CONTENT_TYPE, mediaType);
                if (property != null && (mediaType.startsWith(MIME_TYPE_JSON) || mediaType.contains("*/*"))) {
                    String example = Json.pretty(resolvePropertyToExample("", mediaType, property, processedModels));
                    if (example != null) {
                        kv.put(EXAMPLE, example);
                        kv.put(GENERATED_CONTENT_TYPE, MIME_TYPE_JSON);
                        output.add(kv);
                    }
                } else if (property != null && mediaType.startsWith(MIME_TYPE_XML)) {
                    String example = new XmlExampleGenerator(this.examples).toXml(property);
                    if (example != null) {
                        kv.put(EXAMPLE, example);
                        kv.put(GENERATED_CONTENT_TYPE, MIME_TYPE_XML);
                        output.add(kv);
                    }
                }
            }
        } else {
            for (Map.Entry<String, Object> entry : examples.entrySet()) {
                final Map<String, String> kv = new HashMap<>();
                kv.put(CONTENT_TYPE, entry.getKey());
                kv.put(EXAMPLE, Json.pretty(entry.getValue()));
                output.add(kv);
            }
        }

        if (output.size() == 0) {
            Map<String, String> kv = new HashMap<>();
            kv.put(OUTPUT, NONE);
            output.add(kv);
        }
        return output;
    }

    public List<Map<String, String>> generate(Map<String, Object> examples, List<String> mediaTypes, String modelName) {
        List<Map<String, String>> output = new ArrayList<>();
        Set<String> processedModels = new HashSet<>();
        if (examples == null) {
            if (mediaTypes == null) {
                // assume application/json for this
                mediaTypes = Collections.singletonList(MIME_TYPE_JSON); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
            }
            for (String mediaType : mediaTypes) {
                Map<String, String> kv = new HashMap<>();
                kv.put(CONTENT_TYPE, mediaType);
                if (modelName != null && (mediaType.startsWith(MIME_TYPE_JSON) || mediaType.contains("*/*"))) {
                    final Schema schema = this.examples.get(modelName);
                    if (schema != null) {
                        String example = Json.pretty(resolveModelToExample(modelName, mediaType, schema, processedModels));

                        if (example != null) {
                            kv.put(EXAMPLE, example);
                            kv.put(GENERATED_CONTENT_TYPE, MIME_TYPE_JSON);
                            output.add(kv);
                        }
                    }
                } else if (modelName != null && mediaType.startsWith(MIME_TYPE_XML)) {
                    final Schema schema = this.examples.get(modelName);
                    String example = new XmlExampleGenerator(this.examples).toXml(schema, 0, Collections.emptySet());
                    if (example != null) {
                        kv.put(EXAMPLE, example);
                        kv.put(GENERATED_CONTENT_TYPE, MIME_TYPE_XML);
                        output.add(kv);
                    }
                } else {
                    kv.put(EXAMPLE, "Custom MIME type example not yet supported: " + mediaType);
                    output.add(kv);
                }
            }
        } else {
            for (Map.Entry<String, Object> entry : examples.entrySet()) {
                final Map<String, String> kv = new HashMap<>();
                kv.put(CONTENT_TYPE, entry.getKey());
                kv.put(EXAMPLE, Json.pretty(entry.getValue()));
                output.add(kv);
            }
        }

        if (output.size() == 0) {
            Map<String, String> kv = new HashMap<>();
            kv.put(OUTPUT, NONE);
            output.add(kv);
        }
        return output;
    }

    private List<Map<String, String>> generate(Object example, List<String> mediaTypes) {
        List<Map<String, String>> output = new ArrayList<>();
        if (examples != null) {
            if (mediaTypes == null) {
                // assume application/json for this
                mediaTypes = Collections.singletonList(MIME_TYPE_JSON);
            }
            for (String mediaType : mediaTypes) {
                Map<String, String> kv = new HashMap<>();
                kv.put(CONTENT_TYPE, mediaType);
                if ((mediaType.startsWith(MIME_TYPE_JSON) || mediaType.contains("*/*"))) {
                    kv.put(EXAMPLE, Json.pretty(example));
                    kv.put(GENERATED_CONTENT_TYPE, MIME_TYPE_JSON);
                    output.add(kv);
                } else if (mediaType.startsWith(MIME_TYPE_XML)) {
                    // TODO
                    LOGGER.warn("XML example value of (array/primitive) is not handled at the moment: {}", example);
                }
            }
        }

        if (output.size() == 0) {
            Map<String, String> kv = new HashMap<>();
            kv.put(OUTPUT, NONE);
            output.add(kv);
        }
        return output;
    }

    private Object resolvePropertyToExample(String propertyName, String mediaType, Schema property, Set<String> processedModels) {
        if (property == null) {
            LOGGER.error("Property schema shouldn't be null. Please report the issue to the openapi-generator team.");
            return "";
        }
        LOGGER.debug("Resolving example for property {}...", property);
        if (property.getExample() != null) {
            LOGGER.debug("Example set in openapi spec, returning example: '{}'", property.getExample().toString());
            // When a property is of type Date, we want to ensure that we're returning a formatted Date.
            // And not returning the Date object directly.
            if (property.getExample() instanceof Date) {
                return dateFormat.format(property.getExample());
            }
            return property.getExample();
        } else if (ModelUtils.isBooleanSchema(property)) {
            Object defaultValue = property.getDefault();
            if (defaultValue != null) {
                return defaultValue;
            }
            return Boolean.TRUE;
        } else if (ModelUtils.isArraySchema(property)) {
            Schema innerType = ModelUtils.getSchemaItems(property);
            if (innerType != null) {
                int arrayLength = null == property.getMaxItems() ? 2 : property.getMaxItems();
                // avoid memory issues by limiting to max. 5 items
                arrayLength = Math.min(arrayLength, 5);
                Object[] objectProperties = new Object[arrayLength];
                Object objProperty = resolvePropertyToExample(propertyName, mediaType, innerType, processedModels);
                for (int i = 0; i < arrayLength; i++) {
                    objectProperties[i] = objProperty;
                }
                return objectProperties;
            }
        } else if (ModelUtils.isDateSchema(property)) {
            return "2000-01-23";
        } else if (ModelUtils.isDateTimeSchema(property)) {
            return "2000-01-23T04:56:07.000+00:00";
        } else if (ModelUtils.isNumberSchema(property)) {
            Double min = getPropertyValue(property.getMinimum());
            Double max = getPropertyValue(property.getMaximum());
            if (ModelUtils.isFloatSchema(property)) { // float
                return (float) randomNumber(min, max);
            } else if (ModelUtils.isDoubleSchema(property)) { // decimal/double
                return BigDecimal.valueOf(randomNumber(min, max));
            } else { // no format defined
                return randomNumber(min, max);
            }
        } else if (ModelUtils.isFileSchema(property)) {
            return "";  // TODO

        } else if (ModelUtils.isIntegerSchema(property)) {
            Double min = getPropertyValue(property.getMinimum());
            Double max = getPropertyValue(property.getMaximum());
            if (ModelUtils.isLongSchema(property)) {
                return (long) randomNumber(min, max);
            }
            return (int) randomNumber(min, max);
        } else if (ModelUtils.isMapSchema(property)) {
            Map<String, Object> mp = new HashMap<String, Object>();
            if (property.getName() != null) {
                mp.put(property.getName(),
                        resolvePropertyToExample(propertyName, mediaType, ModelUtils.getAdditionalProperties(property), processedModels));
            } else {
                mp.put("key",
                        resolvePropertyToExample(propertyName, mediaType, ModelUtils.getAdditionalProperties(property), processedModels));
            }
            return mp;
        } else if (ModelUtils.isUUIDSchema(property)) {
            return "046b6c7f-0b8a-43b9-b35d-6489e6daee91";
        } else if (ModelUtils.isURISchema(property)) {
            return "https://openapi-generator.tech";
        } else if (ModelUtils.isStringSchema(property)) {
            LOGGER.debug("String property");
            if (property.getDefault() != null) {
                return String.valueOf(property.getDefault());
            }
            List<String> enumValues = property.getEnum();
            if (enumValues != null && !enumValues.isEmpty()) {
                LOGGER.debug("Enum value found: '{}'", enumValues.get(0));
                return enumValues.get(0);
            }
            String format = property.getFormat();
            if (format != null && (URI.equals(format) || URL.equals(format))) {
                LOGGER.debug("URI or URL format, without default or enum, generating random one.");
                return "http://example.com/aeiou";
            }
            LOGGER.debug("No values found, using property name {} as example", propertyName);
            return propertyName;
        } else if (!StringUtils.isEmpty(property.get$ref())) { // model
            String simpleName = ModelUtils.getSimpleRef(property.get$ref());
            Schema schema = ModelUtils.getSchema(openAPI, simpleName);
            if (schema == null) { // couldn't find the model/schema
                return "{}";
            }
            return resolveModelToExample(simpleName, mediaType, schema, processedModels);

        } else if (ModelUtils.isObjectSchema(property)) {
            return "{}";
        }

        return "";
    }

    private Double getPropertyValue(BigDecimal propertyValue) {
        return propertyValue == null ? null : propertyValue.doubleValue();
    }

    private double randomNumber(Double min, Double max) {
        if (min != null && max != null) {
            double range = max - min;
            return random.nextDouble() * range + min;
        } else if (min != null) {
            return random.nextDouble() + min;
        } else if (max != null) {
            return random.nextDouble() * max;
        } else {
            return random.nextDouble() * 10;
        }
    }

    private Object resolveModelToExample(String name, String mediaType, Schema schema, Set<String> processedModels) {
        if (processedModels.contains(name)) {
            return schema.getExample();
        }

        processedModels.add(name);
        Map<String, Object> values = new HashMap<>();
        LOGGER.debug("Resolving model '{}' to example", name);
        if (schema.getExample() != null) {
            LOGGER.debug("Using example from spec: {}", schema.getExample());
            return schema.getExample();
        } else if (schema.getProperties() != null) {
            LOGGER.debug("Creating example from model values");
            traverseSchemaProperties(mediaType, schema, processedModels, values);
            schema.setExample(values);
            return schema.getExample();
        } else if (ModelUtils.isAllOf(schema) || ModelUtils.isAllOfWithProperties(schema)) {
            LOGGER.debug("Resolving allOf model '{}' to example", name);
            resolveAllOfSchemaProperties(mediaType, schema, processedModels, values);
            schema.setExample(values);
            return schema.getExample();
        } else if (ModelUtils.isAnyOf(schema) || ModelUtils.isOneOf(schema)) {
            LOGGER.debug("Resolving anyOf/oneOf model '{}' using the first schema to example", name);
            Optional<Schema> found = ModelUtils.getInterfaces(schema)
                    .stream()
                    .filter(this::hasValidRef)
                    .findFirst();

            if (found.isEmpty()) {
                return null;
            }
            return resolvePropertyToExample(name, mediaType, found.get(), processedModels);
        } else if (ModelUtils.isArraySchema(schema) || ModelUtils.isEnumSchema(schema)) {
            return resolvePropertyToExample(schema.getName(), mediaType, schema, processedModels);
        } else {
            // TODO log an error message as the model does not have any properties
            return null;
        }
    }

    private void traverseSchemaProperties(String mediaType, Schema schema, Set<String> processedModels, Map<String, Object> values) {
        if (schema.getProperties() != null) {
            for (Object propertyName : schema.getProperties().keySet()) {
                Schema property = (Schema) schema.getProperties().get(propertyName.toString());
                values.put(propertyName.toString(), resolvePropertyToExample(propertyName.toString(), mediaType, property, processedModels));
            }
        } else if (ModelUtils.isAllOf(schema) || ModelUtils.isAllOfWithProperties(schema)) {
            resolveAllOfSchemaProperties(mediaType, schema, processedModels, values);
        }
    }

    /**
     * Transverse and resolves all property examples for `allOf` composed schemas into `values` map object
     *
     * @param mediaType       MIME type
     * @param schema          OAS schema
     * @param processedModels Set containing all processed models
     * @param values          Example value map
     */
    private void resolveAllOfSchemaProperties(String mediaType, Schema schema, Set<String> processedModels, Map<String, Object> values) {
        List<Schema> interfaces = schema.getAllOf();
        for (Schema composed : interfaces) {
            traverseSchemaProperties(mediaType, composed, processedModels, values);
            if (composed.get$ref() != null) {
                String ref = ModelUtils.getSimpleRef(composed.get$ref());
                Schema resolved = ModelUtils.getSchema(openAPI, ref);
                if (resolved != null) {
                    traverseSchemaProperties(mediaType, resolved, processedModels, values);
                }
            }
        }
    }

    private boolean hasValidRef(Schema schema) {
        if (schema.get$ref() != null) {
            String ref = ModelUtils.getSimpleRef(schema.get$ref());
            Schema resolved = ModelUtils.getSchema(openAPI, ref);
            return resolved != null;
        }

        return true;
    }
}
