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
 *   19 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 * A table-based dialog component for the {@code SettingsModelDateRanges}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class DialogComponentDateRangeArray extends DialogComponent {

    private final ValidatedTextField<LocalDate> m_fromInput;
    private final ValidatedTextField<LocalDate> m_toInput;
    private final ValidatedTextField<String> m_nameInput;

    private final int m_capacity;
    private final JTable m_table;
    private final DateRangeArrayTableModel m_model;

    DialogComponentDateRangeArray(final SettingsModelDateRanges settingsModel, final String title,
            final int capacity) {
        super(settingsModel);

        m_capacity = capacity;

        final var addBtn = new JButton("Add");

        final Function<String, LocalDate> dateParser = s -> LocalDate.parse(s, GADateRange.DATE_FMT);
        final BiPredicate<String, Consumer<String>> nonEmptyDate = (input, err) -> {
            if (input.isEmpty()) {
                err.accept("Date must not be empty.");
                return false;
            }
            return true;
        };
        final BiPredicate<String, Consumer<String>> validDate = (input, err) -> {
            try {
                dateParser.apply(input);
                return true;
            } catch (final DateTimeParseException e) {
                err.accept("Date must follow the ISO-8601 format, e.g. \"2006-07-28\":<br>" + e.getLocalizedMessage());
                return false;
            }
        };

        final BiPredicate<String, Consumer<String>> nonWhitespaceName =
                (s, err) -> {
                    if (!s.isEmpty() && s.trim().isEmpty()) {
                        err.accept("Name must not consist of only whitespace.<br>"
                            + "Leave empty for an auto-generated name.");
                        return false;
                    }
                    return true;
                };
        final BiPredicate<String, Consumer<String>> notReserved = (input, err) -> {
            final var match = Arrays.asList("date_range_", "RESERVED_").stream().filter(input::startsWith)
                    .findFirst();
            if (match.isEmpty()) {
                return true;
            }
            final var prefix = match.get();
            err.accept(String.format("Name may not start with \"%s\".", prefix));
            return false;
        };

        m_model = new DateRangeArrayTableModel(settingsModel.getRanges());
        final var tcm = new DefaultTableColumnModel();
        final var tc = new TableColumn();
        tc.setHeaderValue("From");
        tcm.addColumn(tc);

        final Consumer<Boolean> enableAdd = valid -> addBtn.setEnabled(valid && m_model.getRowCount() < m_capacity);

        m_fromInput = createValidatedTextInput("From date", nonEmptyDate.and(validDate), dateParser, enableAdd);
        m_toInput = createValidatedTextInput("To date", nonEmptyDate.and(validDate), dateParser, enableAdd);
        m_nameInput = createValidatedTextInput("Name (optional)", nonWhitespaceName.and(notReserved), s -> s, enableAdd);

        m_model.addTableModelListener(e -> {
            final var currSettings = settingsModel.getRanges();
            final var inTable = m_model.getRanges();
            final var differ = m_model.getRowCount() != currSettings.size() || !inTable.equals(currSettings);
            if (differ) {
                settingsModel.setRanges(inTable);
            }
        });

        m_table = new JTable(m_model);
        m_table.setAutoscrolls(true);
        final var scrollPane = new JScrollPane(m_table);
        // don't stretch the table too much but also don't just collapse an empty table
        final var tablePref = m_table.getPreferredScrollableViewportSize();
        m_table.setPreferredScrollableViewportSize(new Dimension(tablePref.width,
            Math.min(tablePref.height, m_table.getRowHeight() * m_capacity)));

        final var removeBtn = new JButton("Remove");

        // "global" error message for validation of whole date ranges
        final var errMsg = new JLabel("");
        errMsg.setForeground(Color.RED);

        addBtn.setEnabled(false);

        final var addAction = new ValidatingAddAction("Add", errMsg);
        final var removeAction = new AbstractAction("Remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                final var selected = m_table.getSelectedRow();
                if (selected < 0) {
                    return;
                }
                m_model.removeRow(selected);
                final var next = Math.min(selected, m_model.getRowCount() - 1);
                if (m_model.getRowCount() > 0) {
                    m_table.setRowSelectionInterval(next, next);
                }
            }
        };

        addBtn.addActionListener(addAction);
        final var enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        m_fromInput.putActionOnInput(JComponent.WHEN_FOCUSED, enterKey, addAction);
        m_toInput.putActionOnInput(JComponent.WHEN_FOCUSED, enterKey, addAction);
        m_nameInput.putActionOnInput(JComponent.WHEN_FOCUSED, enterKey, addAction);

        removeBtn.addActionListener(removeAction);
        final var deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        m_table.getActionMap().put(m_table.getInputMap(JComponent.WHEN_FOCUSED).get(deleteKey), removeAction);

        m_model.addTableModelListener(e -> {
            final var rows = m_model.getRowCount();
            final var canRemove = rows > 0;
            final var canAdd = rows < m_capacity;
            removeAction.setEnabled(canRemove);
            removeBtn.setEnabled(canRemove);
            addAction.setEnabled(canAdd);
            addBtn.setEnabled(canAdd);
        });

        // reusable layouter for validated input fields
        final ValidatedTextField.Layouter layouter = (panel, gbc, label, input, err) -> { // NOSONAR
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(label, gbc);
            gbc.gridx++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(input, gbc);
            gbc.gridx = 1;
            gbc.gridy++;
            panel.add(err, gbc);
        };

        final var panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        if (title != null) {
            panel.setBorder(BorderFactory.createTitledBorder(title));
        }

        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        m_fromInput.layoutComponents(panel, gbc, layouter);
        gbc.gridy++;
        gbc.gridx = 0;
        m_toInput.layoutComponents(panel, gbc, layouter);
        gbc.gridy++;
        gbc.gridx = 0;
        m_nameInput.layoutComponents(panel, gbc, layouter);

        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        panel.add(addBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(errMsg, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(scrollPane, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        panel.add(removeBtn, gbc);
    }

    private static <T> ValidatedTextField<T> createValidatedTextInput(final String label,
        final BiPredicate<String, Consumer<String>> validator,
        final Function<String, T> parser,
        final Consumer<Boolean> onValid) {
        final var errMsg = new JLabel("");
        return new ValidatedTextField<>(new JLabel(label), new JTextField(), errMsg, validator, parser, onValid);
    }

    private final class ValidatingAddAction extends AbstractAction {

        private final JLabel m_errMsg;

        private static final long serialVersionUID = 1L;

        private ValidatingAddAction(final String name, final JLabel errMsg) {
            super(name);
            m_errMsg = errMsg;
        }

        @Override
        public void actionPerformed(final ActionEvent ae) {
            final var from = m_fromInput.getValidatedValue();
            final var to = m_toInput.getValidatedValue();
            final var nameIn = m_nameInput.getValidatedValue();
            if (from.isEmpty() || to.isEmpty() || nameIn.isEmpty()) {
                return;
            }
            final var name = nameIn.get();
            final var range = new GADateRange(from.get(), to.get(), name.isEmpty() ? null : name);
            try {
                range.validate();
                m_fromInput.indicateInvalid(false);
                m_toInput.indicateInvalid(false);
                m_errMsg.setText("");
            } catch (final InvalidSettingsException e) { // NOSONAR
                m_errMsg.setText("<html>" + e.getLocalizedMessage() + "</html>");
                m_fromInput.indicateInvalid(true);
                m_toInput.indicateInvalid(true);
                return;
            }
            m_model.addRow(range);
            resetInputComponents();
        }
    }

    private static final class ValidatedTextField<T> {

        private final JLabel m_label;
        private final JTextField m_input;
        private final JLabel m_errMsg;
        private final BiPredicate<String, Consumer<String>> m_validator;
        private final Function<String, T> m_parser;
        private final Consumer<Boolean> m_onValid;

        private final Color m_origFgColor;

        ValidatedTextField(final JLabel label, final JTextField input, final JLabel errMsg,
                final BiPredicate<String, Consumer<String>> validator,
                final Function<String, T> parser, final Consumer<Boolean> onValid) {
            m_label = label;
            m_input = input;
            m_errMsg = errMsg;
            m_validator = validator;
            m_parser = parser;
            m_onValid = onValid;
            m_origFgColor = input.getForeground();

            errMsg.setForeground(Color.RED);
            errMsg.setVisible(false);

            m_input.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    validateInput();
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    validateInput();
                }

                @Override
                public void changedUpdate(final DocumentEvent e) {
                    validateInput();
                }
            });
        }

        void putActionOnInput(final int condition, final KeyStroke key, final Action action) {
            m_input.getActionMap().put(m_input.getInputMap(condition).get(key), action);
        }

        @FunctionalInterface
        interface Layouter {
            void layout(JPanel panel, GridBagConstraints gbc, JLabel label, JTextField textField, JLabel errMsg);
        }

        void layoutComponents(final JPanel panel, final GridBagConstraints gbc, final Layouter layouter) {
            layouter.layout(panel, gbc, m_label, m_input, m_errMsg);
        }

        void clear(final boolean resetValidation) {
            m_input.setText("");
            if (resetValidation) {
                m_errMsg.setText("");
                indicateInvalid(false);
            }
        }

        void indicateInvalid(final boolean invalid) {
            m_input.setForeground(invalid ? Color.RED : m_origFgColor);
        }

        private boolean validateInput() {
            if (!m_input.isEnabled()) {
                return true;
            }
            final var isValid = m_validator.test(m_input.getText(),
                msg -> m_errMsg.setText("<html>" + msg + "</html>"));
            m_errMsg.setVisible(!isValid);
            if (m_onValid != null) {
                m_onValid.accept(isValid);
            }
            indicateInvalid(!isValid);
            return isValid;
        }

        Optional<T> getValidatedValue() {
            if (!m_input.isEnabled()) {
                return Optional.empty();
            }
            final var isValid = validateInput();
            if (!isValid) {
                return Optional.empty();
            }
            return Optional.of(m_parser.apply(m_input.getText()));
        }

        void setEnabled(final boolean enabled) {
            m_input.setEnabled(enabled);
        }
    }

    private static final class DateRangeArrayTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private final List<GADateRange> m_ranges = new ArrayList<>();

        DateRangeArrayTableModel(final List<GADateRange> initial) {
            m_ranges.addAll(initial);
        }

        @Override
        public int getRowCount() {
            return m_ranges.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(final int column) {
            switch (column) {
                case 0:
                    return "From Date";
                case 1:
                    return "To Date";
                case 2:
                    return "Name";
                default:
                    throw new IllegalArgumentException(
                        String.format("Column index \"%d\" outside of allowed range: [0, 2).", column));
            }
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            switch (column) {
                case 0:
                    return LocalDate.class;
                case 1:
                    return LocalDate.class;
                case 2:
                    return String.class;
                default:
                    throw new IllegalArgumentException(
                        String.format("Column index \"%d\" outside of allowed range: [0, 2).", column));
            }
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (rowIndex > m_ranges.size() - 1 || rowIndex < 0) {
                throw new IllegalArgumentException(
                    String.format("Selected row index \"%d\" outside of allowed range: [%d, %d).", rowIndex, 0,
                        m_ranges.size()));
            }
            final var range = m_ranges.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return range.m_fromDate;
                case 1:
                    return range.m_toDate;
                case 2:
                    return range.m_rangeName;
                default:
                    throw new IllegalArgumentException(
                        String.format("Column index \"%d\" outside of allowed range: [0, 2).", columnIndex));
            }

        }

        List<GADateRange> getRanges() {
            return Collections.unmodifiableList(m_ranges);
        }

        void addRow(final GADateRange newValue) {
            final var idx = m_ranges.size();
            m_ranges.add(newValue);
            fireTableRowsInserted(idx, idx);
        }

        void setRows(final List<GADateRange> ranges) {
            final var size = m_ranges.size();
            if (size > 0) {
                m_ranges.clear();
                fireTableRowsDeleted(0, size - 1);
            }
            m_ranges.addAll(ranges);
            fireTableRowsInserted(0, m_ranges.size() - 1);
        }

        void removeRow(final int rowIdx) {
            m_ranges.remove(rowIdx);
            fireTableRowsDeleted(rowIdx, rowIdx);
        }

        void clear() {
            final var curr = m_ranges.size();
            m_ranges.clear();
            if (curr > 0) {
                fireTableRowsDeleted(0, curr - 1);
            }
        }

    }

    @Override
    protected void updateComponent() {
        final var model = (SettingsModelDateRanges) getModel();
        final var fromModel = model.getRanges();

        m_model.clear();
        if (fromModel == null) {
            return;
        }
        m_model.setRows(fromModel);
        setEnabledComponents(model.isEnabled());
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        final var ranges = m_model.getRanges();
        final var numRanges = ranges.size();
        CheckUtils.checkSetting(numRanges > 0, "At least one date range is required.");
        CheckUtils.checkSetting(numRanges <= m_capacity, "At most %d date ranges are supported.", m_capacity);
        for (final var r : ranges) {
            r.validate();
        }
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // no-op
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_table.setEnabled(enabled);
        m_fromInput.setEnabled(enabled);
        m_toInput.setEnabled(enabled);
        m_nameInput.setEnabled(enabled);
    }

    private void resetInputComponents() {
        m_fromInput.clear(true);
        m_toInput.clear(true);
        m_nameInput.clear(true);
    }

    @Override
    public void setToolTipText(final String text) {
        // nothing needed
    }
}
