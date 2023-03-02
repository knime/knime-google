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
 *   16 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.function.BiFunction;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog component for an item that can have an item view providing more information.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <I> item type
 * @param <V> more information type
 * @param <E> item view type
 */
final class DialogComponentItemView<I, V, E extends ItemView<I, V>> extends DialogComponent {

    private final ItemViewComboBoxModel<I, V, E> m_comboBoxModel;

    private final JComboBox<E> m_comboBox;

    private final JLabel m_label;

    private final BiFunction<I, V, E> m_itemViewCreator;

    DialogComponentItemView(final SettingsModelItem<I> settingsModel, final String label,
            final BiFunction<I, V, E> itemViewCreator,
            final ItemViewCellRenderer<I, V, E> cellRenderer) {
        super(settingsModel);
        m_itemViewCreator = itemViewCreator;
        m_comboBoxModel = new ItemViewComboBoxModel<>();
        m_comboBox = new JComboBox<>(m_comboBoxModel);
        m_label = new JLabel(label);

        m_comboBox.setRenderer(new ItemViewListCellRenderer<>(cellRenderer));
        m_comboBox.setKeySelectionManager(new ItemViewListCellRenderer.ItemViewKeySelection<I, V, E>(
                (view, searchTerm) -> searchTerm.toLowerCase().startsWith(cellRenderer.getLabel(view).toLowerCase())));
        m_comboBox.addItemListener(new ItemViewSelectListener<I, V, E>(settingsModel));

        settingsModel.addChangeListener(e -> updateComponent());

        final var v = settingsModel.getValue();
        if (v != null) {
            final var newItem = m_itemViewCreator.apply(v, null);
            m_comboBoxModel.addElement(newItem);
            m_comboBoxModel.setSelectedItem(newItem);
        }

        final var panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipady = 5;
        panel.add(m_label, gbc);
        gbc.gridy++;
        panel.add(m_comboBox, gbc);
    }

    @SuppressWarnings("unchecked")
    private SettingsModelItem<I> getSettingsModel() {
        return (SettingsModelItem<I>) getModel();
    }

    public void addElements(final E[] elements) {
        m_comboBoxModel.addAll(elements);
    }

    @Override
    protected void updateComponent() {
        final var model = getSettingsModel();
        setEnabledComponents(model.isEnabled());

        final var curr = model.getValue();
        if (curr == null) {
            m_comboBoxModel.setSelectedItem(null);
            return;
        }

        final var currComp = m_comboBoxModel.getSelectedItem();
        if (currComp != null && curr.equals(currComp.getItem())) {
            // in sync, nothing to update
            return;
        }
        // select item by its property
        if (comboBoxSelectItem(curr)) {
            return;
        }

        // property from model was not found, so add it to the combobox and select it
        final var newItem = m_itemViewCreator.apply(curr, null);
        m_comboBoxModel.addElement(newItem);
        m_comboBoxModel.setSelectedItem(newItem);
    }

    /**
     * Select the item in the combo box corresponding to the given property.
     *
     * @param property
     * @return {@code true} if the property was found and the item selected, {@code false} otherwise
     */
    private boolean comboBoxSelectItem(final I item) {
        final var i = comboBoxIndexOf(item);
        if (i < 0) {
            return false;
        }
        m_comboBoxModel.setSelectedItem(m_comboBoxModel.getElementAt(i));
        return true;
    }

    private int comboBoxIndexOf(final I item) {
        final var num = m_comboBoxModel.getSize();
        for (var i = 0; i < num; i++) {
            final var at = m_comboBoxModel.getElementAt(i);
            if (item.equals(at.getItem())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        try {
            getSettingsModel().validate();
        } catch (InvalidSettingsException e) {
            getComponentPanel().setBorder(BorderFactory.createLineBorder(Color.RED));
            throw e;
        }
        getComponentPanel().setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_comboBox.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // no-op
    }

    /**
     * Class to keep settings model in sync with the selected item.
     */
    private static final class ItemViewSelectListener<I, V, E extends ItemView<I, V>> implements ItemListener {

        private final SettingsModelItem<I> m_model;

        ItemViewSelectListener(final SettingsModelItem<I> settingsModel) {
            m_model = settingsModel;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void itemStateChanged(final ItemEvent e) {
            final var eventItem = e.getItem();
            if (!(eventItem instanceof ItemView<?, ?>)) {
                throw new IllegalStateException("Attached ItemViewSelectListener to a non ItemView emitter?");
            }
            m_model.set(((E) eventItem).getItem());
        }

    }

}