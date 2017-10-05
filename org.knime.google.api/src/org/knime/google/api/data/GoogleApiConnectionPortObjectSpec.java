/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.google.api.data;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;
import org.knime.core.node.util.ViewUtils;

/**
 * Specification for the {@link GoogleApiConnectionPortObject}.
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public final class GoogleApiConnectionPortObjectSpec extends AbstractSimplePortObjectSpec {
    public static final class Serializer
        extends AbstractSimplePortObjectSpecSerializer<GoogleApiConnectionPortObjectSpec> { }

    private GoogleApiConnection m_googleApiConnection;

    /**
     * Constructor for a port object spec that holds no {@link GoogleApiConnection}.
     */
    public GoogleApiConnectionPortObjectSpec() {
        m_googleApiConnection = null;
    }

    /**
     * @param googleApiConnection The {@link GoogleApiConnection} that will be contained by this port object spec
     */
    public GoogleApiConnectionPortObjectSpec(final GoogleApiConnection googleApiConnection) {
        m_googleApiConnection = googleApiConnection;
    }

    /**
     * @return The contained {@link GoogleApiConnection} object
     */
    public GoogleApiConnection getGoogleApiConnection() {
        return m_googleApiConnection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        m_googleApiConnection.save(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        try {
            m_googleApiConnection = new GoogleApiConnection(model);
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidSettingsException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object ospec) {
        if (this == ospec) {
            return true;
        }
        if (!(ospec instanceof GoogleApiConnectionPortObjectSpec)) {
            return false;
        }
        GoogleApiConnectionPortObjectSpec spec = (GoogleApiConnectionPortObjectSpec)ospec;
        return m_googleApiConnection.equals(spec.m_googleApiConnection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_googleApiConnection != null ? m_googleApiConnection.hashCode() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        String text;
        if (getGoogleApiConnection() != null) {
            text = "<html>" + getGoogleApiConnection().toString().replace("\n", "<br>") + "</html>";
        } else {
            text = "No connection available";
        }
        JPanel f = ViewUtils.getInFlowLayout(new JLabel(text));
        f.setName("Connection");
        return new JComponent[]{f};
    }

}
