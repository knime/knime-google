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
 *   15 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import java.awt.Component;
import java.util.function.BiPredicate;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;

/**
 * List cell renderer for item views.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <I> item type
 * @param <V> view metadata type
 * @param <E> item view type
 */
final class ItemViewListCellRenderer<I, V, E extends ItemView<I, V>> extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    private final ItemViewCellRenderer<I, V, E> m_cellRenderer;

    ItemViewListCellRenderer(final ItemViewCellRenderer<I, V, E> cellRenderer) {
        m_cellRenderer = cellRenderer;
    }

    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
        String toolTip = null;
        String label = value != null ? value.toString() : null;
        if (value instanceof ItemView<?, ?>) {
            @SuppressWarnings("unchecked")
            final var view = (E) value;
            label = m_cellRenderer.getLabel(view);
            toolTip = m_cellRenderer.getToolTipText(view);
        }
        final var component = (JComponent) super.getListCellRendererComponent(list, label, index, isSelected,
            cellHasFocus);
        component.setToolTipText(toolTip);
        return component;
    }


    static class ItemViewKeySelection<I, V, E extends ItemView<I, V>> implements JComboBox.KeySelectionManager {

        private BiPredicate<E, String> m_matcher;

        private static final long WAIT_TIME_MILLIS = 1000;

        private long m_previous;
        private String m_searchTerm;

        ItemViewKeySelection(final BiPredicate<E, String> matcher) {
            m_matcher = matcher;
        }

        @Override
        public int selectionForKey(final char key, final ComboBoxModel<?> aModel) {
            @SuppressWarnings("unchecked")
            final var model = (ComboBoxModel<E>)aModel;
            final var now = System.currentTimeMillis();
            final int num = model.getSize();
            int idx = -1;
            final var selected = model.getSelectedItem();
            if (selected != null) {
                for (var i = 0; i < num; i++) {
                    if (selected.equals(model.getElementAt(i))) {
                        idx = i;
                        break;
                    }
                }
            }
            if (now - m_previous < WAIT_TIME_MILLIS) {
                if ((m_searchTerm.length() == 1) && key == m_searchTerm.charAt(0)) {
                    // repeated key presses cycle through candidates
                    idx++;
                } else {
                    m_searchTerm += key;
                }
            } else {
                // reset term after wait time
                m_searchTerm = key + "";
                idx++;
            }
            m_previous = now;
            if (idx < 0 || idx >= num) {
                idx = 0;
            }
            var found = next(idx, num, model);
            if (found >= 0) {
                return found;
            }
            return next(0, idx, model);
        }

        private int next(final int fromIncl, final int toExcl, final ComboBoxModel<E> model) {
            for (var i = fromIncl; i < toExcl; i++) {
                final var elem = model.getElementAt(i);
                if (elem != null && m_matcher.test(elem, m_searchTerm)) {
                    return i;
                }
            }
            return -1;
        }

    }
}