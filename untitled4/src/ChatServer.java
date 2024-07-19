package com.example.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<UserThread> userThreads = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected");

                UserThread newUser = new UserThread(socket, userThreads);
                userThreads.add(newUser);
                newUser.start();
            }
        } catch (IOException ex) {
            System.out.println("Error in the server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    static boolean sendPrivateMessage(String recipient, String message, String sender) {
        for (UserThread aUser : userThreads) {
            if (aUser.getUserName().equals(recipient)) {
                aUser.sendPrivateMessage(sender, message);
                return true;
            }
        }
        return false;
    }
}

class UserThread extends Thread {
    private Socket socket;
    private PrintWriter writer;
    private static Set<UserThread> userThreads;
    private String userName;

    public UserThread(Socket socket, Set<UserThread> userThreads) {
        this.socket = socket;
        UserThread.userThreads = userThreads;
    }

    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            userName = reader.readLine();
            String serverMessage = "New user connected: " + userName;
            broadcast(serverMessage, this);

            printUsers(); // Send the list of users to the new user

            String clientMessage;

            do {
                clientMessage = reader.readLine();
                if (clientMessage != null) {
                    System.out.println("Received message: " + clientMessage); // Debug message
                    if (clientMessage.startsWith("/w ")) {
                        String[] messageParts = clientMessage.split(" ", 3);
                        if (messageParts.length == 3) {
                            String recipient = messageParts[1];
                            String privateMessage = messageParts[2];
                            if (!ChatServer.sendPrivateMessage(recipient, privateMessage, userName)) {
                                writer.println("User " + recipient + " not found.");
                            }
                        }
                    } else {
                        serverMessage = "[" + userName + "]: " + clientMessage;
                        broadcast(serverMessage, this);
                    }
                }
            } while (clientMessage != null && !clientMessage.equalsIgnoreCase("bye"));

            removeUser(this);
            socket.close();

            serverMessage = userName + " has quitted.";
            broadcast(serverMessage, this);

        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void printUsers() {
        if (userThreads.isEmpty()) {
            writer.println("No other users connected");
        } else {
            writer.println("Connected users: ");
            for (UserThread userThread : userThreads) {
                if (userThread.userName != null) {
                    writer.println(userThread.userName);
                }
            }
        }
    }

    void broadcast(String message, UserThread excludeUser) {
        for (UserThread aUser : userThreads) {
            if (aUser != excludeUser) {
                aUser.writer.println(message);
            }
        }
    }

    void removeUser(UserThread aUser) {
        boolean removed = userThreads.remove(aUser);
        if (removed) {
            System.out.println("The user " + userName + " quitted");
        }
    }

    public String getUserName() {
        return userName;
    }

    public void sendPrivateMessage(String sender, String message) {
        writer.println("Private message from " + sender + ": " + message);
    }
}





























