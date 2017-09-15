package edu.uci.ics.texera.api.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.uci.ics.texera.api.constants.DataConstants;
import edu.uci.ics.texera.api.constants.SchemaConstants;
import edu.uci.ics.texera.api.constants.DataConstants.TexeraProject;
import edu.uci.ics.texera.api.exception.StorageException;
import edu.uci.ics.texera.api.field.IField;
import edu.uci.ics.texera.api.schema.Attribute;
import edu.uci.ics.texera.api.schema.Schema;
import edu.uci.ics.texera.api.tuple.Tuple;

public class Utils {
	
	public static Path getDefaultIndexDirectory() throws StorageException {
		return getTexeraHomePath().resolve("index");
	}
    
    /**
     * Gets the path of resource files under the a subproject's resource folder (in src/main/resources)
     * 
     * @param resourcePath, the path to a resource relative to subproject/src/main/resources
     * @param subProject, the sub project where the resource is located
     * @return the path to the resource
     * @throws StorageException if finding fails
     */
    public static Path getResourcePath(String resourcePath, TexeraProject subProject) throws StorageException {
        return getTexeraHomePath()
        			.resolve(subProject.getProjectName())
        			.resolve("src/main/resources")
        			.resolve(resourcePath);
    }
    
    /**
     * Gets the real path of the texera home directory by:
     *   1): try to use TEXERA_HOME environment variable, 
     *   if it fails then:
     *   2): compare if the current directory is texera (where TEXERA_HOME should be), 
     *   if it's not then:
     *   3): compare if the current directory is a texera subproject, 
     *   if it's not then:
     *   
     *   Finding texera home directory will fail
     * 
     * @return the real absolute path to texera home directory
     * @throws StorageException if can not find texera home
     */
    public static Path getTexeraHomePath() throws StorageException {
        try {
            // try to use TEXERA_HOME environment variable first
            if (System.getenv(DataConstants.HOME_ENV_VAR) != null) {
                String texeraHome = System.getenv(DataConstants.HOME_ENV_VAR);
                return Paths.get(texeraHome).toRealPath();
            } else {
                // if the environment variable is not found, try if the current directory is texera
                Path currentWorkingDirectory = Paths.get(".").toRealPath();
                
                // if the current directory is "texera/core" (TEXERA_HOME location)
                boolean isTexeraHome = currentWorkingDirectory.endsWith("core")
                		&& currentWorkingDirectory.getParent().endsWith("texera");
                if (isTexeraHome) {
                    return currentWorkingDirectory;
                }
                
                // if the current directory is one of the sub-projects
                boolean isSubProject = Arrays.asList(TexeraProject.values()).stream()
                    .map(project -> project.getProjectName())
                    .filter(project -> currentWorkingDirectory.endsWith(project)).findAny().isPresent();
                if (isSubProject) {
                    return currentWorkingDirectory.getParent().toRealPath();
                }
                
                throw new StorageException(
                        "Finding texera home path failed. Current working directory is " + currentWorkingDirectory);
            }
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
    
    /**
    *
    * @param schema
    * @about Creating a new schema object, and adding SPAN_LIST_ATTRIBUTE to
    *        the schema. SPAN_LIST_ATTRIBUTE is of type List
    */
   public static Schema createSpanSchema(Schema schema) {
       return addAttributeToSchema(schema, SchemaConstants.SPAN_LIST_ATTRIBUTE);
   }

   /**
    * Add an attribute to an existing schema (if the attribute doesn't exist).
    * 
    * @param schema
    * @param attribute
    * @return new schema
    */
   public static Schema addAttributeToSchema(Schema schema, Attribute attribute) {
       if (schema.containsField(attribute.getAttributeName())) {
           return schema;
       }
       List<Attribute> attributes = new ArrayList<>(schema.getAttributes());
       attributes.add(attribute);
       Schema newSchema = new Schema(attributes.toArray(new Attribute[attributes.size()]));
       return newSchema;
   }
   
   /**
    * Removes one or more attributes from the schema and returns the new schema.
    * 
    * @param schema
    * @param attributeName
    * @return
    */
   public static Schema removeAttributeFromSchema(Schema schema, String... attributeName) {
       return new Schema(schema.getAttributes().stream()
               .filter(attr -> (! Arrays.asList(attributeName).contains(attr.getAttributeName())))
               .toArray(Attribute[]::new));
   }
   
   /**
    * Converts a list of attributes to a list of attribute names
    * 
    * @param attributeList, a list of attributes
    * @return a list of attribute names
    */
   public static List<String> getAttributeNames(List<Attribute> attributeList) {
       return attributeList.stream()
               .map(attr -> attr.getAttributeName())
               .collect(Collectors.toList());
   }
   
   /**
    * Converts a list of attributes to a list of attribute names
    * 
    * @param attributeList, a list of attributes
    * @return a list of attribute names
    */
   public static List<String> getAttributeNames(Attribute... attributeList) {
       return Arrays.asList(attributeList).stream()
               .map(attr -> attr.getAttributeName())
               .collect(Collectors.toList());
   }
   
   /**
    * Creates a new schema object, with "_ID" attribute added to the front.
    * If the schema already contains "_ID" attribute, returns the original schema.
    * 
    * @param schema
    * @return
    */
   public static Schema getSchemaWithID(Schema schema) {
       if (schema.containsField(SchemaConstants._ID)) {
           return schema;
       }
       
       List<Attribute> attributeList = new ArrayList<>();
       attributeList.add(SchemaConstants._ID_ATTRIBUTE);
       attributeList.addAll(schema.getAttributes());
       return new Schema(attributeList.stream().toArray(Attribute[]::new));      
   }
   
   /**
    * Remove one or more fields from each tuple in tupleList.
    * 
    * @param tupleList
    * @param removeFields
    * @return
    */
   public static List<Tuple> removeFields(List<Tuple> tupleList, String... removeFields) {
       List<Tuple> newTuples = tupleList.stream().map(tuple -> removeFields(tuple, removeFields))
               .collect(Collectors.toList());
       return newTuples;
   }
   
   /**
    * Remove one or more fields from a tuple.
    * 
    * @param tuple
    * @param removeFields
    * @return
    */
   public static Tuple removeFields(Tuple tuple, String... removeFields) {
       List<String> removeFieldList = Arrays.asList(removeFields);
       List<Integer> removedFeidsIndex = removeFieldList.stream()
               .map(attributeName -> tuple.getSchema().getIndex(attributeName)).collect(Collectors.toList());
       
       Attribute[] newAttrs = tuple.getSchema().getAttributes().stream()
               .filter(attr -> (! removeFieldList.contains(attr.getAttributeName()))).toArray(Attribute[]::new);
       Schema newSchema = new Schema(newAttrs);
       
       IField[] newFields = IntStream.range(0, tuple.getSchema().getAttributes().size())
           .filter(index -> (! removedFeidsIndex.contains(index)))
           .mapToObj(index -> tuple.getField(index)).toArray(IField[]::new);
       
       return new Tuple(newSchema, newFields);
   }

}
