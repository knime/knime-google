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
 *   19 Jul 2023 (Rupert Ettrich): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.credentials.base.CredentialCache;
import org.knime.google.api.analytics.ga4.node.connector.GAConnectorNodeSettings.AnalyticsPropertiesProvider;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.google.api.credential.GoogleCredential;
import org.knime.google.api.data.GoogleApiConnectionPortObjectSpec;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.google.api.services.analyticsadmin.v1beta.model.GoogleAnalyticsAdminV1betaAccountSummary;

/**
 *
 * @author Rupert Ettrich
 */
class GAConnectorNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    MockedConstruction<AnalyticsPropertiesProvider> mockedPropertiesProviderConstruction;

    @BeforeEach
    void beforeTest() {
        mockedPropertiesProviderConstruction =
            Mockito.mockConstruction(AnalyticsPropertiesProvider.class, (mock, context) -> {
                when(mock.choices(ArgumentMatchers.any())).thenReturn(new String[]{"choice1", "choice2"});
                when(mock.choicesWithIdAndText(ArgumentMatchers.any()))
                    .thenReturn(new IdAndText[]{IdAndText.fromId("choice1"), IdAndText.fromId("choice2")});
            });
    }

    @AfterEach
    void afterTest() {
        mockedPropertiesProviderConstruction.close();
    }

    @SuppressWarnings("restriction")
    protected GAConnectorNodeSettingsTest() {
        super(Map.of(SettingsType.MODEL, GAConnectorNodeSettings.class), new PortObjectSpec[]{createPortObjectSpec()});
    }

    private static GoogleApiConnectionPortObjectSpec createPortObjectSpec() {

        final var gaConnMock = Mockito.mock(GAConnection.class);
        try {
            Mockito.when(gaConnMock.accountSummaries())
                .thenReturn(List.of(new GoogleAnalyticsAdminV1betaAccountSummary().setAccount("accounts/424242")
                    .setName("accountsummaries/424242")));
        } catch (KNIMEException e) {
            fail("Failed to set up mock connection.", e);
        }
        final var credentials = Mockito.mock(GoogleCredential.class);
        final var cacheId = CredentialCache.store(credentials);
        return new GoogleApiConnectionPortObjectSpec(cacheId);
    }
}
