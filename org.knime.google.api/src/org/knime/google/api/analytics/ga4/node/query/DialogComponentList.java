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
package org.knime.google.api.analytics.ga4.node.query;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * List-based component with add/remove functionality for a {@code SettingsModelList}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <T> list item type
 */
final class DialogComponentList<T> extends DialogComponent {

    private final int m_capacity;
    private final int m_minimumElements;

    private final JList<T> m_list;
    private final Border m_origBorder;
    private final DefaultListModel<T> m_listModel;

    private final Function<String, T> m_parseFn;
    private final JTextField m_input;

    private final JButton m_addBtn;
    private final JButton m_removeBtn;


    DialogComponentList(final SettingsModelList<T> settingsModel, final String title,
            final Function<String, T> parseFn, final int minimumElements, final int maxElements) {
        super(settingsModel);
        m_parseFn = parseFn;
        m_minimumElements = minimumElements;
        m_capacity = maxElements;

        final var panel = getComponentPanel();
        if (title != null) {
            panel.setBorder(BorderFactory.createTitledBorder(title));
        }

        m_listModel = new DefaultListModel<>();
        m_listModel.addListDataListener(new ComponentToSettingsModelSyncer(settingsModel));

        m_list = new JList<>(m_listModel);
        m_origBorder = m_list.getBorder();
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setLayoutOrientation(JList.VERTICAL);
        m_list.setVisibleRowCount(Math.min(Math.max(minimumElements, 5), maxElements));
        final var scrollPane = new JScrollPane(m_list);

        m_input = new JTextField();
        m_addBtn = new JButton("Add");
        m_removeBtn = new JButton("Remove");

        final var addTextAction = new AbstractAction("Add") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(final ActionEvent e) {
                final var text = m_input.getText();
                if (StringUtils.isBlank(text)) {
                    return;
                }
                m_listModel.addElement(m_parseFn.apply(text));
                m_input.setText("");
                // reset border set to red after unsuccessful dialog close
                m_list.setBorder(m_origBorder);
            }
        };
        final var removeAction = new AbstractAction("Remove") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(final ActionEvent e) {
                final var idx = m_list.getSelectedIndex();
                if (idx < 0) {
                    return;
                }
                m_listModel.removeElementAt(idx);
                m_list.setSelectedIndex(Math.min(idx, m_listModel.size() - 1));
            }
        };

        m_addBtn.setAction(addTextAction);
        final var enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        m_input.getActionMap().put(m_input.getInputMap(JComponent.WHEN_FOCUSED).get(enterKey), addTextAction);

        m_removeBtn.setAction(removeAction);
        final var deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        m_list.getActionMap().put(m_list.getInputMap(JComponent.WHEN_FOCUSED).get(deleteKey), removeAction);

        final ListDataListener toggleButtons = new ToggleAddRemoveActions(removeAction, addTextAction);

        m_listModel.addListDataListener(toggleButtons);

        /**
         *  --Title-------------------------
         * |  -----------------             |
         * | |                 |    [Add]   |
         * |  -----------------             |
         * |  -----------------             |
         * | | first           |  [Remove]  |
         * | | [second]        |            |
         * | | third           |            |
         * |  -----------------             |
         *  --------------------------------
         */

        panel.setLayout(new GridBagLayout());
        final var gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = .9;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.PAGE_START;
        panel.add(m_input, gbc);

        gbc.gridx++;
        gbc.weightx = .1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_addBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = .9;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(scrollPane, gbc);

        gbc.gridx++;
        gbc.weightx = .1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_removeBtn, gbc);

    }

    @Override
    protected void updateComponent() {
        @SuppressWarnings("unchecked")
        final var model = (SettingsModelList<T>) getModel();
        final var fromModel = model.getValue();

        m_listModel.clear();
        if (fromModel == null) {
            return;
        }
        m_listModel.addAll(fromModel);

        setEnabledComponents(model.isEnabled());
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        if (m_minimumElements > 0) {
            final var elem = m_minimumElements == 1 ? "one element is" : (m_minimumElements + " elements are");
            if (m_listModel.getSize() < m_minimumElements) {
                m_list.setBorder(BorderFactory.createLineBorder(Color.RED));
                throw new InvalidSettingsException(String.format("At least %s requred.", elem));
            }
        }
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // no-op
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_list.setEnabled(enabled);

    }

    @Override
    public void setToolTipText(final String text) {
        // nothing needed
    }

    private final class ToggleAddRemoveActions implements ListDataListener {

        private final AbstractAction m_removeAction;

        private final AbstractAction m_addTextAction;

        private ToggleAddRemoveActions(final AbstractAction removeAction, final AbstractAction addTextAction) {
            m_removeAction = removeAction;
            m_addTextAction = addTextAction;
        }

        private void toggleButtonStates() {
            final var currSize = m_listModel.size();
            final var canRemove = currSize > 0;
            final var canAdd = currSize < m_capacity;
            m_removeAction.setEnabled(canRemove);
            m_removeBtn.setEnabled(canRemove);
            m_addTextAction.setEnabled(canAdd);
            m_addBtn.setEnabled(canAdd);
        }

        @Override
        public void intervalRemoved(final ListDataEvent e) {
            toggleButtonStates();
        }

        @Override
        public void intervalAdded(final ListDataEvent e) {
            toggleButtonStates();
        }

        @Override
        public void contentsChanged(final ListDataEvent e) {
            toggleButtonStates();
        }
    }

    private final class ComponentToSettingsModelSyncer implements ListDataListener {

        private final SettingsModelList<T> m_settingsModel;

        private ComponentToSettingsModelSyncer(final SettingsModelList<T> settingsModel) {
            m_settingsModel = settingsModel;
        }

        private void syncWithSettingsModel() {
            final var currSettings = m_settingsModel.getValue();
            final List<T> inList = new ArrayList<>();
            for (var i = 0; i < m_listModel.size(); i++) {
                inList.add(m_listModel.get(i));
            }
            final var differ = inList.size() != currSettings.size() || !inList.equals(currSettings);
            if (differ) {
                m_settingsModel.set(inList);
            }
        }

        @Override
        public void intervalRemoved(final ListDataEvent e) {
            syncWithSettingsModel();
        }

        @Override
        public void intervalAdded(final ListDataEvent e) {
            syncWithSettingsModel();
        }

        @Override
        public void contentsChanged(final ListDataEvent e) {
            syncWithSettingsModel();
        }
    }
}
