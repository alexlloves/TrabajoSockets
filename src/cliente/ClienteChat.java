package cliente;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteChat {
    private final String serverAddress;
    private final int serverPort;
    private final String nick;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final Scanner scanner = new Scanner(System.in);

    public ClienteChat(String serverAddress, int serverPort, String nick) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.nick = nick;
    }

    public void start() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            // Enviar nick al servidor
            out.writeUTF(nick);

            // Recibir mensaje de bienvenida del servidor
            String serverMessage = in.readUTF();
            System.out.println(serverMessage);

            // Iniciar hilo para escuchar mensajes del servidor
            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.start();

            // Leer y enviar comandos y mensajes al servidor
            while (true) {
                String message = scanner.nextLine();
                out.writeUTF(message);
                if (message.equalsIgnoreCase("#salir")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void listenToServer() {
        try {
            String messageFromServer;
            while ((messageFromServer = in.readUTF()) != null) {
                System.out.println(messageFromServer);
            }
        } catch (IOException e) {
            System.out.println("Conexión perdida con el servidor.");
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Uso: java ClienteChat <direccion_servidor> <puerto_servidor> <nombre_nic>");
            return;
        }
        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String nick = args[2];

        ClienteChat client = new ClienteChat(serverAddress, serverPort, nick);
        client.start();
    }
}
