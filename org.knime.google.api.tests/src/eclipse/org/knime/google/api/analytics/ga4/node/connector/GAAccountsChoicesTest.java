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
 *   Jan 10, 2025 (lw): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.google.api.analytics.ga4.node.GAAccount;
import org.knime.google.api.analytics.ga4.node.connector.GAConnectorNodeSettings.AnalyticsAccountUpdateHandler;
import org.knime.google.api.analytics.ga4.node.connector.GAConnectorNodeSettings.AnalyticsAccountsProvider;
import org.knime.google.api.credential.GoogleCredential;

/**
 * Tests the {@link AnalyticsAccountsProvider} and {@link AnalyticsAccountUpdateHandler} from the
 * {@link GAConnectorNodeSettings}. These classes all have package scope for testing here.
 */
@SuppressWarnings("restriction") // WebUI* classes
class GAAccountsChoicesTest {

    private static Stream<PortObjectSpec> createEmptyCredentialSpecs() {
        return Stream.of( //
            new CredentialPortObjectSpec(GoogleCredential.TYPE, UUID.randomUUID()), //
            new CredentialPortObjectSpec(), //
            null);
    }

    private static Stream<String> createEmptyAccountSettings() {
        return Stream.of( //
            UUID.randomUUID().toString(), //
            null);
    }

    @Test
    void testAccountChoicesProvider() {
        final var choicesProvider = new GAConnectorNodeSettings.AnalyticsAccountsProvider();

        createEmptyCredentialSpecs().forEach(spec -> {
            final var ctx = DefaultNodeSettings.createDefaultNodeSettingsContext(new PortObjectSpec[]{spec});
            Assertions.assertTrue(choicesProvider.computeState(ctx).isEmpty(), //
                "Account choices were null or not empty.");
        });
    }

    @Test
    void testAccountUpdateHandler() {
        createEmptyCredentialSpecs().forEach(spec -> {
            final var ctx = DefaultNodeSettings.createDefaultNodeSettingsContext(new PortObjectSpec[]{spec});
            createEmptyAccountSettings().forEach(settings -> {
                final var updateHandler = new GAConnectorNodeSettings.AnalyticsAccountUpdateHandler();
                updateHandler.m_analyticsAccountIdSupplier = () -> GAAccount.of(settings);
                Assertions.assertTrue(updateHandler.computeState(ctx).isEmpty(), //
                    "Account choices were null or not empty.");
            });
        });
    }
}
