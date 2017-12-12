import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class ChatClient {

	String name;
    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("IRC Chat Project");
    JTextField textField = new JTextField(100);
    JPanel topPanel = new JPanel();
    JTextPane tPane = new JTextPane();


    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        frame.setPreferredSize(new Dimension(1000, 800));
        topPanel.add(tPane);
        tPane.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(tPane), "Center");
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    //Prompt for and return the address of the server.
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "CPE 490 IRC Project",
            JOptionPane.QUESTION_MESSAGE);
    }


    //Prompt for and return the desired screen name.
    private String getName() {
        name = JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
        return name;
    }

    //Connects to the server then enters the processing loop.
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } 
            else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } 
            else if (line.startsWith("MESSAGE")) {
            	tPane.setEditable(true);
            	appendToPane(tPane, line.substring(8) + "\n", Color.BLACK);
            	tPane.setEditable(false);
            }
            else if (line.startsWith("SERVER")) {
            	tPane.setEditable(true);
            	appendToPane(tPane, line.substring(8) + "\n", Color.BLUE);
            	tPane.setEditable(false);
            }
            else if (line.startsWith("COMMAND /whisper")) {
            	tPane.setEditable(true);
            	int endOfString = line.lastIndexOf("from");
            	if(line.substring(endOfString+5).startsWith(name)) {
	            	String lineMsg = line.substring(0, endOfString);
            		appendToPane(tPane, "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "] you whisper: " + lineMsg.substring(18+name.length()) + "\n", Color.RED);
            	}
            	if(line.startsWith("COMMAND /whisper " + name)) {
	            	String lineMsg = line.substring(0, endOfString);
	            	String sender = line.substring(endOfString);
	            	appendToPane(tPane, "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "] " + sender.substring(5) + " whispers to you: " + lineMsg.substring(18+name.length()) + "\n", Color.RED);
            	}
            	tPane.setEditable(false);
            }
            else if (line.startsWith("COMMAND /poke " + name)) {
            	tPane.setEditable(true);
                int endOfString = line.lastIndexOf("from");
                String lineMsg = line.substring(0, endOfString);
                String sender = line.substring(endOfString);
                appendToPane(tPane, "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "] " + sender.substring(5) + " poked you." + "\n", Color.RED);
                tPane.setEditable(false);
            }
            else if (line.startsWith("COMMAND /help from " + name)) { //lists commands
            	tPane.setEditable(true);
                appendToPane(tPane, "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "]" + " ---HELP MENU---"  + "\n", Color.BLUE);
                appendToPane(tPane, "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "]" + " Type /whisper [user] [message] to send a user a message."  + "\n", Color.BLUE);
                appendToPane(tPane, "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "]" + " Type /poke [user] poke a user."  + "\n", Color.BLUE);
                tPane.setEditable(false);
            }
            else if (line.startsWith("COMMAND /users from " + name)) {
            	tPane.setEditable(true);
            	appendToPane(tPane, line, Color.BLUE);
            	tPane.setEditable(false);
            }
            else if (line.startsWith("COMMAND /disconnect")) {
            	tPane.setEditable(true);
            	int endOfString = line.lastIndexOf("from");
            	appendToPane(tPane, "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "] " + line.substring(endOfString+5) + " has disconnected" + "\n", Color.BLUE);
            	if (line.startsWith("COMMAND /disconnect from " + name)) {
                	socket.close();
                }
            	tPane.setEditable(false);
            }
            else {
            	//break;
            }
        }
        //socket.close();
    }
    private void appendToPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_LEFT);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
    }

    //Runs the client as an application with a closeable frame.
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
