/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;

public interface PictureResourceAdapter {

    void setDocumentModel(DocumentModel doc);

    /**
     * @deprecated since 5.7. Use
     *             {@link #fillPictureViews(org.nuxeo.ecm.core.api.Blob, String, String, java.util.ArrayList)} instead.
     */
    @Deprecated
    boolean createPicture(Blob fileContent, String filename, String title,
            ArrayList<Map<String, Object>> pictureConversions) throws IOException;

    /**
     * Fill this Picture views using the given {@code pictureConversions} and {@code blob} to compute the picture views.
     * <p>
     * The {@code blob} is converted to fit the defined {@code pictureConversions}.
     *
     * @since 5.7
     */
    boolean fillPictureViews(Blob blob, String filename, String title, ArrayList<Map<String, Object>> pictureConversions)
            throws IOException;

    /**
     * This method just delegate the job to
     * {@link PictureResourceAdapter#fillPictureViews(Blob, String, String, ArrayList)} by passing null instead of
     * statics picture templates. <br/>
     * <br/>
     * This will fill the picture views by using the registered picture templates.
     *
     * @see {@link PictureResourceAdapter#fillPictureViews(Blob, String, String, ArrayList)}
     * @since 6.9.6
     */
    boolean fillPictureViews(Blob blob, String filename, String title) throws IOException;

    /**
     * Pre-fill this Picture views using the given {@code pictureConversions} and {@code blob}.
     * <p>
     * The {@code blob} is not converted and just stored as the Blob of the picture views.
     *
     * @since 5.7
     */
    void preFillPictureViews(Blob blob, List<Map<String, Object>> pictureConversions, ImageInfo imageInfo)
            throws IOException;

    void doRotate(int angle);

    void doCrop(String coords);

    Blob getPictureFromTitle(String title) throws PropertyException;

    /**
     * Returns the XPath of the given view name, or {@code null} if the view is not found on the Picture.
     *
     * @param viewName the view name
     */
    String getViewXPath(String viewName);

    /**
     * Convenience method to get the XPath of the first view of the Picture.
     *
     * @return the XPath of the first view
     */
    String getFirstViewXPath();

}
