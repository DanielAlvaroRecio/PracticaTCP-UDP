package servidor;

import hilos.Cliente;

import java.io.*;
import java.net.*;
import java.util.*;

public class MainServer {

    //Definimos el puerto, una lista con todos los mensajes mandados y un usuario,
    //que sera un Map (Esto hace que guardemos el nickname y el socket de cada usuario
    //durante el programa.
    private static final int PORT = 12345;
    private static final List<String> historialMensajes = new ArrayList<>();
    private static final Map<String, Socket> usuarios = new HashMap<>();

    public static void main(String[] args) {
        //Creamos el server y mientras que este esta activo
        //ira creando un hilo para manejar cada cliente.
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new Cliente(socket, usuarios, historialMensajes)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
