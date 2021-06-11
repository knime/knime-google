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
 *   2020-11-23 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.fs;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

/**
 * Helper for invoking with retry.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
final class RetryHelper {

    private static final int DEFAULT_MAX_RETRY_COUNT = 8;

    private RetryHelper() {
    }

    /**
     * @param <T>
     *            return type.
     * @param retryable
     *            retryable argument
     * @return execution result.
     * @throws IOException
     */
    @SuppressWarnings("null")
    public static <T> T doWithRetryable(final IoRetryable<T> retryable) throws IOException {

        IOException savedEx = null;
        Duration currSleepTime = Duration.ofSeconds(1);

        for (int i = 0; i < DEFAULT_MAX_RETRY_COUNT; i++) {
            try {
                return retryable.invoke();
            } catch (IOException ex) {
                if (isExceededRateLimit(ex) && (i + 1 < DEFAULT_MAX_RETRY_COUNT)) {
                    savedEx = ex;
                    doPause(currSleepTime);
                    currSleepTime = currSleepTime.multipliedBy(2);
                } else {
                    throw ex;
                }
            }
        }

        throw savedEx; // NOSONAR cannot be null
    }

    private static void doPause(final Duration sleepTime) throws InterruptedIOException {
        try {
            Thread.sleep(sleepTime.toMillis());
        } catch (InterruptedException ex) { // NOSONAR
            throw new InterruptedIOException(); // NOSONAR
        }
    }

    private static boolean isExceededRateLimit(final IOException exc) {
        if (exc instanceof GoogleJsonResponseException) {
            final GoogleJsonResponseException googleEx = (GoogleJsonResponseException) exc;
            final GoogleJsonError parsedError = googleEx.getDetails();
            if (parsedError != null //
                    && parsedError.getCode() == 403 //
                    && parsedError.getErrors().stream().anyMatch(e -> e.getDomain().equals("usageLimits"))) {
                return true;
            }
        }

        return false;
    }
}