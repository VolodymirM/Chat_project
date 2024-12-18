import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public static ArrayList<String> rooms = new ArrayList<>();
    public static ArrayList<String> messages = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    public String clientUsername;
    private String currentRoom = "";

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this);

            broadcastUserAdded(clientUsername);
            sendExistingUsersAndRooms();

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient != null) {
                    if (messageFromClient.startsWith("CREATE_ROOM:")) {
                        String roomName = messageFromClient.substring("CREATE_ROOM:".length());
                        createRoom(roomName);
                    } else if (messageFromClient.startsWith("JOIN_ROOM:")) {
                        this.currentRoom = messageFromClient.substring("JOIN_ROOM:".length());
                        broadcastMessage("SERVER: " + clientUsername + " joined room " + currentRoom, currentRoom);
                    } else {
                        broadcastMessage(messageFromClient, currentRoom);
                    }
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend, String room) {
        messages.add(messageToSend); // Log the message
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (clientHandler.currentRoom.equals(room)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void broadcastUserAdded(String username) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.bufferedWriter.write("USER_ADDED:" + username);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void broadcastUserRemoved(String username) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.bufferedWriter.write("USER_REMOVED:" + username);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void sendExistingUsersAndRooms() {
        try {
            for (ClientHandler clientHandler : clientHandlers) {
                if (!clientHandler.clientUsername.equals(this.clientUsername)) {
                    bufferedWriter.write("USER_ADDED:" + clientHandler.clientUsername);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            }

            for (String room : rooms) {
                bufferedWriter.write("ROOM_ADDED:" + room);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void createRoom(String roomName) {
        if (!rooms.contains(roomName)) {
            rooms.add(roomName);
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                    clientHandler.bufferedWriter.write("ROOM_ADDED:" + roomName);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastUserRemoved(clientUsername);
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
