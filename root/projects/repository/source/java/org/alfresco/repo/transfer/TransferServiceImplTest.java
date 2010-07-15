/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.repo.transfer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

import javax.transaction.UserTransaction;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transfer.manifest.TransferManifestNodeFactory;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.transfer.TransferCallback;
import org.alfresco.service.cmr.transfer.TransferDefinition;
import org.alfresco.service.cmr.transfer.TransferEvent;
import org.alfresco.service.cmr.transfer.TransferEventBegin;
import org.alfresco.service.cmr.transfer.TransferException;
import org.alfresco.service.cmr.transfer.TransferReceiver;
import org.alfresco.service.cmr.transfer.TransferService;
import org.alfresco.service.cmr.transfer.TransferTarget;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.ResourceUtils;

/**
 * Unit test for TransferServiceImpl
 * 
 * Contains some unit tests for the transfer definitions.
 *
 * @author Mark Rogers
 */
@SuppressWarnings("deprecation")
public class TransferServiceImplTest extends BaseAlfrescoSpringTest 
{
    private TransferService transferService;
    private ContentService contentService;
    private TransferServiceImpl transferServiceImpl;
    private SearchService searchService;
    private TransactionService transactionService;
    private TransferReceiver receiver;
    private TransferManifestNodeFactory transferManifestNodeFactory; 
    private PermissionService permissionService;
    
    String COMPANY_HOME_XPATH_QUERY = "/{http://www.alfresco.org/model/application/1.0}company_home";
    String GUEST_HOME_XPATH_QUERY = "/{http://www.alfresco.org/model/application/1.0}company_home/{http://www.alfresco.org/model/application/1.0}guest_home";

    
    
    @Override
    public void runBare() throws Throwable
    {
        preventTransaction();
        super.runBare();
    }

