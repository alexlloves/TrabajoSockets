/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author node
 */
public class ServidorChat extends Thread {
    private static final ConcurrentHashMap<String, ManejarCliente> manejadorClientes = new ConcurrentHashMap<>();
    public static final int PUERTO = 1234;
    

    public static void main(String[] args) throws IOException {
       
        
        int port = PUERTO; // El puerto donde el servidor estará escuchando
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en el puerto: " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ManejarCliente clientHandler = new ManejarCliente(clientSocket);
            clientHandler.start();
        }
    }

    private static class ManejarCliente extends Thread {
        private String chattingWith = null;
        private Socket clientSocket;
        private String clientNick;
        private DataOutputStream out;
        private DataInputStream in;

        public ManejarCliente(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new DataOutputStream(clientSocket.getOutputStream());
                in = new DataInputStream(clientSocket.getInputStream());

                // Primer mensaje del cliente es su nick
                clientNick = in.readUTF();
                if (manejadorClientes.containsKey(clientNick)) {
                    out.writeUTF("[ERROR] El nick ya está en uso.");
                    cerrarTodo();
                    return;
                }
                //Obtiene la dirección del cliente conectado al servidor y la almacena en una variable 
                String clientAddress = clientSocket.getRemoteSocketAddress().toString();
                System.out.println("Cliente conectado: " + clientNick + " en " + clientAddress);


                manejadorClientes.put(clientNick, this);
                out.writeUTF("Estas conectado con el nick "+clientNick);

                String command;
                while ((command = in.readUTF()) != null) {
                    if (command.startsWith("#")) {
                        manejarComando(command);
                    } else {
                        if (chattingWith != null && manejadorClientes.containsKey(chattingWith)) {
                            manejadorClientes.get(chattingWith).out.writeUTF(">" + clientNick + ": " + command);
                        } else {
                            out.writeUTF("[ERROR] No estás conectado con ningún usuario.");
                        }
                    }
                }
            } catch (IOException e) {
                //System.err.println("Error en la conexión con el cliente " + clientNick + ": " + e.getMessage());
            } finally {
                System.out.println("Cliente desconectado: " + clientNick + " en " + clientSocket.getRemoteSocketAddress());
                manejadorClientes.remove(clientNick);
                enviarMensajeTodos("SERVIDOR: " + clientNick + " ha salido del chat.");
                cerrarTodo();
            }
        }

        private void manejarComando(String command) throws IOException {
            if (command.equalsIgnoreCase("#ayuda")) {
                out.writeUTF("Comandos disponibles:\n#ayuda\n#listar: lista todos los usuarios conectados\n#charlar <usuario>:comienza la comunicacion con el usuario <usuario>\n#salir: se desconecta del chat");
            } else if (command.equalsIgnoreCase("#listar")) {
                StringBuilder sb = new StringBuilder("Usuarios conectados:\n");
                for (String nick : manejadorClientes.keySet()) {
                    sb.append(nick).append("\n");
                }
                out.writeUTF(sb.toString());
            } else if (command.startsWith("#charlar ")) {
                String targetNick = command.substring(9);
                if (manejadorClientes.containsKey(targetNick)) {
                    chattingWith = targetNick; // Suponiendo que existe esta variable para controlar con quién está chateando el cliente
                    out.writeUTF("Ahora estás conectado con " + targetNick + ". Escribe para hablarle.");
                } else {
                    out.writeUTF("[ERROR] El usuario " + targetNick + " no se encuentra conectado.");
                }
            } else if (command.equalsIgnoreCase("#salir")) {
                cerrarTodo();
            } else {
                out.writeUTF("[ERROR] Comando no reconocido. Escribe #ayuda para ver la lista de comandos.");
            }
        }

        private void enviarMensajeTodos(String message) {
            for (ManejarCliente handler : manejadorClientes.values()) {
                try {
                    if (!handler.clientNick.equals(clientNick)) {
                        handler.out.writeUTF(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void cerrarTodo() {
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
