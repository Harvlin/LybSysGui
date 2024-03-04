package LibSysGui;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.sql.*;
import java.util.HashMap;
import java.util.Optional;

public class Main extends Application {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/userrecord";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "awsddwsa";

    private BookSystem bookSystem = new BookSystem();
    private BorrowSystem borrowSystem = new BorrowSystem(bookSystem);
    private TextArea outputTextArea;
    private Label notificationLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initializeDatabase();

        primaryStage.setTitle("Library");

        Button addButton = new Button("Add Book");
        Button removeButton = new Button("Remove Book");
        Button updateButton = new Button("Update Book");
        Button listButton = new Button("Book List");
        Button borrowButton = new Button("Borrow Book");
        Button returnButton = new Button("Return Book");
        Button borrowedButton = new Button("Borrowed Book List");

        outputTextArea = new TextArea();

        addButton.setOnAction(e -> showAddBookDialog());
        removeButton.setOnAction(e -> showRemoveBookDialog());
        updateButton.setOnAction(e -> showUpdateBookDialog());
        listButton.setOnAction(e -> showBookList());
        borrowButton.setOnAction(e -> showBorrowBookDialog());
        returnButton.setOnAction(e -> showReturnBookDialog());
        borrowedButton.setOnAction(e -> showBorrowedList());

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(10));
        notificationLabel = new Label();
        layout.getChildren().addAll(addButton, removeButton, updateButton, listButton, borrowButton, returnButton, borrowedButton, outputTextArea, notificationLabel);

        Scene scene = new Scene(layout, 400, 400, Color.BEIGE);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    // CRUD operations for books in the database

    private void addBookToDatabase(Book book) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO books (title, author, book_id) VALUES (?, ?, ?)")) {

            preparedStatement.setString(1, book.getTitle());
            preparedStatement.setString(2, book.getAuthor());
            preparedStatement.setInt(3, book.getId());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateBookInDatabase(Book updatedBook) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE books SET author = ?, book_id = ? WHERE title = ?")) {

            preparedStatement.setString(1, updatedBook.getAuthor());
            preparedStatement.setInt(2, updatedBook.getId());
            preparedStatement.setString(3, updatedBook.getTitle());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteBookFromDatabase(String title) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "DELETE FROM books WHERE title = ?")) {

            preparedStatement.setString(1, title);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, Book> getAllBooksFromDatabase() {
        HashMap<String, Book> books = new HashMap<>();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM books")) {

            while (resultSet.next()) {
                String title = resultSet.getString("title");
                String author = resultSet.getString("author");
                int id = resultSet.getInt("book_id");

                Book book = new Book(title, author, id);
                books.put(title, book);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    // UI methods

    private void showAddBookDialog() {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("Add Book");
        dialog.setHeaderText("Enter book details:");

        TextField titleField = new TextField();
        TextField authorField = new TextField();
        TextField idField = new TextField();

        dialog.getDialogPane().setContent(new VBox(10, new Label("Title:"), titleField, new Label("Author:"), authorField, new Label("ID:"), idField));

        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == addButton) {
                try {
                    String title = titleField.getText();
                    String author = authorField.getText();
                    int id = Integer.parseInt(idField.getText());
                    return new Book(title, author, id);
                } catch (NumberFormatException e) {
                    showAlert("Invalid ID. Please enter a valid number.");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(book -> {
            addBookToDatabase(book);
            bookSystem.refreshBooks(getAllBooksFromDatabase());
            showBookList();
        });
    }

    private void showRemoveBookDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Remove Book");
        dialog.setHeaderText("Enter the title of the book to remove:");
        dialog.setContentText("Title:");

        dialog.showAndWait().ifPresent(title -> {
            deleteBookFromDatabase(title);
            bookSystem.refreshBooks(getAllBooksFromDatabase());
            showBookList();
        });
    }

    private void showUpdateBookDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Update Book");
        dialog.setHeaderText("Enter the title of the book to update:");
        dialog.setContentText("Title:");

        dialog.showAndWait().ifPresent(title -> {
            if (bookSystem.isBookExist(title)) {
                Book currentBook = bookSystem.getBook(title);
                showUpdateBookDialog(currentBook);
            } else {
                showAlert("Book does not exist");
            }
        });
    }

    private void showBookList() {
        StringBuilder bookList = new StringBuilder();
        for (Book book : bookSystem.getBooks()) {
            bookList.append(String.format("Title: %s, Author: %s, ID: %d\n", book.getTitle(), book.getAuthor(), book.getId()));
        }
        outputTextArea.setText(bookList.toString());
        notificationLabel.setText("");
    }

    private void showBorrowedList() {
        StringBuilder borrowedList = new StringBuilder();
        for (Book book : borrowSystem.getBorrowedBooks()) {
            borrowedList.append(String.format("Title: %s, Author: %s, ID: %d\n", book.getTitle(), book.getAuthor(), book.getId()));
        }
        outputTextArea.setText(borrowedList.toString());
        notificationLabel.setText(""); // Clear notification label
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showBorrowBookDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Borrow Book");
        dialog.setHeaderText("Enter the title of the book to borrow:");
        dialog.setContentText("Title:");

        dialog.showAndWait().ifPresent(title -> {
            if (bookSystem.isBookExist(title)) {
                borrowSystem.borrowBook(title);
                showBookList();
                showAlert(title + " Borrowed");
            } else {
                showAlert("Book does not exist");
            }
        });
    }

    private void showReturnBookDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Return Book");
        dialog.setHeaderText("Enter the title of the book to return:");
        dialog.setContentText("Title:");

        dialog.showAndWait().ifPresent(title -> {
            if (borrowSystem.isBookBorrowed(title)) {
                borrowSystem.returnBook(title);
                showBookList();
                showAlert(title + " Returned");
            } else {
                showAlert("Book not borrowed");
            }
        });
    }

    private void showUpdateBookDialog(Book currentBook) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("Update Book");
        dialog.setHeaderText("Enter updated book details:");

        TextField titleField = new TextField(currentBook.getTitle());
        TextField authorField = new TextField(currentBook.getAuthor());
        TextField idField = new TextField(String.valueOf(currentBook.getId()));

        dialog.getDialogPane().setContent(new VBox(10, new Label("Title:"), titleField, new Label("Author:"), authorField, new Label("ID:"), idField));

        ButtonType updateButton = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButton, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == updateButton) {
                try {
                    String title = titleField.getText();
                    String author = authorField.getText();
                    int id = Integer.parseInt(idField.getText());
                    return new Book(title, author, id);
                } catch (NumberFormatException e) {
                    showAlert("Invalid ID. Please enter a valid number.");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedBook -> {
            updateBookInDatabase(updatedBook);
            bookSystem.refreshBooks(getAllBooksFromDatabase());
            showBookList();
        });
    }

    private void showDeleteBookDialog(String title) {
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Delete Book");
        confirmationDialog.setHeaderText("Are you sure you want to delete this book?");
        confirmationDialog.setContentText("Title: " + title);

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteBookFromDatabase(title);
            bookSystem.refreshBooks(getAllBooksFromDatabase());
            showBookList();
        }
    }

    public static class Book {
        private String title;
        private String author;
        private int id;

        public String getTitle() {
            return title;
        }

        public int getId() {
            return id;
        }

        public String getAuthor() {
            return author;
        }

        Book(String title, String author, int id) {
            this.title = title;
            this.author = author;
            this.id = id;
        }
    }

    public static class BookSystem {
        private HashMap<String, Book> bookMap = new HashMap<>();

        public void refreshBooks(HashMap<String, Book> books) {
            bookMap = books;
        }

        public void addBook(String title, String author, int id) {
            Book obj = new Book(title, author, id);
            if (bookMap.containsKey(title)) {
                System.out.printf("%s Already Exist\n", title);
            } else {
                bookMap.put(title, obj);
                System.out.printf("%s Added\n", title);
            }
        }

        public void removeBook(String title) {
            if (bookMap.containsKey(title)) {
                bookMap.remove(title);
                System.out.printf("%s Deleted\n", title);
            } else {
                System.out.println("Not exist");
            }
        }

        public boolean isBookExist(String title) {
            return bookMap.containsKey(title);
        }

        public Book getBook(String title) {
            return bookMap.get(title);
        }

        public Iterable<Book> getBooks() {
            return bookMap.values();
        }
    }

    public static class BorrowSystem {
        private HashMap<String, Book> borrowedMap;
        private BookSystem bookSystem;

        public BorrowSystem(BookSystem bookSystem) {
            borrowedMap = new HashMap<>();
            this.bookSystem = bookSystem;
        }

        public void borrowBook(String title) {
            if (bookSystem.isBookExist(title)) {
                borrowedMap.put(title, bookSystem.bookMap.get(title));
                bookSystem.removeBook(title);
            }
        }

        public void returnBook(String title) {
            if (borrowedMap.containsKey(title)) {
                bookSystem.bookMap.put(title, borrowedMap.get(title));
                borrowedMap.remove(title);
            }
        }

        public boolean isBookBorrowed(String title) {
            return borrowedMap.containsKey(title);
        }

        public Iterable<Book> getBorrowedBooks() {
            return borrowedMap.values();
        }
    }
}