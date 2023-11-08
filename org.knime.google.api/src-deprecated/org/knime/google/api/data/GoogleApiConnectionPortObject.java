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
package org.knime.google.api.data;

import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.credentials.base.Credential;
import org.knime.credentials.base.CredentialPortObject;

/**
 * Legacy port object containing a {@link Credential} for the Google API.
 *
 * <p>
 * NOTE: This class only exists for backwards compatibility reasons. It is there so we can load partially executed
 * worflows that were created prior to AP 5.2. In those AP versions, this class was not a {@link CredentialPortObject},
 * instead it saved a GoogleApiConnection that contained the node settings of the authenticator node.
 * </p>
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @deprecated Since 5.2. Use {@link CredentialPortObject} instead.
 */
@Deprecated(since = "5.2")
public final class GoogleApiConnectionPortObject extends CredentialPortObject {

    /**
     * Port type.
     */
    @SuppressWarnings("hiding")
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(GoogleApiConnectionPortObject.class);

    /**
     * Optional port type.
     */
    @SuppressWarnings("hiding")
    public static final PortType TYPE_OPTIONAL =
        PortTypeRegistry.getInstance().getPortType(GoogleApiConnectionPortObject.class, true);

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractSimplePortObjectSerializer<GoogleApiConnectionPortObject> {}

    /**
     * Constructor used by the framework.
     */
    public GoogleApiConnectionPortObject() {
        // used by the framework
    }

    /**
     * @param spec The specification of this port object.
     */
    public GoogleApiConnectionPortObject(final GoogleApiConnectionPortObjectSpec spec) {
        super(spec);
    }

    @Override
    public GoogleApiConnectionPortObjectSpec getSpec() {
        return (GoogleApiConnectionPortObjectSpec) super.getSpec();
    }
}
