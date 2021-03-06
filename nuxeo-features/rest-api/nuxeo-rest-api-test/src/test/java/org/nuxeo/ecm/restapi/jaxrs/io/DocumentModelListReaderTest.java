/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JSONDocumentModelReader;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.jaxrs.io.documents.JSONDocumentModelListReader;
import org.nuxeo.ecm.restapi.test.JSONDocumentHelper;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionRef;
import org.nuxeo.ecm.webengine.jaxrs.session.impl.PerRequestCoreProvider;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.3
 * @deprecated since 7.10 see {@link JSONDocumentModelListReader}
 */
@Deprecated
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentModelListReaderTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    protected final PerRequestCoreProvider coreProvider = new PerRequestCoreProvider();

    @Inject
    CoreSession session;

    @Inject
    JsonFactoryManager factoryManager;

    @Before
    public void doBefore() {
        when(request.getUserPrincipal()).thenReturn(session.getPrincipal());
        when(request.getAttribute(SessionFactory.SESSION_FACTORY_KEY)).thenReturn(coreProvider);
    }

    @After
    public void closeWebEngineSession() {
        for (SessionRef ref : coreProvider.getSessions()) {
            ref.destroy();
        }
    }

    @Test
    public void iCanReadADocument() throws Exception {
        DocumentModel note1 = RestServerInit.getNote(1, session);

        String json = JSONDocumentHelper.getDocAsJson(note1);

        JsonParser jp = getParserFor(json);
        DocumentModel doc = JSONDocumentModelReader.readJson(jp, null, request);

        assertNotNull(doc);
        assertEquals(note1.getId(), doc.getId());

    }

    @Test
    public void iCanReadADocumentModelList() throws Exception {
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel note2 = RestServerInit.getNote(2, session);

        String docsAsJson = JSONDocumentHelper.getDocsListAsJson(note1, note2);

        JsonParser jp = getParserFor(docsAsJson);

        DocumentModelList docs = JSONDocumentModelListReader.readRequest(jp, null, request);
        assertEquals(2, docs.size());

        assertEquals(RestServerInit.getNote(1, session).getId(), docs.get(0).getId());

    }

    private JsonParser getParserFor(String docsAsJson) throws IOException, JsonParseException {
        return factoryManager.getJsonFactory().createJsonParser(docsAsJson);
    }

}
