package main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * UPDATED DASHBOARD VIEW (DashboardView.java):
 * Features: Interactive Station Sidebar Filter, Directional Platform Renderers (NB/SB),
 * and Dark Mode Event Console.
 */
public class DashboardView extends JFrame {
    private static final long serialVersionUID = 1L;
    
    // Core UI Components
    private JTable stationTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextArea logArea;
    private JLabel statusLabel;
    
    // Sidebar Filter System
    private JList<String> stationFilterList;
    private DefaultListModel<String> filterListModel;
    
    // Header Indicators
    private JLabel timeDisplayLabel;
    private JLabel seasonDisplayLabel;
    private JLabel scheduleStatusLabel;

    public DashboardView() {
        super("TrackOne - LRT-1 Congestion & Delay Monitoring System");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(245, 246, 250));

        // Top Header Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(30, 39, 46));
        topPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel("LRT-1 Real-Time Station Monitor");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        infoPanel.setOpaque(false);

        timeDisplayLabel = new JLabel("Simulated Time: Day 1 | 04:00 AM", SwingConstants.RIGHT);
        timeDisplayLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        timeDisplayLabel.setForeground(new Color(255, 211, 42));

        seasonDisplayLabel = new JLabel("Season: DRY | Regular Weekday", SwingConstants.RIGHT);
        seasonDisplayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        seasonDisplayLabel.setForeground(new Color(210, 218, 226));

        scheduleStatusLabel = new JLabel("Status: CLOSED | Rush Hour: OFF", SwingConstants.RIGHT);
        scheduleStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        scheduleStatusLabel.setForeground(new Color(255, 94, 87));

