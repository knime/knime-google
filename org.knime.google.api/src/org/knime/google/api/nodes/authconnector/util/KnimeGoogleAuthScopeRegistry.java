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
 *   Oct 2, 2018 (oole): created
 */
package org.knime.google.api.nodes.authconnector.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Registry holding the KnimeGoogleAuthScopes.
 *
 * A KnimeGoogleAuthScope is a feature/extension where a KnimeGoogleAuthScope describes a set of necessary Google Scopes
 * for the feature/extension to perform properly.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public final class KnimeGoogleAuthScopeRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(KnimeGoogleAuthScopeRegistry.class);

    /** The id of the converter extension point. */
    public static final String EXT_POINT_ID = "org.knime.google.api.knimeGoogleAuthScope";

    /** The attribute of the converter extension point. */
    public static final String EXT_POINT_ATTR_DF = "KnimeGoogleAuthScope";

    private static volatile KnimeGoogleAuthScopeRegistry instance;

    private final List<KnimeGoogleAuthScope> m_knimeGoogleAuthScopes = new ArrayList<KnimeGoogleAuthScope>();

    private KnimeGoogleAuthScopeRegistry() {
        // Collect at extension point.
        registerExtensionPoints();
    }

    /**
     * Returns the only instance of this class.
     *
     * @return the only instance
     */
    public static KnimeGoogleAuthScopeRegistry getInstance() {
        if (instance == null) {
            synchronized (KnimeGoogleAuthScopeRegistry.class) {
                if (instance == null) {
                    instance = new KnimeGoogleAuthScopeRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Registers all extension point implementations.
     */
    private void registerExtensionPoints() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String converter = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

                if (converter == null || converter.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                        + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }
                try {
                    final KnimeGoogleAuthScope typeConverter =
                        (KnimeGoogleAuthScope)elem.createExecutableExtension(EXT_POINT_ATTR_DF);
                    addKnimeGoogleAuthScope(typeConverter);
                } catch (final Throwable t) {
                    LOGGER.error(
                        "Problems during initialization of KnimeGoogleAuthScope (with id '" + converter + "'.)", t);
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering KnimeGoogleAuthScope extensions", e);
        }
    }

    /**
     * @param typeConverter
     */
    private void addKnimeGoogleAuthScope(final KnimeGoogleAuthScope knimeGoogleAuthScope) {
        m_knimeGoogleAuthScopes.add(knimeGoogleAuthScope);
    }

    /**
     * Returns the registered {@link KnimeGoogleAuthScope}s.
     *
     * @return The registered KnimeGoogleAuthScopes
     */
    public List<KnimeGoogleAuthScope> getKnimeGoogleAuthScopes() {
        return m_knimeGoogleAuthScopes;
    }

    /**
     * Returns all knimeGoogleScope ids.
     *
     * @return All knimeGoogleScope ids
     */
    public String[] getAllKnimeGoogleScopeIds() {
        List<String> allScopes = new ArrayList<String>();
        for (KnimeGoogleAuthScope scope : m_knimeGoogleAuthScopes) {
            allScopes.add(scope.getScopeID());
        }
        return allScopes.toArray(new String[allScopes.size()]);
    }

    /**
     * Returns the Google scopes for the provided {@link KnimeGoogleAuthScope}s
     *
     * @param knimeScopes The {@link KnimeGoogleAuthScope}s for which the Google scopes should be returned
     * @return The Google scopes for the provided {@link KnimeGoogleAuthScope}s
     */
    public static String[] getKnimeGoogleScopeIds(final List<KnimeGoogleAuthScope> knimeScopes) {
        List<String> collect = knimeScopes.stream().map(s -> s.getScopeID()).collect(Collectors.toList());
        return collect.toArray(new String[collect.size()]);
    }

    /**
     * Returns the list of Google Scopes given the provided {@link KnimeGoogleAuthScope}s.
     *
     * @param knimeScopeList the {@link KnimeGoogleAuthScope}s for which the Google Scopes should be returned
     * @return The list of Google Scopes corresponding to the given {@link KnimeGoogleAuthScope}s
     */
    public static List<String> getAuthScopes(final List<KnimeGoogleAuthScope> knimeScopeList) {
        ArrayList<String> stringScopes = new ArrayList<>();
        for (KnimeGoogleAuthScope kScope : knimeScopeList) {
            stringScopes.addAll(kScope.getAuthScopes());
        }
        return stringScopes;
    }

    /**
     * Returns the list of {@link KnimeGoogleAuthScope} given the list of identifiers.
     *
     * @param scopeStringList list of Scope identifiers for which the {@link KnimeGoogleAuthScope}s should be returned
     * @return The {@link KnimeGoogleAuthScope}s for the given identifiers
     */
    public List<KnimeGoogleAuthScope> getScopesFromString(final List<String> scopeStringList) {
        List<KnimeGoogleAuthScope> kList = getKnimeGoogleAuthScopes().stream()
            .filter(k -> scopeStringList.stream().anyMatch(s -> s.equals(k.getScopeID())))
            .collect(Collectors.toList());
        return kList;
    }
}
