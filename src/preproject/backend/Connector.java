package preproject.backend;

import preproject.backend.handlers.DataImportHandler;
import preproject.backend.models.Message;
import preproject.backend.models.User;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Connector {
    private Connection connect;

    /**
     * The joined table for finding the users for a certain group chat.
     */
    private static final String GROUP_CHAT_TABLE = "messenger.group_chat";

    /**
     * The table consisting all the current user in the database.
     */
    private static final String USER_ACCOUNTS_TABLE = "messenger.user_acc";

    /**
     * The table consisting of all the messages from each user.
     */
    private static final String MESSAGES_TABLE = "messenger.message";

    public static void main(String[] args) {
        new Connector();
    }

    private Connector() {
        createConnection();

        ResultSet gcTable = readDatabase(GROUP_CHAT_TABLE);
        ResultSet userTable = readDatabase (USER_ACCOUNTS_TABLE);
        ResultSet msgTable = readDatabase(MESSAGES_TABLE);

        try {
            Map<String, Set<String>> groupData = DataImportHandler.parseGroupChatTable(gcTable);
            List<User> userData = DataImportHandler.parseUserTable(userTable);
            Map<String, List<Message>> messageData = DataImportHandler.parseMessageTable(msgTable);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void createConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            connect = DriverManager.getConnection("jdbc:mysql://localhost/feedback?autoReconnect=true&useSSL=false"
                    ,"cs222-pregrp3", "prelimgroup3");

            System.out.println("DATABASE CONNECTION SUCCESS");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("DATABASE CONNECTION FAIL");
            e.printStackTrace();
        }
    }

    public ResultSet readDatabase(String table) {
        ResultSet resultSet = null;
        try {
            Statement statement = connect.createStatement();
            resultSet = statement.executeQuery(String.format("SELECT * FROM %s", table));

            System.out.println("GET data from " + table + " SUCCESS");
        } catch (SQLException throwables) {
            System.err.println("GET data from " + table + " FAIL");
            throwables.printStackTrace();
        }
        return resultSet;
    }
}