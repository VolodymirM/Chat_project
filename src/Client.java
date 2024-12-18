import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.Arrays;
import java.awt.event.*;

public class Client {
    // Connection variables
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String room = "";

    // UI variables
    private JTextArea chatTextArea;
    private JTextField messageField;
    private JButton sendButton;
    private JTextField roomField;
    private JButton createRoomButton;
    private DefaultListModel<String> roomListModel;
    private JList<String> roomListView;
    private DefaultListModel<String> userListModel; // For connected users
    private JList<String> userListView;

    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.username = username;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            UIinit();
            sendUsername();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void sendUsername() {
        try {
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void createRoom() {
        createRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String roomName = roomField.getText().trim();
                    if (!roomName.isEmpty()) {
                        bufferedWriter.write("CREATE_ROOM:" + roomName);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        roomField.setText("");
                    }
                } catch (IOException ex) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        });
    }

    public void listenForMessage() {
        new Thread(() -> {
            String messageFromChat;
    
            while (socket.isConnected()) {
                try {
                    messageFromChat = bufferedReader.readLine();
                    if (messageFromChat != null) {
                        if (messageFromChat.startsWith("USER_ADDED:")) {
                            String newUser = messageFromChat.substring("USER_ADDED:".length());
                            // Avoid adding the user's own name to the user list
                            if (!newUser.equals(username) && !userListModel.contains(newUser)) {
                                userListModel.addElement(newUser);
                            }
                        } else if (messageFromChat.startsWith("USER_REMOVED:")) {
                            String userToRemove = messageFromChat.substring("USER_REMOVED:".length());
                            userListModel.removeElement(userToRemove);
                        } else if (messageFromChat.startsWith("ROOM_ADDED:")) {
                            String newRoom = messageFromChat.substring("ROOM_ADDED:".length());
                            if (!roomListModel.contains(newRoom)) {
                                roomListModel.addElement(newRoom);
                            }
                        } else {
                            // Display messages received from the server
                            appendMessage(messageFromChat);
                        }
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
            }
        }).start();
    }
    
    public void sendMessage() {
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (room.equals("")) {
                    messageField.setText("");
                    return;
                }
                try {
                    String messageToSend = messageField.getText().trim();
                    if (!messageToSend.isEmpty()) {
                        // Send the message to the server without appending locally
                        bufferedWriter.write(username + ": " + messageToSend);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        messageField.setText(""); // Clear the message field
                    }
                } catch (IOException ex) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        };
    
        sendButton.addActionListener(sendAction);
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendAction.actionPerformed(null);
                }
            }
        });
    }
    
    public void chooseChat() {
        roomListView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    room = roomListView.getSelectedValue();
                    chatTextArea.setText("");
                    try {
                        // Inform the server about joining the selected room
                        bufferedWriter.write("JOIN_ROOM:" + room);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } catch (IOException ex) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                    System.out.println("Joined room: " + room);
                }
                else {
                    userListView.clearSelection();
                }
            }
        });

       userListView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    room = combineStrings(username, userListView.getSelectedValue());
                    chatTextArea.setText("");
                    try {
                        // Inform the server about joining the selected room
                        bufferedWriter.write("JOIN_ROOM:" + room);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } catch (IOException ex) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                    System.out.println("Joined room: " + room);
                }
                else {
                    roomListView.clearSelection();
                }
            }
        });
    }
    
    // The function creates a unique name for a private dialoge
    public static String combineStrings(String string1, String string2) {
        String sorted1 = sortString(string1);
        String sorted2 = sortString(string2);

        if (sorted1.compareTo(sorted2) <= 0) {
            return sorted1 + sorted2;
        } else {
            return sorted2 + sorted1;
        }
    }

    private static String sortString(String input) {
        char[] chars = input.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    private void appendMessage(String message) {
        chatTextArea.append(message + "\n");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null)
                bufferedReader.close();
            if (bufferedWriter != null)
                bufferedWriter.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void UIinit() {
        JFrame frame = new JFrame("Chat Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Main container panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Left panel for users and rooms
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Users & Rooms"));

        // User list
        userListModel = new DefaultListModel<>();
        userListView = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userListView);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Users"));
        leftPanel.add(userListScrollPane, BorderLayout.NORTH);

        // Room list and input for creating a room
        roomListModel = new DefaultListModel<>();
        roomListView = new JList<>(roomListModel);
        JScrollPane roomListScrollPane = new JScrollPane(roomListView);
        roomListScrollPane.setBorder(BorderFactory.createTitledBorder("Rooms"));
        leftPanel.add(roomListScrollPane, BorderLayout.CENTER);

        JPanel roomInputPanel = new JPanel(new BorderLayout());
        roomField = new JTextField();
        createRoomButton = new JButton("Create");
        roomInputPanel.add(new JLabel("Name: "), BorderLayout.WEST);
        roomInputPanel.add(roomField, BorderLayout.CENTER);
        roomInputPanel.add(createRoomButton, BorderLayout.EAST);
        leftPanel.add(roomInputPanel, BorderLayout.SOUTH);

        // Right panel for the currently opened chat
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat Window"));

        // Text area for chat messages
        chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatTextArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Bottom panel for typing and sending messages
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.setEnabled(false);

        // Text field for typing a message
        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        // Send button
        sendButton = new JButton("Send");
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Add input panel to the bottom of the chat panel
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // Add panels to the main panel
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(chatPanel, BorderLayout.CENTER);

        // Add the main panel to the frame
        frame.add(mainPanel);

        // Setup event listeners
        chooseChat();
        sendMessage();
        createRoom();
        
        frame.setLocationRelativeTo(null);
        // Make the frame visible
        frame.setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.isEmpty()) {
            Socket socket = new Socket("localhost", 1234);
            Client client = new Client(socket, username);
            client.listenForMessage();
        } else {
            System.exit(0);
        }
    }
}
