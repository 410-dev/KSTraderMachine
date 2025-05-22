package me.hysong.kynesystems.apps.kstradermachine.subwins;

import lombok.Getter;
import me.hysong.apis.kstrader.v1.misc.CurrencyUnitConverter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;
import me.hysong.kynesystems.apps.kstradermachine.backend.Drivers;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.UnaryOperator;

@Getter
public class ProfitLogs extends KSGraphicalApplication implements KSApplication {
    private final String appDisplayName = "Profit Logs";
    private final int closeBehavior = JFrame.DISPOSE_ON_CLOSE;
    private final int windowWidth = 800;
    private final int windowHeight = 600;

    private final static ArrayList<ProfitEntry> profitEntries = new ArrayList<>(); // Oldest goes latest
    private static String sumUnit = "USD";


    // Declare UI components as instance variables if they need to be accessed by other methods (e.g., refresh button)
    private JTable profitTable;
    private ProfitTableModel tableModel;
    private JLabel totalProfitLabel;
    private JButton refreshButton;

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        // setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // Old
        setLayout(new GridBagLayout()); // Use GridBagLayout for more control
        GridBagConstraints gbc = new GridBagConstraints();

        // --- Dummy Data for Testing ---
        // Remove or comment out once you have real data flow
        if (profitEntries.isEmpty()) {
            add(new ProfitEntry(1700000000L, "BTCUSDT", "Binance", 3600, 0.15, "USD", 50, "USD"));
            add(new ProfitEntry(1700000000L - 36000, "XRPUSDT", "Binance", 720, 0.15, "USD", 10, "USD"));
            add(new ProfitEntry(1700000000L - 72000, "SOLKRW", "UpBit", 3600000, 0.15, "KRW", 50000, "KRW"));
            add(new ProfitEntry(System.currentTimeMillis() / 1000L - 86400 * 3, "BTCKRW", "UpBit", 3600, 0.15, "USD", 50, "USD"));
            add(new ProfitEntry(System.currentTimeMillis() / 1000L - 86400 * 2, "XRPUSDT", "ByBit", 3600, 0.15, "USD", 50, "USD"));
            add(new ProfitEntry(System.currentTimeMillis() / 1000L - 86400, "BTCUSDT", "Binance", 3600, 0.15, "USD", 50, "USD"));
            add(new ProfitEntry(System.currentTimeMillis() / 1000L, "ETHUSDT", "Binance", 2500, 0.15, "USD", 75, "USD"));
        }
        // --- End Dummy Data ---

        // 1. Table Area
        tableModel = new ProfitTableModel(profitEntries);
        profitTable = new JTable(tableModel);
        profitTable.setFillsViewportHeight(true); // Makes table use entire height of scroll pane
        profitTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        profitTable.setRowHeight(25);

        // Optional: Set column widths (example)
        profitTable.getColumnModel().getColumn(0).setPreferredWidth(160); // Time
        profitTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Stock ID
        profitTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Exchange
        profitTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Amount
        profitTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Fee
        profitTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Profits

        // Optional: Align text in cells (e.g., center or right for numbers)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        profitTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Amount
        profitTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Fee
        profitTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Profits

        JScrollPane scrollPane = new JScrollPane(profitTable);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0; // Table area takes most of the vertical space
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(scrollPane, gbc);

