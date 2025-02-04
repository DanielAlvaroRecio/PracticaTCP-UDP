import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class MainClient {
    public static final int PUERTO = 12345;
    public static final String HOST = "localhost";
    private static DatagramSocket datagramSocket;
    private static InetAddress inetAddress;
    private static String nickname;
    private static JTextArea chatArea;
    private static JTextField messageField;

    public static void main(String[] args) {
        //Configuración de la interfaz gráfica
        JFrame frame = new JFrame("Chat Cliente");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 500);

        //Panel principal
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        frame.add(panel);

        //Caja de texto para los mensajes
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        //Panel extra que almacena campo de texto y boton para enviar mensajes
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.CENTER));  // Para alinearlos a la izquierda
        panel.add(inputPanel, BorderLayout.SOUTH);

        //Campo de texto para enviar mensajes
        messageField = new JTextField(20);
        inputPanel.add(messageField);

        //Botón para enviar mensajes
        JButton sendButton = new JButton("Enviar");
        sendButton.setPreferredSize(new Dimension(80, 25));
        inputPanel.add(sendButton);

        sendButton.addActionListener(e -> {
            String mensaje = messageField.getText();
            if (!mensaje.trim().isEmpty()) {
                enviarMensaje("[MSG] " + nickname + ": " + mensaje);
                messageField.setText("");
            }
        });

        //Evento cuando cerramos el form.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //Enviar un mensaje de desconexión antes de cerrar la ventana
                if (nickname != null && !nickname.trim().isEmpty()) {
                    enviarMensaje("[DESCONECTADO] " + nickname);
                }
                //Cerrar el socket
                if (datagramSocket != null && !datagramSocket.isClosed()) {
                    datagramSocket.close();
                }
                System.exit(0);  // Finalizar la aplicación
            }
        });

        frame.setVisible(true);

        //Conexión y registro del usuario
        try {
            datagramSocket = new DatagramSocket();
            inetAddress = InetAddress.getByName(HOST);
            boolean nicknameValido = false;

            //Registro del usuario, enviamos el nickname y el servidor comprueba si esta ocupado o no
            while (!nicknameValido) {
                nickname = JOptionPane.showInputDialog(frame, "Introduce tu nombre:");
                if (nickname != null && !nickname.trim().isEmpty()) {
                    enviarMensaje(nickname);
                    String respuesta = recibirMensaje(); //Respuesta del server
                    //Ocupado
                    if (respuesta.equals("[ERROR]")) {
                        JOptionPane.showMessageDialog(frame, "Nickname ocupado, prueba otro.");
                    //Disponible, salimos del bucle.
                    } else {
                        JOptionPane.showMessageDialog(frame, "Nickname válido. ¡Bienvenido!");
                        nicknameValido = true;
                        frame.setTitle(nickname); //Cambiamos el nombre del formulario al nickname del usuario, para diferenciarlo.
                    }
                }
            }

            //Hilo para recibir mensajes y ponerlos en la caja de texto, de esta manera funciona
            //independiente y mejoramos el rendimiento de la interfaz.
            Thread recibirMensajes = new Thread(() -> {
                while (true) {
                    try {
                        String mensaje = recibirMensaje();
                        chatArea.append(mensaje + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            recibirMensajes.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Metodo para enviar información al server.
    private static void enviarMensaje(String mensaje) {
        try {
            byte[] info = mensaje.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(info, info.length, inetAddress, PUERTO);
            datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Metodo para recibir info del server
    private static String recibirMensaje() throws IOException {
        byte[] infoServer = new byte[1024];
        DatagramPacket datagramPacketServer = new DatagramPacket(infoServer, infoServer.length);
        datagramSocket.receive(datagramPacketServer);
        return new String(datagramPacketServer.getData(), 0, datagramPacketServer.getLength());
    }
}
