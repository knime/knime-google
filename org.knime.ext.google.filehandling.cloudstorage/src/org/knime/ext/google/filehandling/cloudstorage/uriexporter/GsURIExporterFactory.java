/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 */
package org.knime.ext.google.filehandling.cloudstorage.uriexporter;

import java.net.URI;

import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStoragePath;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.base.BaseURIExporterMetaInfo;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;

/**
 * {@link URIExporterFactory} implementation using "gs" as scheme.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 * @author Sascha Wolke, KNIME GmbH
 */
public final class GsURIExporterFactory extends NoConfigURIExporterFactory {

    /**
     * ID of this exporter.
     */
    public static final URIExporterID EXPORTER_ID = new URIExporterID("google-cloudstorage-gs");

    private static final String SCHEME = "gs";

    private static final BaseURIExporterMetaInfo META_INFO = new BaseURIExporterMetaInfo("gs:// URL",
            "Generates gs://&lt;bucket&gt;/&lt;object&gt; URLs.");

    private static final GsURIExporterFactory INSTANCE = new GsURIExporterFactory();

    /**
     * @return singleton instance of this exporter factory
     */
    public static GsURIExporterFactory getInstance() {
        return INSTANCE;
    }

    private GsURIExporterFactory() {
        super(META_INFO, p -> {
            final CloudStoragePath csPath = (CloudStoragePath) p.toAbsolutePath();
            return new URI(SCHEME, csPath.getBucketName(), '/' + csPath.getBlobName(), null);
        });
    }

}
