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
 *   Aug 25, 2023 (Zkriya Rakhimberdiyev, Redfield SE): created
 */
package org.knime.google.api.nodes.authenticator;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.google.api.nodes.authenticator.GoogleAuthenticatorSettings.APIKeyTypeSection;

/**
 * Implementation of {@link DefaultNodeSettings} to specify API key settings.
 *
 * @author Zkriya Rakhimberdiyev, Redfield SE
 */
@SuppressWarnings("restriction")
class APIKeySettings implements DefaultNodeSettings {

    enum APIKeyType {
        @Label("JSON")
        JSON,
        @Label("P12")
        P12;
    }

    interface APIKeyTypeRef extends Reference<APIKeyType> {
    }

    static class APIKeyTypeIsJSON implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(APIKeyTypeRef.class).isOneOf(APIKeyType.JSON);
        }
    }

    @Widget(title = "Type",//
            description = "Which format of API key to use. Google cloud provides API keys as either JSON or P12 (legacy).")
    @Layout(APIKeyTypeSection.TypeSwitcher.class)
    @ValueReference(APIKeyTypeRef.class)
    @ValueSwitchWidget
    APIKeyType m_apiKeyFormat = APIKeyType.JSON;

    @Widget(title = "JSON file", description = "Path to the private JSON key file.")
    @Layout(APIKeyTypeSection.Content.class)
    @Effect(predicate = APIKeyTypeIsJSON.class, type = EffectType.SHOW)
    FileSelection m_jsonFile = new FileSelection();

    @Widget(title = "Service account email", description = "Email address of the service account.")
    @Layout(APIKeyTypeSection.Content.class)
    @Effect(predicate = APIKeyTypeIsJSON.class, type = EffectType.HIDE)
    String m_serviceAccountEmail;

    @Widget(title = "P12 file", description = "Path to the private P12 key file.")
    @Layout(APIKeyTypeSection.Content.class)
    @Effect(predicate = APIKeyTypeIsJSON.class, type = EffectType.HIDE)
    FileSelection m_p12File = new FileSelection();

    /**
     * @throws InvalidSettingsException
     *             when one of the settings was invalid.
     */
    public void validate() throws InvalidSettingsException {
        if (m_apiKeyFormat == APIKeyType.P12) {
            if (StringUtils.isEmpty(m_serviceAccountEmail)) {
                throw new InvalidSettingsException("Please specify email address of the service account");
            }
            if (StringUtils.isEmpty(m_p12File.getFSLocation().getPath())) {
                throw new InvalidSettingsException("Please specify a path to the P12 key file");
            }
        } else {
            if (StringUtils.isEmpty(m_jsonFile.getFSLocation().getPath())) {
                throw new InvalidSettingsException("Please specify a path to the JSON file");
            }
        }
    }
}
