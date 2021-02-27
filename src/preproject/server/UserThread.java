package preproject.server;

import preproject.server.handlers.LoginHandler;
import preproject.server.handlers.RegisterHandler;
import preproject.server.models.User;

import java.io.*;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static preproject.server.State.*;
import static preproject.server.State.SUCCESS_POST_VERIFIED_USERS;

/**
 * This class runs a session for the server. Where each {@code UserThread} is considered as one session.
 * The server will continually accept the ongoing connection to the socket as much as possible which leads to
 * multiple users accessing in one client.
 */
public class UserThread extends Thread {
    private final Socket SOCKET;
    private final ChatServer SERVER;
    private ObjectOutputStream objOut;
    private User user;

    public UserThread(Socket socket, ChatServer server) {
        this.SOCKET = socket;
        this.SERVER = server;
    }

    @Override
    public void run() {
        try {
            InputStream input = SOCKET.getInputStream();
            ObjectInputStream reader = new ObjectInputStream(input);

            OutputStream output = SOCKET.getOutputStream();
            objOut = new ObjectOutputStream(output);

            // constantly receive data being sent and process it
            SOCKET.setKeepAlive(true);
            while (SOCKET.isConnected()) {
                processReadData(reader);
            }

        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void processReadData(ObjectInputStream reader) throws IOException, SQLException, ClassNotFoundException {

        HashMap<String, Object> readData = (HashMap<String, Object>) reader.readObject();
        System.out.println("============ NEW ACTION ============ USER: ");
        switch (String.valueOf(readData.get("action"))) {
            case Action.LOGIN_USER:
                this.loginAuthentication(new LoginHandler(), readData);
                break;
            case Action.LOGOUT_USER:
                this.SOCKET.close(); // close the socket when user logouts
                break;
            case Action.REGISTER_USER:
                this.registerUser(new RegisterHandler(), readData);
                break;
            case Action.SEND_MESSAGE:
                this.broadcastMessage(readData);
                break;
            case Action.ADD_FAVOURITE:
                this.toggleFavorite(readData);
                break;
            case Action.ADD_GROUP:
                this.addGroup(readData);
                break;
            case Action.ADD_GROUP_NEW_MEMBER:
                this.addGroupMembers((Map<String, Object>) readData.get("membersRepo"));
                break;
            case Action.GET_VERIFIED_USERS:
                this.getUsersForAdmin(true);
                break;
            case Action.GET_UNVERIFIED_USERS:
                this.getUsersForAdmin(false);
                break;
            case Action.POST_VERIFIED_USERS:
                this.updateVerifiedUsers((String)readData.get("email"), (String)readData.get("isVerified"));
                break;
            case Action.ACCEPT_ALL_USERS:
                this.updateALlRegistrations(true);
                break;
            case Action.DECLINE_ALL_USERS:
                this.updateALlRegistrations(false);
                break;
            case Action.GET_GROUP_LIST:
                this.writeGroupList();
                break;
            case Action.GET_USER_INFORMATION:
                this.writeUserInformation(this.user.getEmail());
                break;
            case Action.GET_GROUP_MEMBERS:
                this.getGroupMembersList((String) readData.get("groupId"));
                break;
            case Action.GET_GROUP_MESSAGES:
                this.getGroupMessages((String) readData.get("groupId"));
                break;
        }
    }

    private void updateALlRegistrations(boolean isVerified) throws IOException {
        try {
            if (isVerified) {
                PreparedStatement updateStatement = Connector.connect.prepareStatement(
                        "UPDATE user_acc SET verified = ? WHERE verified = ?");
                updateStatement.setBoolean(1, false);
                updateStatement.setBoolean(2, true); //Verify unverified users
                updateStatement.executeUpdate();
            } else {
                PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                        "DELETE FROM user_acc WHERE verified =?");
                preparedStatement.setBoolean(1, false);
                preparedStatement.executeUpdate();
            }
            objOut.writeObject(true);
            return;
        }catch(Exception e){
            e.printStackTrace();
        }
        objOut.writeObject(false);
    }

    private void updateVerifiedUsers(String email, String isVerified) throws IOException {
        Map<String, Boolean> writeObject= new HashMap<>();
        try {
            if (Boolean.parseBoolean(isVerified)) {
                PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                        "UPDATE user_acc SET verified = ? WHERE user_id = ?");
                preparedStatement.setBoolean(1, true);
                preparedStatement.setInt(2, getUserId(email));
                preparedStatement.executeUpdate();
            }else {
                PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                        "DELETE FROM user_acc WHERE user_id = ?");
                preparedStatement.setInt(1, getUserId(email));
                preparedStatement.executeUpdate();
            }
            writeObject.put(SUCCESS_POST_VERIFIED_USERS,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        writeObject.put(SUCCESS_POST_VERIFIED_USERS,true);
        objOut.writeObject(writeObject);
    }

    private void getUsersForAdmin(boolean isVerified) {
        try {
            PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                    "SELECT * FROM user_acc WHERE verified = ?"
            );
            preparedStatement.setBoolean(1, isVerified);

            ResultSet resultSet = preparedStatement.executeQuery();
//            System.out.println("test");

            List<Map<String, String>> userRepo = new ArrayList<>(); // list of userRepo
            while (resultSet.next()) {
                Map<String, String> mapRepo = new HashMap<>();
                String userId = String.valueOf(resultSet.getInt("user_id"));
                String email = resultSet.getString("email");
                String firstName = resultSet.getString("user_fname");
                String lastName = resultSet.getString("user_lname");
                String isAdmin = String.valueOf(resultSet.getBoolean("is_admin"));

                mapRepo.put("userId", userId);
                mapRepo.put("email", email);
                mapRepo.put("firstName", firstName);
                mapRepo.put("lastName", lastName);
                mapRepo.put("isVerified", String.valueOf(resultSet.getBoolean("verified")));
                mapRepo.put("isAdmin", isAdmin);

                userRepo.add(mapRepo);
//                System.out.println("SIZE OF REPO " + userRepo.size());
            }

            objOut.writeObject(userRepo);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: hook up with SERVER groupList update
    @SuppressWarnings("unchecked")
    private void addGroupMembers(Map<String, Object> groupMap) {
        System.out.println("ADD GROUP MEMBER");
        String groupAlias = (String) groupMap.get("alias");
        String creator = (String) groupMap.get("creator");
        List<String> userList = (List<String>) groupMap.get("members");
        List<Integer> userIdList = getUserIdList(userList);
        userIdList.forEach((id) ->
                SERVER.updateGroupList(
                        String.valueOf(getGroupId(groupAlias, Integer.parseInt(creator))),
                        id.toString()));
        try {
            this.importGroupMembers(userIdList, groupAlias, creator);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getUserIdList(List<String> emailList) {
        System.out.println("=== getUserIDList");
        List<Integer> userIdList = new ArrayList<>();
        for (String email : emailList) {
            System.out.println("email: " + email);
            int userId = getUserId(email);
            if (userId != -1) {
                userIdList.add(getUserId(email));
            }
        }
        return userIdList;
    }

    /**
     *
     * @param userIdList list of user ID's
     * @param groupAlias
     * @param creator creator's email
     * @throws IOException
     */
    private void importGroupMembers(List<Integer> userIdList, String groupAlias, String creator) throws IOException {
        System.out.println("==== Import group members");
        System.out.println("creator: " + creator);
        System.out.println("UserIDLIST " + userIdList);
        System.out.println("Alias " + groupAlias);
        System.out.println("userid(0) " + userIdList.get(0));

        try {
            PreparedStatement insertGroupMembers = Connector.connect.prepareStatement(
                    "INSERT INTO group_msg (group_id, user_id, is_fav) VALUES (?,?,?)"
            );

            int groupId = getGroupId(groupAlias, getUserId(creator));

            for (Integer userId : userIdList) {
                insertGroupMembers.setInt(1, groupId);
                insertGroupMembers.setInt(2, userId);
                insertGroupMembers.setBoolean(3, false);
                insertGroupMembers.executeUpdate();
            }

            objOut.writeObject(true);
        } catch (SQLException e) {
            objOut.writeObject(false);
            e.printStackTrace();
        }
    }

    private int getUserId(String email) {
        try {
            PreparedStatement getUserId = Connector.connect.prepareStatement(
                    "SELECT * FROM user_acc WHERE email = ?");

            getUserId.setString(1, email);
            ResultSet resultSet = getUserId.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int getGroupId(String alias, int adminUserId) {
        try {
            PreparedStatement getGroupId = Connector.connect.prepareStatement(
                    "SELECT * FROM group_repo WHERE alias = ? AND uid_admin = ?");

            getGroupId.setString(1, alias);
            getGroupId.setInt(2, adminUserId);

            ResultSet returnedValue = getGroupId.executeQuery();
            if (returnedValue.next()) {
                int id = returnedValue.getInt("group_id");
                return id;
            }
//            return getGroupId.executeQuery().getInt("group_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * TODO: CONVERT THE EMAIL LIST INTO LIST OF UID FROM SQL QUERY
     * This function creates a new group in the database then adds all the given users into it,
     * it then returns a group id to the client for rendering the group name
     * <p>
     * steps
     * 1. add user to group repo (is admin, alias, uid_admin or creator)
     * 3. get the id of that newly created group and insert all members to group_msg
     *
     * @param groupMap a map of the group alias and list of users in it
     */
    @SuppressWarnings("unchecked")
    private void addGroup(HashMap<String, Object> groupMap) throws IOException {
        System.out.println("==== add group ====");
        String groupAlias = (String) groupMap.get("alias");
        String creator = (String) groupMap.get("creator");
        List<String> userList = (List<String>) groupMap.get("members");
        userList.add(0, creator);
        List<Integer> userIdList = getUserIdList(userList);
        System.out.println("Alias " + (String)groupMap.get("alias"));
        System.out.println("Users " + userList);
        System.out.println("IDs " + userIdList);
        System.out.println("properties succesfully initialized");
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("action", Action.ON_GROUP_CREATION);

        if (doesGroupExists(groupAlias, Integer.toString(getUserId(creator)))) {
            objOut.writeObject(false);
            System.out.println("Group already exists");
            return;
        }

        System.out.println("checked if groups exist");
        try {
            PreparedStatement insertGroupRepo = Connector.connect.prepareStatement(
                    "INSERT INTO group_repo (is_admin, alias, uid_admin) VALUES (?,?,?)"
            );

            System.out.println("prepared the query statement");

            insertGroupRepo.setBoolean(1, false);
            insertGroupRepo.setString(2, groupAlias);
            insertGroupRepo.setInt(3, userIdList.get(0));

            System.out.println("creator: " + userIdList.get(0));

            insertGroupRepo.executeUpdate();
            System.out.println("query executed update");

            this.importGroupMembers(userIdList, groupAlias, creator);

            // Update groupList in server
            SERVER.updateGroupList(
                    String.valueOf(getGroupId(groupAlias, userIdList.get(0))),
                    userIdList.stream().map(String::valueOf).collect(Collectors.toList())
            );

            responseMap.put("status", "true");
            objOut.writeObject(responseMap);
            System.out.println("successful group creation");
        } catch (SQLException e) {
            responseMap.put("status", "false");
            objOut.writeObject(responseMap);
            System.out.println("unsuccessful group creation");
            e.printStackTrace();
        }
    }

    /**
     * @param groupAlias
     * @param groupCreator group creator ID
     * @return
     */
    private boolean doesGroupExists(String groupAlias, String groupCreator) {
        try {
            PreparedStatement findGroup = Connector.connect.prepareStatement(
                    "SELECT * FROM group_repo WHERE alias = ? AND uid_admin = ?"
            );

            findGroup.setString(1, groupAlias);
            findGroup.setInt(2, Integer.parseInt(groupCreator));

            ResultSet result = findGroup.executeQuery();
            if (result.next())
                return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void toggleFavorite(HashMap<String, Object> faveRepo) throws IOException {
        System.out.println("FAVORITE TOGGLED");
        String userId = (String)faveRepo.get("userId");
        String groupId = (String)faveRepo.get("groupId");
        String isFav = (String)faveRepo.get("isFav");

        try {
            // TODO: find the group id of the given data first
            PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                    "UPDATE group_msg SET is_fav = ? where user_id = ? AND group_id = ?"
            );

            preparedStatement.setInt(1, Integer.parseInt(isFav));
            preparedStatement.setString(2, userId);
            preparedStatement.setString(3, groupId);

            preparedStatement.executeUpdate();

            Map<String, Object> response = new HashMap<>();
            response.put("action", Action.ON_FAVORITE_TOGGLED);
            response.put("groupId", groupId);
            response.put("isFav", isFav);
            objOut.writeObject(response);
        } catch (SQLException e) {
            System.out.println("FAVORITE TOGGLE UNSUCCESSFUL");
            e.printStackTrace();
        }
    }

    private void broadcastMessage(HashMap<String, Object> messagesRepo) {
        System.out.println("=== BROADCAST MESSAGE");
        List<Map<String, Object>> messageList = (List<Map<String, Object>> )messagesRepo.get("messagesList");
        for (Map<String, Object> messageRepo : messageList) {
            try {
                PreparedStatement messageInput = Connector.connect.prepareStatement(
                        "INSERT INTO messenger.message VALUES (?, ?, ?, ?)"
                );

                String sender = (String)messageRepo.get("userId");
                String recipient = (String)messageRepo.get("groupId");
                String message = (String)messageRepo.get("messageSent");
                Timestamp timeSent = Timestamp.valueOf((String)messageRepo.get("timeSent"));

                System.out.println(sender);
                System.out.println(recipient);
                System.out.println(message);
                System.out.println(timeSent);

                messageInput.setString(1, sender);
                messageInput.setString(2, recipient);
                messageInput.setString(3, message);
                messageInput.setTimestamp(4, timeSent);

                messageInput.executeUpdate();

                SERVER.broadcast(sender, message, recipient);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO: Register the student into admin braodcast group
    private void registerUser(RegisterHandler registerHandler, HashMap<String, Object> userRepo) throws IOException {
        boolean registerAttempt = registerHandler.registerUser(
                new PasswordAuthentication((String) userRepo.get("email"), ((String)userRepo.get("password")).toCharArray()),
                (String)userRepo.get("firstName"), (String)userRepo.get("lastName"));

        // send a boolean back to the client that identifies if the register attempt is a success or not
        // if not, tell the user to try again later.
        if (registerAttempt)
            objOut.writeObject(true);
        else
            objOut.writeObject(false);
    }

    private void loginAuthentication(LoginHandler loginHandler, HashMap<String, Object> userAuth) throws IOException, SQLException {
        System.out.println(userAuth.get("email") + " " + userAuth.get("password"));
        Optional<ResultSet> userLoginRepo = loginHandler.loginUser(new PasswordAuthentication((String) userAuth.get("email"), ((String) userAuth.get("password")).toCharArray()));
        if (userLoginRepo.isPresent()) {
            // TODO: if userRepo exists then load all of the data that needs to be rendered and send it to the client
            //  data includes: contacts (groups), messages in each group, user account information
            ResultSet resultSet = userLoginRepo.get();
            String email = resultSet.getString("email");
            String firstName = resultSet.getString("user_fname");
            String lastName = resultSet.getString("user_lname");
            String isAdmin = resultSet.getString("is_admin");
            String verified = resultSet.getString("verified");
            String id = resultSet.getString("user_id");

            Map<String, String> userRepo = new HashMap<>();
//            userRepo.put("email", email);
//            userRepo.put("firstName", firstName);
//            userRepo.put("lastName", lastName);
            userRepo.put("isAdmin", isAdmin);
            userRepo.put("isVerified", verified);
//            userRepo.put("userid", id);

            // send data back to client
            objOut.writeObject(true);
            objOut.writeObject(userRepo);
            System.out.println("USER SUCCESSFULLY LOGGED IN");

            // TODO:after sending the data into the client, set this thread into the current user's information containing
            //  the data for the group lists, user info, and all of the messages connected to them
            this.user = new User(resultSet.getInt("user_id"), email, firstName, lastName);
        } else {
            objOut.writeObject(false); // if userRepo does not have any value in it, return false (login failed)
            System.out.println("userRepo no value log in UNSUCCESSFUL");
        }
    }

    private void writeUserInformation(String email) {
        System.out.println("USER INFORMATION REQUEST");
        try {
            PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                    "SELECT * FROM user_acc WHERE email = ?"
            );

            preparedStatement.setString(1, email);
            System.out.println("email: " + email);

            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            // TODO: if userRepo exists then load all of the data that needs to be rendered and send it to the client
            //  data includes: contacts (groups), messages in each group, user account information
            Map<String, String> userRepo = new HashMap<>();
            String userId = String.valueOf(resultSet.getInt("user_id"));
            String firstName = resultSet.getString("user_fname");
            String lastName = resultSet.getString("user_lname");
            String isVerified = String.valueOf(resultSet.getBoolean("verified"));
            String isAdmin = String.valueOf(resultSet.getBoolean("is_admin"));

            userRepo.put("userId", userId);
            userRepo.put("email", email);
            userRepo.put("firstName", firstName);
            userRepo.put("lastName", lastName);
            userRepo.put("isVerified", isVerified);
            userRepo.put("isAdmin", isAdmin);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("action", Action.ON_USER_INFO_SEND);
            responseMap.put("data", userRepo);

            objOut.writeObject(responseMap);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO: Only return groups that the user requesting is part of
     */
    private void writeGroupList() {
        System.out.println("GET GROUP LIST REQUEST");
        try {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("action", Action.ON_GROUP_LIST_SEND);
            System.out.println("responseMap created");
            List<Map<String, String>> groupList = getGroupList();
            responseMap.put("data", groupList);
            System.out.println("groupList added to responseMap");
            objOut.writeObject(responseMap);
            System.out.println("group list returned");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return a list of groups the client is a member of.
     *
     * Algorithm:
     * 1. Get group_ids the client is part of in group_msg table
     * 2. Get rows corresponding to those group_id's in the group_repo table
     */
    private List<Map<String, String>> getGroupList() {
        List<Map<String, String>> groupList = new ArrayList<>();
        try {

            // get group_ids the client is part of
            PreparedStatement getGroup_idsStatement = Connector.connect.prepareStatement(
                    "SELECT group_id, is_fav FROM group_msg WHERE user_id = ?"
            );
            getGroup_idsStatement.setInt(1, this.user.getUserId());
            ResultSet group_idsStatement = getGroup_idsStatement.executeQuery();

            // get group info of those group_ids and add each of them to groupList
            while (group_idsStatement.next()) {
                PreparedStatement getGroupInfoStatement = Connector.connect.prepareStatement(
                        "SELECT * FROM group_repo WHERE group_id = ?"
                );
                getGroupInfoStatement.setInt(1, group_idsStatement.getInt("group_id"));
                ResultSet resultSet = getGroupInfoStatement.executeQuery();

                resultSet.next();
                String groupId = String.valueOf(resultSet.getInt("group_id"));
                String isAdmin = String.valueOf(resultSet.getBoolean("is_admin"));
                String alias = resultSet.getString("alias");
                String uidAdmin = String.valueOf(resultSet.getInt("uid_admin"));

                Map<String, String> groupMap = new HashMap<>();
                groupMap.put("groupId", groupId);
                groupMap.put("isAdmin", isAdmin);
                groupMap.put("alias", alias);
                groupMap.put("uidAdmin",uidAdmin);
                // TODO: Update this when messages table is already functional
                groupMap.put("unreadMessages", "0");
                groupMap.put("is_fav", String.valueOf(group_idsStatement.getInt("is_fav")));

                groupList.add(groupMap);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groupList;
    }

    private void getGroupMembersList(String groupId) {
        List<Map<String, String>> memberRepo = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                    "SELECT user_id FROM group_msg WHERE group_id = ?"
            );

            preparedStatement.setInt(1, Integer.parseInt(groupId));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String userId = resultSet.getString("user_id");
                Optional<ResultSet> resultSetOptional = queryUserInformation(userId);

                if (resultSetOptional.isPresent()) {
                    ResultSet userResult = resultSetOptional.get();
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("userId", userId);
                    userMap.put("email", userResult.getString("email"));
                    userMap.put("firstName", userResult.getString("user_fname"));
                    userMap.put("lastName", userResult.getString("user_lname"));

                    memberRepo.add(userMap);
                }
            }
            objOut.writeObject(memberRepo);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO: Only select up to 10 at a time. joke lang pala
     * @param groupId
     */
    private void getGroupMessages(String groupId) {
        System.out.println("GET INITIAL GROUP MESSAGES");
        Map<String, Object> response = new HashMap<>();
        response.put("action", Action.ON_INITIAL_MESSAGES_RECEIVED);
        List<Map<String, String>> groupMsgRepo = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                    "SELECT * FROM message WHERE group_id = ?"
            );
            preparedStatement.setInt(1, Integer.parseInt(groupId));

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, String> msgRepo = new HashMap<>();
                // TODO: GET sender name
                msgRepo.put("senderName", resultSet.getString("from_user"));
                msgRepo.put("senderId", resultSet.getString("from_user"));
                msgRepo.put("groupId", groupId);
                msgRepo.put("message", resultSet.getString("message"));
                msgRepo.put("timeSent", String.valueOf(resultSet.getTimestamp("time_sent")));
                groupMsgRepo.add(msgRepo);
            }
            response.put("messages", groupMsgRepo);
            objOut.writeObject(response);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private Optional<ResultSet> queryUserInformation(String userId) {
        try {
            PreparedStatement preparedStatement = Connector.connect.prepareStatement(
                    "SELECT email,user_fname,user_lname FROM user_acc WHERE user_id = ?"
            );

            preparedStatement.setInt(1, Integer.parseInt(userId));

            return Optional.ofNullable(preparedStatement.executeQuery());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * This is used to send the message from each user to the client. This is a helper method for the {@link ChatServer}
     * class
     *
     * TODO: Send sender name as well
     *
     * @param message message to send
     * @param address who receives the message
     */
    protected void sendMessage(String sender, String address, String message, Timestamp timeSent) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("action", Action.ON_MESSAGE_RECEIVE);
            Map<String, String> messageRepo = new HashMap<>();
            messageRepo.put("sender", sender);
            messageRepo.put("address", address);
            messageRepo.put("message", message);
            messageRepo.put("timeSent", timeSent.toString());
            response.put("messages",messageRepo);
            objOut.writeObject(response); // send message to the server
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected User getUser() {
        return this.user;
    }
}