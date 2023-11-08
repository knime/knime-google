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
package org.knime.google.api.analytics.ga4.node.query;

import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObjectSpec;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.api.services.analyticsdata.v1beta.model.DimensionMetadata;
import com.google.api.services.analyticsdata.v1beta.model.Metadata;
import com.google.api.services.analyticsdata.v1beta.model.MetricMetadata;

/**
 *
 * @author Rupert Ettrich
 */
@SuppressWarnings("restriction")
class GAQueryNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    private MockedStatic<GADateRange> dateRangeMock;

    private final static GADateRange testDateRange = GADateRange.lastWeek();

    @BeforeAll
    static void beforeAll() {
        testDateRange.m_fromDate = LocalDate.of(2023, 01, 02);
        testDateRange.m_toDate = LocalDate.of(2023, 02, 03);
    }

    @BeforeEach
    void beginTest() {
        dateRangeMock = Mockito.mockStatic(GADateRange.class);
        dateRangeMock.when(() -> GADateRange.lastWeek()).thenReturn(testDateRange);
    }

    @AfterEach
    void endTest() {
        dateRangeMock.close();
    }

    protected GAQueryNodeSettingsTest() {
        super(Map.of(SettingsType.MODEL, GAQueryNodeSettings.class), new PortObjectSpec[] { createPortObjectSpec() });
    }

    private static GAConnectionPortObjectSpec createPortObjectSpec() {
        // Mock the connection just enough to configure metrics and dimension choices providers in the settings
        final var prop = new GAProperty("424242424242");
        final var metadata = new Metadata()
                .setDimensions(List.of(
                    new DimensionMetadata().setApiName("testDim1"),
                    new DimensionMetadata().setApiName("testDim2")))
                .setMetrics(List.of(
                    new MetricMetadata().setApiName("testMetric1"),
                    new MetricMetadata().setApiName("testMetric2")));
        final var connMock = Mockito.mock(GAConnection.class);
        try {
            Mockito.when(connMock.metadata(prop)).thenReturn(metadata);
        } catch (final KNIMEException e) {
            fail("Failed to set up mock connection.", e);
        }
        return new GAConnectionPortObjectSpec(connMock, prop);
    }
}