    /**
     * Called during the transaction setup
     */
    protected void onSetUp() throws Exception
    {
        super.onSetUp();
        
        // Get the required services
        this.transferService = (TransferService)this.applicationContext.getBean("TransferService");
        this.contentService = (ContentService)this.applicationContext.getBean("ContentService");
        this.transferServiceImpl = (TransferServiceImpl)this.applicationContext.getBean("transferService");
        this.searchService = (SearchService)this.applicationContext.getBean("SearchService");
        this.transactionService = (TransactionService)this.applicationContext.getBean("TransactionService");
        this.nodeService = (NodeService) this.applicationContext.getBean("nodeService");
        this.contentService = (ContentService) this.applicationContext.getBean("contentService");
        this.authenticationService = (MutableAuthenticationService) this.applicationContext.getBean("authenticationService");
        this.actionService = (ActionService)this.applicationContext.getBean("actionService");
        this.permissionService = (PermissionService)this.applicationContext.getBean("permissionService");
        this.receiver = (TransferReceiver)this.applicationContext.getBean("transferReceiver");
        this.transferManifestNodeFactory = (TransferManifestNodeFactory)this.applicationContext.getBean("transferManifestNodeFactory");
        this.authenticationComponent = (AuthenticationComponent) this.applicationContext.getBean("authenticationComponent");
        authenticationComponent.setSystemUserAsCurrentUser();
        setTransactionDefinition(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        assertNotNull("receiver is null", this.receiver);
        
    }
    
    /**
     * Test create target.
     * 
     * @throws Exception
     */
    public void testCreateTarget() throws Exception
    {
        String name = "name";
        String title = "title";
        String description = "description";
        String endpointProtocol = "http";
        String endpointHost = "localhost";
        int endpointPort = 8080;
        String endpointPath = "rhubarb";
        String username = "admin";
        char[] password = "password".toCharArray();
        
        
        startNewTransaction();
        try 
        {
            /**
             * Now go ahead and create our first transfer target
             */
            TransferTarget ret = transferService.createAndSaveTransferTarget(name, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
            assertNotNull("return value is null", ret);
            assertNotNull("node ref is null", ret.getNodeRef());
            
            //titled aspect
            assertEquals("name not equal", ret.getName(), name);
            assertEquals("title not equal", ret.getTitle(), title);
            assertEquals("description not equal", ret.getDescription(), description);
            
            // endpoint 
            assertEquals("endpointProtocol not equal", ret.getEndpointProtocol(), endpointProtocol);
            assertEquals("endpointHost not equal", ret.getEndpointHost(), endpointHost);
            assertEquals("endpointPort not equal", ret.getEndpointPort(), endpointPort);
            assertEquals("endpointPath not equal", ret.getEndpointPath(), endpointPath);
            
            // authentication
            assertEquals("username not equal", ret.getUsername(), username);
            char[] password2 = ret.getPassword();
            
            assertEquals(password.length, password2.length);
            
            for(int i = 0; i < password.length; i++)
            {
                if(password[i] != password2[i])
                {
                    fail("password not equal:" + new String(password) + new String(password2));
                }
            }

            
            /**
             * Negative test - try to create a transfer target with a name that's already used.
             */
            transferService.createAndSaveTransferTarget(name, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
            fail("duplicate name not detected");
        }
        catch (TransferException e)
        {
            // expect to go here
        }
        finally
        {
            endTransaction();
        }
    }
    
    /**
     * Test create target via in memory data object.
     * 
     * @throws Exception
     */
    public void testCreateTargetSyntax2() throws Exception
    {
        String name = "nameSyntax2";
        String title = "title";
        String description = "description";
        String endpointProtocol = "http";
        String endpointHost = "localhost";
        int endpointPort = 8080;
        String endpointPath = "rhubarb";
        String username = "admin";
        char[] password = "password".toCharArray();
        
        
        startNewTransaction();
        try 
        {
            /**
             * Now go ahead and create our first transfer target
             */
            TransferTarget newValue = transferService.createTransferTarget(name);
            newValue.setDescription(description);
            newValue.setEndpointHost(endpointHost);
            newValue.setEndpointPort(endpointPort);
            newValue.setEndpointPath(endpointPath);
            newValue.setEndpointProtocol(endpointProtocol);
            newValue.setPassword(password);
            newValue.setTitle(title);
            newValue.setUsername(username);
            
            TransferTarget ret = transferService.saveTransferTarget(newValue);
            
            assertNotNull("return value is null", ret);
            assertNotNull("node ref is null", ret.getNodeRef());
            
            //titled aspect
            assertEquals("name not equal", ret.getName(), name);
            assertEquals("title not equal", ret.getTitle(), title);
            assertEquals("description not equal", ret.getDescription(), description);
            
            // endpoint 
            assertEquals("endpointProtocol not equal", ret.getEndpointProtocol(), endpointProtocol);
            assertEquals("endpointHost not equal", ret.getEndpointHost(), endpointHost);
            assertEquals("endpointPort not equal", ret.getEndpointPort(), endpointPort);
            assertEquals("endpointPath not equal", ret.getEndpointPath(), endpointPath);
            
            // authentication
            assertEquals("username not equal", ret.getUsername(), username);
            char[] password2 = ret.getPassword();
            
            assertEquals(password.length, password2.length);
            
            for(int i = 0; i < password.length; i++)
            {
                if(password[i] != password2[i])
                {
                    fail("password not equal:" + new String(password) + new String(password2));
                }
            }

            
            /**
             * Negative test - try to create a transfer target with a name that's already used.
             */
            transferService.createAndSaveTransferTarget(name, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
            fail("duplicate name not detected");
        }
        catch (TransferException e)
        {
            // expect to go here
        }
        finally
        {
            endTransaction();
        }
    }
    
    /**
     * Test of Get TransferTargets
     * 
     * @throws Exception
     */
    public void testGetTransferTargets() throws Exception
    {
        String nameA = "nameA";
        String nameB = "nameB";
        String title = "title";
        String description = "description";
        String endpointProtocol = "http";
        String endpointHost = "localhost";
        int endpointPort = 8080;
        String endpointPath = "rhubarb";
        String username = "admin";
        char[] password = "password".toCharArray();
        
        startNewTransaction();
        try
        {
            /**
             * Now go ahead and create our first transfer target
             */
            TransferTarget targetA = transferService.createAndSaveTransferTarget(nameA, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
            TransferTarget targetB = transferService.createAndSaveTransferTarget(nameB, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
            
            Set<TransferTarget> targets = transferService.getTransferTargets();
            assertTrue("targets is empty", targets.size() > 0);
            assertTrue("didn't find target A", targets.contains(targetA) );
            assertTrue("didn't find target B", targets.contains(targetB));
            for(TransferTarget target : targets)
            {
                System.out.println("found target: " + target.getName());
            }
        }
        finally
        {
            endTransaction();
        }
    }
    
    /**
     * Test of Get All Trabsfer Targets By Group
     */
    //TODO Test not complete - can't yet put targets in different groups
    public void testGetAllTransferTargetsByGroup() throws Exception
    {
        String getMe = "getMe";
        String title = "title";
        String description = "description";
        String endpointProtocol = "http";
        String endpointHost = "localhost";
        int endpointPort = 8080;
        String endpointPath = "rhubarb";
        String username = "admin";
        char[] password = "password".toCharArray();
        
        startNewTransaction();
        try
        {
            /**
             * Now go ahead and create our first transfer target
             */
            transferService.createAndSaveTransferTarget(getMe, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
            
            Set<TransferTarget> targets = transferService.getTransferTargets("Default Group");
            assertTrue("targets is empty", targets.size() > 0);     
            /**
             * Negative test - group does not exist
             */
            targets = transferService.getTransferTargets("Rubbish");
            assertTrue("targets is empty", targets.size() > 0);
            fail("group does not exist");
        }
        catch (TransferException te)
        {
            // expect to go here
        }
        finally
        {
            endTransaction();
        }
    }
    
    /**
     * 
     */
    public void testUpdateTransferTarget() throws Exception
    {
        startNewTransaction();
        try
        {
            String updateMe = "updateMe";
            String title = "title";
            String description = "description";
            String endpointProtocol = "http";
            String endpointHost = "localhost";
            int endpointPort = 8080;
            String endpointPath = "rhubarb";
            String username = "admin";
            char[] password = "password".toCharArray();
    
              
            /**
             * Create our transfer target
             */
            TransferTarget target = transferService.createAndSaveTransferTarget(updateMe, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
            
            /*
             * Now update with exactly the same values.
             */
            TransferTarget update1 = transferService.saveTransferTarget(target);
            
            assertNotNull("return value is null", update1);
            assertNotNull("node ref is null", update1.getNodeRef());
            
            //titled aspect
            assertEquals("name not equal", update1.getName(), updateMe);
            assertEquals("title not equal", update1.getTitle(), title);
            assertEquals("description not equal", update1.getDescription(), description);
            
            // endpoint 
            assertEquals("endpointProtocol not equal", update1.getEndpointProtocol(), endpointProtocol);
            assertEquals("endpointHost not equal", update1.getEndpointHost(), endpointHost);
            assertEquals("endpointPort not equal", update1.getEndpointPort(), endpointPort);
            assertEquals("endpointPath not equal", update1.getEndpointPath(), endpointPath);
            
            // authentication
            assertEquals("username not equal", update1.getUsername(), username);
            char[] pass = update1.getPassword();
            
            assertEquals(password.length, pass.length);
            
            for(int i = 0; i < password.length; i++)
            {
                if(password[i] != pass[i])
                {
                    fail("password not equal:" + new String(password) + new String(pass));
                }
            }
            
            /**
             * Now update with different values
             */
            String title2 = "Two";
            String description2 = "descriptionTwo";
            String endpointProtocol2 = "https";
            String endpointHost2 = "1.0.0.127";
            int endpointPort2 = 4040;
            String endpointPath2 = "custard";
            String username2 = "admin_two";
            char[] password2 = "two".toCharArray();
            
            target.setDescription(description2);
            target.setTitle(title2);
            target.setEndpointHost(endpointHost2);
            target.setEndpointPath(endpointPath2);
            target.setEndpointPort(endpointPort2);
            target.setEndpointProtocol(endpointProtocol2);
            target.setPassword(password2);
            target.setUsername(username2);
            
            TransferTarget update2 = transferService.saveTransferTarget(target);
            assertNotNull("return value is null", update2);
            assertNotNull("node ref is null", update2.getNodeRef());
            
            //titled aspect
            assertEquals("name not equal", update2.getName(), updateMe);
            assertEquals("title not equal", update2.getTitle(), title2);
            assertEquals("description not equal", update2.getDescription(), description2);
            
            // endpoint 
            assertEquals("endpointProtocol not equal", update2.getEndpointProtocol(), endpointProtocol2);
            assertEquals("endpointHost not equal", update2.getEndpointHost(), endpointHost2);
            assertEquals("endpointPort not equal", update2.getEndpointPort(), endpointPort2);
            assertEquals("endpointPath not equal", update2.getEndpointPath(), endpointPath2);
            
            // authentication
            assertEquals("username not equal", update2.getUsername(), username2);
            pass = update2.getPassword();
            
            assertEquals(password2.length, pass.length);
            
            for(int i = 0; i < pass.length; i++)
            {
                if(pass[i] != password2[i])
                {
                    fail("password not equal:" + new String(pass) + new String(password2));
                }
            }
        }
        finally
        {
            endTransaction();
        }
    }
    
    /**
     * 
     */
    public void testDeleteTransferTarget() throws Exception
    {
        startNewTransaction();
        try
        {
            String deleteMe = "deleteMe";
            String title = "title";
            String description = "description";
            String endpointProtocol = "http";
            String endpointHost = "localhost";
            int endpointPort = 8080;
            String endpointPath = "rhubarb";
            String username = "admin";
            char[] password = "password".toCharArray();
            
            /**
             * Now go ahead and create our first transfer target
             */
            transferService.createAndSaveTransferTarget(deleteMe, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
                            
            transferService.deleteTransferTarget(deleteMe);
            
            /**
             * Negative test - try to delete the transfer target we deleted above.
             */
            try 
            {
                transferService.deleteTransferTarget(deleteMe);
                fail("duplicate name not detected");
            }
            catch (TransferException e)
            {
                // expect to go here
            }   
            
            /**
             * Negative test - try to delete a transfer target that has never existed
             */
            try 
            {
                transferService.deleteTransferTarget("rubbish");
               
                fail("rubbish deleted");
            }
            catch (TransferException e)
            {
                // expect to go here
            }
        }
        finally
        {
            endTransaction();
        }
    }
    
    public void testEnableTransferTarget() throws Exception
    {
        startNewTransaction();
        try
        {
            String targetName = "enableMe";
            
            /**
             * Now go ahead and create our first transfer target
             */
            TransferTarget enableMe = createTransferTarget(targetName);
            try 
            {
                /**
                 * Check a new target is enabled
                 */
                TransferTarget target = transferService.getTransferTarget(targetName);
                assertTrue("new target is not enabled", enableMe.isEnabled());
            
                /**
                 * Diasble the target
                 */
                transferService.enableTransferTarget(targetName, false);
                target = transferService.getTransferTarget(targetName);
                assertFalse("target is not disabled", target.isEnabled());
            
                /**
                 * Now re-enable the target
                 */
                transferService.enableTransferTarget(targetName, true);
                target = transferService.getTransferTarget(targetName);
                assertTrue("re-enabled target is not enabled", target.isEnabled());
            }
            finally
            {
                transferService.deleteTransferTarget(targetName);
            }
        }
        finally
        {
            endTransaction();
        }
    }
        
    /**
     * Test the transfer method by sending one node.
     * 
     * This is a unit test so it does some shenanigans to send to the same instance of alfresco.
     */
    public void testTransferOneNode() throws Exception
    {
        setDefaultRollback(false);
        
        String CONTENT_TITLE = "ContentTitle";
        String CONTENT_TITLE_UPDATED = "ContentTitleUpdated";
        Locale CONTENT_LOCALE = Locale.GERMAN; 
        String CONTENT_STRING = "Hello";

        /**
         *  For unit test 
         *  - replace the HTTP transport with the in-process transport
         *  - replace the node factory with one that will map node refs, paths etc.
         */
        TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(receiver, contentService, transactionService);
        transferServiceImpl.setTransmitter(transmitter);
        UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
        transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
        List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
        // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
        pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
          
        /**
          * Now go ahead and create our first transfer target
          */
        String targetName = "testTransferOneNode";
        TransferTarget transferMe;
        NodeRef contentNodeRef;
        NodeRef destNodeRef;
        
        startNewTransaction();
        try
        {
            /**
              * Get guest home
              */
            String guestHomeQuery = "/app:company_home/app:guest_home";
            ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
            assertEquals("", 1, guestHomeResult.length());
            NodeRef guestHome = guestHomeResult.getNodeRef(0); 
    
            /**
             * Create a test node that we will read and write
             */
            
            String name = GUID.generate();
            ChildAssociationRef child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(name), ContentModel.TYPE_CONTENT);
            contentNodeRef = child.getChildRef();
            nodeService.setProperty(contentNodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
            nodeService.setProperty(contentNodeRef, ContentModel.PROP_NAME, name);
            
            ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
            writer.setLocale(CONTENT_LOCALE);
            writer.putContent(CONTENT_STRING);
              
            if(!transferService.targetExists(targetName))
            {
                transferMe = createTransferTarget(targetName);
            }
            else
            {
                transferMe = transferService.getTransferTarget(targetName);
            }
        }
        finally
        {
            endTransaction();
        }
        
        logger.debug("First transfer - create new node (no content yet)");
        startNewTransaction();
        try 
        {
           /**
             * Transfer our transfer target node which has no content
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    Set<NodeRef>nodes = new HashSet<NodeRef>();
                    nodes.add(contentNodeRef);
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
            
        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(contentNodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref does not exist", nodeService.exists(destNodeRef));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE); 
            assertEquals("type is wrong", nodeService.getType(contentNodeRef), nodeService.getType(destNodeRef));
            
            // Check the modified time of the destination node is the same as the source node.
            Date destModifiedDate = (Date)nodeService.getProperty(destNodeRef, ContentModel.PROP_MODIFIED);
            Date srcModifiedDate = (Date)nodeService.getProperty(contentNodeRef, ContentModel.PROP_MODIFIED);
            
            logger.debug("srcModifiedDate : " + srcModifiedDate + " destModifiedDate : " + destModifiedDate);
            assertTrue("dest modified date is not correct", destModifiedDate.compareTo(srcModifiedDate)== 0);
            
            Date destCreatedDate = (Date)nodeService.getProperty(destNodeRef, ContentModel.PROP_CREATED);
            Date srcCreatedDate = (Date)nodeService.getProperty(contentNodeRef, ContentModel.PROP_CREATED);
            
            logger.debug("srcCreatedDate : " + srcCreatedDate + " destCreatedDate : " + destCreatedDate);
            assertTrue("dest created date is not correct", destCreatedDate.compareTo(srcCreatedDate)== 0);
      
            
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 
            
            // Now set up the next test which is to change the title 
            nodeService.setProperty(contentNodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE_UPDATED);   
        }
        finally
        {
            endTransaction();
        }
        
        logger.debug("Second transfer - update title property (no content yet)");
        startNewTransaction();
        try
        {
            /**
             * Transfer our node again - so this is an update of the title property
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(contentNodeRef);
                definition.setNodes(nodes);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref does not exist", nodeService.exists(destNodeRef));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE_UPDATED); 
            assertEquals("type is wrong", nodeService.getType(contentNodeRef), nodeService.getType(destNodeRef));
            
            // Check the modified time of the destination node is the same as the source node.
            Date destModifiedDate = (Date)nodeService.getProperty(destNodeRef, ContentModel.PROP_MODIFIED);
            Date srcModifiedDate = (Date)nodeService.getProperty(contentNodeRef, ContentModel.PROP_MODIFIED);

            logger.debug("srcModifiedDate : " + srcModifiedDate + " destModifiedDate : " + destModifiedDate);
            assertTrue("after update, modified date is not correct", destModifiedDate.compareTo(srcModifiedDate) == 0);
            
            Date destCreatedDate = (Date)nodeService.getProperty(destNodeRef, ContentModel.PROP_CREATED);
            Date srcCreatedDate = (Date)nodeService.getProperty(contentNodeRef, ContentModel.PROP_CREATED);
            
            logger.debug("srcCreatedDate : " + srcCreatedDate + " destCreatedDate : " + destCreatedDate);
            assertTrue("after update, created date is not correct", destCreatedDate.compareTo(srcCreatedDate)== 0);
      
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 

        }
        finally
        {
            endTransaction();
        }
        
        logger.debug("Transfer again - this is an update");
        startNewTransaction();
        try
        {
            /**
             * Transfer our node again - so this is an update
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(contentNodeRef);
                definition.setNodes(nodes);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
  
        /**
         * Now transfer nothing - content items do not need to be transferred since its already on 
         * the destination.
         */
        logger.debug("Transfer again - with no new content");
        startNewTransaction();
        try
        {
            TransferDefinition definition = new TransferDefinition();
            Set<NodeRef>nodes = new HashSet<NodeRef>();
            nodes.add(contentNodeRef);
            definition.setNodes(nodes);
            transferService.transfer(targetName, definition);
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try
        {
            // Now validate that the target node still exists and in particular that the old content is still there
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref does not exist", nodeService.exists(destNodeRef));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE_UPDATED); 
            assertEquals("type is wrong", nodeService.getType(contentNodeRef), nodeService.getType(destNodeRef));
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 

        }
        finally
        {
            endTransaction();
        }
        
        /**
          * Negative test transfer nothing
          */
        logger.debug("Transfer again - with no content - should throw exception");
        try
        {
                TransferDefinition definition = new TransferDefinition();
                transferService.transfer(targetName, definition);
                fail("exception not thrown");
        }
        catch(TransferException te)
        {
                // expect to go here
        }
    }
    
    /**
     * Test the transfer method by sending a graph of nodes.
     * 
     * This is a unit test so it does some shenanigans to send to he same instance of alfresco.
     */
    public void testManyNodes() throws Exception
    {
        setDefaultRollback(false);
        
        String CONTENT_TITLE = "ContentTitle";
        String CONTENT_TITLE_UPDATED = "ContentTitleUpdated";
        String CONTENT_NAME = "Demo Node 1";
        Locale CONTENT_LOCALE = Locale.GERMAN; 
        String CONTENT_STRING = "The quick brown fox";
        Set<NodeRef>nodes = new HashSet<NodeRef>();

        String targetName = "testManyNodes";
        
        NodeRef nodeA;
        NodeRef nodeB;
        NodeRef nodeAA;
        NodeRef nodeAB;
        NodeRef nodeABA;
        NodeRef nodeABB;
        NodeRef nodeABC;
        
        ChildAssociationRef child;
        
        /**
         *  For unit test 
         *  - replace the HTTP transport with the in-process transport
         *  - replace the node factory with one that will map node refs, paths etc.
         */
        TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(this.receiver, this.contentService, transactionService);
        transferServiceImpl.setTransmitter(transmitter);
        UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
        transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
        List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
        // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
        pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
          
        TransferTarget transferMe;
        
        startNewTransaction();
        try
        {
            /**
              * Get guest home
              */
            String guestHomeQuery = "/app:company_home/app:guest_home";
            ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
            assertEquals("", 1, guestHomeResult.length());
            NodeRef guestHome = guestHomeResult.getNodeRef(0); 
    
            /**
             * Create a test node that we will read and write
             */
            String guid = GUID.generate();
            
            /**
             * Create a tree
             * ManyNodesRoot
             * AC (Content Node)
             * A (Folder)
             * --AA
             * --AB (Folder)
             * ----ABA (Folder)
             * -------- 100+ nodes
             * ----ABB
             * ----ABC   
             * B
             */
            
             child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(guid), ContentModel.TYPE_FOLDER);
             NodeRef testRootNode = child.getChildRef();
             nodeService.setProperty(testRootNode , ContentModel.PROP_TITLE, guid);   
             nodeService.setProperty(testRootNode , ContentModel.PROP_NAME, guid);
             nodes.add(testRootNode);
         
             child = nodeService.createNode(testRootNode, ContentModel.ASSOC_CONTAINS, QName.createQName("testNodeAC"), ContentModel.TYPE_CONTENT);
             NodeRef nodeAC = child.getChildRef();
             nodeService.setProperty(nodeAC , ContentModel.PROP_TITLE, CONTENT_TITLE + "AC");   
             nodeService.setProperty(nodeAC , ContentModel.PROP_NAME, "DemoNodeAC");
         
             {
                 ContentWriter writer = contentService.getWriter(nodeAC , ContentModel.PROP_CONTENT, true);
                 writer.setLocale(CONTENT_LOCALE);
                 writer.putContent(CONTENT_STRING);
                 nodes.add(nodeAC);
             }
         
             child = nodeService.createNode(testRootNode, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,"testNodeA"), ContentModel.TYPE_FOLDER);
             nodeA = child.getChildRef();
             nodeService.setProperty(nodeA , ContentModel.PROP_TITLE, "TestNodeA");   
             nodeService.setProperty(nodeA , ContentModel.PROP_NAME, "TestNodeA");
             nodes.add(nodeA);
         
             child = nodeService.createNode(testRootNode, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,"testNodeB"), ContentModel.TYPE_FOLDER);
             nodeB = child.getChildRef();
             nodeService.setProperty(nodeB , ContentModel.PROP_TITLE, "TestNodeB");   
             nodeService.setProperty(nodeB , ContentModel.PROP_NAME, "TestNodeB");
             nodes.add(nodeB);
         
             child = nodeService.createNode(nodeA, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,"testNodeAA"), ContentModel.TYPE_FOLDER);
             nodeAA = child.getChildRef();
             nodeService.setProperty(nodeAA , ContentModel.PROP_TITLE, CONTENT_TITLE);   
             nodeService.setProperty(nodeAA , ContentModel.PROP_NAME, "DemoNodeAA" );
             nodes.add(nodeAA);
         
             child = nodeService.createNode(nodeA, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,"testNodeAB"), ContentModel.TYPE_FOLDER);
             nodeAB = child.getChildRef();
             nodeService.setProperty(nodeAB , ContentModel.PROP_TITLE, CONTENT_TITLE);   
             nodeService.setProperty(nodeAB , ContentModel.PROP_NAME, "DemoNodeAB" );
             nodes.add(nodeAB);
         
             child = nodeService.createNode(nodeAB, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,"testNodeABA"), ContentModel.TYPE_FOLDER);
             nodeABA = child.getChildRef();
             nodeService.setProperty(nodeABA , ContentModel.PROP_TITLE, CONTENT_TITLE);   
             nodeService.setProperty(nodeABA , ContentModel.PROP_NAME, "DemoNodeABA" );
             nodes.add(nodeABA);
         
             child = nodeService.createNode(nodeAB, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,"testNodeABB"), ContentModel.TYPE_FOLDER);
             nodeABB = child.getChildRef();
             nodeService.setProperty(nodeABB , ContentModel.PROP_TITLE, CONTENT_TITLE);   
             nodeService.setProperty(nodeABB , ContentModel.PROP_NAME, "DemoNodeABB" );
             nodes.add(nodeABB);
         
             child = nodeService.createNode(nodeAB, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,"testNodeABC"), ContentModel.TYPE_FOLDER);
             nodeABC = child.getChildRef();
             nodeService.setProperty(nodeABC , ContentModel.PROP_TITLE, CONTENT_TITLE);   
             nodeService.setProperty(nodeABC , ContentModel.PROP_NAME, "DemoNodeABC" );
             nodes.add(nodeABC);
             
             /**
              * Now go ahead and create our first transfer target
              */
             if(!transferService.targetExists(targetName))
             {
                 transferMe = createTransferTarget(targetName);
             }
             else
             {
                 transferMe = transferService.getTransferTarget(targetName);
             }       
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try 
        {
           /**
             * Transfer our transfer target nodes 
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
            
        NodeRef destNodeA;
        NodeRef destNodeB;
        NodeRef destNodeAA;
        NodeRef destNodeAB;
        NodeRef destNodeABA;
        NodeRef destNodeABB;
        NodeRef destNodeABC;
        
        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeA = testNodeFactory.getMappedNodeRef(nodeA);
            assertFalse("unit test stuffed up - comparing with self", destNodeA.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref A does not exist", nodeService.exists(destNodeA));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeA, ContentModel.PROP_TITLE), "TestNodeA"); 
            assertEquals("type is wrong", nodeService.getType(nodeA), nodeService.getType(destNodeA));
            
            destNodeB = testNodeFactory.getMappedNodeRef(nodeB);
            assertTrue("dest node B does not exist", nodeService.exists(destNodeB));
            
            destNodeAA = testNodeFactory.getMappedNodeRef(nodeAA);
            assertTrue("dest node AA ref does not exist", nodeService.exists(destNodeAA));
            
            destNodeAB = testNodeFactory.getMappedNodeRef(nodeAB);
            assertTrue("dest node AB ref does not exist", nodeService.exists(destNodeAB));
            
            destNodeABA = testNodeFactory.getMappedNodeRef(nodeABA);
            assertTrue("dest node ABA ref does not exist", nodeService.exists(destNodeABA));
            
            destNodeABB = testNodeFactory.getMappedNodeRef(nodeABB);
            assertTrue("dest node ABB ref does not exist", nodeService.exists(destNodeABB));
            
            destNodeABC = testNodeFactory.getMappedNodeRef(nodeABC);
            assertTrue("dest node ABC ref does not exist", nodeService.exists(destNodeABC));
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try
        {
            /**
             * Update a single node (NodeAB) from the middle of the tree
             */
            {
                nodeService.setProperty(nodeAB , ContentModel.PROP_TITLE, CONTENT_TITLE_UPDATED);   
                
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>toUpdate = new HashSet<NodeRef>();
                toUpdate.add(nodeAB);
                definition.setNodes(toUpdate);
                transferService.transfer(targetName, definition);
                
//                assertEquals("title is wrong", (String)nodeService.getProperty(destNodeAB, ContentModel.PROP_TITLE), CONTENT_TITLE_UPDATED);        
            }  
        }
        finally
        {
            endTransaction();
        }
        
        
        startNewTransaction();
        try 
        {
            
            /**
             * Now generate a large number of nodes
             */
            
            for(int i = 0; i < 100; i++)
            {
                child = nodeService.createNode(nodeABA, ContentModel.ASSOC_CONTAINS, QName.createQName(GUID.generate() + i), ContentModel.TYPE_CONTENT);
            
                NodeRef nodeX = child.getChildRef();
                nodeService.setProperty(nodeX , ContentModel.PROP_TITLE, CONTENT_TITLE + i);   
                nodeService.setProperty(nodeX , ContentModel.PROP_NAME, CONTENT_NAME +i);
                nodes.add(nodeX);
                
                ContentWriter writer = contentService.getWriter(nodeX, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING + i);
            }
            
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try
        {
            /**
             * Transfer our transfer target nodes 
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            } 
            

        }
        finally     
        {
            transferService.deleteTransferTarget(targetName);
            endTransaction();
        }
    } // end many nodes

    
    /**
     * Test the path based update.
     * 
     * This is a unit test so it does some shenanigans to send to the same instance of alfresco.
     */
    public void testPathBasedUpdate() throws Exception
    {
        setDefaultRollback(false);
        
        /**
         *  For unit test 
         *  - replace the HTTP transport with the in-process transport
         *  - replace the node factory with one that will map node refs, paths etc.
         */
       TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(this.receiver, this.contentService, transactionService);
       transferServiceImpl.setTransmitter(transmitter);
       UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
       transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
       List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
       // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
       pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
         
       String CONTENT_TITLE = "ContentTitle";
       String CONTENT_TITLE_UPDATED = "ContentTitleUpdated";
       String CONTENT_NAME = GUID.generate();
       Locale CONTENT_LOCALE = Locale.GERMAN; 
       String CONTENT_STRING = "Hello";
       String targetName = GUID.generate();
       NodeRef contentNodeRef;
       NodeRef newContentNodeRef;
       
       QName TEST_QNAME = QName.createQName(CONTENT_NAME);
       NodeRef guestHome;
       ChildAssociationRef child;
       
       startNewTransaction();
        try
        {
            /**
              * Get guest home
              */
            String guestHomeQuery = "/app:company_home/app:guest_home";
            ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
            assertEquals("", 1, guestHomeResult.length());
            guestHome = guestHomeResult.getNodeRef(0); 
    
            /**
             * Create a test node that we will transfer.   Its path is what is important
             */
            child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, TEST_QNAME, ContentModel.TYPE_CONTENT);
            contentNodeRef = child.getChildRef();
            nodeService.setProperty(contentNodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
            nodeService.setProperty(contentNodeRef, ContentModel.PROP_NAME, CONTENT_NAME);
            
            ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
            writer.setLocale(CONTENT_LOCALE);
            writer.putContent(CONTENT_STRING);
             
            /**
             * Now go ahead and create our first transfer target
             */
            if(!transferService.targetExists(targetName))
            {
                createTransferTarget(targetName);
            }
            else
            {
                transferService.getTransferTarget(targetName);
            }   
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try 
        {
           /**
             * Transfer our transfer target node
             */     
           TransferDefinition definition = new TransferDefinition();
           Set<NodeRef>nodes = new HashSet<NodeRef>();
           nodes.add(contentNodeRef);
           definition.setNodes(nodes);
           transferService.transfer(targetName, definition);
       }
       finally     
       {
           endTransaction();
       }
       
       startNewTransaction();
       try 
       {    
            // Now validate that the target node exists and has similar properties to the source
            NodeRef destNodeRef = testNodeFactory.getMappedNodeRef(contentNodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(contentNodeRef));
            assertTrue("dest node ref does not exist", nodeService.exists(destNodeRef));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE); 
            assertEquals("type is wrong", nodeService.getType(contentNodeRef), nodeService.getType(destNodeRef));
                                   
            /**
             * Now delete the content node and re-create another one with the old path
             */
            nodeService.deleteNode(contentNodeRef);
            
            child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, TEST_QNAME, ContentModel.TYPE_CONTENT);
            newContentNodeRef = child.getChildRef();
            nodeService.setProperty(newContentNodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE_UPDATED); 
            
            /**
             * Transfer our node which is a new node (so will not exist on the back end) with a path that already has a node.
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    Set<NodeRef>nodes = new HashSet<NodeRef>();
                    nodes.add(newContentNodeRef);
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            }
       } 
       finally     
       {
           endTransaction();
       }
            
       startNewTransaction();
       try 
       {    
           NodeRef oldDestNodeRef = testNodeFactory.getMappedNodeRef(contentNodeRef);
           NodeRef newDestNodeRef = testNodeFactory.getMappedNodeRef(newContentNodeRef);
           
           // Now validate that the target node does not exist - it should have 
           // been updated by path.
           assertFalse("unit test stuffed up - comparing with self", oldDestNodeRef.equals(newDestNodeRef));
           assertFalse("new dest node ref exists", nodeService.exists(newDestNodeRef));
           assertTrue("old dest node does not exists", nodeService.exists(oldDestNodeRef));
           
           assertEquals("title is wrong", (String)nodeService.getProperty(oldDestNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE_UPDATED); 
//         assertEquals("type is wrong", nodeService.getType(contentNodeRef), nodeService.getType(destNodeRef));
        }
        finally     
        {
            endTransaction();
        }
    } // Path based update

    
    /**
     * Test the transfer method when it is running async.
     * 
     * This is a unit test so it does some shenanigans to send to the same instance of alfresco.
     */
    public void testAsyncCallback() throws Exception
    {
        int MAX_SLEEPS = 5;
       
        /**
          * Get guest home
          */
        String guestHomeQuery = "/app:company_home/app:guest_home";
        ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
        assertEquals("", 1, guestHomeResult.length());
        NodeRef guestHome = guestHomeResult.getNodeRef(0); 

        /**
          *  For unit test 
          *  - replace the HTTP transport with the in-process transport
          *  - replace the node factory with one that will map node refs, paths etc.
          */
        TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(this.receiver, this.contentService, transactionService);
        transferServiceImpl.setTransmitter(transmitter);
        UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
        transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
        List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
        // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
        pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
          
        /**
          * Now go ahead and create our first transfer target
          * This needs to be committed before we can call transfer asycnc.
          */
        String CONTENT_TITLE = "ContentTitle";
        String CONTENT_NAME_A = "Demo Node A";
        String CONTENT_NAME_B = "Demo Node B";
        Locale CONTENT_LOCALE = Locale.GERMAN; 
        String CONTENT_STRING = "Hello";
        
        NodeRef nodeRefA = null;
        NodeRef nodeRefB = null;
        String targetName = "testAsyncCallback";
        {
            UserTransaction trx = transactionService.getNonPropagatingUserTransaction();
            trx.begin();
            try
            {
                nodeRefA = nodeService.getChildByName(guestHome, ContentModel.ASSOC_CONTAINS, CONTENT_NAME_A);
                
                if(nodeRefA == null)
                {
                    /**
                     * Create a test node that we will read and write
                     */
                    ChildAssociationRef child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(GUID.generate()), ContentModel.TYPE_CONTENT);
                    nodeRefA = child.getChildRef();
                    nodeService.setProperty(nodeRefA, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                    nodeService.setProperty(nodeRefA, ContentModel.PROP_NAME, CONTENT_NAME_A);
            
                    ContentWriter writer = contentService.getWriter(nodeRefA, ContentModel.PROP_CONTENT, true);
                    writer.setLocale(CONTENT_LOCALE);
                    writer.putContent(CONTENT_STRING);
                }
                
                nodeRefB = nodeService.getChildByName(guestHome, ContentModel.ASSOC_CONTAINS, CONTENT_NAME_B);
                
                if(nodeRefB == null)
                {
                    ChildAssociationRef  child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(GUID.generate()), ContentModel.TYPE_CONTENT);
                    nodeRefB = child.getChildRef();
                    nodeService.setProperty(nodeRefB, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                    nodeService.setProperty(nodeRefB, ContentModel.PROP_NAME, CONTENT_NAME_B);
            
                    ContentWriter writer = contentService.getWriter(nodeRefB, ContentModel.PROP_CONTENT, true);
                    writer.setLocale(CONTENT_LOCALE);
                    writer.putContent(CONTENT_STRING);
                }
                
                /**
                 * Now go ahead and create our first transfer target
                 */
                if(!transferService.targetExists(targetName))
                {
                    createTransferTarget(targetName);
                }
                else
                {
                    transferService.getTransferTarget(targetName);
                }   
            }
            finally    
            {
                trx.commit();
            }
        }
        
        /**
         * The transfer report is a plain report of the transfer - no async shenanigans to worry about
         */
        List<TransferEvent>transferReport = new ArrayList<TransferEvent>(50);
        
        startNewTransaction();
        try 
        {
           /**
             * Call the transferAsync method.
             */
            {
                TestTransferCallback callback = new TestTransferCallback();
                Set<TransferCallback> callbacks = new HashSet<TransferCallback>();
                callbacks.add(callback);
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(nodeRefA);
                nodes.add(nodeRefB);
                definition.setNodes(nodes);
                
                transferService.transferAsync(targetName, definition, callbacks);
                logger.debug("transfer async has returned");
                
                /**
                 * Need to poll the transfer events here until callback receives the last event
                 */
                Queue<TransferEvent> events = callback.getEvents();
                
                int sleepCount = MAX_SLEEPS;
                boolean ended = false;
                
                TransferEvent event = events.poll();
                while (!ended)
                {
                    logger.debug("polling loop:" + sleepCount);
                    
                    while(event != null)
                    {
                        /**
                         * Got an event - reset the sleep counter
                         */
                        sleepCount = MAX_SLEEPS;
                                                
                        logger.debug("Got an event" + event.toString());
                        
                        /**
                         * Squirrel away the event for analysis later
                         */
                        transferReport.add(event);
                        
                        /**
                         * If we read the last record which will either be SUCCESS or ERROR then we we have finished
                         */
                        if(event.isLast())
                        {
                            logger.debug("got last event");
                            ended = true;                            
                        }
                        
                        /**
                         * Try to get the next event
                         */
                        event = events.poll();
                    }
                    
                    if(event == null && !ended)
                    {
                        if(sleepCount <= 0)
                        {
                            fail("timed out without receiving last event");
                            ended = true;
                        }
                        else
                        {
                            /**
                             *  No content - go to sleep to wait for some more
                             */
                            if(sleepCount-- > 0)
                            {
                                // Sleep for 5 second
                                Thread.sleep(5000);
                            }                
                        }
                        
                        /**
                         * Try to get the next event
                         */
                        event = events.poll();
                    }
                }
            }  
                        
            /**
             * Now validate the transferReport
             */
            assertTrue("transfer report is too small", transferReport.size() > 2);
            assertTrue("transfer report does not start with START", transferReport.get(0).getTransferState().equals(TransferEvent.TransferState.START));
            assertTrue("transfer report does not end with SUCCESS", transferReport.get(transferReport.size()-1).getTransferState().equals(TransferEvent.TransferState.SUCCESS));
        }
        finally     
        {   
//            UserTransaction trx = transactionService.getNonPropagatingUserTransaction();
//            trx.begin();
            transferService.deleteTransferTarget(targetName);     
//            trx.commit();
            endTransaction();
        }
    } // test async callback
    
    
    /**
     * Test the transfer cancel method when it is running async.
     * 
     * This is a unit test so it does some shenanigans to send to the same instance of alfresco.
     */
    public void testAsyncCancel() throws Exception
    {
        int MAX_SLEEPS = 5;
        
        /**
          * Get guest home
          */
        String guestHomeQuery = "/app:company_home/app:guest_home";
        ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
        assertEquals("", 1, guestHomeResult.length());
        NodeRef guestHome = guestHomeResult.getNodeRef(0); 

        /**
          *  For unit test 
          *  - replace the HTTP transport with the in-process transport
          *  - replace the node factory with one that will map node refs, paths etc.
          */
        TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(this.receiver, this.contentService, transactionService);
        transferServiceImpl.setTransmitter(transmitter);
        UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
        transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
        List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
        // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
        pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
          
        /**
          * Now go ahead and create our first transfer target
          * This needs to be committed before we can call transfer asycnc.
          */
        String CONTENT_TITLE = "ContentTitle";
        String CONTENT_NAME_A = "Demo Node A";
        String CONTENT_NAME_B = "Demo Node B";
        Locale CONTENT_LOCALE = Locale.GERMAN; 
        String CONTENT_STRING = "Hello";
        
        NodeRef nodeRefA = null;
        NodeRef nodeRefB = null;
        String targetName = "testAsyncCallback";
        {
            UserTransaction trx = transactionService.getNonPropagatingUserTransaction();
            trx.begin();
            try
            {
                nodeRefA = nodeService.getChildByName(guestHome, ContentModel.ASSOC_CONTAINS, CONTENT_NAME_A);
                
                if(nodeRefA == null)
                {
                    /**
                     * Create a test node that we will read and write
                     */
                    ChildAssociationRef child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(GUID.generate()), ContentModel.TYPE_CONTENT);
                    nodeRefA = child.getChildRef();
                    nodeService.setProperty(nodeRefA, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                    nodeService.setProperty(nodeRefA, ContentModel.PROP_NAME, CONTENT_NAME_A);
            
                    ContentWriter writer = contentService.getWriter(nodeRefA, ContentModel.PROP_CONTENT, true);
                    writer.setLocale(CONTENT_LOCALE);
                    writer.putContent(CONTENT_STRING);
                }
                
                nodeRefB = nodeService.getChildByName(guestHome, ContentModel.ASSOC_CONTAINS, CONTENT_NAME_B);
                
                if(nodeRefB == null)
                {
                    ChildAssociationRef  child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(GUID.generate()), ContentModel.TYPE_CONTENT);
                    nodeRefB = child.getChildRef();
                    nodeService.setProperty(nodeRefB, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                    nodeService.setProperty(nodeRefB, ContentModel.PROP_NAME, CONTENT_NAME_B);
            
                    ContentWriter writer = contentService.getWriter(nodeRefB, ContentModel.PROP_CONTENT, true);
                    writer.setLocale(CONTENT_LOCALE);
                    writer.putContent(CONTENT_STRING);
                }
                
                /**
                 * Now go ahead and create our first transfer target
                 */
                if(!transferService.targetExists(targetName))
                {
                    createTransferTarget(targetName);
                }
                else
                {
                    transferService.getTransferTarget(targetName);
                }   
            }
            finally    
            {
                trx.commit();
            }
        }
        
        /**
         * The transfer report is a plain report of the transfer - no async shenanigans to worry about
         */
        List<TransferEvent>transferReport = new ArrayList<TransferEvent>(50);
        
        startNewTransaction();
        try 
        {
           /**
             * Call the transferAsync method.
             */
            {
                
                /**
                 * The poison callback will cancel the transfer after 
                 * the begin
                 */
                TransferCallback poison = new TransferCallback() 
                {
                    String transferId = null;

                    public void processEvent(TransferEvent event)
                    {
                        logger.debug(event.toString());
                                              
                        if(event instanceof TransferEventBegin)
                        {
                            TransferEventBegin beginEvent = (TransferEventBegin)event;
                            transferId = beginEvent.getTransferId();
                            
                            transferService.cancelAsync(transferId);
                        }
                    }
                };
                
                TestTransferCallback callback = new TestTransferCallback();
                Set<TransferCallback> callbacks = new HashSet<TransferCallback>();
                callbacks.add(callback);
                callbacks.add(poison);
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(nodeRefA);
                nodes.add(nodeRefB);
                definition.setNodes(nodes);
                
                transferService.transferAsync(targetName, definition, callbacks);
                logger.debug("transfer async has returned");
                
                /**
                 * Need to poll the transfer events here until callback receives the last event
                 */
                Queue<TransferEvent> events = callback.getEvents();
                
                int sleepCount = MAX_SLEEPS;
                boolean ended = false;
                
                TransferEvent event = events.poll();
                while (!ended)
                {
                    logger.debug("polling loop:" + sleepCount);
                    
                    while(event != null)
                    {
                        /**
                         * Got an event - reset the sleep counter
                         */
                        sleepCount = MAX_SLEEPS;
                                                
                        logger.debug("Got an event" + event.toString());
                        
                        /**
                         * Squirrel away the event for analysis later
                         */
                        transferReport.add(event);
                        
                        /**
                         * If we read the last record which will either be SUCCESS or ERROR then we we have finished
                         */
                        if(event.isLast())
                        {
                            logger.debug("got last event");
                            ended = true;                            
                        }
                        
                        /**
                         * Try to get the next event
                         */
                        event = events.poll();
                    }
                    
                    if(event == null && !ended)
                    {
                        if(sleepCount <= 0)
                        {
                            fail("timed out without receiving last event");
                            ended = true;
                        }
                        else
                        {
                            /**
                             *  No content - go to sleep to wait for some more
                             */
                            if(sleepCount-- > 0)
                            {
                                // Sleep for 5 second
                                Thread.sleep(5000);
                            }                
                        }
                        
                        /**
                         * Try to get the next event
                         */
                        event = events.poll();
                    }
                }
            }  
                        
            /**
             * Now validate the transferReport
             */
            assertTrue("transfer report is too small", transferReport.size() > 2);
            assertTrue("transfer report does not start with START", transferReport.get(0).getTransferState().equals(TransferEvent.TransferState.START));
            assertTrue("transfer report does not end with ERROR", transferReport.get(transferReport.size()-1).getTransferState().equals(TransferEvent.TransferState.ERROR));
        }
        finally     
        {   
//            UserTransaction trx = transactionService.getNonPropagatingUserTransaction();
//            trx.begin();
            transferService.deleteTransferTarget(targetName);     
//            trx.commit();
            endTransaction();
        }
    } // test async cancel

    
    /**
     * Test the transfer report.
     * 
     * This is a unit test so it does some shenanigans to send to the same instance of alfresco.
     */
    public void testTransferReport() throws Exception
    {
        setDefaultRollback(false);
        
        /**
          * Get guest home
          */
        String guestHomeQuery = "/app:company_home/app:guest_home";
        ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
        assertEquals("", 1, guestHomeResult.length());
        NodeRef guestHome = guestHomeResult.getNodeRef(0); 

        /**
          *  For unit test 
          *  - replace the HTTP transport with the in-process transport
          *  - replace the node factory with one that will map node refs, paths etc.
          */
        TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(this.receiver, this.contentService, transactionService);
        transferServiceImpl.setTransmitter(transmitter);
        UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
        transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
        List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
        // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
        pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
          
        /**
          * Now go ahead and create our first transfer target
          * This needs to be committed before we can call transfer asycnc.
          */
        String CONTENT_TITLE = "ContentTitle";
        String CONTENT_NAME_A = "Demo Node A";
        String CONTENT_NAME_B = "Demo Node B";
        Locale CONTENT_LOCALE = Locale.GERMAN; 
        String CONTENT_STRING = "Hello";
        
        NodeRef nodeRefA = null;
        NodeRef nodeRefB = null;
        String targetName = "testTransferReport";
     
        startNewTransaction();
        try
        {
            nodeRefA = nodeService.getChildByName(guestHome, ContentModel.ASSOC_CONTAINS, CONTENT_NAME_A);
                    
            if(nodeRefA == null)
            {
                /**
                 * Create a test node that we will read and write
                 */
                ChildAssociationRef child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(GUID.generate()), ContentModel.TYPE_CONTENT);
                nodeRefA = child.getChildRef();
                nodeService.setProperty(nodeRefA, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(nodeRefA, ContentModel.PROP_NAME, CONTENT_NAME_A);
                
                ContentWriter writer = contentService.getWriter(nodeRefA, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
                    
            nodeRefB = nodeService.getChildByName(guestHome, ContentModel.ASSOC_CONTAINS, CONTENT_NAME_B);
                    
            if(nodeRefB == null)
            {
                ChildAssociationRef  child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(GUID.generate()), ContentModel.TYPE_CONTENT);
                nodeRefB = child.getChildRef();
                nodeService.setProperty(nodeRefB, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(nodeRefB, ContentModel.PROP_NAME, CONTENT_NAME_B);
                
                ContentWriter writer = contentService.getWriter(nodeRefB, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
            
            /**
             * Now go ahead and create our first transfer target
             */
            if(!transferService.targetExists(targetName))
            {
                createTransferTarget(targetName);
            }
        }
        finally
        {
            endTransaction();
        }
        
        NodeRef transferReport = null;
        startNewTransaction();
        try
        {
            /**
              * Call the transfer method.
              */
            {
                TestTransferCallback callback = new TestTransferCallback();
                Set<TransferCallback> callbacks = new HashSet<TransferCallback>();
                callbacks.add(callback);
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(nodeRefA);
                nodes.add(nodeRefB);
                definition.setNodes(nodes);
                    
                transferReport = transferService.transfer(targetName, definition, callbacks);
                assertNotNull("transfer report is null", transferReport);
                // Can't dirty read transfer report here
            }
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try
        {
            ContentReader reader = contentService.getReader(transferReport, ContentModel.PROP_CONTENT);
            assertNotNull("transfer reader is null", reader);
            
            ContentReader reader2 = contentService.getReader(transferReport, ContentModel.PROP_CONTENT);
            assertNotNull("transfer reader is null", reader);
            
            logger.debug("now show the contents of the transfer report");
            reader2.getContent(System.out);  
            
            // Now validate the client side transfer report against the XSD
            Source transferReportSource = new StreamSource(reader.getContentInputStream());
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final String TRANSFER_REPORT_SCHEMA_LOCATION = "classpath:org/alfresco/repo/transfer/report/TransferReport.xsd";
            Schema schema = sf.newSchema(ResourceUtils.getURL(TRANSFER_REPORT_SCHEMA_LOCATION));
            Validator validator = schema.newValidator();
            try 
            {
                validator.validate(transferReportSource);
            }
            catch (Exception e)
            {
                fail(e.getMessage() );
            }
            
            logger.debug("now delete the target:" + targetName);
            
            transferService.deleteTransferTarget(targetName);     
        }
        finally
        {
            endTransaction();
        }          
    } // test transfer report

    
    
    
//    /**
//     * Test the transfer method with big content - commented out since it takes a long time to run.
//     */
//    public void testTransferOneNodeWithBigContent() throws Exception
//    {
//        String CONTENT_TITLE = "ContentTitle";
//        String CONTENT_NAME = "Demo Node 6";
//        Locale CONTENT_LOCALE = Locale.GERMAN; 
//        String CONTENT_STRING = "Hello";
//        
//        String targetName = "testTransferOneNodeWithBigContent";
//        
//        String guestHomeQuery = "/app:company_home/app:guest_home";
//        ResultSet result = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
//        
//        assertEquals("", 1, result.length());
//        NodeRef guestHome = result.getNodeRef(0);
//        ChildAssociationRef childAssoc = result.getChildAssocRef(0);
//        System.out.println("Guest home:" + guestHome);
//        assertNotNull(guestHome);
//        
//        /**
//         * Now go ahead and create our first transfer target
//         */
//        TransferTarget transferMe = createTransferTarget(targetName);
//        
//        /**
//         * Create a test node that we will read and write
//         */
//        ChildAssociationRef child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName("testNode6"), ContentModel.TYPE_CONTENT);
//        
//        
//        File tempFile = TempFileProvider.createTempFile("test", ".dat");
//        FileWriter fw = new FileWriter(tempFile);
//        for(int i = 0; i < 100000000; i++)
//        {
//            fw.write("hello world this is my text, I wonder how much text I can transfer?" + i);
//        }
//        System.out.println("Temp File Size is:" + tempFile.length());
//        fw.close();
// 
//        NodeRef contentNodeRef = child.getChildRef();
//        ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
//        writer.setLocale(CONTENT_LOCALE);
//        //File file = new File("c:/temp/images/BigCheese1.bmp");
//        writer.setMimetype("application/data");
//        //writer.putContent(file);
//        writer.putContent(tempFile);
//        
//        tempFile.delete();
//        
//        nodeService.setProperty(contentNodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
//        nodeService.setProperty(contentNodeRef, ContentModel.PROP_NAME, CONTENT_NAME);
//
//        try 
//        {
//            /**
//             * Transfer the node created above 
//             */
//            {
//                TransferDefinition definition = new TransferDefinition();
//                Set<NodeRef>nodes = new HashSet<NodeRef>();
//                nodes.add(contentNodeRef);
//                definition.setNodes(nodes);
//                transferService.transfer(targetName, definition, null);
//            }  
//                      
//            /**
//             * Negative test transfer nothing
//             */
//            try
//            {
//                TransferDefinition definition = new TransferDefinition();
//                transferService.transfer(targetName, definition, null);
//                fail("exception not thrown");
//            }
//            catch(TransferException te)
//            {
//                // expect to go here
//            }
//        }
//        finally
//        {
//            transferService.deleteTransferTarget(targetName);
//        } 
//    }

    /**
     * Test the transfer method behaviour with respect to sync folders - sending a complete set of nodes and implying delete from
     * absence of a node.
     * 
     * This is a unit test so it does some shenanigans to send to the same instance of alfresco.
     * 
     * Tree of nodes
     * 
     *      A1
     *   |      |                 | 
     *   A2     A3 (Content Node) B6  
     *   |
     * A4 A5 B7 (Content Node) 
     * 
     * Test steps - 
     * 1 add A1
     *   transfer(sync)
     * 2 add A2, A3, A4, A5
     *   transfer(sync)
     * 3 remove A2 
     *   transfer(sync) A4 and A5 should cascade delete on source
     * 4 remove A3
     *   transfer(sync)
     * 5 add back A3 - new node ref
     *   transfer(sync)
     * 6 add A2, A4, A5
     *   transfer(sync)
     * 7 add B6 and B7 directly to target (so not transferred) 
     *   transfer again, B6 and B7 should not be removed.
     * 8 remove A2 (A2, A5, B7) should go.    TODO is it correct that B7 goes?
     *   transfer
     *   restore node A2
     *   transfer
     */
    public void testTransferSyncNodes() throws Exception
    {
        setDefaultRollback(false);
        
        String CONTENT_TITLE = "ContentTitle";
        String CONTENT_TITLE_UPDATED = "ContentTitleUpdated";
        Locale CONTENT_LOCALE = Locale.GERMAN; 
        String CONTENT_STRING = "Hello";

        /**
         *  For unit test 
         *  - replace the HTTP transport with the in-process transport
         *  - replace the node factory with one that will map node refs, paths etc.
         */
        TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(receiver, contentService, transactionService);
        transferServiceImpl.setTransmitter(transmitter);
        UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
        transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
        List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
        // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
        pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
          
        /**
          * Now go ahead and create our first transfer target
          */
        String targetName = "testTransferSyncNodes";
        TransferTarget transferMe;
        NodeRef A1NodeRef;
        NodeRef A2NodeRef;
        NodeRef A3NodeRef;
        NodeRef A4NodeRef;
        NodeRef A5NodeRef;
        NodeRef B6NodeRef;
        NodeRef B7NodeRef;
        
        NodeRef destNodeRef;
        
        startNewTransaction();
        try
        {
            /**
              * Get guest home
              */
            String guestHomeQuery = "/app:company_home/app:guest_home";
            ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
            assertEquals("", 1, guestHomeResult.length());
            NodeRef guestHome = guestHomeResult.getNodeRef(0); 
    
            /**
             * Create a test nodes A1 through A5 that we will read and write
             */
            {
                // Node A1
                String name = GUID.generate();
                ChildAssociationRef child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(name), ContentModel.TYPE_FOLDER);
                A1NodeRef = child.getChildRef();
                nodeService.setProperty(A1NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A1NodeRef, ContentModel.PROP_NAME, name);
            }
       
            {
                // Node A2
                ChildAssociationRef child = nodeService.createNode(A1NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A2"), ContentModel.TYPE_FOLDER);
                A2NodeRef = child.getChildRef();
                nodeService.setProperty(A2NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A2NodeRef, ContentModel.PROP_NAME, "A2");
            }
            
            {
                // Node A3
                ChildAssociationRef child = nodeService.createNode(A1NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A3"), ContentModel.TYPE_CONTENT);
                A3NodeRef = child.getChildRef();
                nodeService.setProperty(A3NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A3NodeRef, ContentModel.PROP_NAME, "A3");
            
                ContentWriter writer = contentService.getWriter(A3NodeRef, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
            {
                // Node A4
                ChildAssociationRef child = nodeService.createNode(A2NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A4"), ContentModel.TYPE_CONTENT);
                A4NodeRef = child.getChildRef();
                nodeService.setProperty(A4NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A4NodeRef, ContentModel.PROP_NAME, "A4");
            
                ContentWriter writer = contentService.getWriter(A4NodeRef, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
            {
                // Node A5
                ChildAssociationRef child = nodeService.createNode(A2NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A5"), ContentModel.TYPE_CONTENT);
                A5NodeRef = child.getChildRef();
                nodeService.setProperty(A5NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A5NodeRef, ContentModel.PROP_NAME, "A5");
            
                ContentWriter writer = contentService.getWriter(A5NodeRef, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
 
            // Create the transfer target if it does not already exist
            if(!transferService.targetExists(targetName))
            {
                transferMe = createTransferTarget(targetName);
            }
            else
            {
                transferMe = transferService.getTransferTarget(targetName);
            }
        }
        finally
        {
            endTransaction();
        }    

        
        /**
         * Step 1.   Add Node A1.
         */
        startNewTransaction();
        try 
        {
           /**
             * Transfer Node A with no children
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(A1NodeRef);
                definition.setNodes(nodes);
                definition.setSync(true);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref does not exist", nodeService.exists(destNodeRef));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE); 
            assertEquals("type is wrong", nodeService.getType(A1NodeRef), nodeService.getType(destNodeRef));
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 
        }
        finally
        {
            endTransaction();
        }
        
        /**
         * Step 2.   Add Node A2, A3, A4, A5.
         */
        startNewTransaction();
        try 
        {
           /**
             * Transfer Node A 1-5
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(A1NodeRef);
                nodes.add(A2NodeRef);
                nodes.add(A3NodeRef);
                nodes.add(A4NodeRef);
                nodes.add(A5NodeRef);
                definition.setNodes(nodes);
                definition.setSync(true);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
            assertTrue("dest node ref A2 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
            assertTrue("dest node ref A3 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
            assertTrue("dest node ref A4 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
            assertTrue("dest node ref A5 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 
        }
        finally
        {
            endTransaction();
        }
        
        /**
         * Step 3 - remove folder node A2
         */
        startNewTransaction();
        try 
        {
            nodeService.deleteNode(A2NodeRef);
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try 
        {
           /**
             * Transfer Node A 1-5
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(A1NodeRef);
                //nodes.add(A2NodeRef);
                nodes.add(A3NodeRef);
                //nodes.add(A4NodeRef);
                //nodes.add(A5NodeRef);
                definition.setNodes(nodes);
                definition.setSync(true);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
            assertFalse("dest node ref A2 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
            assertTrue("dest node ref A3 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
            assertFalse("dest node ref A4 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
            assertFalse("dest node ref A5 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 
        }
        finally
        {
            endTransaction();
        }
        
        /**
         * Step 4 - remove content node A3
         */
        startNewTransaction();
        try 
        {
            nodeService.deleteNode(A3NodeRef);
        }
        finally
        {
            endTransaction();
        }
        startNewTransaction();
        try 
        {
           /**
             * Transfer Node A 1-5
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(A1NodeRef);
                //nodes.add(A2NodeRef);
                //nodes.add(A3NodeRef);
                //nodes.add(A4NodeRef);
                //nodes.add(A5NodeRef);
                definition.setNodes(nodes);
                definition.setSync(true);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
            assertFalse("dest node ref A2 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
            assertFalse("dest node ref A3 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
            assertFalse("dest node ref A4 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
            assertFalse("dest node ref A5 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 
        }
        finally
        {
            endTransaction();
        }
        
        /**
         * Step 5.   Add back A3.
         */
        startNewTransaction();
        try 
        {
            ChildAssociationRef child = nodeService.createNode(A1NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A3"), ContentModel.TYPE_CONTENT);
            A3NodeRef = child.getChildRef();
            nodeService.setProperty(A3NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
            nodeService.setProperty(A3NodeRef, ContentModel.PROP_NAME, "A3");
        
            ContentWriter writer = contentService.getWriter(A3NodeRef, ContentModel.PROP_CONTENT, true);
            writer.setLocale(CONTENT_LOCALE);
            writer.putContent(CONTENT_STRING);
        }
        finally
        {
            endTransaction();
        }
        startNewTransaction();
        try 
        {
           /**
             * Transfer Node A 1-5
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(A1NodeRef);
                //nodes.add(A2NodeRef);
                nodes.add(A3NodeRef);
                //nodes.add(A4NodeRef);
                //nodes.add(A5NodeRef);
                definition.setNodes(nodes);
                definition.setSync(true);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
            assertFalse("dest node ref A2 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
            assertTrue("dest node A3 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
            assertFalse("dest node ref A4 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
            assertFalse("dest node ref A5 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 
        }
        finally
        {
            endTransaction();
        }
        
        /**
         * Step 6.   add A2, A4, A5
         */
        startNewTransaction();
        try 
        {
            
            {
                // Node A2
                ChildAssociationRef child = nodeService.createNode(A1NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A2"), ContentModel.TYPE_FOLDER);
                A2NodeRef = child.getChildRef();
                nodeService.setProperty(A2NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A2NodeRef, ContentModel.PROP_NAME, "A2");
            }
            

            {
                // Node A4
                ChildAssociationRef child = nodeService.createNode(A2NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A4"), ContentModel.TYPE_CONTENT);
                A4NodeRef = child.getChildRef();
                nodeService.setProperty(A4NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A4NodeRef, ContentModel.PROP_NAME, "A4");
            
                ContentWriter writer = contentService.getWriter(A4NodeRef, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
            {
                // Node A5
                ChildAssociationRef child = nodeService.createNode(A2NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A5"), ContentModel.TYPE_CONTENT);
                A5NodeRef = child.getChildRef();
                nodeService.setProperty(A5NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(A5NodeRef, ContentModel.PROP_NAME, "A5");
            
                ContentWriter writer = contentService.getWriter(A5NodeRef, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
        }
        finally
        {
            endTransaction();
        }
        startNewTransaction();
        try 
        {
           /**
             * Transfer Node A 1-5
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(A1NodeRef);
                nodes.add(A2NodeRef);
                nodes.add(A3NodeRef);
                nodes.add(A4NodeRef);
                nodes.add(A5NodeRef);
                definition.setNodes(nodes);
                definition.setSync(true);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
            assertTrue("dest node A2 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
            assertTrue("dest node A3 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
            assertTrue("dest node A4 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
            assertTrue("dest node A5 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
            
            // Check injected transferred aspect.
            assertNotNull("transferredAspect", (String)nodeService.getProperty(destNodeRef, TransferModel.PROP_REPOSITORY_ID)); 
        }
        finally
        {
            endTransaction();
        }
        
        /**
         * Step 7 add B6 and B7 directly to target (so not transferred) transfer again and verify that 
         * the nodes are unchanged.
         */
        startNewTransaction();
        try 
        {
            {
                // Node B6
                ChildAssociationRef child = nodeService.createNode(testNodeFactory.getMappedNodeRef(A1NodeRef), ContentModel.ASSOC_CONTAINS, QName.createQName("B6"), ContentModel.TYPE_CONTENT);
                B6NodeRef = child.getChildRef();
                nodeService.setProperty(B6NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(B6NodeRef, ContentModel.PROP_NAME, "B6");
            
                ContentWriter writer = contentService.getWriter(B6NodeRef, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
            {
                // Node B7
                ChildAssociationRef child = nodeService.createNode(testNodeFactory.getMappedNodeRef(A2NodeRef), ContentModel.ASSOC_CONTAINS, QName.createQName("B7"), ContentModel.TYPE_CONTENT);
                B7NodeRef = child.getChildRef();
                nodeService.setProperty(B7NodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
                nodeService.setProperty(B7NodeRef, ContentModel.PROP_NAME, "B7");
            
                ContentWriter writer = contentService.getWriter(B7NodeRef, ContentModel.PROP_CONTENT, true);
                writer.setLocale(CONTENT_LOCALE);
                writer.putContent(CONTENT_STRING);
            }
        }
        finally
        {
            endTransaction();
        }
        startNewTransaction();
        try 
        {
           /**
             * Transfer Node A 1-5, B6 and B7 should remain untouched.
             */
            {
                TransferDefinition definition = new TransferDefinition();
                Set<NodeRef>nodes = new HashSet<NodeRef>();
                nodes.add(A1NodeRef);
                nodes.add(A2NodeRef);
                nodes.add(A3NodeRef);
                nodes.add(A4NodeRef);
                nodes.add(A5NodeRef);
                definition.setNodes(nodes);
                definition.setSync(true);
                transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }

        startNewTransaction();
        try
        {
            // Now validate that the target node exists and has similar properties to the source
            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
            assertTrue("dest node A2 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
            assertTrue("dest node A3 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
            assertTrue("dest node A4 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
            assertTrue("dest node A5 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
            assertTrue("dest node B6 does not exist", nodeService.exists(B6NodeRef));
            assertTrue("dest node B7 does not exist", nodeService.exists(B7NodeRef));
            
        }
        finally
        {
            endTransaction();
        }
        
        //TODO BUGBUG - test fails
//        /** Step 8
//         * remove A2 (A2, A5, B7) should go.    TODO is it correct that B7 goes?
//         * transfer
//         * restore node A2
//         * transfer
//         */
//        /**
//         * Step 3 - remove folder node A2
//         */
//        startNewTransaction();
//        try 
//        {
//            nodeService.deleteNode(A2NodeRef);
//        }
//        finally
//        {
//            endTransaction();
//        }
//
//        startNewTransaction();
//        try 
//        {
//           /**
//             * Transfer Node A 1-5
//             */
//            {
//                TransferDefinition definition = new TransferDefinition();
//                Set<NodeRef>nodes = new HashSet<NodeRef>();
//                nodes.add(A1NodeRef);
//                //nodes.add(A2NodeRef);
//                nodes.add(A3NodeRef);
//                //nodes.add(A4NodeRef);
//                //nodes.add(A5NodeRef);
//                definition.setNodes(nodes);
//                definition.setSync(true);
//                transferService.transfer(targetName, definition);
//            }  
//        }
//        finally
//        {
//            endTransaction();
//        }
//
//        startNewTransaction();
//        try
//        {
//            // Now validate that the target node exists and has similar properties to the source
//            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
//            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
//            assertTrue("dest node ref A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
//            assertFalse("dest node ref A2 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
//            assertTrue("dest node ref A3 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
//            assertFalse("dest node ref A4 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
//            assertFalse("dest node ref A5 has not been deleted", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
// //           assertFalse("dest node B6 has not been deleted", nodeService.exists(B6NodeRef));
// //           assertTrue("dest node B7 does not exist", nodeService.exists(B7NodeRef));
//            }
//        finally
//        {
//            endTransaction();
//        }    
//        
//        startNewTransaction();
//        try 
//        {
//            NodeRef archivedNode = new NodeRef(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, A2NodeRef.getId());
//            nodeService.restoreNode(archivedNode, A1NodeRef, ContentModel.ASSOC_CONTAINS, QName.createQName("A2"));
//        }
//        finally
//        {
//            endTransaction();
//        }
//        startNewTransaction();
//        try 
//        {
//           /**
//             * Transfer Node A 1-5, B6 and B7 should remain untouched.
//             */
//            {
//                TransferDefinition definition = new TransferDefinition();
//                Set<NodeRef>nodes = new HashSet<NodeRef>();
//                nodes.add(A1NodeRef);
//                nodes.add(A2NodeRef);
//                nodes.add(A3NodeRef);
//                nodes.add(A4NodeRef);
//                nodes.add(A5NodeRef);
//                definition.setNodes(nodes);
//                definition.setSync(true);
//                transferService.transfer(targetName, definition);
//            }  
//        }
//        finally
//        {
//            endTransaction();
//        }
//        
//        startNewTransaction();
//        try
//        {
//            // Now validate that the target node exists and has similar properties to the source
//            destNodeRef = testNodeFactory.getMappedNodeRef(A1NodeRef);
//            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
//            assertTrue("dest node ref A1 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A1NodeRef)));
//            assertTrue("dest node ref A2 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A2NodeRef)));
//            assertTrue("dest node ref A3 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A3NodeRef)));
//            assertTrue("dest node ref A4 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A4NodeRef)));
//            assertTrue("dest node ref A5 does not exist", nodeService.exists(testNodeFactory.getMappedNodeRef(A5NodeRef)));
// //           assertFalse("dest node B6 has not been deleted", nodeService.exists(B6NodeRef));
// //           assertTrue("dest node B7 does not exist", nodeService.exists(B7NodeRef));
//            }
//        finally
//        {
//            endTransaction();
//        }    
        
    }
    
    /**
     * Test the transfer method with regard to permissions on a node.
     * 
     * Step 1:  
     * Create a node with a single permission 
     *     Inherit:false
     *     Read, Admin, Allow
     *     Transfer
     * 
     * Step 2:
     * Update it to have several permissions 
     *     Inherit:false
     *     Read, Everyone, DENY
     *     Read, Admin, Allow
     * 
     * Step 3:
     * Remove a permission
     *     Inherit:false
     *     Read, Admin, Allow
     * 
     * Step 4:
     * Revert to inherit all permissions
     *     Inherit:true
     * 
     * This is a unit test so it does some shenanigans to send to the same instance of alfresco.
     */
    public void testTransferWithPermissions() throws Exception
    {
        setDefaultRollback(false);
        
        String CONTENT_TITLE = "ContentTitle";
        String CONTENT_TITLE_UPDATED = "ContentTitleUpdated";
        Locale CONTENT_LOCALE = Locale.GERMAN; 
        String CONTENT_STRING = "Hello";

        /**
         *  For unit test 
         *  - replace the HTTP transport with the in-process transport
         *  - replace the node factory with one that will map node refs, paths etc.
         */
        TransferTransmitter transmitter = new UnitTestInProcessTransmitterImpl(receiver, contentService, transactionService);
        transferServiceImpl.setTransmitter(transmitter);
        UnitTestTransferManifestNodeFactory testNodeFactory = new UnitTestTransferManifestNodeFactory(this.transferManifestNodeFactory); 
        transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory); 
        List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
        // Map company_home/guest_home to company_home so tranferred nodes and moved "up" one level.
        pathMap.add(new Pair<Path, Path>(PathHelper.stringToPath(GUEST_HOME_XPATH_QUERY), PathHelper.stringToPath(COMPANY_HOME_XPATH_QUERY)));
          
        /**
          * Now go ahead and create our transfer target
          */
        String targetName = "testTransferWithPermissions";
        TransferTarget transferMe;
        NodeRef contentNodeRef;
        NodeRef destNodeRef;
        
        startNewTransaction();
        try
        {
            /**
              * Get guest home
              */
            String guestHomeQuery = "/app:company_home/app:guest_home";
            ResultSet guestHomeResult = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, guestHomeQuery);
            assertEquals("", 1, guestHomeResult.length());
            NodeRef guestHome = guestHomeResult.getNodeRef(0); 
    
            /**
             * Create a test node that we will read and write
             */        
            String name = GUID.generate();
            ChildAssociationRef child = nodeService.createNode(guestHome, ContentModel.ASSOC_CONTAINS, QName.createQName(name), ContentModel.TYPE_CONTENT);
            contentNodeRef = child.getChildRef();
            nodeService.setProperty(contentNodeRef, ContentModel.PROP_TITLE, CONTENT_TITLE);   
            nodeService.setProperty(contentNodeRef, ContentModel.PROP_NAME, name);
            
            ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
            writer.setLocale(CONTENT_LOCALE);
            writer.putContent(CONTENT_STRING);
            
            permissionService.setInheritParentPermissions(contentNodeRef, false);
            permissionService.setPermission(contentNodeRef, "admin", PermissionService.READ, true);
              
            if(!transferService.targetExists(targetName))
            {
                transferMe = createTransferTarget(targetName);
            }
            else
            {
                transferMe = transferService.getTransferTarget(targetName);
            }
        }
        finally
        {
            endTransaction();
        }
        
        /**
         * Step 1
         */
        logger.debug("First transfer - create new node with inheritParent permission off");
        startNewTransaction();
        try 
        {
           /**
             * Transfer our transfer target node 
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    Set<NodeRef>nodes = new HashSet<NodeRef>();
                    nodes.add(contentNodeRef);
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
            
        startNewTransaction();
        try
        {
            // Now validate that the target node exists with the correct permissions 
            destNodeRef = testNodeFactory.getMappedNodeRef(contentNodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref does not exist", nodeService.exists(destNodeRef));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE); 
            assertEquals("type is wrong", nodeService.getType(contentNodeRef), nodeService.getType(destNodeRef));
            
            // Check ACL of destination node
            boolean srcInherit = permissionService.getInheritParentPermissions(contentNodeRef);
            Set<AccessPermission> srcPerm = permissionService.getAllSetPermissions(contentNodeRef);
            
            boolean destInherit = permissionService.getInheritParentPermissions(destNodeRef);
            Set<AccessPermission> destPerm = permissionService.getAllSetPermissions(destNodeRef);
            
            assertFalse("inherit parent permissions (src) flag is incorrect", srcInherit);
            assertFalse("inherit parent permissions (dest) flag is incorrect", destInherit);
            
            // Check destination has the source's permissions
            for (AccessPermission p : srcPerm)
            {
                logger.debug("checking permission :" + p);
                assertTrue("permission is missing", destPerm.contains(p));
            }
        }
        finally
        {
            endTransaction();
        }      
        
        /**
         * Step 2
         * Update it to have several permissions 
         *     Inherit:false
         *     Read, Everyone, DENY
         *     Read, Admin, Allow 
         */
        startNewTransaction();
        try
        {
            permissionService.setPermission(contentNodeRef, "EVERYONE", PermissionService.READ, false);
            permissionService.setPermission(contentNodeRef, "admin", PermissionService.FULL_CONTROL, true);
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try 
        {
           /**
             * Transfer our transfer target node 
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    Set<NodeRef>nodes = new HashSet<NodeRef>();
                    nodes.add(contentNodeRef);
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
  
        
        startNewTransaction();
        try
        {
            // Now validate that the target node exists with the correct permissions 
            destNodeRef = testNodeFactory.getMappedNodeRef(contentNodeRef);
            
            // Check ACL of destination node
            boolean srcInherit = permissionService.getInheritParentPermissions(contentNodeRef);
            Set<AccessPermission> srcPerm = permissionService.getAllSetPermissions(contentNodeRef);
            
            boolean destInherit = permissionService.getInheritParentPermissions(destNodeRef);
            Set<AccessPermission> destPerm = permissionService.getAllSetPermissions(destNodeRef);
            
            assertFalse("inherit parent permissions (src) flag is incorrect", srcInherit);
            assertFalse("inherit parent permissions (dest) flag is incorrect", destInherit);
            
            // Check destination has the source's permissions
            for (AccessPermission p : srcPerm)
            {
                logger.debug("checking permission :" + p);
                assertTrue("Step2, permission is missing", destPerm.contains(p));
            }
        }
        finally
        {
            endTransaction();
        }    
        
        /**
         * Step 3 Remove a permission
         */
        startNewTransaction();
        try
        {
            permissionService.deletePermission(contentNodeRef, "admin", PermissionService.FULL_CONTROL);
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try 
        {
           /**
             * Transfer our transfer target node 
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    Set<NodeRef>nodes = new HashSet<NodeRef>();
                    nodes.add(contentNodeRef);
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
  
        startNewTransaction();
        try
        {
            // Now validate that the target node exists with the correct permissions 
            destNodeRef = testNodeFactory.getMappedNodeRef(contentNodeRef);
           
            // Check ACL of destination node
            boolean srcInherit = permissionService.getInheritParentPermissions(contentNodeRef);
            Set<AccessPermission> srcPerm = permissionService.getAllSetPermissions(contentNodeRef);
            
            boolean destInherit = permissionService.getInheritParentPermissions(destNodeRef);
            Set<AccessPermission> destPerm = permissionService.getAllSetPermissions(destNodeRef);
            
            assertFalse("inherit parent permissions (src) flag is incorrect", srcInherit);
            assertFalse("inherit parent permissions (dest) flag is incorrect", destInherit);
            
            // Check destination has the source's permissions
            for (AccessPermission p : srcPerm)
            {
                logger.debug("checking permission :" + p);
                assertTrue("permission is missing", destPerm.contains(p));
            }
        }
        finally
        {
            endTransaction();
        }    
        
        /**
         * Step 4
         * Revert to inherit all permissions
         */
        startNewTransaction();
        try
        {
            permissionService.setInheritParentPermissions(contentNodeRef, true);
        }
        finally
        {
            endTransaction();
        }
        
        startNewTransaction();
        try 
        {
           /**
             * Transfer our transfer target node 
             */
            {
                    TransferDefinition definition = new TransferDefinition();
                    Set<NodeRef>nodes = new HashSet<NodeRef>();
                    nodes.add(contentNodeRef);
                    definition.setNodes(nodes);
                    transferService.transfer(targetName, definition);
            }  
        }
        finally
        {
            endTransaction();
        }
  
        startNewTransaction();
        try
        {
            // Now validate that the target node exists with the correct permissions 
            destNodeRef = testNodeFactory.getMappedNodeRef(contentNodeRef);
            assertFalse("unit test stuffed up - comparing with self", destNodeRef.equals(transferMe.getNodeRef()));
            assertTrue("dest node ref does not exist", nodeService.exists(destNodeRef));
            assertEquals("title is wrong", (String)nodeService.getProperty(destNodeRef, ContentModel.PROP_TITLE), CONTENT_TITLE); 
            assertEquals("type is wrong", nodeService.getType(contentNodeRef), nodeService.getType(destNodeRef));
            
            // Check ACL of destination node
            boolean srcInherit = permissionService.getInheritParentPermissions(contentNodeRef);
            Set<AccessPermission> srcPerm = permissionService.getAllSetPermissions(contentNodeRef);
            
            boolean destInherit = permissionService.getInheritParentPermissions(destNodeRef);
            Set<AccessPermission> destPerm = permissionService.getAllSetPermissions(destNodeRef);
            
            assertTrue("inherit parent permissions (src) flag is incorrect", srcInherit);
            assertTrue("inherit parent permissions (dest) flag is incorrect", destInherit);
            
            // Check destination has the source's permissions
            for (AccessPermission p : srcPerm)
            {
                if(p.isSetDirectly())
                {
                    logger.debug("checking permission :" + p);
                    assertTrue("permission is missing:" + p, destPerm.contains(p));
                }
            }
        }
        finally
        {
            endTransaction();
        }    
     }


    private TransferTarget createTransferTarget(String name)
    {
        String title = "title";
        String description = "description";
        String endpointProtocol = "http";
        String endpointHost = "MARKR02";
        int endpointPort = 7080;
        String endpointPath = "/alfresco/service/api/transfer";
        String username = "admin";
        char[] password = "admin".toCharArray();
    
        /**
         * Now go ahead and create our first transfer target
         */
        TransferTarget target = transferService.createAndSaveTransferTarget(name, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
        return target;
    }
}
