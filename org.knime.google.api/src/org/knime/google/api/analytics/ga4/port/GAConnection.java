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
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.Functions.FailableCallable;
import org.apache.commons.lang3.Functions.FailableFunction;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.message.Message;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.credential.CredentialRefSerializer;
import org.knime.google.api.credential.CredentialUtil;
import org.knime.google.api.nodes.util.GoogleApiUtil;

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
import com.google.api.services.analyticsadmin.v1beta.GoogleAnalyticsAdmin.AccountSummaries;
import com.google.api.services.analyticsadmin.v1beta.GoogleAnalyticsAdmin.Properties;
import com.google.api.services.analyticsadmin.v1beta.model.GoogleAnalyticsAdminV1betaAccountSummary;
import com.google.api.services.analyticsadmin.v1beta.model.GoogleAnalyticsAdminV1betaListAccountSummariesResponse;
import com.google.api.services.analyticsadmin.v1beta.model.GoogleAnalyticsAdminV1betaListPropertiesResponse;
import com.google.api.services.analyticsadmin.v1beta.model.GoogleAnalyticsAdminV1betaProperty;
import com.google.api.services.analyticsdata.v1beta.AnalyticsData;
import com.google.api.services.analyticsdata.v1beta.model.Metadata;
import com.google.api.services.analyticsdata.v1beta.model.RunReportRequest;
import com.google.api.services.analyticsdata.v1beta.model.RunReportResponse;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

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

    /**
     * A {@link CredentialRef} through which the credential to use can be resolved. Resolving may fail, after a
     * partially executed workflow has been loaded.
     */
    private CredentialRef m_credentialRef;

    /** Timeout for connecting to the remote endpoint. */
    private Duration m_connectTimeout;
    /** Default timeout for connecting to the remote endpoint. */
    // 30 seconds since the default of 20 before seemed to be not enough often (see "outdated" PR comment in AP-15929)
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    /** Key for storing the connection timeout. */
    public static final String KEY_CONNECT_TIMEOUT = "connectTimeout";

    /** Timeout for reading from an already established connection. */
    private Duration m_readTimeout;
    /** Default timeout for reading from an established connection. */
    // Default increased from 20s to 30s (see reason above).
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    /** Key for storing the read timeout. */
    public static final String KEY_READ_TIMEOUT = "readTimeout";

    /** Maximum elapsed time to spend on retries on server errors. */
    private Duration m_serverErrorRetryMaxElapsedTime;
    /** Default maximum time to spend on server error retries. */
    public static final Duration DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME = Duration.ofSeconds(60);
    /** Key for storing the maximum time to spend on server error retries. */
    public static final String KEY_ERR_RETRY_MAX_ELAPSED_TIME = "serverErrorRetryMaxElapsedTime";

    /**
     * Prefix all fetched GA account IDs have.
     * We are only interested in the numerical value that comes after the prefix.
     */
    public static final String ACCOUNTS_PREFIX = "accounts/";
    /**
     * Prefix all fetched GA properties IDs have. Same as for accounts.
     */
    public static final String PROPERTIES_PREFIX = "properties/";

    /**
     * Creates a Google Analytics 4 (GA4) connection using the given {@link GoogleCredentials}.
     *
     * @param credentialRef The {@link CredentialRef} to use.
     * @param connectTimeout timeout for connecting to remote endpoint
     * @param readTimeout timeout for reading from remote connection
     * @param retryMaxElapsedTime maximum time to spend on retrying the same request
     */
    public GAConnection(final CredentialRef credentialRef, final Duration connectTimeout,
        final Duration readTimeout, final Duration retryMaxElapsedTime) {

        m_credentialRef = Objects.requireNonNull(credentialRef);
        m_connectTimeout = Objects.requireNonNull(connectTimeout);
        m_readTimeout = Objects.requireNonNull(readTimeout);
        m_serverErrorRetryMaxElapsedTime = retryMaxElapsedTime;
    }

    /**
     * Restores a {@link GAConnection} from a saved model (used by the framework).
     *
     * @param model model containing connection information
     * @throws InvalidSettingsException
     */
    public GAConnection(final ModelContentRO model) throws InvalidSettingsException {
        m_connectTimeout = getDurationFromSettings(model, KEY_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
        m_readTimeout = getDurationFromSettings(model, KEY_READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
        m_serverErrorRetryMaxElapsedTime = getDurationFromSettings(model, KEY_ERR_RETRY_MAX_ELAPSED_TIME,
            DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME);
        m_credentialRef = CredentialRefSerializer.loadRefWithLegacySupport(model);
    }

    /**
     * Saves the connection information to the model.
     *
     * @param model model to save into
     */
    void saveTo(final ModelContentWO model) {
        CredentialRefSerializer.saveRef(m_credentialRef, model);
        addDurationToSettings(model, KEY_CONNECT_TIMEOUT, m_connectTimeout);
        addDurationToSettings(model, KEY_READ_TIMEOUT, m_readTimeout);
        addDurationToSettings(model, KEY_ERR_RETRY_MAX_ELAPSED_TIME, m_serverErrorRetryMaxElapsedTime);
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
        final var sb = new StringBuilder();
        sb.append("Connection timeout: %ds%n".formatted(m_connectTimeout.toSeconds()));
        sb.append("Read timeout: %ds%n".formatted(m_readTimeout.toSeconds()));
        sb.append("Retry maximum elapsed time: %ds%n".formatted(m_serverErrorRetryMaxElapsedTime.toSeconds()));
        return sb.toString();
    }

    /**
     * Get available account summaries from the connection.
     *
     * @param credentials Google credentials
     * @param connectTimeout connection timeout
     * @param readTimeout read timeout
     * @param retryMaxElapsedTime maximum elapsed time after the first request for retries
     * @return available account summaries or empty list if there are no accounts
     * @throws IOException exception thrown from underlying API requests
     */
    private static List<GoogleAnalyticsAdminV1betaAccountSummary> accountSummaries(
            final Credentials credentials, final Duration connectTimeout, final Duration readTimeout,
            final Duration retryMaxElapsedTime) throws IOException {
        /**
         * Just a function that makes a request to the admin API for account summaries. Always uses the provided request
         * parameters. Unfortunately, this code block is impossible to generify, `#getNextPageToken` and
         * `#setNextPageToken` are not part of any interface.
         */
        final FailableFunction<
            FailableFunction<
                AccountSummaries.List,
                GoogleAnalyticsAdminV1betaListAccountSummariesResponse,
                IOException
            >,
            GoogleAnalyticsAdminV1betaListAccountSummariesResponse,
            IOException
        > makeRequest = query ->
            withAdminAPI(credentials, connectTimeout, readTimeout, retryMaxElapsedTime, admin ->
                query.apply(admin.accountSummaries().list()));

        final List<GoogleAnalyticsAdminV1betaAccountSummary> summaries = new LinkedList<>();
        var result = makeRequest.apply(AccountSummaries.List::execute);
        while (result != null && result.getAccountSummaries() != null) {
            summaries.addAll(result.getAccountSummaries());
            // paginating responses until no next page token is provided
            final var nextToken = result.getNextPageToken();
            if (StringUtils.isBlank(nextToken)) {
                break;
            }
            result = makeRequest.apply(query -> query.setPageToken(nextToken).execute());
        }
        return summaries;
    }

    /**
     * Get available properties from a given GA account, using the connection.
     *
     * @param credentials Google credentials
     * @param accountId ID of the GA parent account, holding the properties
     * @param connectTimeout connection timeout
     * @param readTimeout read timeout
     * @param retryMaxElapsedTime maximum elapsed time after the first request for retries
     * @return available properties or empty list if there are no properties
     * @throws IOException exception thrown from underlying API requests
     */
    private static List<GoogleAnalyticsAdminV1betaProperty> propertiesForAccount(final Credentials credentials,
        final String accountId, final Duration connectTimeout, final Duration readTimeout,
        final Duration retryMaxElapsedTime) throws IOException {
        /**
         * Just a function that makes a request to the admin API for properties. Always uses the provided request
         * parameters. Unfortunately, this code block is impossible to generify, `#getNextPageToken` and
         * `#setNextPageToken` are not part of any interface.
         */
        final FailableFunction<
            FailableFunction<
                Properties.List,
                GoogleAnalyticsAdminV1betaListPropertiesResponse,
                IOException
            >,
            GoogleAnalyticsAdminV1betaListPropertiesResponse,
            IOException
        > makeRequest = query ->
            withAdminAPI(credentials, connectTimeout, readTimeout, retryMaxElapsedTime, admin ->
                query.apply(admin.properties().list().setFilter("parent:%s%s".formatted(ACCOUNTS_PREFIX, accountId))));

        final List<GoogleAnalyticsAdminV1betaProperty> properties = new LinkedList<>();
        var result = makeRequest.apply(Properties.List::execute);
        while (result != null && result.getProperties() != null) {
            properties.addAll(result.getProperties());
            // paginating responses until no next page token is provided
            final var nextToken = result.getNextPageToken();
            if (StringUtils.isBlank(nextToken)) {
                break;
            }
            result = makeRequest.apply(query -> query.setPageToken(nextToken).execute());
        }
        return properties;
    }

    private Credentials resolveCredentials() throws KNIMEException {
        try {
            return CredentialUtil.toOAuth2Credentials(m_credentialRef);
        } catch (NoSuchCredentialException | IOException e) {
            throw new KNIMEException(e.getMessage(), e);
        }
    }

    /**
     * Get available account summaries from the connection.
     *
     * @return available account summaries
     * @throws KNIMEException exception throw from underlying API requests
     */
    public List<GoogleAnalyticsAdminV1betaAccountSummary> accountSummaries() throws KNIMEException {
        final var creds = resolveCredentials();

        return exceptionsWrapped(() -> accountSummaries(creds,
            m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime));
    }

    /**
     * Get available properties for a given account from the connection.
     *
     * @param accountId ID of the GA parent account, holding the properties
     * @return available properties
     * @throws KNIMEException exception throw from underlying API requests
     */
    public List<GoogleAnalyticsAdminV1betaProperty> propertiesForAccount(final String accountId) throws KNIMEException {
        final var creds = resolveCredentials();

        return exceptionsWrapped(() -> propertiesForAccount(creds, accountId,
            m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime));
    }


    /**
     * Get the property object from the admin API for a given ID.
     *
     * @param propertyId ID of the GA property
     * @return property object
     * @throws KNIMEException exception throw from underlying API requests
     */
    public GoogleAnalyticsAdminV1betaProperty property(final String propertyId) throws KNIMEException {
        final var creds = resolveCredentials();

        return exceptionsWrapped(
            () -> withAdminAPI(creds, m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime,
                admin -> admin.properties().get("%s%s".formatted(PROPERTIES_PREFIX, propertyId)).execute()));
    }

    /**
     * Get
     * <a href="https://developers.google.com/
     *analytics/devguides/reporting/data/v1/rest/v1beta/properties/getMetadata">metadata</a>
     * for a given property, such as dimensions and metrics.
     *
     * @param property Google Analytics 4 property
     * @param credentials Google credentials
     * @param connectTimeout connection timeout
     * @param readTimeout read timeout
     * @param retryMaxElapsedTime maximum elapsed time after the first request for retries
     * @return available metadata for the property
     * @throws IOException exception thrown from underlying API requests
     */
    private static Metadata metadata(final GAProperty property,
        final Credentials credentials, final Duration connectTimeout, final Duration readTimeout,
        final Duration retryMaxElapsedTime) throws IOException {
        return withDataAPI(credentials, connectTimeout, readTimeout, retryMaxElapsedTime,
            data -> data.properties().getMetadata("properties/%s/metadata".formatted(property.m_propertyId())).execute()
            );
    }

    /**
     * Get metadata for a given property.
     * @param property Google Analytics 4 property
     * @return available metadata for the property
     * @throws KNIMEException exception throw from underlying API requests
     */
    public Metadata metadata(final GAProperty property) throws KNIMEException {
        final var creds = resolveCredentials();

        return exceptionsWrapped(
            () -> metadata(property, creds, m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime));
    }

    /**
     * Run a report request against the configured property.
     *
     * @param property Google Analytics property to run the report against
     * @param req report request to run
     * @return the response for the request
     * @throws KNIMEException exception throw from underlying API requests
     */
    public RunReportResponse runReport(final GAProperty property, final RunReportRequest req) throws KNIMEException {

        final var creds = resolveCredentials();

        return exceptionsWrapped(() -> //
            withDataAPI(creds, m_connectTimeout, m_readTimeout, m_serverErrorRetryMaxElapsedTime, //
                data -> data.properties().runReport(PROPERTIES_PREFIX + property.m_propertyId(), req).execute()));
    }

    /* Static utility methods (API requests, error handling, ...) */

    private static <O> O exceptionsWrapped(final FailableCallable<O, IOException> callable) throws KNIMEException {
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
     * @param credentials Google credentials
     * @param connectTimeout timeout to establish a connection
     * @param readTimeout timeout to read from an established connection
     * @param retryMax maximum elapsed time to spend on retrying the same request from the point in time when the
     *            request was first made
     * @param callable function retrieving data from the Admin API
     * @return a user defined value extracted from the Admin API
     * @throws IOException
     */
    private static <R> R withAdminAPI(final Credentials credentials, final Duration connectTimeout,
            final Duration readTimeout, final Duration retryMax,
            final FailableFunction<GoogleAnalyticsAdmin, R, IOException> callable) throws IOException {
        return withAPI(credentials, connectTimeout, readTimeout, retryMax, "KNIME-Google-Analytics-4-Connector",
            GoogleAnalyticsAdmin.Builder::new, callable);
    }

    /**
     * Make a request against the Data API.
     * @param <R> type of the value returned from the request
     * @param credentials Google credentials
     * @param connectTimeout timeout to establish a connection
     * @param readTimeout timeout to read from an established connection
     * @param retryMax maximum elapsed time to spend on retrying the same request from the point in time when the
     *            request was first made
     * @param callable function retrieving data from the Data API
     * @return a user defined value extracted from the Data API
     * @throws IOException
     */
    private static <R> R withDataAPI(final Credentials credentials, final Duration connectTimeout,
            final Duration readTimeout, final Duration retryMax,
            final FailableFunction<AnalyticsData, R, IOException> callable) throws IOException {
        return withAPI(credentials, connectTimeout, readTimeout, retryMax, "KNIME-Google-Analytics-4-Query",
            AnalyticsData.Builder::new, callable);
    }

    private static <R, C extends AbstractGoogleJsonClient> R
        withAPI(final Credentials credentials, final Duration connectTimeout,
            final Duration readTimeout, final Duration retryMax,
            final String appName,
            final ToClientBuilder<C.Builder> builder,
            final FailableFunction<C, R, IOException> callable) throws IOException {
        @SuppressWarnings("unchecked")
        final var client = (C) builder.build(GoogleApiUtil.getHttpTransport(),
            GoogleApiUtil.getJsonFactory(),
                configureRequestInitializer(credentials, connectTimeout, readTimeout, retryMax))
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

    private static HttpRequestInitializer configureRequestInitializer(final Credentials credentials,
            final Duration connectTimeout, final Duration readTimeout, final Duration retryIOMaxElapsedTime) {
        HttpRequestInitializer init = new HttpCredentialsAdapter(credentials);
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

    private static KNIMEException wrapGoogleJsonResponseException(final GoogleJsonResponseException e) {
        final var b = Message.builder();
        b.withSummary("[Error %d]: %s".formatted(e.getStatusCode(), e.getStatusMessage()));
        for (final var err : e.getDetails().getErrors()) {
            final var msg = err.getMessage();
            if (msg != null) {
                b.addTextIssue(msg);
            }
        }
        return KNIMEException.of(b.build().orElseThrow(), e);
    }

    private static KNIMEException wrapUnknownHostException(final UnknownHostException e) {
        final var b = Message.builder();
        b.withSummary("Unknown host: %s".formatted(e.getMessage()));
        return KNIMEException.of(b.build().orElseThrow(), e);
    }

    private static KNIMEException wrapGenericIOException(final IOException e) {
        final var b = Message.builder();
        b.withSummary("Error while contacting Google Analytics: %s".formatted(e.getMessage()));
        return KNIMEException.of(b.build().orElseThrow(), e);
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
