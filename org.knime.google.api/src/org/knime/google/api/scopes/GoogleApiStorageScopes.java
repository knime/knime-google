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
 *   Sep 20, 2023 (Zkriya Rakhimberdiyev, Redfield SE): created
 */
package org.knime.google.api.scopes;

/**
 * String constants for google Storage Scopes https://cloud.google.com/storage/docs/authentication#oauth-scopes
 *
 * @author Zkriya Rakhimberdiyev, Redfield SE
 */
public final class GoogleApiStorageScopes {

    private GoogleApiStorageScopes() {
    }

    /** View and manage data across all Google Cloud services. */
    public static final String CLOUD_PLATFORM = "https://www.googleapis.com/auth/cloud-platform";

    /** View your data across Google Cloud Platform services. */
    public static final String CLOUD_PLATFORM_READ_ONLY = "https://www.googleapis.com/auth/cloud-platform.read-only";

    /** Manage your data and permissions in Google Cloud Storage. */
    public static final String DEVSTORAGE_FULL_CONTROL = "https://www.googleapis.com/auth/devstorage.full_control";

    /** View your data in Google Cloud Storage. */
    public static final String DEVSTORAGE_READ_ONLY = "https://www.googleapis.com/auth/devstorage.read_only";

    /** Manage your data in Google Cloud Storage. */
    public static final String DEVSTORAGE_READ_WRITE = "https://www.googleapis.com/auth/devstorage.read_write";

}
