/*-
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.mycompany.limproject;

/**
 *
 * @author jonathan
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class TPSAppGUI2 extends JFrame {

    // DB connection settings
    static final String DB_URL = "jdbc:sqlite:tps_db.sqlite";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "";

    CardLayout cardLayout;
    JPanel mainPanel;
    String loggedInUser = null;

    public TPSAppGUI2() {
        setTitle("Transaction Processing System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(null); // Center the window

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add panels for different views
        mainPanel.add(buildLoginPanel(), "login");
        mainPanel.add(buildRegisterPanel(), "register");
        mainPanel.add(buildDashboardPanel(), "dashboard");

        add(mainPanel); // Add the main panel to the frame
        cardLayout.show(mainPanel, "login"); // Show the login panel first
        setVisible(true); // Make the frame visible
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1)); // 6 rows, 1 column
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JButton loginBtn = new JButton("Login");
        JButton toRegisterBtn = new JButton("Go to Register");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(loginBtn);
        panel.add(toRegisterBtn);

        // Action listener for login button
        loginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());

if (authenticate(user, pass)) {
                loggedInUser = user; // Store the logged-in username
                showDashboard(); // Switch to the dashboard
            } else {
                JOptionPane.showMessageDialog(this, "Invalid login", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Action listener to switch to the register panel
        toRegisterBtn.addActionListener(e -> cardLayout.show(mainPanel, "register"));

        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JButton registerBtn = new JButton("Register");
        JButton backBtn = new JButton("Back to Login");

        panel.add(new JLabel("Choose Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Choose Password:"));
        panel.add(passwordField);
        panel.add(registerBtn);
        panel.add(backBtn);

        // Action listener for register button
        registerBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            // Basic validation: Check if username or password is empty
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (registerUser(user, pass)) {
                JOptionPane.showMessageDialog(this, "Registered successfully!");
                cardLayout.show(mainPanel, "login"); // Go back to login after successful registration
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed! Username may already exist.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Action listener to switch back to the login panel
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "login"));

        return panel;
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Input fields for adding transactions
        JTextField descField = new JTextField();
        JTextField amountField = new JTextField();
        JButton addBtn = new JButton("Add Transaction");
        JButton refreshBtn = new JButton("Refresh");
        JButton logoutBtn = new JButton("Logout");

        // Table to display transactions
        JTable table = new JTable();

        JPanel inputPanel = new JPanel(new GridLayout(3, 2)); // Grid for input fields and buttons
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descField);
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(amountField);
        inputPanel.add(addBtn);
        inputPanel.add(refreshBtn);
        
panel.add(inputPanel, BorderLayout.NORTH); // Input panel at the top
        panel.add(new JScrollPane(table), BorderLayout.CENTER); // Table in the center with scroll
        panel.add(logoutBtn, BorderLayout.SOUTH); // Logout button at the bottom

        // Action listener for add transaction button
        addBtn.addActionListener(e -> {
            String desc = descField.getText();
            double amt;

            // Input validation for description
            if (desc.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Description cannot be empty.");
                return;
            }

            try {
                amt = Double.parseDouble(amountField.getText());
                // Validate if amount is positive
                if (amt <= 0) {
                    JOptionPane.showMessageDialog(this, "Amount must be a positive number.");
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount. Please enter a number.");
                return;
            }

            addTransaction(loggedInUser, desc, amt); // Add transaction to DB
            descField.setText(""); // Clear fields after adding
            amountField.setText("");
            loadTransactions(table); // Refresh the table
        });

        // Action listener for refresh button
        refreshBtn.addActionListener(e -> loadTransactions(table));

        // Action listener for logout button
        logoutBtn.addActionListener(e -> {
            loggedInUser = null; // Clear logged-in user
            cardLayout.show(mainPanel, "login"); // Go back to login screen
        });

        return panel;
    }
    
// Authenticates user against the database
    private boolean authenticate(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Use PreparedStatement to prevent SQL injection
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password = ?"
            );
            stmt.setString(1, username);
            stmt.setString(2, password); // In a real app, passwords should be hashed and compared securely
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // If a row is returned, credentials are valid
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error during authentication: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // Registers a new user in the database
    private boolean registerUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Check if username already exists
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return false; // User already exists
            }

            // Insert new user
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)"
            );
            stmt.setString(1, username);
            stmt.setString(2, password); // Again, hash passwords in a real application
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error during registration: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
// Adds a new transaction to the database
    private void addTransaction(String user, String desc, double amount) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO transactions (username, description, amount, timestamp) VALUES (?, ?, ?, NOW())" // NOW() adds current timestamp
            );
            stmt.setString(1, user);
            stmt.setString(2, desc);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Transaction added successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding transaction: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    // Loads transactions for the logged-in user into the JTable

    

// Loads transactions for the logged-in user into the JTable
    private void loadTransactions(JTable table) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, description, amount, timestamp FROM transactions WHERE username = ? ORDER BY timestamp DESC" // Added id and order
            );
            stmt.setString(1, loggedInUser);
            ResultSet rs = stmt.executeQuery();

            // Get metadata to set column names
            ResultSetMetaData meta = rs.getMetaData();
            Vector<String> columnNames = new Vector<>();
            int colCount = meta.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                columnNames.add(meta.getColumnName(i));
            }

            // Get data for the table
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }

            // Set the table model
            table.setModel(new javax.swing.table.DefaultTableModel(data, columnNames) {
                // Make cells non-editable
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading transactions: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper method to display the dashboard and load transactions
    private void showDashboard() {
        // Get the JTable component from the dashboard panel
        // This is a bit brittle, assumes the structure of the dashboard panel
        JTable dashboardTable = (JTable)((JScrollPane)((JPanel)mainPanel.getComponent(2)).getComponent(1)).getViewport().getView();
        loadTransactions(dashboardTable);
        cardLayout.show(mainPanel, "dashboard");
    }

    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Before starting the GUI, let's ensure the database and tables exist.
                // This is a simplified approach for demonstration. In a real app,
                // you'd have a more robust DB setup script.
                try {
                    Class.forName("org.sqlite.JDBC"); // Load the MySQL JDBC driver
                    createDatabaseAndTables();
                } catch (ClassNotFoundException e) {
                    JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found. Please add it to your classpath.", "Driver Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(null, "Error setting up database: " + e.getMessage(), "DB Setup Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                TPSAppGUI2 tpsAppGUI2 = new TPSAppGUI2();
            }
        });
    }
    
// Helper method to create the database and tables if they don't exist
    private static void createDatabaseAndTables() throws SQLException {
        // Connect without specifying a database to create it if it doesn't exist
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", DB_USER, DB_PASSWORD)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS tps_db");
        }

        // Now connect to the specific database
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Statement stmt = conn.createStatement();

            // Create users table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "username VARCHAR(50) PRIMARY KEY," +
                "password VARCHAR(50) NOT NULL" + // In production, store hashed passwords
                ")"
            );

            // Create transactions table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(50) NOT NULL," +
                "description VARCHAR(255) NOT NULL," +
                "amount DOUBLE NOT NULL," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," + // Automatically records transaction time
                "FOREIGN KEY (username) REFERENCES users(username)" + // Link to users table
                ")"
            );
        }
    }
}