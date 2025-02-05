package org.example.demo1;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.sql.*;

public class SupplyChainQueriesApp extends Application {

    private static final String URL = "jdbc:mysql://localhost:3306/colocviu";
    private static final String USER = "root";
    private static final String PASSWORD = "Anifoxy1";
    private Connection connection;
    private TextArea resultArea;
    private TableView<ObservableList<String>> resultTable;
    private TabPane tabPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        connect();

        primaryStage.setTitle("Supply Chain Queries");
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        Label label = new Label("Select a query to execute:");
        ComboBox<String> queryComboBox = new ComboBox<>();
        queryComboBox.getItems().addAll(
                "Query 3.a",
                "Query 3.b",
                "Query 4.a",
                "Query 4.b",
                "Query 5.a",
                "Query 5.b",
                "Query 6.a",
                "Query 6.b"
        );

        Button executeButton = new Button("Execute Query");
        Button insertButton = new Button("Insert Record");
        Button deleteButton = new Button("Delete Record");
        Button openTableButton = new Button("Open Table in New Tab");

        // TabPane for dynamic tabs
        tabPane = new TabPane();

        // ComboBox for table selection
        Label tableLabel = new Label("Select a table to open:");
        ComboBox<String> tableComboBox = new ComboBox<>();
        populateTableComboBox(tableComboBox);

        // Button actions
        executeButton.setOnAction(e -> {
            String selectedQuery = queryComboBox.getValue();
            if (selectedQuery != null) {
                executeQuery(getQuery(selectedQuery));
            } else {
                resultTable.getItems().clear();
                resultTable.getColumns().clear();
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a query.", ButtonType.OK);
                alert.show();
            }
        });

        insertButton.setOnAction(e -> showInsertDialog());
        deleteButton.setOnAction(e -> showDeleteDialog());

        openTableButton.setOnAction(e -> {
            String selectedTable = tableComboBox.getValue();
            if (selectedTable != null) {
                openTableInNewTab(selectedTable);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a table.", ButtonType.OK);
                alert.show();
            }
        });

        // Create a horizontal row for the buttons
        HBox buttonRow = new HBox(10);
        buttonRow.setPadding(new Insets(5, 0, 5, 0));
        buttonRow.getChildren().addAll(executeButton, insertButton, deleteButton, openTableButton);

        resultTable = new TableView<>();

        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(100);

        layout.getChildren().addAll(label, queryComboBox, tableLabel, tableComboBox, buttonRow, resultTable, resultArea, tabPane);

        Scene scene = new Scene(layout, 700, 500);
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        primaryStage.getIcons().add(new Image(getClass().getResource("/icons/app-icon.png").toExternalForm()));
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> closeConnection());
    }

    private void executeQuery(String query) {
        if (query == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid query selection.", ButtonType.OK);
            alert.show();
            return;
        }

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            resultTable.getItems().clear();
            resultTable.getColumns().clear();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Dynamically add columns
            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
                column.setCellValueFactory(param ->
                        new SimpleStringProperty(param.getValue().get(columnIndex - 1))
                );
                resultTable.getColumns().add(column);
            }

            // Populate rows
            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }

            resultTable.setItems(data);

        } catch (SQLException e) {
            showError("Query failed: " + e.getMessage());
        }
    }

    private void populateTableComboBox(ComboBox<String> tableComboBox) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tableComboBox.getItems().add(rs.getString(1));
            }
        } catch (SQLException e) {
            showError("Failed to load table names: " + e.getMessage());
        }
    }

    private void openTableInNewTab(String tableName) {
        Tab tableTab = new Tab(tableName);

        TableView<ObservableList<String>> tableView = new TableView<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Dynamically add columns to the TableView
            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
                column.setCellValueFactory(param ->
                        new SimpleStringProperty(param.getValue().get(columnIndex - 1))
                );
                tableView.getColumns().add(column);
            }

            // Populate rows in the TableView
            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }

            tableView.setItems(data);
        } catch (SQLException e) {
            showError("Failed to display table: " + e.getMessage());
        }

        tableTab.setContent(tableView);
        tabPane.getTabs().add(tableTab);
        tabPane.getSelectionModel().select(tableTab);
    }

    private void showInsertDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Insert Record");
        dialog.setHeaderText("Insert a new record");
        dialog.setContentText("Enter your SQL INSERT command:");
        dialog.showAndWait().ifPresent(this::executeUpdate);
    }

    private void showDeleteDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Delete Record");
        dialog.setHeaderText("Delete a record");
        dialog.setContentText("Enter your SQL DELETE command:");
        dialog.showAndWait().ifPresent(this::executeUpdate);
    }

    private void executeUpdate(String sql) {
        try (Statement stmt = connection.createStatement()) {
            int rowsAffected = stmt.executeUpdate(sql);
            resultArea.setText(rowsAffected + " rows affected.");
        } catch (SQLException e) {
            showError("Operation failed: " + e.getMessage());
        }
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to the database.");
        } catch (ClassNotFoundException | SQLException e) {
            showError("Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private String getQuery(String queryName) {
        return switch (queryName) {
            case "Query 3.a" -> "SELECT idf, idc, idp FROM Livrari WHERE cantitate BETWEEN 100 AND 1000 ORDER BY cantitate;";
            case "Query 3.b" -> "SELECT idf, numef, stare, oras FROM Furnizori WHERE numef LIKE '%S.A.%' ORDER BY oras ASC, numef DESC;";
            case "Query 4.a" -> "SELECT F.numef, C.numec, F.oras FROM Livrari L JOIN Furnizori F ON L.idf = F.idf JOIN Componente C ON L.idc = C.idc JOIN Proiecte P ON L.idp = P.idp WHERE F.oras = P.oras;";
            case "Query 4.b" -> "SELECT L1.idc AS idc1, L2.idc AS idc2 FROM Livrari L1 JOIN Livrari L2 ON L1.idf = L2.idf AND L1.idp = L2.idp AND L1.idc < L2.idc;";
            case "Query 5.a" -> "SELECT C.numec FROM Componente C WHERE C.idc IN (SELECT L.idc FROM Livrari L JOIN Proiecte P ON L.idp = P.idp WHERE P.oras = 'Gherla' AND L.cantitate = ( SELECT MAX(L1.cantitate) FROM Livrari L1 JOIN Proiecte P1 ON L1.idp = P1.idp WHERE P1.oras = 'Gherla'));";
            case "Query 5.b" -> "SELECT F.numef FROM Furnizori F WHERE F.oras = (SELECT C.oras FROM Componente C WHERE C.idc = 'C001') AND EXISTS (SELECT 1 FROM Livrari L WHERE L.idf = F.idf AND L.idc = 'C001');";
            case "Query 6.a" -> "SELECT oras, (SELECT COUNT(*) FROM Proiecte P WHERE P.oras = F.oras) AS numar_proiecte, (SELECT COUNT(*) FROM Componente C WHERE C.oras = F.oras) AS numar_componente, (SELECT COUNT(*) FROM Furnizori F1 WHERE F1.oras = F.oras) AS numar_furnizori FROM Furnizori F GROUP BY oras;";
            case "Query 6.b" -> "SELECT um, MIN(cantitate) AS cantitate_minima, AVG(cantitate) AS cantitate_medie, MAX(cantitate) AS cantitate_maxima FROM Livrari WHERE idc = 'C001' GROUP BY um;";
            default -> null;
        };
    }

    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            showError("Error closing connection: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
