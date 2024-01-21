package servidor;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorChat {
    private static final ConcurrentHashMap<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 12345; // El puerto donde el servitor estará escuchando
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en el puerto: " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            new Thread(clientHandler).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private String chattingWith = null;
        private Socket clientSocket;
        private String clientNick;
        private DataOutputStream out;
        private DataInputStream in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new DataOutputStream(clientSocket.getOutputStream());
                in = new DataInputStream(clientSocket.getInputStream());

                // Primer mensaje del cliente es su nick
                clientNick = in.readUTF();
                if (clientHandlers.containsKey(clientNick)) {
                    out.writeUTF("[ERROR] El nick ya está en uso.");
                    closeEverything();
                    return;
                }
                String clientAddress = clientSocket.getRemoteSocketAddress().toString();
                System.out.println("Cliente conectado: " + clientNick + " en " + clientAddress);


                clientHandlers.put(clientNick, this);
                out.writeUTF("Estas conectado con el nic "+clientNick);
                //broadcastMessage("SERVIDOR: " + clientNick + " se ha unido al chat.");

                String command;
                while ((command = in.readUTF()) != null) {
                    if (command.startsWith("#")) {
                        handleCommand(command);
                    }else{
                        if (chattingWith != null && clientHandlers.containsKey(chattingWith)) {
                            clientHandlers.get(chattingWith).out.writeUTF(">" + clientNick + ": " + command);
                        } else {
                            out.writeUTF("[ERROR] No estás conectado con ningún usuario.");
                        }
                    }
                }
            } catch (IOException e) {
                //System.err.println("Error en la conexión con el cliente " + clientNick + ": " + e.getMessage());
            } finally {
                System.out.println("Cliente desconectado: " + clientNick + " en " + clientSocket.getRemoteSocketAddress());
                clientHandlers.remove(clientNick);
                broadcastMessage("SERVIDOR: " + clientNick + " ha salido del chat.");
                closeEverything();
            }
        }

        private void handleCommand(String command) throws IOException {
            if (command.equalsIgnoreCase("#ayuda")) {
                out.writeUTF("Comandos disponibles:\n#ayuda\n#listar: lista todos los usuarios conectados\n#charlar <usuario>:comienza la comunicacion con el usuario <usuario>\n#salir: se desconecta del chat");
            } else if (command.equalsIgnoreCase("#listar")) {
                StringBuilder sb = new StringBuilder("Usuarios conectados:\n");
                for (String nick : clientHandlers.keySet()) {
                    sb.append(nick).append("\n");
                }
                out.writeUTF(sb.toString());
            } else if (command.startsWith("#charlar ")) {
                String targetNick = command.substring(9);
                if (clientHandlers.containsKey(targetNick)) {
                    chattingWith = targetNick; // Suponiendo que existe esta variable para controlar con quién está chateando el cliente
                    out.writeUTF("Ahora estás conectado con " + targetNick + ". Escribe para hablarle.");
                } else {
                    out.writeUTF("[ERROR] El usuario " + targetNick + " no se encuentra conectado.");
                }
            } else if (command.equalsIgnoreCase("#salir")) {
                closeEverything();
            } else {
                out.writeUTF("[ERROR] Comando no reconocido. Escribe #ayuda para ver la lista de comandos.");
            }

        }

        private void broadcastMessage(String message) {
            for (ClientHandler handler : clientHandlers.values()) {
                try {
                    if (!handler.clientNick.equals(clientNick)) {
                        handler.out.writeUTF(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeEverything() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}