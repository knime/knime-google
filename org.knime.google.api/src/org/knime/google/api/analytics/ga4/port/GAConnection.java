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
 *   23 Feb 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.port;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.Functions.FailableCallable;
import org.apache.commons.lang3.Functions.FailableFunction;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.data.GoogleApiConnection;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler.BackOffRequired;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.analyticsadmin.v1beta.GoogleAnalyticsAdmin;
import com.google.api.services.analyticsadmin.v1beta.model.GoogleAnalyticsAdminV1betaAccountSummary;
import com.google.api.services.analyticsdata.v1beta.AnalyticsData;
import com.google.api.services.analyticsdata.v1beta.model.Metadata;
import com.google.api.services.analyticsdata.v1beta.model.RunReportRequest;
import com.google.api.services.analyticsdata.v1beta.model.RunReportResponse;

/**
 * The {@link GAConnection} provides request methods against the
 * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1">Google Analytics Data API</a>
 * and
 * <a href="https://developers.google.com/analytics/devguides/config/admin/v1">Google Analytics Admin API</a>.
 *
 * The <em>Google Analytics Data API v1</em> gives programmatic access to Google Analytics 4 (GA4) report data.
 * It can only be used to access Google Analytics 4 properties and is <em>not</em>
 * compatible with Universal Analytics.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class GAConnection {

    private final GoogleApiConnection m_connection;

    /** Timeout for connecting to the remote endpoint. */
    private final Duration m_connectTimeout;
    /** Default timeout for connecting to the remote endpoint. */
    // 30 seconds since the default of 20 before seemed to be not enough often (see "outdated" PR comment in AP-15929)
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    /** Key for storing the connection timeout. */
    public static final String KEY_CONNECT_TIMEOUT = "connectTimeout";

    /** Timeout for reading from an already established connection. */
    private final Duration m_readTimeout;
    /** Default timeout for reading from an established connection. */
    // Default increased from 20s to 30s (see reason above).
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    /** Key for storing the read timeout. */
    public static final String KEY_READ_TIMEOUT = "readTimeout";

    /** Maximum elapsed time to spend on retries on server errors. */
    private final Duration m_serverErrorRetryMaxElapsedTime;
    /** Default maximum time to spend on server error retries. */
    public static final Duration DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME = Duration.ofSeconds(60);
    /** Key for storing the maximum time to spend on server error retries. */
    public static final String KEY_ERR_RETRY_MAX_ELAPSED_TIME = "serverErrorRetryMaxElapsedTime";


    /**
     * Creates a Google Analytics 4 (GA4) connection to the given GA4 property, using the given
     * {@link GoogleApiConnection Google API connection}.
     *
     * @param connection connection to Google API to use
     * @param connectTimeout timeout for connecting to remote endpoint
     * @param readTimeout timeout for reading from remote connection
     * @param retryMaxElapsedTime maximum time to spend on retrying the same request
     */
    public GAConnection(final GoogleApiConnection connection,
            final Duration connectTimeout, final Duration readTimeout, final Duration retryMaxElapsedTime) {
        m_connectTimeout = Objects.requireNonNull(connectTimeout);
        m_readTimeout = Objects.requireNonNull(readTimeout);
        m_connection = Objects.requireNonNull(connection);
        m_serverErrorRetryMaxElapsedTime = retryMaxElapsedTime;
    }


    /**
     * Restores a {@link GAConnection} from a saved model (used by the framework).
     *
     * @param model model containing connection information
     * @return the Google Analytics connection
     * @throws InvalidSettingsException if the model did not contain the required settings, there was a problem with
     *          the key file, or the key file was not accessible
     */
    static GAConnection loadFrom(final ModelContentRO model) throws InvalidSettingsException {
        final var ct = getDurationFromSettings(model, KEY_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
        final var rt = getDurationFromSettings(model, KEY_READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
        final var retryTime = getDurationFromSettings(model, KEY_ERR_RETRY_MAX_ELAPSED_TIME,
            DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME);
        try {
            return new GAConnection(new GoogleApiConnection(model), ct, rt, retryTime);
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidSettingsException("Unable to load Google Analytics 4 (GA4) connection.", e);
        }
    }

    /**
     * Saves the connection information to the model.
     *
     * @param model model to save into
     */
    void saveTo(final ModelContentWO model) {
        addDurationToSettings(model, KEY_CONNECT_TIMEOUT, m_connectTimeout);
        addDurationToSettings(model, KEY_READ_TIMEOUT, m_readTimeout);
        addDurationToSettings(model, KEY_ERR_RETRY_MAX_ELAPSED_TIME, m_serverErrorRetryMaxElapsedTime);
        m_connection.save(model);
    }

    private static Duration getDurationFromSettings(final ConfigRO cfg, final String keyDuration,
            final Duration defaultValue) {
        return Duration.ofMillis(Math.max(cfg.getLong(keyDuration, defaultValue.toSeconds()), 0));
    }

    private static void addDurationToSettings(final ConfigWO cfg, final String keyConnectTimeout,
            final Duration connectTimeout) {
        cfg.addLong(keyConnectTimeout, Math.max(connectTimeout.toMillis(), 0));
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder(m_connection.toString());
        sb.append(String.format("Connection timeout: %ds%n", m_connectTimeout.toSeconds()));
        sb.append(String.format("Read timeout: %ds%n", m_readTimeout.toSeconds()));
        sb.append(String.format("Retry maximum elapsed time: %ds%n", m_serverErrorRetryMaxElapsedTime.toSeconds()));
        return sb.toString();
    }

    /**
     * Get available account summaries from the connection.
     *
     * @param connection Google API connection to use
     * @param connectTimeout connection timeout
     * @param readTimeout read timeout
     * @param retryMaxElapsedTime maximum elapsed time after the first request for retries
     * @return available account summaries or empty list if there are no accounts
     * @throws IOException exception thrown from underlying API requests
     */
    private static final List<GoogleAnalyticsAdminV1betaAccountSummary> accountSummaries(
            final GoogleApiConnection connection, final Duration connectTimeout, final Duration readTimeout,
            final Duration retryMaxElapsedTime) throws IOException {
        final var result = withAdminAPI(connection, connectTimeout, readTimeout, retryMaxElapsedTime,
            admin -> admin.accountSummaries().list().execute().getAccountSummaries());
        return result != null ? result : Collections.emptyList();
    }

    /**
     * Get available account summaries from the connection.
     *
     * @return available account summaries
     * @throws IOException exception throw from underlying API requests
     */
    public List<GoogleAnalyticsAdminV1betaAccountSummary> accountSummaries() throws IOException {
        return exceptionsWrapped(
            () -> accountSummaries(m_connection, m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime));
    }

    /**
     * Get
     * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/properties/getMetadata">metadata</a>
     * for a given property, such as dimensions and metrics.
     *
     * @param property Google Analytics 4 property
     * @param connection Google API connection to use
     * @param connectTimeout connection timeout
     * @param readTimeout read timeout
     * @param retryMaxElapsedTime maximum elapsed time after the first request for retries
     * @return available metadata for the property
     * @throws IOException exception thrown from underlying API requests
     */
    public static final Metadata metadata(final GAProperty property,
        final GoogleApiConnection connection, final Duration connectTimeout, final Duration readTimeout,
        final Duration retryMaxElapsedTime) throws IOException {
        return withDataAPI(connection, connectTimeout, readTimeout, retryMaxElapsedTime, //
            data -> data.properties().getMetadata( //
                String.format("properties/%s/metadata", property.getPropertyId())).execute());
    }

    /**
     * Get metadata for a given property.
     * @param property Google Analytics 4 property
     * @return available metadata for the property
     * @throws IOException exception throw from underlying API requests
     */
    public Metadata metadata(final GAProperty property) throws IOException {
        return exceptionsWrapped( //
            () -> metadata(property, m_connection, m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime));
    }

    /**
     * Run a report request against the configured property.
     *
     * @param property Google Analytics property to run the report against
     * @param req report request to run
     * @return the response for the request
     * @throws IOException exception throw from underlying API requests
     */
    public RunReportResponse runReport(final GAProperty property, final RunReportRequest req) throws IOException {
        return exceptionsWrapped(() -> //
            withDataAPI(m_connection, m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime, //
                data -> data.properties().runReport("properties/" + property.getPropertyId(), req).execute()));
    }

    /* Static utility methods (API requests, error handling, ...) */

    private static <O> O exceptionsWrapped(final FailableCallable<O, IOException> callable) throws IOException {
        // We also wrap the "Unknown Host" exception (and do this separately to the IOException),
        // since the message is not very informative otherwise
        try {
            return callable.call();
        } catch (GoogleJsonResponseException ge) {
                throw wrapGoogleJsonResponseException(ge);
        } catch (UnknownHostException uh) {
                throw wrapUnknownHostException(uh);
        } catch (IOException io) {
            throw wrapGenericIOException(io);
        }
    }

    /**
     * Make a request against the Admin API.
     * @param <R> type of the value returned from the request
     * @param connection underlying Google API connection to use
     * @param connectTimeout timeout to establish a connection
     * @param readTimeout timeout to read from an established connection
     * @param retryMax maximum elapsed time to spend on retrying the same request from the point in time when the
     *            request was first made
     * @param callable function retrieving data from the Admin API
     * @return a user defined value extracted from the Admin API
     * @throws IOException
     */
    private static final <R> R withAdminAPI(final GoogleApiConnection connection, final Duration connectTimeout,
            final Duration readTimeout, final Duration retryMax,
            final FailableFunction<GoogleAnalyticsAdmin, R, IOException> callable) throws IOException {
        return withAPI(connection, connectTimeout, readTimeout, retryMax, "KNIME-Google-Analytics-4-Connector",
            GoogleAnalyticsAdmin.Builder::new, callable);
    }

    /**
     * Make a request against the Data API.
     * @param <R> type of the value returned from the request
     * @param connection underlying Google API connection to use
     * @param connectTimeout timeout to establish a connection
     * @param readTimeout timeout to read from an established connection
     * @param retryMax maximum elapsed time to spend on retrying the same request from the point in time when the
     *            request was first made
     * @param callable function retrieving data from the Data API
     * @return a user defined value extracted from the Data API
     * @throws IOException
     */
    private static final <R> R withDataAPI(final GoogleApiConnection connection, final Duration connectTimeout,
            final Duration readTimeout, final Duration retryMax,
            final FailableFunction<AnalyticsData, R, IOException> callable) throws IOException {
        return withAPI(connection, connectTimeout, readTimeout, retryMax, "KNIME-Google-Analytics-4-Query",
            AnalyticsData.Builder::new, callable);
    }

    private static final <R, C extends AbstractGoogleJsonClient> R
        withAPI(final GoogleApiConnection connection, final Duration connectTimeout,
            final Duration readTimeout, final Duration retryMax,
            final String appName,
            final ToClientBuilder<C.Builder> builder,
            final FailableFunction<C, R, IOException> callable) throws IOException {
        @SuppressWarnings("unchecked")
        final var client = (C) builder.build(GoogleApiConnection.getHttpTransport(),
                GoogleApiConnection.getJsonFactory(),
                configureRequestInitializer(connection, connectTimeout, readTimeout, retryMax))
                .setApplicationName(appName).build();
        return callable.apply(client);
    }

    @FunctionalInterface
    private interface ToClientBuilder<B> {
        B build(HttpTransport transport, JsonFactory json, HttpRequestInitializer init);
    }

    private static HttpRequestInitializer wrap(final HttpRequestInitializer wrapped,
        final Consumer<HttpRequest> requestInitialization) {
        Objects.requireNonNull(wrapped);
        Objects.requireNonNull(requestInitialization);
        return new HttpRequestInitializer() {
            @Override
            public void initialize(final HttpRequest request) throws IOException {
                wrapped.initialize(request);
                requestInitialization.accept(request);
            }
        };
    }

    private static HttpRequestInitializer configureRequestInitializer(final GoogleApiConnection connection,
            final Duration connectTimeout, final Duration readTimeout, final Duration retryIOMaxElapsedTime) {
        HttpRequestInitializer init = CheckUtils.checkNotNull(connection.getCredential(),
                "Google API credentials missing. Re-execute Google Authenticator node.");
        init = wrap(init, request -> {
            request.setConnectTimeout((int)connectTimeout.toMillis());
            request.setReadTimeout((int)readTimeout.toMillis());
        });
        final var retryMaxMillis = (int)retryIOMaxElapsedTime.toMillis();
        if (retryMaxMillis > 0) {
            // Could also limit numer of retries: request#setNumberOfRetries
            init = wrap(init,  request -> {
                request.setIOExceptionHandler(
                    new HttpBackOffIOExceptionHandler(new ExponentialBackOff.Builder()
                        .setMaxElapsedTimeMillis(retryMaxMillis).build()));
                request.setUnsuccessfulResponseHandler(
                    new HttpBackOffUnsuccessfulResponseHandler(
                        new ExponentialBackOff.Builder().setMaxElapsedTimeMillis(retryMaxMillis).build())
                        .setBackOffRequired(new ConditionalBackOff(
                            // Since Google also suggests to retry on request timeout, we include it here
                            List.of(BackOffRequired.ON_SERVER_ERROR, ConditionalBackOff.ON_REQUEST_TIMEOUT))));
            });
        }
        return init;
    }

    private static IOException wrapGoogleJsonResponseException(final GoogleJsonResponseException e) {
        final var sb = new StringBuilder();
        sb.append(String.format("[Error %d]: %s", e.getStatusCode(), e.getStatusMessage()));
        for (final var err : e.getDetails().getErrors()) {
            final var msg = err.getMessage();
            if (msg != null) {
                sb.append(msg);
            }
        }
        return new IOException(sb.toString(), e);
    }

    private static IOException wrapUnknownHostException(final UnknownHostException e) {
        return new IOException(String.format("Unknown host: %s", e.getMessage()));
    }

    private static IOException wrapGenericIOException(final IOException e) {
        return new IOException(String.format("Error while contacting Google Analytics: %s", e.getMessage()), e);
    }

    static final class ConditionalBackOff implements BackOffRequired {

        static final BackOffRequired ON_REQUEST_TIMEOUT = new OnRequestTimeout();
        static final BackOffRequired ON_TOO_MANY_REQUESTS = new OnTooManyRequests();

        private List<BackOffRequired> m_subs;

        ConditionalBackOff(final List<BackOffRequired> subs) {
            m_subs = subs;
        }

        @Override
        public boolean isRequired(final HttpResponse response) {
            return m_subs.stream().anyMatch(b -> b.isRequired(response));
        }

    }

    private static final class OnRequestTimeout implements BackOffRequired {
        @Override
        public boolean isRequired(final HttpResponse response) {
            // Google suggests to retry using truncated exponential backoff.
            // https://cloud.google.com/storage/docs/json_api/v1/status-codes#408_Request_Timeout
            return response.getStatusCode() == 408; // Request Timeout
        }
    }

    private static final class OnTooManyRequests implements BackOffRequired {
        @Override
        public boolean isRequired(final HttpResponse response) {

            // Exceeding property quotas leads to this error.
            // https://cloud.google.com/storage/docs/json_api/v1/status-codes#429_Too_Many_Requests

            // Note: the Google Analytics Data API (currently) does NOT include a `RetryInfo` field in the response!
            // https://cloud.google.com/apis/design/errors#error_details

            // Google suggests to retry using truncated exponential backoff
            return response.getStatusCode() == 429; // Too Many Requests
        }
    }

}
