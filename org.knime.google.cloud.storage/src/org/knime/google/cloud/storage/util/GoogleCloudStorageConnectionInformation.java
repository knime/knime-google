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
package org.knime.google.cloud.storage.util;

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
import org.knime.google.cloud.storage.filehandler.GoogleCSRemoteFileHandler;

import com.google.auth.Credentials;

/**
 * Google cloud connection informations with API connection and project identifier.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class GoogleCloudStorageConnectionInformation extends CloudConnectionInformation {

    private static final long serialVersionUID = 1L;

    private static final String SERVICE_NAME = "Google Cloud Storage";

    private CredentialRef m_credentialRef;

    private static final String CFG_PROJECT = "googleCloudProjectId";
    private String m_project;

    /**
     * Default constructor.
     *
     * @param credentialRef Goolge API connection with credentials
     * @param project Unique Google Cloud project identifier
     */
    public GoogleCloudStorageConnectionInformation(final CredentialRef credentialRef, final String project) {
        m_credentialRef = credentialRef;
        m_project = project;
        setProtocol(GoogleCSRemoteFileHandler.PROTOCOL.getName());
        setHost(m_project);
    }

    GoogleCloudStorageConnectionInformation(final ModelContentRO model) throws InvalidSettingsException {
        m_credentialRef = CredentialRefSerializer.loadRefWithLegacySupport(model);
        m_project = model.getString(CFG_PROJECT, "");
        setProtocol(GoogleCSRemoteFileHandler.PROTOCOL.getName());
        setHost(m_project);
    }

    /**
     * @return the credentials
     * @throws CredentialNotFoundException
     */
    public Credentials getCredentials() throws CredentialNotFoundException {
        return m_credentialRef.resolveCredential(GoogleCredential.class).getCredentials();
    }

    /**
     * @return Unique Google Cloud project identifier
     */
    public String getProject() {
        return m_project;
    }

    @Override
    public void save(final ModelContentWO model) {
        CredentialRefSerializer.saveRef(m_credentialRef, model);
        model.addString(CFG_PROJECT, m_project);
    }

    /**
     * Load connection informations from stored model.
     */
    public static GoogleCloudStorageConnectionInformation load(final ModelContentRO model) throws InvalidSettingsException {
        return new GoogleCloudStorageConnectionInformation(model);
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public int hashCode() {
        final var builder = new HashCodeBuilder();
        return builder.appendSuper(super.hashCode())//
            .append(m_credentialRef)//
            .append(m_project)//
            .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GoogleCloudStorageConnectionInformation other = (GoogleCloudStorageConnectionInformation)obj;

        return new EqualsBuilder().appendSuper(super.equals(obj))//
                .append(m_credentialRef, other.m_credentialRef)//
                .append(m_project, other.m_project)//
                .isEquals();
    }
}
