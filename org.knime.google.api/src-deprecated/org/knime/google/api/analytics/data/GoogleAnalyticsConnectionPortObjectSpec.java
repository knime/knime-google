/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.analytics.data;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;
import org.knime.core.node.util.ViewUtils;

/**
 * Specification for the {@link GoogleAnalyticsConnectionPortObject}.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @deprecated
 */
@Deprecated(since = "5.4")
public final class GoogleAnalyticsConnectionPortObjectSpec extends AbstractSimplePortObjectSpec {
    public static final class Serializer
        extends AbstractSimplePortObjectSpecSerializer<GoogleAnalyticsConnectionPortObjectSpec> { }

    private GoogleAnalyticsConnection m_googleAnalyticsConnection;

    /**
     * Constructor for a port object spec that holds no {@link GoogleAnalyticsConnection}.
     */
    public GoogleAnalyticsConnectionPortObjectSpec() {
        m_googleAnalyticsConnection = null;
    }

    /**
     * @param googleAnalyticsConnection The {@link GoogleAnalyticsConnection} that will be contained by this port object spec
     */
    public GoogleAnalyticsConnectionPortObjectSpec(final GoogleAnalyticsConnection googleAnalyticsConnection) {
        m_googleAnalyticsConnection = googleAnalyticsConnection;
    }

    /**
     * @return The contained {@link GoogleAnalyticsConnection} object
     */
    public GoogleAnalyticsConnection getGoogleAnalyticsConnection() {
        return m_googleAnalyticsConnection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        m_googleAnalyticsConnection.save(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_googleAnalyticsConnection = new GoogleAnalyticsConnection(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object ospec) {
        if (this == ospec) {
            return true;
        }
        if (!(ospec instanceof GoogleAnalyticsConnectionPortObjectSpec)) {
            return false;
        }
        GoogleAnalyticsConnectionPortObjectSpec spec = (GoogleAnalyticsConnectionPortObjectSpec)ospec;
        return m_googleAnalyticsConnection.equals(spec.m_googleAnalyticsConnection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_googleAnalyticsConnection != null ? m_googleAnalyticsConnection.hashCode() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        String text;
        if (getGoogleAnalyticsConnection() != null) {
            text = "<html>" + getGoogleAnalyticsConnection().toString().replace("\n", "<br>") + "</html>";
        } else {
            text = "No connection available";
        }
        JPanel f = ViewUtils.getInFlowLayout(new JLabel(text));
        f.setName("Connection");
        return new JComponent[]{f};
    }

}
