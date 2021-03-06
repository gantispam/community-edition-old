/*
 * #%L
 * Alfresco Solr Client
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.solr.client;

import java.util.List;

/**
 * SOLR-side representation of ACL Readers information.
 * 
 * @since 4.0
 */
public class AclReaders
{
    private final long id;

    private final List<String> readers;
    
    private final List<String> denied;

    private final long aclChangeSetId;
    
    private final String tenantDomain;

    public AclReaders(long id, List<String> readers, List<String> denied, long aclChangeSetId, String tenantDomain)
    {
        this.id = id;
        this.readers = readers;
        this.denied = denied;
        this.aclChangeSetId = aclChangeSetId;
        this.tenantDomain = tenantDomain;
    }

    @Override
    public String toString()
    {
        return "AclReaders [id=" + id + ", readers=" + readers + ", denied=" + denied + ", tenantDomain=" + tenantDomain + "]";
    }

    /**
     * ID should be enough for hashCode() and equals().
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    /**
     * ID should be enough for hashCode() and equals().
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AclReaders other = (AclReaders) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public long getId()
    {
        return id;
    }

    public List<String> getReaders()
    {
        return readers;
    }

    public List<String> getDenied()
    {
        return denied;
    }

    public long getAclChangeSetId()
    {
        return aclChangeSetId;
    }
    
    public String getTenantDomain()
    {
        return tenantDomain;
    }
}
