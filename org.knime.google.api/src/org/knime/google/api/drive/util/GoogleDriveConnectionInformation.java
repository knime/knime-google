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
 *   Jun 15, 2018 (jtyler): created
 */
package org.knime.google.api.drive.util;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.cloud.core.util.port.CloudConnectionInformation;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.CredentialRef.CredentialNotFoundException;
import org.knime.google.api.credential.CredentialRefSerializer;
import org.knime.google.api.credential.GoogleCredential;
import org.knime.google.api.drive.filehandler.GoogleDriveRemoteFileHandler;

import com.google.auth.Credentials;

/**
 * Extended {@link CloudConnectionInformation} for the Google Drive Connection.
 *
 * @author jtyler
 */
public class GoogleDriveConnectionInformation extends CloudConnectionInformation {

    private static final long serialVersionUID = 1L;

    private CredentialRef m_credentialRef;

    /**
     * @param credentials Google Credentials
     */
    public GoogleDriveConnectionInformation(final CredentialRef credentialRef) {
        m_credentialRef = Objects.requireNonNull(credentialRef);
        setProtocol(GoogleDriveRemoteFileHandler.PROTOCOL.getName());
        setHost("google-drive-api");
        setUser("user");
    }

    /**
     * @param model
     * @throws InvalidSettingsException
     */
    public GoogleDriveConnectionInformation(final ModelContentRO model)
            throws InvalidSettingsException {
        super(model);
        m_credentialRef = CredentialRefSerializer.loadRefWithLegacySupport(model);
    }

    @Override
    public void save(final ModelContentWO model) {
        super.save(model);
        CredentialRefSerializer.saveRef(m_credentialRef, model);
    }

    /**
     * @return the credentials
     * @throws CredentialNotFoundException
     */
    public Credentials getCredentials() throws CredentialNotFoundException {
        return m_credentialRef.resolveCredential(GoogleCredential.class).getCredentials();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final GoogleDriveConnectionInformation rhs = (GoogleDriveConnectionInformation)obj;
        return new EqualsBuilder().appendSuper(super.equals(obj))
                .append(m_credentialRef, rhs.m_credentialRef).isEquals();
    }

    @Override
    public int hashCode() {
        final var builder = new HashCodeBuilder();
        return builder.appendSuper(super.hashCode())//
            .append(m_credentialRef)//
            .toHashCode();
    }
}
