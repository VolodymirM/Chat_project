import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Client {
    // Connection variables
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    // UI variables
    private JTextArea chatTextArea;
    private JTextField messageField;
    private JButton sendButton;
    private JTextField roomField;
    private JButton createRoomButton;
    private DefaultListModel<String> roomListModel;
    private DefaultListModel<String> userListModel; // For connected users

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

    public void sendMessage() {
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String messageToSend = messageField.getText();
                    if (!messageToSend.isEmpty()) {
                        bufferedWriter.write(username + ": " + messageToSend);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        messageField.setText("");
                        appendMessage(username + ": " + messageToSend);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                String messageFromChat;

                while (socket.isConnected()) {
                    try {
                        messageFromChat = bufferedReader.readLine();
                        if (messageFromChat != null) {
                            appendMessage(messageFromChat);
                        }
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                        break;
                    }
                }
            }
        }).start();
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
        JList<String> userListView = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userListView);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Users"));
        leftPanel.add(userListScrollPane, BorderLayout.NORTH);

        // Room list and input for creating a room
        roomListModel = new DefaultListModel<>();
        JList<String> roomListView = new JList<>(roomListModel);
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
