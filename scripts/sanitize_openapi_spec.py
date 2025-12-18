#!/usr/bin/env python3
"""
Sanitize the Metabase OpenAPI specification to make it compatible with OpenAPI Generator.

This script addresses the following issues:
1. Schema names with special characters (*, /, -, ~)
2. Deeply nested anyOf/oneOf structures
3. Schemas that reference non-existent types
4. Duplicate property definitions in merged schemas
"""

import json
import re
import sys
from pathlib import Path

# Schemas to remove entirely (problematic structures)
SCHEMAS_TO_REMOVE = {
    # Schemas with special characters that can't become valid Java class names
    "metabase.legacy-mbql.schema.*",
    "metabase.legacy-mbql.schema.-",
    "metabase.legacy-mbql.schema./",
    "mbql.clause.*",
    "mbql.clause./",
    "mbql.clause.~1",
    # Schemas that cause AnyOf/ModelNull issues
    "AnyOf",
    "ModelNull",
    "null",
    # Schemas with duplicate properties due to allOf merging
    "metabase.lib.schema.metadata.lib-or-legacy-column",
    "metabase.lib.schema.metadata.column",
    "metabase.legacy-mbql.schema..legacy-column-metadata",
    "metabase.legacy-mbql.schema..legacy-column-metadata.qualified-keys",
    "metabase.legacy-mbql.schema.legacy-column-metadata",
    # Schemas with const values used as variables
    "metabase.lib.schema.query",
    "metabase.lib.schema.join.join",
    "metabase.lib.schema.metadata.stage",
    "metabase.queries.schema.query",
}

# Pattern to identify problematic schema names
PROBLEMATIC_NAME_PATTERN = re.compile(r'[*/~]')


def is_problematic_schema_name(name: str) -> bool:
    """Check if a schema name contains problematic characters."""
    return bool(PROBLEMATIC_NAME_PATTERN.search(name))


def replace_problematic_refs(obj: any, removed_schemas: set) -> any:
    """
    Recursively replace $ref to removed schemas with a simple object type.
    """
    if isinstance(obj, dict):
        if "$ref" in obj:
            ref = obj["$ref"]
            # Handle case where $ref might not be a string
            if isinstance(ref, str):
                schema_name = ref.replace("#/components/schemas/", "")
                if schema_name in removed_schemas or is_problematic_schema_name(schema_name):
                    # Replace with a generic object type
                    return {"type": "object"}
        
        return {k: replace_problematic_refs(v, removed_schemas) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [replace_problematic_refs(item, removed_schemas) for item in obj]
    return obj


def simplify_anyof_oneof(obj: any) -> any:
    """
    Simplify anyOf/oneOf structures that are too complex.
    If anyOf/oneOf has many options, replace with a generic object.
    """
    if isinstance(obj, dict):
        # Handle anyOf with too many options
        if "anyOf" in obj and len(obj["anyOf"]) > 5:
            return {"type": "object", "description": "Complex anyOf simplified to object"}
        
        # Handle oneOf with too many options
        if "oneOf" in obj and len(obj["oneOf"]) > 5:
            return {"type": "object", "description": "Complex oneOf simplified to object"}
        
        # Handle nested anyOf within anyOf
        if "anyOf" in obj:
            simplified_items = []
            for item in obj["anyOf"]:
                if isinstance(item, dict) and ("anyOf" in item or "oneOf" in item):
                    # Flatten nested anyOf/oneOf by replacing with object
                    simplified_items.append({"type": "object"})
                else:
                    simplified_items.append(simplify_anyof_oneof(item))
            obj["anyOf"] = simplified_items
            return obj
        
        return {k: simplify_anyof_oneof(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [simplify_anyof_oneof(item) for item in obj]
    return obj


def remove_duplicate_properties(schema: dict) -> dict:
    """
    Remove schemas that would cause duplicate property issues.
    These typically come from allOf merging schemas with overlapping properties.
    """
    if not isinstance(schema, dict):
        return schema
    
    # If it's an allOf, check for duplicate properties
    if "allOf" in schema:
        seen_properties = set()
        new_allof = []
        
        for item in schema["allOf"]:
            if isinstance(item, dict):
                if "properties" in item:
                    props = set(item["properties"].keys())
                    # Check for overlap
                    overlap = props & seen_properties
                    if overlap:
                        # Remove overlapping properties from this item
                        item = item.copy()
                        item["properties"] = {
                            k: v for k, v in item["properties"].items()
                            if k not in overlap
                        }
                    seen_properties.update(props)
                new_allof.append(remove_duplicate_properties(item))
            else:
                new_allof.append(item)
        
        schema["allOf"] = new_allof
    
    # Recursively process nested structures
    for key, value in schema.items():
        if isinstance(value, dict):
            schema[key] = remove_duplicate_properties(value)
        elif isinstance(value, list):
            schema[key] = [remove_duplicate_properties(item) if isinstance(item, dict) else item for item in value]
    
    return schema


def sanitize_spec(spec: dict) -> dict:
    """
    Main sanitization function.
    """
    # Step 1: Ensure info.version exists
    if "info" not in spec:
        spec["info"] = {}
    if "version" not in spec["info"] or not spec["info"]["version"]:
        spec["info"]["version"] = "1.0.0"
    
    # Step 2: Identify all schemas to remove
    schemas_to_remove = set(SCHEMAS_TO_REMOVE)
    
    if "components" in spec and "schemas" in spec["components"]:
        schemas = spec["components"]["schemas"]
        
        # Find additional problematic schemas
        for schema_name in list(schemas.keys()):
            if is_problematic_schema_name(schema_name):
                schemas_to_remove.add(schema_name)
        
        # Step 3: Remove problematic schemas
        for schema_name in schemas_to_remove:
            if schema_name in schemas:
                del schemas[schema_name]
                print(f"Removed schema: {schema_name}")
        
        # Step 4: Remove duplicate properties in allOf schemas
        for schema_name in schemas:
            schemas[schema_name] = remove_duplicate_properties(schemas[schema_name])
        
        # Step 5: Simplify complex anyOf/oneOf
        for schema_name in schemas:
            schemas[schema_name] = simplify_anyof_oneof(schemas[schema_name])
    
    # Step 6: Replace references to removed schemas throughout the spec
    spec = replace_problematic_refs(spec, schemas_to_remove)
    
    return spec


def main():
    if len(sys.argv) < 2:
        input_file = Path(__file__).parent.parent / "src/main/resources/metabase-api-spec.json"
    else:
        input_file = Path(sys.argv[1])
    
    if len(sys.argv) < 3:
        output_file = Path(__file__).parent.parent / "src/main/resources/metabase-api-spec-sanitized.json"
    else:
        output_file = Path(sys.argv[2])
    
    print(f"Reading spec from: {input_file}")
    with open(input_file, 'r') as f:
        spec = json.load(f)
    
    print("Sanitizing spec...")
    sanitized = sanitize_spec(spec)
    
    print(f"Writing sanitized spec to: {output_file}")
    with open(output_file, 'w') as f:
        json.dump(sanitized, f, indent=2)
    
    print("Done!")


if __name__ == "__main__":
    main()