        // 2. Bottom Panel (Total Profit and Refresh Button)
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0)); // BorderLayout with horizontal gap

        totalProfitLabel = new JLabel("Total Profit: calculating... USDT"); // Initial text
        totalProfitLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        bottomPanel.add(totalProfitLabel, BorderLayout.WEST);

        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        // Add an empty panel to push the refresh button to the right if desired,
        // or use a different layout for more precise control if BorderLayout isn't enough.
        JPanel refreshButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0,0)); // Aligns button to right
        refreshButtonPanel.add(refreshButton);
        bottomPanel.add(refreshButtonPanel, BorderLayout.EAST);


        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0; // Bottom panel takes its preferred height
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 10, 10); // More bottom padding
        add(bottomPanel, gbc);

        // Add ActionListeners and initial data loading
        refreshButton.addActionListener(e -> refreshData());
        refreshData(); // Initial data load and total profit calculation

        return 0;
    }

    // Method to calculate total profit and refresh table
    private void refreshData() {
        // Calculate total profit (assuming all profits are in USDT or need conversion)
        // For this example, let's assume we want to display total in USDT.
        // You'll need a more robust way if currencies vary and need live conversion.
        double totalProfit = 0;

        HashMap<String, CurrencyUnitConverter> cachedConverters = new HashMap<>();

        for (ProfitEntry entry : profitEntries) {
            if (entry.getAmountProfitRealCurrencyUnit().equals(sumUnit)) {
                totalProfit += entry.getAmountProfitRealCurrency();
            } else {
                String converterCacheId = entry.getAmountProfitRealCurrencyUnit() + "2" + sumUnit;
                CurrencyUnitConverter converter;
                if (!cachedConverters.containsKey(converterCacheId)) {
                    try {
                        Map<String, Class<?>> converterDrivers = Drivers.DriverIntrospection.findImplementations(CurrencyUnitConverter.class);
                        System.out.println("Found " + converterDrivers.size() + " converters. Looking for " + converterCacheId);
                        for (Class<?> driver : converterDrivers.values()) {
                            CurrencyUnitConverter instantConverter = (CurrencyUnitConverter) driver.getDeclaredConstructor().newInstance();
                            if (!instantConverter.getTo().equals(sumUnit)) {
                                System.out.println("Warning: " + converterCacheId + " does not support " + instantConverter.getTo() + " currency units");
                                continue; // Don't load unnecessary converters
                            }
                            cachedConverters.put(instantConverter.getFrom() + "2" + instantConverter.getTo(), instantConverter);
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                converter = cachedConverters.getOrDefault(converterCacheId, null);

                if (converter == null) {
                    System.err.println("Can't find converter for " + entry.getAmountProfitRealCurrencyUnit() + " to " + sumUnit + " driver. Skipping entry on " + entry.getTimestamp() + " in " + entry.getExchange());
                    continue;
                }

                totalProfit += converter.apply(entry.getAmountProfitRealCurrency());
            }
        }
        totalProfitLabel.setText(String.format("Total Profit: %.2f %s", totalProfit, sumUnit));

        // Notify the table model that data might have changed
        // This is important if profitEntries can be modified externally
        if (tableModel != null) {
            tableModel.fireTableDataChangedExternal();
        }
    }

    // Override static add method to potentially refresh UI if it's active
    public static void add(ProfitEntry entry) {
        profitEntries.add(entry);
        // If UI is active and an instance exists, you might want to call refreshData() on it
        // This requires SystemLogs.activeInstance pattern like in SystemLogs if this is intended for live updates
        // For now, refresh button handles updates.
    }

    @Getter
    public static class ProfitEntry implements Serializable {
        private final int precision = 2;
        private final long epochTime;
        private final String timestamp;
        private final String symbol;
        private final String exchange;
        private double amountTradeRealCurrency;
        private double fee;
        private String amountTradeRealCurrencyUnit;
        private double amountProfitRealCurrency;
        private String amountProfitRealCurrencyUnit;

        public ProfitEntry(long epochTime, String symbol, String exchange, double amountTradeRealCurrency, double fee, String amountTradeRealCurrencyUnit, double amountProfitRealCurrency, String amountProfitRealCurrencyUnit) {
            this.epochTime = epochTime;
            this.symbol = symbol;
            this.exchange = exchange;
            this.amountTradeRealCurrency = amountTradeRealCurrency;
            this.fee = fee;
            this.amountTradeRealCurrencyUnit = amountTradeRealCurrencyUnit;
            this.amountProfitRealCurrency = amountProfitRealCurrency;
            this.amountProfitRealCurrencyUnit = amountProfitRealCurrencyUnit;

            // Convert epochTime to "yyyy-MM-dd HH:mm:ss" format
            long epochMillis = epochTime;
            // Assuming epochTime might be in seconds (10 digits) or milliseconds (13 digits)
            // Standard Java Date constructor expects milliseconds.
            if (String.valueOf(epochTime).length() == 10) {
                epochMillis = epochTime * 1000L;
            }
            Date date = new Date(epochMillis);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.timestamp = sdf.format(date);
        }

        public ProfitEntry(String line) {
            String[] splits = line.split("//");
            this.epochTime = Long.parseLong(splits[0]);
            this.symbol = splits[1];
            this.exchange = splits[2];
            this.amountTradeRealCurrency = Double.parseDouble(splits[3]);
            this.fee = Double.parseDouble(splits[4]);
            this.amountTradeRealCurrencyUnit = splits[5];
            this.amountProfitRealCurrency = Double.parseDouble(splits[6]);
            this.amountProfitRealCurrencyUnit = splits[7];
            long epochMillis = epochTime;
            if (String.valueOf(epochTime).length() == 10) {
                epochMillis = epochTime * 1000L;
            }
            Date date = new Date(epochMillis);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.timestamp = sdf.format(date);
        }

        public void changeCurrencyForProfit(String newUnit, UnaryOperator<Double> originalToUSD, UnaryOperator<Double> usdToNewCurrency) {
            this.amountProfitRealCurrencyUnit = newUnit;
            this.amountProfitRealCurrency = usdToNewCurrency.apply(originalToUSD.apply(this.amountProfitRealCurrency));
        }

        public void changeCurrencyForTrade(String newUnit, UnaryOperator<Double> originalToUSD, UnaryOperator<Double> usdToNewCurrency) {
            this.amountTradeRealCurrencyUnit = newUnit;
            this.amountTradeRealCurrency = usdToNewCurrency.apply(originalToUSD.apply(this.amountTradeRealCurrency));
        }

        public String toSerializeString() {
            return new StringBuilder()
                    .append(this.epochTime).append("//")
                    .append(this.symbol).append("//")
                    .append(this.exchange).append("//")
                    .append(this.amountTradeRealCurrency).append("//")
                    .append(this.fee).append("//")
                    .append(this.amountTradeRealCurrencyUnit).append("//")
                    .append(this.amountProfitRealCurrency).append("//")
                    .append(this.amountProfitRealCurrencyUnit).append("//")
                    .toString();
        }
    }

    class ProfitTableModel extends AbstractTableModel {
        private final List<ProfitEntry> entries;
        private final String[] columnNames = {"Time", "Stock ID", "Exchange", "Amount", "Fee", "Profits"};

        public ProfitTableModel(List<ProfitLogs.ProfitEntry> entries) {
            this.entries = entries;
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            // Display latest entry at the top (row 0)
            ProfitLogs.ProfitEntry entry = entries.get(entries.size() - 1 - rowIndex);
            switch (columnIndex) {
                case 0: return entry.getTimestamp();
                case 1: return entry.getSymbol();
                case 2: return entry.getExchange();
                case 3: return String.format("%.2f %s", entry.getAmountTradeRealCurrency(), entry.getAmountTradeRealCurrencyUnit()); // Assuming no decimals for trade amount as per image
                case 4: return String.format("%.2f %s", entry.getFee(), entry.getAmountTradeRealCurrencyUnit()); // Assuming no decimals for profit as per image
                case 5: return String.format("%.2f %s", entry.getAmountProfitRealCurrency(), entry.getAmountProfitRealCurrencyUnit()); // Assuming no decimals for profit as per image
                default: return null;
            }
        }

        // Call this method when data changes to update the table
        public void fireTableDataChangedExternal() {
            fireTableDataChanged();
        }
    }

}
