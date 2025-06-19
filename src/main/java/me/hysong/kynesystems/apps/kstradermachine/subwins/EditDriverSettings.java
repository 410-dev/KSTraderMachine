package me.hysong.kynesystems.apps.kstradermachine.subwins;

import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.driver.TraderDriverSettingsV1;
import me.hysong.atlas.utils.LanguageKit;
import me.hysong.kynesystems.apps.kstradermachine.KSTraderMachine;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditDriverSettings extends JFrame {

    private final TraderDriverManifestV1 manifest;
    private final HashMap<String, JTextField> inputFields = new HashMap<>(); // To store references to text fields

    public EditDriverSettings(TraderDriverManifestV1 manifest) {
        this.manifest = manifest;

        // Get preferences
        String prefLoc = MFS1.realPath(KSTraderMachine.storagePath + "/configs/drivers/" + manifest.getFileSystemIdentifier() + ".json");
        TraderDriverSettingsV1 settings = manifest.getPreferenceObject(prefLoc);

        // Get language code
        String language = LanguageKit.getValue("CURRENT_LANGUAGE");

        // Get elements
        // Ensure labels and descriptions are not null, even if the language or "en-us" is missing.
        HashMap<String, String> labels = settings.getLabels().getOrDefault(language, new HashMap<>());
        if (labels.isEmpty() && !language.equals("en-us")) { // Fallback to en-us if current language labels are empty
            labels = settings.getLabels().getOrDefault("en-us", new HashMap<>());
        }

        HashMap<String, String> descriptions = settings.getDescriptions().getOrDefault(language, new HashMap<>());
        if (descriptions.isEmpty() && !language.equals("en-us")) { // Fallback to en-us
            descriptions = settings.getDescriptions().getOrDefault("en-us", new HashMap<>());
        }

        HashMap<String, Object> defaults = settings.getDefaults();
        HashMap<String, Object> values = settings.getValues(); // These are the current values to display
        ArrayList<String> keys = settings.getOrderedKey();

        // --- Compose UI ---
        setTitle("Exchanges Settings (" + manifest.getDriverName() + ")"); // Dynamic title
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose on close to not exit the main app
        // Set a reasonable minimum and preferred size
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(700, 500));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Add some padding around the panel
        GridBagConstraints gbc = new GridBagConstraints();

        // Title Label (though the JFrame title is usually sufficient)
        // If you want an in-window title, you can add it here.
        // For this example, we'll rely on the JFrame's title.

        // "Edit using editor" Button
        JButton editRawButton = new JButton("Edit using editor (Open notepad, Text Editor, or run nano on terminal)");
        editRawButton.addActionListener(e -> {
            // Placeholder for action: Open the raw JSON file in a text editor
            JOptionPane.showMessageDialog(this,
                    "Functionality to open in external editor needs to be implemented.\n" +
                            "File location: " + prefLoc,
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            // Example: You might use Desktop.getDesktop().edit(new File(prefLoc));
            // Make sure to handle potential exceptions if you implement this.
        });
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3; // Span across all columns
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 15, 0); // Bottom margin
        mainPanel.add(editRawButton, gbc);

        // Dynamically create input fields
        int currentGridY = 1; // Start adding components from the second row
        for (String key : keys) {
            String labelText = labels.getOrDefault(key, key); // Use key as fallback for label
            String valueText = "";
            if (values != null && values.containsKey(key)) {
                Object val = values.get(key);
                valueText = (val != null) ? String.valueOf(val) : "";
            } else if (defaults != null && defaults.containsKey(key)) { // Fallback to default if no value
                Object defVal = defaults.get(key);
                valueText = (defVal != null) ? String.valueOf(defVal) : "";
            }
            String descriptionText = descriptions.getOrDefault(key, "No description available.");

            // Label
            JLabel label = new JLabel(labelText);
            label.setHorizontalAlignment(SwingConstants.LEFT);
            gbc.gridx = 0;
            gbc.gridy = currentGridY;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.2; // Give some weight to the label column
            gbc.insets = new Insets(5, 0, 5, 10); // Top, Left, Bottom, Right
            mainPanel.add(label, gbc);

            // TextField
            JTextField textField = new JTextField(valueText);
            inputFields.put(key, textField); // Store for later retrieval
            gbc.gridx = 1;
            gbc.gridy = currentGridY;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.7; // Give more weight to the text field column
            gbc.insets = new Insets(5, 0, 5, 5);
            mainPanel.add(textField, gbc);

            // Description Button ("?")
            JButton descButton = new JButton("?");
            descButton.setToolTipText("Click for description");
            // Set a fixed preferred size for the "?" button to make it square-ish
            descButton.setPreferredSize(new Dimension(40, (int) textField.getPreferredSize().getHeight()));
            descButton.setMargin(new Insets(2,2,2,2)); // Reduce padding inside button
            final String finalDescriptionText = descriptionText; // Effectively final for lambda
            descButton.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "<html><body style='width: 300px;'>" + // Basic HTML for word wrapping
                            "<b>" + labelText + ":</b><br>" +
                            finalDescriptionText +
                            "</body></html>",
                    "Description",
                    JOptionPane.INFORMATION_MESSAGE));
            gbc.gridx = 2;
            gbc.gridy = currentGridY;
            gbc.fill = GridBagConstraints.NONE; // Don't stretch the button
            gbc.weightx = 0.1; // Give some weight to the button column
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 0, 5, 0);
            mainPanel.add(descButton, gbc);

            currentGridY++;
        }

        // Apply Button
        JButton applyButton = new JButton("Apply");
        applyButton.setFont(applyButton.getFont().deriveFont(Font.BOLD)); // Make Apply button bold
        applyButton.addActionListener(e -> {
            // Retrieve values from text fields and update the settings object
            for (Map.Entry<String, JTextField> entry : inputFields.entrySet()) {
                String key = entry.getKey();
                String valueStr = entry.getValue().getText();

                // Cast value based on the designated type.
                // If cast fails, don't save.
                Class<?> type = settings.getTypes().get(key);
                Object object;
                try {
                    if (type.equals(String.class)) {
                        object = valueStr;
                    } else if (type.equals(Integer.class)) {
                        object = Integer.parseInt(valueStr);
                    } else if (type.equals(Double.class)) {
                        object = Double.parseDouble(valueStr);
                    } else if (type.equals(Long.class)) {
                        object = Long.parseLong(valueStr);
                    } else if (type.equals(Float.class)) {
                        object = Float.parseFloat(valueStr);
                    } else if (type.equals(Short.class)) {
                        object = Short.parseShort(valueStr);
                    } else if (type.equals(Byte.class)) {
                        object = Byte.parseByte(valueStr);
                    } else if (type.equals(Boolean.class)) {
                        object = Boolean.parseBoolean(valueStr);
                    } else if (type.equals(Number.class)) {
                        // Attempt to parse as Double first, then Long if it fails
                        try {
                            object = Double.parseDouble(valueStr);
                        } catch (NumberFormatException ex) {
                            object = Long.parseLong(valueStr);
                        }
                    } else {
                        // Handle other types or throw an error
                        JOptionPane.showMessageDialog(EditDriverSettings.this,
                                "Unsupported setting type for key: " + key,
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return; // Stop processing and don't save
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(EditDriverSettings.this,
                            "Invalid number format for key: " + key + "\nExpected type: " + type.getSimpleName(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return; // Stop processing and don't save
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EditDriverSettings.this,
                            "Error parsing value for key: " + key + "\nError: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return; // Stop processing and don't save
                }
                settings.getValues().put(key, object);
            }

            // Validate
            for (String key : settings.getValues().keySet()) {
                String evaluationResult = settings.validateValue(key);
                if (evaluationResult != null) {
                    JOptionPane.showMessageDialog(null, evaluationResult, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Save the updated settings
            boolean success = settings.save();
            if (success) {
                JOptionPane.showMessageDialog(EditDriverSettings.this,
                        "Settings applied successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose(); // Close the window after applying
            } else {
                JOptionPane.showMessageDialog(EditDriverSettings.this,
                        "Failed to save settings.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = currentGridY;
        gbc.gridwidth = 3; // Span all columns
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 0, 0, 0); // Top margin before apply button
        mainPanel.add(applyButton, gbc);

        // Add a filler component to push everything to the top if the window is resized vertically
        gbc.gridx = 0;
        gbc.gridy = currentGridY + 1;
        gbc.weighty = 1.0; // This component will take up all extra vertical space
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(new JLabel(" "), gbc); // An empty label or JPanel can work as a filler


        add(mainPanel);
        pack(); // Adjusts window size to fit components
        setLocationRelativeTo(null); // Center on screen
    }
}
