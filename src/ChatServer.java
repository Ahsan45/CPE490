import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.time.LocalTime;

public class ChatServer {

    private static final int PORT = 9001;

    private static HashSet<String> names = new HashSet<String>();

    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    
     public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            System.out.println(name + " has joined. Type /help for help.");
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);
                for (PrintWriter writer : writers) {
                    writer.println("SERVER " + "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "]" + " " + name + " has joined. Type /help for help.");
                }

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    if(input.length() != 0) {

	                    if (input.charAt(0) == '/') {
	                    	if (input.startsWith("/users")) {
		                    	for (PrintWriter writer :writers) {
		                    		writer.println("COMMAND /users from " + name + "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "]" + " " + names);
		                    	}
	                    	}
	                    	else {
		                    	for (PrintWriter writer : writers) {
		                            writer.println("COMMAND " + input + " from " + name);
		                        }		                    	
	                    	}
	                    	System.out.println("COMMAND " + name + " " + input);
	                    }
	                    else {
	                    	for (PrintWriter writer : writers) {
	                            writer.println("MESSAGE " + "[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "]" + " " + name + ": " + input);
	                        }
	                        System.out.println("[" + LocalTime.of(LocalTime.now().getHour(), LocalTime.now().getMinute()) + "]" + " " + name + ": " + input);
	                    }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } 
            
            finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                	System.out.println(name + " has left");
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } 
                catch (IOException e) {
                }
            }
        }
    }
}
