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
 *   Nov 2, 2023 (bjoern): created
 */
package org.knime.google.api.nodes.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

import com.google.api.client.util.SecurityUtils;
import com.google.auth.oauth2.ServiceAccountCredentials;

/**
 * Helper class to load {@link ServiceAccountCredentials} from json/p12 files.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class ServiceAccountCredentialsUtil {

    private ServiceAccountCredentialsUtil() {
    }

    private static PrivateKey loadPrivateKeyFromP12(final InputStream stream) throws IOException {
        try {
            return SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(), stream, "notasecret",
                "privatekey", "notasecret");
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to load private key: " + e.getMessage(), e);
        }
    }

    /**
     * Loads {@link ServiceAccountCredentials} from the given key file in p12 format.
     *
     * @param serviceAccountEmail The service account email.
     * @param keyFilePath Path to the p12 key file.
     * @return the loaded {@link ServiceAccountCredentials}
     * @throws IOException If something went wrong while loading, e.g. the key file was not found or had the wrong
     *             format.
     */
    public static ServiceAccountCredentials loadFromP12(final String serviceAccountEmail, final Path keyFilePath)
        throws IOException {

        final PrivateKey privKey;

        try (final var in = Files.newInputStream(keyFilePath)) {
            privKey = loadPrivateKeyFromP12(in);
        } catch (NoSuchFileException e) {
            throw ExceptionUtil.createFormattedNoSuchFileException(e, "File");
        } catch (AccessDeniedException e) { // NOSONAR
            throw ExceptionUtil.createAccessDeniedException(keyFilePath);
        }

        return ServiceAccountCredentials.newBuilder()//
            .setHttpTransportFactory(GoogleApiUtil::getHttpTransport)//
            .setClientEmail(serviceAccountEmail)//
            .setPrivateKey(privKey)//
            .build();
    }

    /**
     * Loads {@link ServiceAccountCredentials} from the given input stream which must provide access to the contents of
     * a p12 file.
     *
     * @param serviceAccountEmail The service account email.
     * @param p12InputStream Input stream from which to read the p12 file.
     * @return the loaded {@link ServiceAccountCredentials}
     * @throws IOException If something went wrong while loading, e.g. the key file had the wrong format.
     */
    public static ServiceAccountCredentials loadFromP12(final String serviceAccountEmail,
        final InputStream p12InputStream) throws IOException {

        final var privateKey = loadPrivateKeyFromP12(p12InputStream);

        return ServiceAccountCredentials.newBuilder()//
            .setHttpTransportFactory(GoogleApiUtil::getHttpTransport)//
            .setClientEmail(serviceAccountEmail)//
            .setPrivateKey(privateKey)//
            .build();
    }

    /**
     * Loads {@link ServiceAccountCredentials} from the given key file in json format.
     *
     * @param keyFilePath Path to the json key file.
     * @return the loaded {@link ServiceAccountCredentials}
     * @throws IOException If something went wrong while loading, e.g. the key file was not found or had the wrong
     *             format.
     */
    public static ServiceAccountCredentials loadFromJson(final Path keyFilePath) throws IOException {

        try (final var inputStream = Files.newInputStream(keyFilePath)) {
            return ServiceAccountCredentials.fromStream(inputStream, GoogleApiUtil::getHttpTransport);
        } catch (NoSuchFileException e) {
            throw ExceptionUtil.createFormattedNoSuchFileException(e, "File");
        } catch (AccessDeniedException e) { // NOSONAR
            throw ExceptionUtil.createAccessDeniedException(keyFilePath);
        }
    }
}
