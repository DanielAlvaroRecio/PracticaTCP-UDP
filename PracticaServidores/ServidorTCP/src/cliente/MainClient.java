package cliente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class MainClient {
    //Definimos variables.
    public static final int PUERTO = 12345;
    public static final String HOST = "localhost";
    private static Socket socket;
    private static PrintWriter salida;
    private static BufferedReader entrada;
    private static String nickname;
    private static JTextArea chatArea;
    private static JTextField messageField;

    public static void main(String[] args) {

        //Jframe (Formulario)
        JFrame frame = new JFrame("Chat Cliente");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 500);

        //Panel que contendra los elementos y añadiremos al frame.
        JPanel panel = new JPanel(new BorderLayout());
        frame.add(panel);

        //Cuadro que contendra los mensajes que se iran enviando.
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        //Habilitamos el scroll y lo ponemos en el centro del panel.
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        //Creamos otro panel que contendra la caja para escribir y el botón para enviar.
        //Lo ponemos abajo del panel principal
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); //Alinemos las cosas al centro
        panel.add(inputPanel, BorderLayout.SOUTH);

        //Caja de texto de los mensajes y lo añadimos al panel secundario
        messageField = new JTextField(20);
        inputPanel.add(messageField);
        //Boton para enviar los mensajes. Lo añadimos al panel secundario.
        JButton sendButton = new JButton("Enviar");
        inputPanel.add(sendButton);

        //Evento del boton, obtenemos el texto de la caja de texto y lo ponemos
        //en la caja de los mensajes
        sendButton.addActionListener(e -> {
            String mensaje = messageField.getText();
            if (!mensaje.trim().isEmpty()) {
                enviarMensaje("[MSG] " + nickname + ": " + mensaje);
                messageField.setText("");
            }
        });

        //Evento que sucede cuando cerramos el formulario.
        //Añadira un mensaje en la caja de mensajes informando de la desconexión.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (nickname != null && !nickname.trim().isEmpty()) {
                    enviarMensaje("[DESCONECTADO] " + nickname);
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                System.exit(0);
            }
        });

        //Hacemos visible el formulario.
        frame.setVisible(true);

        //Se conecta al servidor y creamos la salida y entrada de datos.
        try {
            socket = new Socket(HOST, PUERTO);
            salida = new PrintWriter(socket.getOutputStream(), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            boolean nicknameValido = false;

            //Mientras que el nickname sea false, seguira pidiendo un nombre.
            //Cuando ponemos un nickname, se lo mandamos al servidor, y este nos dara una respuesta
            //según si ya existe (está en el Map del server) o no.

            //si existe nos mandara un String = [ERROR], si no nos mandara [OK].

            //Esto lo maneja cada hilo independiente Cliente, que creamos desde el server.
            while (!nicknameValido) {
                nickname = JOptionPane.showInputDialog(frame, "Introduce tu nombre:");
                if (nickname != null && !nickname.trim().isEmpty()) {
                    salida.println(nickname);
                    String respuesta = entrada.readLine();
                    //Existe el nickname
                    if ("[ERROR]".equals(respuesta)) {
                        JOptionPane.showMessageDialog(frame, "Nickname ocupado, prueba otro.");
                        //No existe
                    } else {
                        JOptionPane.showMessageDialog(frame, "Nickname válido. ¡Bienvenido!");
                        frame.setTitle(nickname);
                        nicknameValido = true;
                    }
                }
            }
            //Creamos un hilo, que leera los mensajes que manda el servidor y los
            //irá añadiendo a la caja de los mensajes.

            //Uso un hilo porque al ser independiente del
            // resto del programa de esta manera
            // evitamos que se bloquee la interfaz mientras
            //se reciben los mensajes.
            Thread recibirMensajes = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        chatArea.append(mensaje + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            recibirMensajes.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //método con el que enviamos información al server
    private static void enviarMensaje(String mensaje) {
        if (salida != null) {
            salida.println(mensaje);
        }
    }
}
