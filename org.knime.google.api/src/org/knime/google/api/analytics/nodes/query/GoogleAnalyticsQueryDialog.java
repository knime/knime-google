/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.analytics.nodes.query;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnection;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnectionPortObjectSpec;

import com.google.api.services.analytics.model.Column;
import com.google.api.services.analytics.model.Columns;
import com.google.api.services.analytics.model.Segment;
import com.google.api.services.analytics.model.Segments;

/**
 * The dialog to the GoogleAnalyticsConnector node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class GoogleAnalyticsQueryDialog extends NodeDialogPane {

    private Columns m_columns;

    private Map<String, Column> m_columnMap;

    private Map<String, String> m_segmentMap;

    private JComboBox<String> m_groupSelection;

    private JComboBox<String> m_columnSelection;

    private JButton m_addColumnButton;

    private JList<String> m_dimensions;

    private DefaultListModel<String> m_dimensionsModel;

    private JList<String> m_metrics;

    private DefaultListModel<String> m_metricsModel;

    private JComboBox<String> m_segment;

    private JTextField m_filters;

    private JTextField m_sort;

    private JSpinner m_startDate;

    private JSpinner m_endDate;

    private JSpinner m_startIndex;

    private JSpinner m_maxResults;

    private JComponent m_columnSelectionPanel;

    private JLabel m_warning;

    /**
     * Constructor creating the dialogs content.
     */
    public GoogleAnalyticsQueryDialog() {
        m_warning = new JLabel("Warning: Could not connect to the Google API");
        m_warning.setForeground(Color.RED);
        m_segment = new JComboBox<String>(new DefaultComboBoxModel<String>());
        m_segment.setEditable(true);
        m_filters = new JTextField();
        m_sort = new JTextField();
        m_startDate = new JSpinner(new SpinnerDateModel());
        m_startDate.setEditor(new JSpinner.DateEditor(m_startDate, "yyyy-MM-dd"));
        m_endDate = new JSpinner(new SpinnerDateModel());
        m_endDate.setEditor(new JSpinner.DateEditor(m_endDate, "yyyy-MM-dd"));
        m_startIndex = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_maxResults = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_warning, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 2;
        panel.add(createDimensionsAndMetricsPanel(), gbc);
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weighty = 0;
        gbc.gridy++;
        panel.add(new JLabel("Segment:"), gbc);
        gbc.gridy++;
        panel.add(m_segment, gbc);
        gbc.gridy++;
        panel.add(new JLabel("Filters:"), gbc);
        gbc.gridy++;
        panel.add(m_filters, gbc);
        gbc.gridy++;
        panel.add(new JLabel("Sort:"), gbc);
        gbc.gridy++;
        panel.add(m_sort, gbc);
        gbc.gridy++;
        panel.add(new JLabel("Start date:"), gbc);
        gbc.gridy++;
        panel.add(m_startDate, gbc);
        gbc.gridy++;
        panel.add(new JLabel("End date:"), gbc);
        gbc.gridy++;
        panel.add(m_endDate, gbc);
        gbc.gridy++;
        panel.add(new JLabel("Start index:"), gbc);
        gbc.gridy++;
        panel.add(m_startIndex, gbc);
        gbc.gridy++;
        panel.add(new JLabel("Max results:"), gbc);
        gbc.gridy++;
        panel.add(m_maxResults, gbc);
        addTab("Settings", panel);
    }

    /**
     * @return The dimensions and metrics panel together with the column selection panel
     */
    private JComponent createDimensionsAndMetricsPanel() {
        m_columnSelectionPanel = createColumnSelectionPanel();
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createDimensionsPanel(), gbc);
        gbc.gridy++;
        panel.add(createMetricsPanel(), gbc);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.gridx++;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(m_columnSelectionPanel, gbc);
        return panel;
    }

    /**
     * @return The column selection panel
     */
    private JComponent createColumnSelectionPanel() {
        m_groupSelection = new JComboBox<String>();
        m_columnSelection = new JComboBox<String>();
        m_addColumnButton = new JButton("Add");
        m_addColumnButton.setToolTipText("Add dimension/metric");
        final JLabel description = new JLabel();
        description.setVerticalAlignment(SwingConstants.TOP);
        m_groupSelection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // Change the available columns in the column selection
                refreshAvailableColumns();
            }
        });
        m_addColumnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // Add selected column to query
                String selection = (String)m_columnSelection.getSelectedItem();
                if (selection != null) {
                    Column column = m_columnMap.get(selection);
                    if (column != null) {
                        String id = column.getId().replaceFirst("ga:", "");
                        String type = column.getAttributes().get("type");
                        boolean cancel = false;
                        if (id.contains("XX")) {
                            int number = 1;
                            if (type.equals("DIMENSION")) {
                                number = openNumberDialog("Select custom dimension", id, m_addColumnButton);
                            } else if (type.equals("METRIC")) {
                                number = openNumberDialog("Select custom metric", id, m_addColumnButton);
                            }
                            id = id.replace("XX", ""+number);
                            cancel = number == -1;
                        }
                        if (!cancel) {
                            if (type.equals("DIMENSION")) {
                                m_dimensionsModel.addElement(id);
                            } else if (type.equals("METRIC")) {
                                m_metricsModel.addElement(id);
                            }
                            refreshAddColumnButton();
                        }
                    }
                }
            }
        });
        m_columnSelection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // Refresh column description
                description.setText("");
                String selection = (String)m_columnSelection.getSelectedItem();
                if (selection != null) {
                    Column column = m_columnMap.get(selection);
                    if (column != null) {
                        String name = column.getAttributes().get("uiName");
                        String descriptionText = column.getAttributes().get("description");
                        String info = column.getAttributes().get("type").toLowerCase() + " - " + column.getId().replaceFirst("ga:", "");
                        String html =
                                "<html><body style=\"width: 300px\"><h1>" + name + "</h1><br>" + descriptionText
                                        + "<br><br><i>" + info + "</i></body></html>";
                        description.setText(html);
                    }
                }
                refreshAddColumnButton();
            }
        });
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(m_groupSelection, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(m_columnSelection, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        panel.add(m_addColumnButton, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 15, 5, 15);
        panel.add(description, gbc);
        return new JScrollPane(panel);
    }

    /**
     * @return The dimensions list with label and buttons
     */
    private JComponent createDimensionsPanel() {
        m_dimensionsModel = new DefaultListModel<String>();
        m_dimensions = new JList<String>();
        m_dimensions.setModel(m_dimensionsModel);
        m_dimensions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_dimensions.setPrototypeCellValue("MMMMMMMMMMMMMMMMMMMM");
        JButton dimensionUp = new JButton("" + (char)9650);
        JButton dimensionDown = new JButton("" + (char)9660);
        final JButton dimensionAdd = new JButton("+");
        JButton dimensionRemove = new JButton("X");
        Insets buttonMargin = new Insets(0, 3, 0, 3);
        dimensionUp.setMargin(buttonMargin);
        dimensionDown.setMargin(buttonMargin);
        dimensionAdd.setMargin(buttonMargin);
        dimensionRemove.setMargin(buttonMargin);
        dimensionUp.setToolTipText("Move up");
        dimensionDown.setToolTipText("Move down");
        dimensionAdd.setToolTipText("Add");
        dimensionRemove.setToolTipText("Remove");
        dimensionUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                moveListElement(true, m_dimensions);
            }
        });
        dimensionDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                moveListElement(false, m_dimensions);
            }
        });
        dimensionAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                openAddDialog("Add dimension", m_dimensionsModel, dimensionAdd);
            }
        });
        dimensionRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_dimensions.getSelectedIndex() >= 0) {
                    m_dimensionsModel.remove(m_dimensions.getSelectedIndex());
                    refreshAddColumnButton();
                }
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Dimensions:"), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weighty = 1;
        gbc.gridheight = 4;
        panel.add(new JScrollPane(m_dimensions), gbc);
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx++;
        panel.add(dimensionUp, gbc);
        gbc.gridy++;
        panel.add(dimensionDown, gbc);
        gbc.gridy++;
        panel.add(dimensionAdd, gbc);
        gbc.gridy++;
        panel.add(dimensionRemove, gbc);
        return panel;
    }

    /**
     * @return The metrics list with label and buttons
     */
    private JComponent createMetricsPanel() {
        m_metricsModel = new DefaultListModel<String>();
        m_metrics = new JList<String>();
        m_metrics.setModel(m_metricsModel);
        m_metrics.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_metrics.setPrototypeCellValue("MMMMMMMMMMMMMMMMMMMM");
        JButton metricUp = new JButton("" + (char)9650);
        JButton metricDown = new JButton("" + (char)9660);
        final JButton metricAdd = new JButton("+");
        JButton metricRemove = new JButton("X");
        Insets buttonMargin = new Insets(0, 3, 0, 3);
        metricUp.setMargin(buttonMargin);
        metricDown.setMargin(buttonMargin);
        metricAdd.setMargin(buttonMargin);
        metricRemove.setMargin(buttonMargin);
        metricUp.setToolTipText("Move up");
        metricDown.setToolTipText("Move down");
        metricAdd.setToolTipText("Add");
        metricRemove.setToolTipText("Remove");
        metricUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                moveListElement(true, m_metrics);
            }
        });
        metricDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                moveListElement(false, m_metrics);
            }
        });
        metricAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                openAddDialog("Add metric", m_metricsModel, metricAdd);
            }
        });
        metricRemove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_metrics.getSelectedIndex() >= 0) {
                    m_metricsModel.remove(m_metrics.getSelectedIndex());
                    refreshAddColumnButton();
                }
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Metrics:"), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weighty = 1;
        gbc.gridheight = 4;
        panel.add(new JScrollPane(m_metrics), gbc);
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx++;
        panel.add(metricUp, gbc);
        gbc.gridy++;
        panel.add(metricDown, gbc);
        gbc.gridy++;
        panel.add(metricAdd, gbc);
        gbc.gridy++;
        panel.add(metricRemove, gbc);
        return panel;
    }

    /**
     * Refreshes the columns that are available in the column selection, based on the group selection.
     */
    private void refreshAvailableColumns() {
        String group = (String)m_groupSelection.getSelectedItem();
        boolean all = group == null || group.isEmpty() || group.equals("All");
        m_columnSelection.removeAllItems();
        for (Column column : m_columnMap.values()) {
            String columnGroup = column.getAttributes().get("group");
            if (all || columnGroup.equals(group)) {
                m_columnSelection.addItem(createReadableName(column));
            }
        }
        if (m_columnSelection.getItemCount() > 0) {
            m_columnSelection.setSelectedIndex(0);
        }
    }

    /**
     * Refreshes the enabled state of the add column button.
     */
    private void refreshAddColumnButton() {
        boolean enabled = false;
        String selected = (String)m_columnSelection.getSelectedItem();
        if (selected != null) {
            Column column = m_columnMap.get(selected);
            if (column != null) {
                String id = column.getId();
                String type = column.getAttributes().get("type");
                if (type.equals("DIMENSION")) {
                    boolean found = false;
                    for (int i = 0; i < m_dimensionsModel.getSize(); i++) {
                        if (id.equals(m_dimensionsModel.getElementAt(i))) {
                            found = true;
                            break;
                        }
                    }
                    enabled = !found;
                } else if (type.equals("METRIC")) {
                    boolean found = false;
                    for (int i = 0; i < m_metricsModel.getSize(); i++) {
                        if (id.equals(m_metricsModel.getElementAt(i))) {
                            found = true;
                            break;
                        }
                    }
                    enabled = !found;
                }
            }
        }
        m_addColumnButton.setEnabled(enabled);
    }

    /**
     * @param up Moves the selected element up if true, down if false
     * @param list The list with the element
     */
    private void moveListElement(final boolean up, final JList<String> list) {
        DefaultListModel<String> model = (DefaultListModel<String>)list.getModel();
        int sourceI = list.getSelectedIndex();
        // Check if something is selected
        if (sourceI >= 0) {
            int targetI = sourceI + (up ? -1 : 1);
            // Target must be in the current boundaries, otherwise do nothing
            if (targetI >= 0 && targetI <= model.getSize() - 1) {
                String element = list.getSelectedValue();
                model.remove(sourceI);
                model.add(targetI, element);
                list.setSelectedIndex(targetI);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        GoogleAnalyticsQueryConfiguration config = new GoogleAnalyticsQueryConfiguration();
        String[] dimensions = new String[m_dimensionsModel.getSize()];
        for (int i = 0; i < m_dimensionsModel.getSize(); i++) {
            dimensions[i] = m_dimensionsModel.getElementAt(i);
        }
        config.setDimensions(dimensions);
        String[] metrics = new String[m_metricsModel.getSize()];
        for (int i = 0; i < m_metricsModel.getSize(); i++) {
            metrics[i] = m_metricsModel.getElementAt(i);
        }
        config.setMetrics(metrics);
        String segment = m_segment.getEditor().getItem().toString();
        if (m_segmentMap.containsKey(segment)) {
            segment = m_segmentMap.get(segment);
        }
        config.setSegment(segment);
        config.setFilters(m_filters.getText());
        config.setSort(m_sort.getText());
        config.setStartDate(((JSpinner.DefaultEditor)m_startDate.getEditor()).getTextField().getText());
        config.setEndDate(((JSpinner.DefaultEditor)m_endDate.getEditor()).getTextField().getText());
        config.setStartIndex((int)m_startIndex.getValue());
        config.setMaxResults((int)m_maxResults.getValue());
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        GoogleAnalyticsQueryConfiguration config = new GoogleAnalyticsQueryConfiguration();
        config.loadInDialog(settings);
        String segment = config.getSegment();
        m_segmentMap = new TreeMap<String, String>();
        ((DefaultComboBoxModel<String>)m_segment.getModel()).removeAllElements();
        try {
            GoogleAnalyticsConnectionPortObjectSpec spec = (GoogleAnalyticsConnectionPortObjectSpec)specs[0];
            if (spec != null) {
                GoogleAnalyticsConnection connection = spec.getGoogleAnalyticsConnection();
                Segments segments = connection.getAnalytics().management().segments().list().execute();
                for (Segment seg : segments.getItems()) {
                    m_segmentMap.put(seg.getName(), seg.getId());
                    m_segment.addItem(seg.getName());
                    if (segment.equals(seg.getId())) {
                        segment = seg.getName();
                    }
                }
                if (m_columns == null || !m_columns.getEtag().equals(getEtag(connection))) {
                    m_columns = connection.getAnalytics().metadata().columns().list("ga").execute();
                    m_columnMap = new LinkedHashMap<String, Column>();
                    Set<String> groups = new TreeSet<String>();
                    for (Column column : m_columns.getItems()) {
                        if (!"DEPRECATED".equals(column.getAttributes().get("status"))) {
                            m_columnMap.put(createReadableName(column), column);
                            groups.add(column.getAttributes().get("group"));
                        }
                    }
                    m_groupSelection.removeAllItems();
                    m_groupSelection.addItem("All");
                    for (String group : groups) {
                        m_groupSelection.addItem(group);
                    }
                    m_groupSelection.setSelectedItem("All");
                }
            }
            m_columnSelectionPanel.setVisible(m_columnSelection.getModel().getSize() > 0);
            m_warning.setVisible(false);
        } catch (IOException e) {
            m_columnSelectionPanel.setVisible(false);
        }
        m_segment.setSelectedItem(segment);
        m_dimensionsModel.removeAllElements();
        for (String dimension : config.getDimensions()) {
            m_dimensionsModel.addElement(dimension);
        }
        m_metricsModel.removeAllElements();
        for (String metric : config.getMetrics()) {
            m_metricsModel.addElement(metric);
        }
        m_filters.setText(config.getFilters());
        m_sort.setText(config.getSort());
        ((JSpinner.DefaultEditor)m_startDate.getEditor()).getTextField().setText(config.getStartDate());
        ((JSpinner.DefaultEditor)m_endDate.getEditor()).getTextField().setText(config.getEndDate());
        m_startIndex.setValue(config.getStartIndex());
        m_maxResults.setValue(config.getMaxResults());
    }

    /**
     * @param connection The connection used to acquire the etag
     * @return The current etag for the Google Analytics Metadata
     * @throws IOException If an IO error occurs during execution
     */
    private String getEtag(final GoogleAnalyticsConnection connection) throws IOException {
        com.google.api.services.analytics.Analytics.Metadata.Columns.List query =
                connection.getAnalytics().metadata().columns().list("ga");
        // Only get etag nothing else
        query.put("fields", "etag");
        return query.execute().getEtag();
    }

    private void openAddDialog(final String title, final DefaultListModel<String> listModel, final Component relativeTo) {
        Frame f = null;
        Container c = getPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        final JDialog dialog = new JDialog(f);
        final AtomicBoolean apply = new AtomicBoolean(false);
        JTextField name = new JTextField();
        name.setColumns(30);
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        Insets buttonMargin = new Insets(0, 0, 0, 0);
        ok.setMargin(buttonMargin);
        cancel.setMargin(buttonMargin);
        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    dialog.setVisible(false);
                }
            }
        });
        name.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    apply.set(true);
                    dialog.setVisible(false);
                } else if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    dialog.setVisible(false);
                }
            }
        });
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                apply.set(true);
                dialog.setVisible(false);
            }
        });
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        ok.setPreferredSize(cancel.getPreferredSize());
        dialog.setLayout(new GridBagLayout());
        dialog.setTitle(title);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        dialog.add(name, gbc);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.gridwidth = 1;
        gbc.gridy++;
        dialog.add(new JLabel(), gbc);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx++;
        dialog.add(ok, gbc);
        gbc.gridx++;
        dialog.add(cancel, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(relativeTo);
        dialog.setModal(true);
        dialog.setVisible(true);
        // Continues here after dialog is closed
        if (apply.get()) {
            String column = name.getText();
            if (!column.isEmpty()) {
                boolean found = false;
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.get(i).equals(column)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    listModel.addElement(column);
                    refreshAddColumnButton();
                }
            }
        }
        dialog.dispose();
    }

    private int openNumberDialog(final String title, final String id, final Component relativeTo) {
        Frame f = null;
        Container c = getPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        final JDialog dialog = new JDialog(f);
        final AtomicBoolean apply = new AtomicBoolean(false);
        JSpinner number = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        Insets buttonMargin = new Insets(0, 0, 0, 0);
        ok.setMargin(buttonMargin);
        cancel.setMargin(buttonMargin);
        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    dialog.setVisible(false);
                }
            }
        });
        number.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    apply.set(true);
                    dialog.setVisible(false);
                } else if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    dialog.setVisible(false);
                }
            }
        });
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                apply.set(true);
                dialog.setVisible(false);
            }
        });
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        ok.setPreferredSize(cancel.getPreferredSize());
        dialog.setLayout(new GridBagLayout());
        dialog.setTitle(title);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel(id), gbc);
        gbc.weightx = 1;
        gbc.gridx++;
        gbc.gridwidth = 3;
        dialog.add(number, gbc);
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.gridwidth = 1;
        gbc.gridy++;
        dialog.add(new JLabel(), gbc);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx++;
        dialog.add(ok, gbc);
        gbc.gridx++;
        dialog.add(cancel, gbc);
        dialog.pack();
        dialog.setLocationRelativeTo(relativeTo);
        dialog.setModal(true);
        dialog.setVisible(true);
        // Continues here after dialog is closed
        int result = -1;
        if (apply.get()) {
            result = (Integer)number.getValue();
        }
        dialog.dispose();
        return result;
    }

    private String createReadableName(final Column column) {
        String readableName = column.getAttributes().get("uiName");
        readableName += "  (" + column.getId().replaceFirst("ga:", "") + ")";
        readableName = StringUtils.abbreviate(readableName, 50);
        return readableName;
    }

}
