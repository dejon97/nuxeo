/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors: Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.scripting;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core" })
@LocalDeploy("org.nuxeo.ecm.automation.core:test-doc-wrapper.xml")
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class TestDocumentWrapperGetRef {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Test
    /**
     * Test the documentWrapper getRef method to be used in script.
     * This test use the operation chain defined in the test extension src/test/resources/test-doc-wrapper.xml
     */
    public void testDocumentWrapperGetRef() throws InvalidChainException, OperationException, Exception {

        // testing scripts using document wrapper get ref method: follow
        // transition

        // create a File document
        DocumentModel doc = session.createDocumentModel("/", "TestFile", "File");
        doc = session.createDocument(doc);

        // before starting, check it's in project
        String lifecycleState = session.getCurrentLifeCycleState(doc.getRef());
        Assert.assertEquals("At first, the document currentlifecycle state is", "project", lifecycleState);

        // The automation chain should be similar to the following:
        //
        // session.followTransition(doc.getRef(), "delete");
        // session.save();
        //
        // Run the script operation:
        runChain(doc, "followtransitiondelete");

        // the script operation should run a Session.followTransition using
        // getRef of document wrapper
        lifecycleState = session.getCurrentLifeCycleState(doc.getRef());
        Assert.assertEquals("After the test, the document currentlifecycle state is", "deleted", lifecycleState);
    }

    private void runChain(DocumentModel inputDoc, String chainId) throws OperationException, InvalidChainException,
            Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(inputDoc);
        automationService.run(ctx, chainId);
    }

    @Test
    public void testGetParentRef() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        DocumentWrapper workspaces = new DocumentWrapper(session, doc);
        DocumentWrapper domain = workspaces.getParent("Domain");
        Assert.assertNotNull(domain);
        DocumentWrapper unknown = workspaces.getParent("pfff");
        Assert.assertNull(unknown);
    }
}
