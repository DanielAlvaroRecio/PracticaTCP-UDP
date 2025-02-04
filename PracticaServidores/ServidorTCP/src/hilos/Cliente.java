package hilos;

import java.io.*;
import java.net.*;
import java.util.*;

//Hilo para cada cliente
public class Cliente implements Runnable {
    //Definimos las variables
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String nickname;
    private final Map<String, Socket> usuarios;
    private final List<String> historialMensajes;

    //Constructor del hilo
    public Cliente(Socket socket, Map<String, Socket> usuarios, List<String> historialMensajes) {
        this.socket = socket;
        this.usuarios = usuarios;
        this.historialMensajes = historialMensajes;
    }

    @Override
    public void run() {
        try {
            //Asignamos los flujos de salida y entra.

            //uso Printwriter porque he visto que es conveniente para escribir texto
            //y me permite usar los prints. como se puede ver más abajo.
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            //Recibimos el nickname que quiere el usuario
            nickname = entrada.readLine();
            //Sincronizamos el map con todos los clientes, de esta manera sabremos
            //si el nickname esta ocupado o no.
            synchronized (usuarios) {
                //si esta ocupado, mandamos el mensaje: [ERROR]
                if (usuarios.containsKey(nickname)) {
                    salida.println("[ERROR]");
                    return;
                //Si no esta ocupado, añadimos el usaurio al map y le mandamos [OK].
                } else {
                    usuarios.put(nickname, socket);
                    salida.println("[OK]");
                }
            }

            //Enviamos a cada cliente el historial de los mensajes anteriores
            synchronized (historialMensajes) {
                for (String mensaje : historialMensajes) {
                    salida.println(mensaje);
                }
            }

            //Avisamos de que un usuario se conectó al chat.
            broadcast("[SERVER] " + nickname + " se ha unido al chat");

            //Mensajes que iremos recibiendo de un cliente, y se lo enviamos a los demas
            //a traves del metodo broadcast.
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                if (mensaje.startsWith("[DESCONECTADO]")) {
                    break;
                }
                broadcast(mensaje);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //Cuando cerramos el form.
            cerrarConexion();
        }
    }

    //Método que ira gestionando todos los mensajes enviados para que
    //el resto de usuarios los puedan ir viendo
    private void broadcast(String mensaje) {
        synchronized (historialMensajes) {
            historialMensajes.add(mensaje);
        }
        synchronized (usuarios) {
            for (Socket userSocket : usuarios.values()) {
                try {
                    PrintWriter out = new PrintWriter(userSocket.getOutputStream(), true);
                    out.println(mensaje);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Si cerramos el form, eliminamos el usuario de la lista y lo ponemos en el chat
    private void cerrarConexion() {
        if (nickname != null) {
            synchronized (usuarios) {
                usuarios.remove(nickname);
                broadcast("[SERVER] " + nickname + " se ha desconectado.");
            }
        }
        //Cerramos el socket
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
