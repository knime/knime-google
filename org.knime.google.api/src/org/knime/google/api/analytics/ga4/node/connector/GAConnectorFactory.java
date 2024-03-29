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
 *
 * History
 *   24 Feb 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.google.api.analytics.ga4.docs.ExternalLinks;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObject;

/**
 * Factory for the Google Analytics Connector node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui classes
public final class GAConnectorFactory extends WebUINodeFactory<GAConnectorNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
            .name("Google Analytics Connector")//
            .icon("./googleanalyticsconnector.png")//
            .shortDescription("Connect to Google Analytics 4 properties.")//
            .fullDescription("""
                    <p>This node connects to a Google Analytics 4 property using the
                    <a href="%s">Google Analytics Admin API v1</a> and the
                    <a href="%s">Google Analytics Data API v1</a>.</p>

                    <p>The Admin API is used to fetch parent accounts and the respective properties which are
                    available from the connected admin account.</p>

                    <p><b>Note:</b> This node can only be used to connect with Google Analytics 4 properties and is not
                    compatible with Universal Analytics.
                    To migrate a website using Universal Analytics to Google Analytics 4, you can find more information
                    in the <a href="%s">official migration guide</a>.
                    </p>
                    """.formatted(ExternalLinks.API_ADMIN, ExternalLinks.API_DATA, ExternalLinks.EXPLAIN_MIGRATION)
                )//
            .modelSettingsClass(GAConnectorNodeSettings.class)//
            .addInputPort("Google Service Connection", CredentialPortObject.TYPE,
                "The Google Service connection to use.")
            .addOutputPort(GAConnectionPortObject.PORT_NAME, GAConnectionPortObject.TYPE,
                "A connection to a Google Analytics 4 property.")
            .sinceVersion(5, 1, 0)
            .build();

    /**
     * Constructor.
     */
    public GAConnectorFactory() {
        super(CONFIG);
    }

    @Override
    public GAConnectorNodeModel createNodeModel() {
        return new GAConnectorNodeModel(CONFIG);
    }
}
