/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.email.server.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailMessageException;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handler implementation address to folder node.
 * 
 * @author Yan O
 * @since 2.2
 */
public class FolderEmailMessageHandler extends AbstractEmailMessageHandler
{
    private static final String MSG_RECEIVED_BY_SMTP = "email.server.msg.received_by_smtp";
    private static final String MSG_DEFAULT_SUBJECT = "email.server.msg.default_subject";
    private static final String ERR_MAIL_READ_ERROR = "email.server.err.mail_read_error";
    
    private static final Log log = LogFactory.getLog(FolderEmailMessageHandler.class);

    /**
     * {@inheritDoc}
     */
    public void processMessage(NodeRef nodeRef, EmailMessage message)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Message is psocessing by SpaceMailMessageHandler");
        }
        try
        {
            // Check type of the node. It must be a SPACE
            QName nodeTypeQName = getNodeService().getType(nodeRef);

            if (getDictionaryService().isSubClass(nodeTypeQName, ContentModel.TYPE_FOLDER))
            {
                // Add the content into the system
                addAlfrescoContent(nodeRef, message);
            }
            else
            {
                throw new AlfrescoRuntimeException("\n" +
                        "Message handler " + this.getClass().getName() + " cannot handle type " + nodeTypeQName + ".\n" +
                        "Check the message handler mappings.");
            }
        }
        catch (IOException ex)
        {
            throw new EmailMessageException(ERR_MAIL_READ_ERROR, ex.getMessage());
        }
    }

    /**
     * Add content to Alfresco repository
     * 
     * @param spaceNodeRef          Addressed node
     * @param mailParser            Mail message
     * @throws IOException          Exception can be thrown while saving a content into Alfresco repository.
     * @throws MessagingException   Exception can be thrown while parsing e-mail message.
     */
    public void addAlfrescoContent(NodeRef spaceNodeRef, EmailMessage message) throws IOException
    {
        // Set default values for email fields
        String messageSubject = message.getSubject();
        if (messageSubject == null || messageSubject.length() == 0)
        {
            Date now = new Date();
            messageSubject = I18NUtil.getMessage(MSG_DEFAULT_SUBJECT, new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(now));
        }

        // Create main content node
        NodeRef contentNodeRef;
        contentNodeRef = addContentNode(getNodeService(), spaceNodeRef, messageSubject);
        // Add titled aspect
        addTitledAspect(contentNodeRef, messageSubject, message.getFrom());
        // Add emailed aspect
        addEmailedAspect(contentNodeRef, message);
        // Write the message content
        if (message.getBody() != null)
        {
            InputStream contentIs = message.getBody().getContent();
            // The message body is plain text, unless an extension has been provided
            MimetypeService mimetypeService = getMimetypeService();
            String mimetype = mimetypeService.guessMimetype(messageSubject);
            if (mimetype.equals(MimetypeMap.MIMETYPE_BINARY))
            {
                mimetype= MimetypeMap.MIMETYPE_TEXT_PLAIN;
            }
            // Use the default encoding.  It will get overridden if the body is text.
            String encoding = message.getBody().getEncoding();
            
            writeContent(contentNodeRef, contentIs, mimetype, encoding);
        }

        // Add attachments
        addAttachments(spaceNodeRef, contentNodeRef, message);
    }

    /**
     * Adds titled aspect to the specified node.
     * 
     * @param nodeRef Target node.
     * @param title Title
     */
    private void addTitledAspect(NodeRef nodeRef, String title, String from)
    {
        Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>();
        titledProps.put(ContentModel.PROP_TITLE, title);
        titledProps.put(ContentModel.PROP_DESCRIPTION, I18NUtil.getMessage(MSG_RECEIVED_BY_SMTP, from));
        getNodeService().addAspect(nodeRef, ContentModel.ASPECT_TITLED, titledProps);
        
        if (log.isDebugEnabled())
        {
            log.debug("Titled aspect has been added.");
        }
    }
}
