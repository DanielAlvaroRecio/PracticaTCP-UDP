import java.io.*;
import java.net.*;
import java.util.*;

public class MainServer {
    //Definimos variables
    private static final int PORT = 12345;
    private static final List<String> historialMensajes = new ArrayList<>();
    private static final List<InetSocketAddress> users = new ArrayList<>();
    private static final List<String> nicknames = new ArrayList<>();

    public static void main(String[] args) {

        //Iniciamos el server y lo mantenemos en bucle hasta que lo cerremos.
        try (DatagramSocket datagramSocket = new DatagramSocket(PORT)) {
            while (true) {

                //Recibimos información del cliente, y dependiendo de como empiece, hacemos una cosa u otra.
                byte[] buffer = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);
                String peticion = new String(datagramPacket.getData(), 0, datagramPacket.getLength()).trim();

                //Mensaje normal
                if (peticion.startsWith("[MSG]")) {
                    reenviarMensaje(peticion, datagramSocket);
                //Desconexión de un cliente.
                } else if(peticion.startsWith("[DESCONECTADO]")) {
                    reenviarMensaje( "[SERVER]"+peticion.substring(14)+ " se ha desconectado",datagramSocket);
                //Registro de usuario.
                }else{
                    registro(peticion, datagramPacket, datagramSocket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Método para reenviar los mensajes de un cliente a todos los que esten conectados
    private static void reenviarMensaje(String mensaje, DatagramSocket datagramSocket) throws IOException {
        historialMensajes.add(mensaje);
        for (InetSocketAddress user : users) {
            byte[] data = mensaje.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, user.getAddress(), user.getPort());
            datagramSocket.send(packet);
        }
    }

    //Método que recibe el nickname solicitado y devuelve info dependiendo si ya existe o no.
    private static void registro(String nickname, DatagramPacket datagramPacket, DatagramSocket datagramSocket) throws IOException {
        //Enviamos [ERROR] si existe (lowercase para no distinguir entre mayus y minus).
        if (nicknames.contains(nickname.toLowerCase())) {
            String mensaje = "[ERROR]";
            byte[] infoRespuesta = mensaje.getBytes();
            DatagramPacket respuesta = new DatagramPacket(infoRespuesta, infoRespuesta.length, datagramPacket.getAddress(), datagramPacket.getPort());
            datagramSocket.send(respuesta);
        //Enviamos [OK] si no existe y lo añadimos a la lista de nicknames
        }else{
            nicknames.add(nickname.toLowerCase());
            users.add(new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort()));
            String mensaje = "[OK]";
            byte[] infoRespuesta = mensaje.getBytes();
            DatagramPacket respuesta = new DatagramPacket(infoRespuesta, infoRespuesta.length, datagramPacket.getAddress(), datagramPacket.getPort());
            datagramSocket.send(respuesta);
            //Tambien le enviamos el historial de todos los mensajes anteriores.
            for(String s : historialMensajes) {
                byte[] mensajes = s.getBytes();
                DatagramPacket mensajesPacket = new DatagramPacket(mensajes, mensajes.length, datagramPacket.getAddress(), datagramPacket.getPort());
                datagramSocket.send(mensajesPacket);
            }
            reenviarMensaje("[SERVER] " + nickname + " se ha unido al chat", datagramSocket);
        }
    }
}
