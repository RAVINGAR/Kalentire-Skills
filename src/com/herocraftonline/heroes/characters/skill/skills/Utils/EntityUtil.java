/*
package com.herocraftonline.heroes.characters.skill.skills.Utils;


import net.minecraft.server.v1_9_R1.Entity;
import net.minecraft.server.v1_9_R1.EntityTypes;
import org.bukkit.entity.EntityType;
import java.lang.reflect.Field;
import java.util.Map;

public class EntityUtil {

    protected static Field nameToClassField, classToNameField, classToIdField, nameToIdField, idToClassField;
    //protected static Field mapIdToClassField;

    static
    {
        try
        {
            nameToClassField = EntityTypes.class.getDeclaredField("c");
            classToNameField = EntityTypes.class.getDeclaredField("d");
            idToClassField = EntityTypes.class.getDeclaredField("e");
            classToIdField = EntityTypes.class.getDeclaredField("f");
            nameToIdField = EntityTypes.class.getDeclaredField("g");

            nameToClassField.setAccessible(true);
            classToNameField.setAccessible(true);
            idToClassField.setAccessible(true);
            classToIdField.setAccessible(true);
            nameToIdField.setAccessible(true);
        }
        catch(Exception e) {e.printStackTrace();}
    }
    /**
     * Registers a custom entity with the minecraft server, allowing the server to process it and the client to render it
     * @param entityClass
     *      The entity's class
     * @param name
     *      Unique name used to represent the custom entity
     * @param id
     *      Entity ID (Used to determine Entity Type rendered by client)
     * @param makeDefault
     *      If true, this will make the custom entity the default type spawned
     */
/**
    public static void registerCustomEntity(Class<? extends Entity> entityClass, String name, int id, boolean makeDefault) {
        if (nameToClassField == null || nameToIdField == null || classToNameField == null || classToIdField == null)
        {
            return;
        }
        else
        {
            try
            {
                @SuppressWarnings("unchecked")
                Map<String, Class<? extends Entity>> nameToClassMap = (Map<String, Class<? extends Entity>>) nameToClassField.get(null);
                @SuppressWarnings("unchecked")
                Map<String, Integer> mapStringToId = (Map<String, Integer>) nameToIdField.get(null);
                @SuppressWarnings("unchecked")
                Map<Class<? extends Entity>, String> mapClasstoString = (Map<Class<? extends Entity>, String>) classToNameField.get(null);
                @SuppressWarnings("unchecked")
                Map<Class<? extends Entity>, Integer> mapClassToId = (Map<Class<? extends Entity>, Integer>) classToIdField.get(null);

                nameToClassMap.put(name, entityClass);
                mapStringToId.put(name, Integer.valueOf(id));
                mapClasstoString.put(entityClass, name);
                mapClassToId.put(entityClass, Integer.valueOf(id));

                if (makeDefault) {
                    String defName = EntityType.fromId(id).getName();
                    nameToClassMap.put(defName, entityClass);
                    @SuppressWarnings("unchecked")
                    Map<Integer, Class<? extends Entity>> mapIdToClass = (Map<Integer, Class<? extends Entity>>) idToClassField.get(null);
                    mapIdToClass.put(Integer.valueOf(id), entityClass);
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}

        **/