import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by polarvenezia on 9/4/17.
 */
public class FTServer {
    private static Executor executor = Executors.newScheduledThreadPool(10);
    static File serverCertFile;
    static PrivateKey privateKey;
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);

            // load server certificate
            serverCertFile = new File("1001442.crt");

            // initialize the private key of server
            File privateKeyFile = new File("privateServer.der");
            byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
            KeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory privateKeyFactory = KeyFactory.getInstance("RSA");
            privateKey = privateKeyFactory.generatePrivate(keySpec);

            while (true) {
                // attempt connection with socket
                System.out.println("waiting for socket to connect");
                Socket socket = serverSocket.accept();
                Thread task = new Thread(new FTSocketAccept(socket));
                executor.execute(task);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    static class FTSocketAccept implements Runnable{
        private final String CA_REQUEST = "Give me your certificate signed by CA";
        private final int CP1 = 101;
        private final int CP2 = 102;
        private final int SEND_FILE = 201;
        private final int SUCCESS = 111;
        Socket socket;

        FTSocketAccept(Socket serverSocket){
            this.socket = serverSocket;
        }

        @Override
        public void run() {
            try {
                System.out.println("client connected!");
                InputStream serverc = new FileInputStream(serverCertFile);
                InputStream inputStream = socket.getInputStream();
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                OutputStream outputStream = socket.getOutputStream();
                byte[] buffer = new byte[1024];
                int length;

                // TODO:Send identity message
                System.out.println("waiting for socket identity message");
                length = inputStream.read(buffer);
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.ENCRYPT_MODE, privateKey);
                byte[] encrypted = rsaCipher.doFinal(buffer, 0, length);
                outputStream.write(encrypted);
                System.out.println(new String(encrypted));

                // TODO: Send certificate signed by CA
                System.out.println("waiting for socket request for ca");
                length = inputStream.read(buffer);
                String caRequest = new String(buffer, 0, length);
                if (caRequest.equals(CA_REQUEST)){
                    outputStream.write(longToBytes(serverCertFile.length()));
                    while ((length = serverc.read(buffer)) > -1){
                        outputStream.write(buffer, 0, length);
                        outputStream.flush();
                    }
                }

                // TCP connected successfully
                // TODO: TCP file transfer handling
                System.out.println("waiting for socket encryption policy...");
                byte[] policybuffer = new byte[4];
                inputStream.read(policybuffer);
                int policy = ByteBuffer.wrap(policybuffer).getInt();
                System.out.println("client wants to use policy "+policy);

                System.out.println("waiting for socket filename...");
                length = inputStream.read(buffer);
                String filename = new String(buffer, 0, length);
                System.out.println("filename: "+filename);
                File file = new File("server"+filename);
                OutputStream outputStreamWriter = new FileOutputStream(file);
                outputStream.write(intToBytes(SEND_FILE));

                rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
                Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

                System.out.println("waiting for socket file content...");
                if (policy == CP2){
                    System.out.println("waiting for socket aes key...");
                    length = inputStream.read(buffer);
                    byte[] encryptedKey = new byte[length];
                    System.arraycopy(buffer, 0, encryptedKey, 0, length);
                    outputStream.write(SUCCESS);
                    byte[] decryptedKey = rsaCipher.doFinal(encryptedKey);
                    SecretKeySpec secretKey = new SecretKeySpec(decryptedKey, "AES");
                    aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
                }
                byte[] sizeBuffer = new byte[8];
                length = inputStream.read(sizeBuffer);
                if (length != 8) System.out.println("Wrong size of file");
                long size = bytesToLong(sizeBuffer, 8);
                System.out.println("File size is: " + size);
                long hasRead = 0;
                byte[] receivedFile = new byte[(int) size]; // here has a restriction of file size with maximum 2GB

                System.out.println("reading file into memory...");

                System.out.println("writing file...");
                if (policy == CP1) {
                    int start = 0;
                    byte[] rsaBuffer = new byte[128];
                    while (start < size){
                        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
                        length = inputStream.read(rsaBuffer);
                        System.out.println(length);
                        byte[] decryptedFile = rsaCipher.doFinal(rsaBuffer);
                        System.arraycopy(decryptedFile, 0, receivedFile, start, decryptedFile.length);
                        start += decryptedFile.length;
                    }
                    System.out.println(new String(receivedFile));
                    outputStreamWriter.write(receivedFile);
                }else if (policy == CP2){
                    int pointer = 0;
                    while (hasRead < size && (length = inputStream.read(buffer)) > 0){
                        hasRead += length;
                        System.out.println(length);
                        System.arraycopy(buffer, 0, receivedFile, pointer, length);
                        pointer += length;
                    }
                    byte[] decryptedFile = aesCipher.doFinal(receivedFile);
                    System.out.println(new String(decryptedFile));
                    outputStreamWriter.write(decryptedFile);
                }

                System.out.println("file transfer finished");
                outputStream.write(SUCCESS);
                inputStream.close();
                printWriter.close();
                outputStreamWriter.close();
                socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private byte[] longToBytes(long length){
            return ByteBuffer.allocate(Long.BYTES).putLong(length).array();
        }

        private static int bytesToInt(byte[] bytes){return ByteBuffer.wrap(bytes).getInt();}
        private static byte[] intToBytes(int integer){ return ByteBuffer.allocate(Integer.BYTES).putInt(integer).array();}
        private long bytesToLong(byte[] bytes, int length){
            return ByteBuffer.wrap(bytes, 0, length).getLong();
        }
    }

}
