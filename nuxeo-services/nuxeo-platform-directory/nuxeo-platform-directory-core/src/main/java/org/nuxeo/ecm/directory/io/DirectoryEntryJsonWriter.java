/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.directory.io;

import static java.util.Locale.ENGLISH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertiesJsonReader;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.MaxDepthReachedException;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link DirectoryEntry} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link DirectoryEntry}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(DirectoryEntry, JsonWriter)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *   "entity-type": "directoryEntry",
 *   "directoryName": "DIRECTORY_NAME", <- use it to update an existing document
 *   "properties": {
 *     <- entry properties depending on the directory schema (password fields are hidden)
 *     <- format is managed by {@link DocumentPropertiesJsonReader}
 *   }
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DirectoryEntryJsonWriter extends ExtensibleEntityJsonWriter<DirectoryEntry> {

    public static final String ENTITY_TYPE = "directoryEntry";

    private static final String MESSAGES_BUNDLE = "messages";

    private static final Log log = LogFactory.getLog(DirectoryEntryJsonWriter.class);

    @Inject
    private SchemaManager schemaManager;

    @Inject
    private DirectoryService directoryService;

    public DirectoryEntryJsonWriter() {
        super(ENTITY_TYPE, DirectoryEntry.class);
    }

    @Override
    protected void writeEntityBody(DirectoryEntry entry, JsonGenerator jg) throws IOException {
        String directoryName = entry.getDirectoryName();
        DocumentModel document = entry.getDocumentModel();
        String schemaName = directoryService.getDirectorySchema(directoryName);
        String passwordField = directoryService.getDirectoryPasswordField(directoryName);
        jg.writeStringField("directoryName", directoryName);
        Schema schema = schemaManager.getSchema(schemaName);
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        // for each properties, fetch it
        jg.writeObjectFieldStart("properties");
        Set<String> translated = ctx.getTranslated(ENTITY_TYPE);
        Set<String> fetched = ctx.getFetched(ENTITY_TYPE);
        for (Field field : schema.getFields()) {
            QName fieldName = field.getName();
            String key = fieldName.getLocalName();
            jg.writeFieldName(key);
            if (key.equals(passwordField)) {
                jg.writeString("");
            } else {
                Property property = document.getProperty(fieldName.getPrefixedName());
                boolean managed = false;
                Object value = property.getValue();
                if (value != null && value instanceof String) {
                    String valueString = (String) value;
                    if (fetched.contains(fieldName.getLocalName())) {
                        // try to fetch a referenced entry (parent for example)
                        try (Closeable resource = ctx.wrap().controlDepth().open()) {
                            managed = writeFetchedValue(jg, directoryName, fieldName.getLocalName(), valueString);
                        } catch (MaxDepthReachedException e) {
                            managed = false;
                        }
                    } else if (translated.contains(fieldName.getLocalName())) {
                        // try to fetch a translated property
                        managed = writeTranslatedValue(jg, fieldName.getLocalName(), valueString);
                    }
                }
                if (!managed) {
                    propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE,
                            new OutputStreamWithJsonWriter(jg));
                }
            }
        }
        jg.writeEndObject();
    }

    protected boolean writeFetchedValue(JsonGenerator jg, String directoryName, String fieldName, String value)
            throws IOException {
        Session session = directoryService.open(directoryName);
        try {
            DocumentModel entryModel = session.getEntry(value);
            if (entryModel != null) {
                DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
                writeEntity(entry, jg);
                return true;
            }
        } finally {
            session.close();
        }
        return false;
    }

    protected boolean writeTranslatedValue(JsonGenerator jg, String fieldName, String value) throws IOException {
        Locale locale = ctx.getLocale();
        String msg = getMessageString(value, new Object[0], locale);
        if (msg == null && locale != ENGLISH) {
            msg = getMessageString(value, new Object[0], ENGLISH);
        }
        if (msg != null && !msg.equals(value)) {
            jg.writeString(msg);
            return true;
        }
        return false;
    }

    public static String getMessageString(String key, Object[] params, Locale locale) {
        try {
            return I18NUtils.getMessageString(MESSAGES_BUNDLE, key, params, locale);
        } catch (MissingResourceException e) {
            log.trace("No bundle found", e);
            return null;
        }
    }

}