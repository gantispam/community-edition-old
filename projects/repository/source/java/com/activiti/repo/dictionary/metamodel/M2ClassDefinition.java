package com.activiti.repo.dictionary.metamodel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.activiti.repo.dictionary.AssociationDefinition;
import com.activiti.repo.dictionary.AssociationRef;
import com.activiti.repo.dictionary.ClassDefinition;
import com.activiti.repo.dictionary.ClassRef;
import com.activiti.repo.dictionary.PropertyDefinition;
import com.activiti.repo.dictionary.PropertyRef;
import com.activiti.repo.ref.QName;


/**
 * Default Read-Only Class Definition Implementation
 * 
 * @author David Caruana
 */
public class M2ClassDefinition implements ClassDefinition
{
    /**
     * Class definition to wrap
     */
    protected M2Class m2Class;
    
    
    /**
     * Construct Read-Only Class Definition
     * 
     * @param m2Class  class definition
     * @return  read-only class definition
     */
    public static ClassDefinition create(M2Class m2Class)
    {
        if (m2Class instanceof M2Type)
        {
            return new M2TypeDefinition((M2Type)m2Class);
        }
        else if (m2Class instanceof M2Aspect)
        {
            return new M2AspectDefinition((M2Aspect)m2Class);
        }
        else
        {
            return new M2ClassDefinition(m2Class);
        }
    }
    
    
    /*package*/ M2ClassDefinition(M2Class m2Class)
    {
        this.m2Class = m2Class;

        // Force load-on-demand of related entities
        this.m2Class.getSuperClass();
        this.m2Class.getInheritedProperties();
        this.m2Class.getInheritedAssociations();
    }

    
    /*package*/ M2Class getM2Class()
    {
        return m2Class;
    }

    
    /* (non-Javadoc)
     * @see com.activiti.repo.dictionary.ClassDefinition#getReference()
     */
    public ClassRef getReference()
    {
        return m2Class.getReference();
    }

    
    /* (non-Javadoc)
     * @see com.activiti.repo.dictionary.ClassDefinition#getName()
     */
    public QName getName()
    {
        return m2Class.getName();
    }


    /* (non-Javadoc)
     * @see com.activiti.repo.dictionary.ClassDefinition#isAspect()
     */
    public boolean isAspect()
    {
        return (m2Class instanceof M2Aspect);
    }

    
    /* (non-Javadoc)
     * @see com.activiti.repo.dictionary.ClassDefinition#getSuperClass()
     */
    public ClassRef getSuperClass()
    {
        return m2Class.getSuperClass().getReference();
    }
    
    
    /* (non-Javadoc)
     * @see com.activiti.repo.dictionary.ClassDefinition#getProperties()
     */
    public Map<PropertyRef, PropertyDefinition> getProperties()
    {
        List aggregatedProperties = aggregateProperties();
        Map<PropertyRef, PropertyDefinition> propertyDefs = new HashMap<PropertyRef, PropertyDefinition>(aggregatedProperties.size());
        for (Iterator iter = aggregatedProperties.iterator(); iter.hasNext(); /**/)
        {
            M2Property m2Property = (M2Property)iter.next();
            propertyDefs.put(m2Property.getPropertyDefinition().getReference(), m2Property.getPropertyDefinition());
        }
        return Collections.unmodifiableMap(propertyDefs);
    }

    
    /**
     * Gets the full list of Properties to include in Class Definition
     * 
     * @return  properties
     */
    protected List<M2Property> aggregateProperties()
    {
        return m2Class.getInheritedProperties();
    }
    
    
    /* (non-Javadoc)
     * @see com.activiti.repo.dictionary.ClassDefinition#getAssociations()
     */
    public Map<AssociationRef, AssociationDefinition> getAssociations()
    {
        List aggregatedAssociations = aggregateAssociations();
        Map<AssociationRef, AssociationDefinition> assocDefs = new HashMap<AssociationRef, AssociationDefinition>(aggregatedAssociations.size());
        for (Iterator iter = aggregatedAssociations.iterator(); iter.hasNext(); /**/)
        {
            M2Association m2Assoc = (M2Association)iter.next();
            assocDefs.put(m2Assoc.getAssociationDefintion().getReference(), m2Assoc.getAssociationDefintion());
        }
        return Collections.unmodifiableMap(assocDefs);
    }


    /**
     * Gets the full list of Associations to include in Class Definition
     * 
     * @return  properties
     */
    protected List<M2Association> aggregateAssociations()
    {
        return m2Class.getInheritedAssociations();
    }

}