        infoPanel.add(timeDisplayLabel);
        infoPanel.add(seasonDisplayLabel);
        infoPanel.add(scheduleStatusLabel);

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(infoPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Sidebar Filter Panel
        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(Color.WHITE);
        sidebarPanel.setPreferredSize(new Dimension(250, 0));
        sidebarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 221, 225)),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel filterTitle = new JLabel("Station Filter");
        filterTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        filterTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        sidebarPanel.add(filterTitle, BorderLayout.NORTH);

        filterListModel = new DefaultListModel<>();
        stationFilterList = new JList<>(filterListModel);
        stationFilterList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stationFilterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        stationFilterList.setSelectionBackground(new Color(11, 105, 205));
        stationFilterList.setSelectionForeground(Color.WHITE);
        
        // Trigger filter event when user clicks station names
        stationFilterList.addListSelectionListener(e -> applyFilter());

        JScrollPane filterScroll = new JScrollPane(stationFilterList);
        filterScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 221, 225)));
        sidebarPanel.add(filterScroll, BorderLayout.CENTER);
        
        JLabel filterHelp = new JLabel("Ctrl+Click to select multiple");
        filterHelp.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        filterHelp.setForeground(Color.GRAY);
        sidebarPanel.add(filterHelp, BorderLayout.SOUTH);

        add(sidebarPanel, BorderLayout.WEST);

        // Main JTable setup
        String[] columns = {"ID", "Station Name", "Max Cap", "Northbound (NB)", "Southbound (SB)", "Overall %", "Overall Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        stationTable = new JTable(tableModel);
        stationTable.setRowHeight(35);
        stationTable.setShowGrid(false);
        stationTable.setIntercellSpacing(new Dimension(0, 0));
        stationTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stationTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        stationTable.getTableHeader().setBackground(new Color(241, 242, 246));
        stationTable.getTableHeader().setPreferredSize(new Dimension(0, 40));

        // Setup Sorter for live station filtering
        sorter = new TableRowSorter<>(tableModel);
        stationTable.setRowSorter(sorter);

        // Custom Renderers for Directional Taps and System Status
        stationTable.getColumnModel().getColumn(3).setCellRenderer(new DirectionalCellRenderer()); // NB Column
        stationTable.getColumnModel().getColumn(4).setCellRenderer(new DirectionalCellRenderer()); // SB Column
        stationTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());      // Overall Status Column

        JScrollPane tableScrollPane = new JScrollPane(stationTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(Color.WHITE);

        // Dark Mode Console Log Output
        logArea = new JTextArea(8, 30);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(47, 54, 64));
        logArea.setForeground(new Color(220, 221, 225));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 221, 225)), "Background Event Logs"
        ));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, logScrollPane);
        splitPane.setDividerLocation(450);
        splitPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        splitPane.setOpaque(false);
        add(splitPane, BorderLayout.CENTER);

        statusLabel = new JLabel(" System initialized... Connected to MySQL Database (lrt1_db).");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void applyFilter() {
        List<String> selected = stationFilterList.getSelectedValuesList();
        if (selected.isEmpty()) {
            sorter.setRowFilter(null); // Show all stations if nothing selected
        } else {
            StringBuilder regex = new StringBuilder();
            for (String s : selected) {
                regex.append(Pattern.quote(s)).append("|");
            }
            regex.deleteCharAt(regex.length() - 1); 
            // Filter targets Column 1 (Station Name)
            sorter.setRowFilter(RowFilter.regexFilter(regex.toString(), 1));
        }
    }

    public void initializeTableRows(List<Object[]> initialData) {
        tableModel.setRowCount(0);
        filterListModel.clear();
        
        if (initialData != null) {
            for (Object[] row : initialData) {
                tableModel.addRow(row);
                // Populate filter list options dynamically
                String stationName = (String) row[1];
                filterListModel.addElement(stationName);
            }
        }
    }

    public void updateTableData(List<Object[]> rowsData, String lastRefreshTime) {
        if (rowsData == null || rowsData.isEmpty()) return;

        if (tableModel.getRowCount() != rowsData.size()) {
            initializeTableRows(rowsData);
        } else {
            for (int i = 0; i < rowsData.size(); i++) {
                Object[] row = rowsData.get(i);
                // Updates underlying DefaultTableModel directly to prevent filter conflicts
                tableModel.setValueAt(row[3], i, 3); // NB Taps
                tableModel.setValueAt(row[4], i, 4); // SB Taps
                tableModel.setValueAt(row[5], i, 5); // Congestion Rate %
                tableModel.setValueAt(row[6], i, 6); // Overall Status
            }
        }
        statusLabel.setText(" Last Refresh: " + lastRefreshTime + " | Window: 10-Sec Rolling Window (MySQL Active)");
    }

    public void updateClockHeader(String timeText, String seasonText, String statusText, boolean isOpen, boolean isRushHour) {
        timeDisplayLabel.setText(timeText);
        seasonDisplayLabel.setText(seasonText);
        scheduleStatusLabel.setText(statusText);

        if (!isOpen) scheduleStatusLabel.setForeground(new Color(255, 94, 87));
        else if (isRushHour) scheduleStatusLabel.setForeground(new Color(255, 168, 1));
        else scheduleStatusLabel.setForeground(new Color(5, 196, 107));
    }

    public void appendLog(String text, String simTimestamp) {
        logArea.append("[" + simTimestamp + "] " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * DIRECTIONAL CELL RENDERER:
     * Color codes NB and SB platform columns based on 50% split of total station capacity.
     */
    private class DirectionalCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Map visual row index to model row index (Crucial for live row filtering)
            int modelRow = table.convertRowIndexToModel(row);
            int taps = (Integer) value;
            int maxCap = (Integer) table.getModel().getValueAt(modelRow, 2); 
            String systemStatus = (String) table.getModel().getValueAt(modelRow, 6);
            
            double maxCapPerDirection = maxCap / 2.0;
            double directionalRate = (taps / maxCapPerDirection) * 100;
            
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));

            if (systemStatus.contains("CLOSED")) {
                c.setBackground(new Color(241, 242, 246));
                c.setForeground(Color.LIGHT_GRAY);
                setText("-");
            } else if (directionalRate >= 80.0) {
                c.setBackground(new Color(255, 234, 234)); // Light Red
                c.setForeground(new Color(194, 54, 22));  // Dark Red
                setText(taps + " (Congested)");
            } else if (directionalRate >= 50.0) {
                c.setBackground(new Color(255, 242, 204)); // Light Orange
                c.setForeground(new Color(204, 111, 0));   // Dark Orange
                setText(taps + " (Moderate)");
            } else {
                c.setBackground(new Color(230, 255, 237)); // Light Green
                c.setForeground(new Color(33, 140, 116));  // Dark Green
                setText(taps + " (Normal)");
            }
            return c;
        }
    }

    /**
     * OVERALL STATUS RENDERER:
     * Formats the final system column (CONGESTED, MODERATE, NORMAL, CLOSED).
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;
            
            setHorizontalAlignment(SwingConstants.CENTER);
            
            if (status != null) {
                if (status.contains("CLOSED")) {
                    c.setBackground(new Color(241, 242, 246)); 
                    c.setForeground(new Color(116, 125, 140));
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else if (status.contains("CONGESTED")) {
                    c.setBackground(new Color(255, 71, 87)); 
                    c.setForeground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (status.contains("MODERATE")) {
                    c.setBackground(new Color(255, 165, 2)); 
                    c.setForeground(Color.BLACK);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setBackground(new Color(46, 213, 115)); 
                    c.setForeground(Color.BLACK);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
            }
            return c;
        }
    }
}