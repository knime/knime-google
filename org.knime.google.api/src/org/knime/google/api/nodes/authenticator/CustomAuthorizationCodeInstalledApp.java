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
 *   Aug 31, 2017 (oole): created
 */
package org.knime.google.api.nodes.authenticator;

import java.awt.Desktop;
import java.io.IOException;

import org.knime.core.util.DesktopUtil;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.util.Preconditions;

/**
 * Custom {@link AuthorizationCodeInstalledApp} to fix GTK3/2 problems. Uses {@link DesktopUtil#browse(java.net.URL)}
 * instead of {@link Desktop#isDesktopSupported()} which crashes when launched without the GTK version being set to 2.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class CustomAuthorizationCodeInstalledApp extends AuthorizationCodeInstalledApp {

    /**
     * Constructor.
     *
     * @param flow authorization code flow
     * @param receiver verification code receiver
     */
    public CustomAuthorizationCodeInstalledApp(final AuthorizationCodeFlow flow,
        final VerificationCodeReceiver receiver) {
        super(flow, receiver);
    }

    @Override
    protected void onAuthorization(final AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
        Preconditions.checkNotNull(authorizationUrl);
        // Attempt to open it in the browser
        DesktopUtil.browse(authorizationUrl.toURL());
    }
}
