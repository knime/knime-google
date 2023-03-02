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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

/**
 * ComboBox model for an item with associated view metadata. An item can be "bare", i.e. have no metadata in the view.
 * When an item with metadata is added and a corresponding "bare" item already exists in the model, it is exchanged,
 * i.e. enriched with the metadata and listeners are notified.
 * There exists a bulk-add method that only notifies listeners at the end of the operation.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <I> item type
 * @param <V> view metadata type
 * @param <E> item view type
 */
final class ItemViewComboBoxModel<I, V, E extends ItemView<I, V>> extends AbstractListModel<E>
        implements MutableComboBoxModel<E> {

    private static final long serialVersionUID = 1L;
    private final List<E> m_entries;
    private E m_selected;

    ItemViewComboBoxModel(final List<E> initial) {
        m_entries = new ArrayList<>(initial);
    }
    ItemViewComboBoxModel() {
        m_entries = new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSelectedItem(final Object anItem) {
        if (anItem == null) {
            m_selected = null;
            return;
        }
        if (!(anItem instanceof ItemView)) {
            throw new IllegalArgumentException("Passed object must be subclass of type " + ItemView.class.getSimpleName());
        }
        m_selected = (E) anItem;
    }

    @Override
    public E getSelectedItem() {
        return m_selected;
    }

    @Override
    public int getSize() {
        return m_entries.size();
    }

    @Override
    public E getElementAt(final int index) {
        return m_entries.get(index);
    }

    @Override
    public void addElement(final E item) {
        addItem(item, true);
    }

    @Override
    public void insertElementAt(final E item, final int index) {
        insertItemAt(item, index, true);
    }

    private int addItem(final E item, final boolean notifyListeners) {
        final var idx = indexOfItem(item.getItem());
        if (idx < 0) {
            final var newIdx = m_entries.size();
            insertItemAt(item, newIdx, notifyListeners);
            return newIdx;
        }
        // already contained as bare item, check if we can "upgrade" to include metadata
        final var existing = m_entries.get(idx);
        if (existing.equals(item)) {
            // nothing to do
            return -1;
        }
        // the items match, but the existing likely is a "bare" one without metadata, swap
        m_entries.remove(idx);
        m_entries.add(idx, item);
        if (m_selected != null && m_selected.getItem().equals(item.getItem())) {
            // swapped item in list, so we need to update the "selected" reference as well, e.g. for the jcombobox
            // to refresh its "reminder"
            m_selected = item;
        }
        // intverval must include both "index0" and "index1" as per Javadoc
        if (notifyListeners) {
            fireIntervalAdded(this, idx, idx);
        }
        return idx;
    }

    private void insertItemAt(final E item, final int index, final boolean notifyListeners) {
        m_entries.add(index, item);
        if (notifyListeners) {
            // intverval must include both "index0" and "index1" as per Javadoc
            fireIntervalAdded(this, index, index);
        }
    }

    private int indexOfItem(final I item) {
        for (var i = 0; i < m_entries.size(); i++) {
            final var e = m_entries.get(i);
            if (e.getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void removeElement(final Object obj) {
        final var idx = m_entries.indexOf(obj);
        if (idx <= 0) {
            return;
        }
        removeElementAt(idx);
    }

    @Override
    public void removeElementAt(final int index) {
        m_entries.remove(index);
        // intverval must include both "index0" and "index1" as per Javadoc
        fireIntervalRemoved(this, index, index);
    }

    public void addAll(final E[] items) {
        if (items == null || items.length == 0) {
            return;
        }
        final var changed = Arrays.stream(items).mapToInt(i -> addItem(i, false)).sorted().toArray();
        if (m_selected == null) {
            m_selected = items[0];
        }
        fireContentsChanged(this, changed[0], changed[changed.length - 1]);
    }

}