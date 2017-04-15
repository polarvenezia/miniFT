package sample;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Created by ntjin on 4/9/2017.
 */
public class Client {
    public static boolean CP1 = true;
    private static final int CP1INT = 101;
    private static final int CP2INT = 102;
    private static final int SEND_FILE = 201;
    private static final int SUCCESS = 111;

//    public static void main(String[] args) throws Exception {
//        File file = new File("test.txt");
//        String hostName = "localhost";
//        sendFile(file, hostName);
//    }

    public static void sendFile(File sending_file, String serverAddress) throws Exception{
        // ====================configure server =========================
        int portNumber = 8080;
        Socket echoSocket = new Socket(serverAddress, portNumber);
        // get output from server
        BufferedReader input_message = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        InputStream serverInput = echoSocket.getInputStream();
        // pass the result of user to server
        OutputStream output_message = echoSocket.getOutputStream();
        // get return result from server
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("---------------------client figured successfully");

        // ====================start to check authentication ====================
        boolean trust_server = false;
        // TODO: store the received certificate somewhere by fis, update in line 42
        InputStream fis;
        // create certificate object
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate CAcert;


        final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int len = 20;
        StringBuilder sb = new StringBuilder( len );
        SecureRandom rnd = new SecureRandom();
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );

        output_message.write(sb.toString().getBytes());

        // 1. encrypted message, TODO: store somewhere as byte[]. Check
        byte[] buffer = new byte[1024];
        String greeting;
//        while (!(greeting = input_message.readLine()).equals("done")) {
//            System.out.println(greeting);
//            finalGreeting += greeting;
//        }
        int length = serverInput.read(buffer);
        greeting = new String(buffer, 0, length);
        System.out.println("Server: " + greeting);
        System.out.println("finish greeting");
        byte[] greeing_in_byte = new byte[length];
        System.arraycopy(buffer, 0, greeing_in_byte, 0, length);

        output_message.write("Give me your certificate signed by CA".getBytes());

        System.out.println("waiting for server public key");
        //  TODO: get public key via chat
        // Save input to crt first.
        String file_name = "Server.crt";
        try{
            FileWriter fw = new FileWriter(file_name);
            BufferedWriter bw = new BufferedWriter(fw);
            length = serverInput.read(buffer);
            long size = bytesToLong(buffer, length);
            long hasRead = 0;
            while (hasRead < size && (length = serverInput.read(buffer)) > 0){
                hasRead += length;
                bw.write(new String(buffer, 0, length));
            }
            bw.close();
            fw.close();
        }catch (Exception e){
            System.out.println("catch exception when save cert to file");
            e.printStackTrace();
        }
        fis = new FileInputStream(file_name);

        InputStream caFis = new FileInputStream("CA.crt");
        CAcert = (X509Certificate) cf.generateCertificate(caFis);
        PublicKey publicKey = CAcert.getPublicKey();

        X509Certificate serverCert =(X509Certificate)cf.generateCertificate(fis);
        // ----------------------- check validity and verify signed certificate
        try{

            serverCert.checkValidity();
            serverCert.verify(publicKey);

            System.out.println("---------------server is verified! Decrypt the message received just now");

            // Create RSA("RSA/ECB/PKCS1Padding") cipher object and initialize is as encrypt mode,
            // TODO: do we use ECB/PLCS1?
            PublicKey serverPublicKey = serverCert.getPublicKey();
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, serverPublicKey);
            System.out.println(greeing_in_byte.length);
            byte[] decrypted_greeing= rsaCipher.doFinal(greeing_in_byte);
            System.out.println("------------Encrypted result of greeting: "+ new String(decrypted_greeing));

            // =======================start messaging ===================
            // =================read file to transfer ===================
            // String server_intput = input_message.readLine();
            String sending_file_name = sending_file.getName();
            System.out.println("------------sent file name: " + sending_file_name);
//            String data = "";
//            String line;
//            BufferedReader bufferedReader = new BufferedReader( new FileReader(sending_file_name));
//            while((line= bufferedReader.readLine())!=null){data = data +"\n" + line;}

            byte[] fileData = new byte[(int) sending_file.length()];
            FileInputStream fileStream = new FileInputStream(sending_file);
            while ((length = fileStream.read(fileData)) > -1){
                System.out.println(length);
            }
            // =======================encrypte file message ===================
            // TODO: encrypted your upload with CP

            if (CP1){
                System.out.println("CP1");
                output_message.write(intToBytes(CP1INT));

                output_message.write(sending_file_name.getBytes()); // send file name

                byte[] sendFileBuffer = new byte[4];
                serverInput.read(sendFileBuffer);
                int sendFileAction = bytesToInt(sendFileBuffer);
                if (sendFileAction == SEND_FILE) System.out.println("Server received file name successfully");

                // TODO: encrypted messagge with CP private key
                rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
                int start_pos = 0;

                // TODO: send encrypted message length
                System.out.println("line 128: origin file length "+fileData.length);
                long encryptedLength = fileData.length;
                output_message.write(longToBytes(encryptedLength));

                byte[] encrypted_message;
                while (start_pos < fileData.length){

                    rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
                    if (fileData.length - start_pos < 116){
                        encrypted_message = rsaCipher.doFinal(fileData,start_pos,fileData.length - start_pos);
                    } else {
                        encrypted_message = rsaCipher.doFinal(fileData,start_pos,116);
                    }
                    start_pos += 116;
                    output_message.write(encrypted_message);
                    System.out.println("------------sent encrypted message: " + encrypted_message);
                }




            }else {// CP2
                System.out.println("CP2");
                // TODO: create message digest and symmetric key, and let server know
                output_message.write(intToBytes(CP2INT));
                output_message.write(sending_file_name.getBytes()); // send file name

                byte[] sendFileBuffer = new byte[4];
                serverInput.read(sendFileBuffer);
                int sendFileAction = bytesToInt(sendFileBuffer);
                if (sendFileAction == SEND_FILE) System.out.println("Server received file name successfully");
                // encrypt message

                try { // how AES work
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    SecretKey key = keyGen.generateKey();
                    rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
                    byte[] encrypted_key = rsaCipher.doFinal(key.getEncoded());

                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, key);

                    // TODO: encrypt message with symmetric key
                    byte[] encrypted_message_byte = cipher.doFinal(fileData);
                    String encrypted_message =  Base64.getEncoder().encodeToString(encrypted_message_byte);

                    // TODO: send secret message and encrypted_message in byte array
                    output_message.write(encrypted_key);
                    int keyStatus = serverInput.read();
                    output_message.write(longToBytes(encrypted_message_byte.length));

                    output_message.write(encrypted_message_byte);
                    System.out.println("------------sent encrypted message: " + encrypted_message);

                }catch (Exception e) {
                    System.out.println("Error while encrypting: " + e.toString());
                }
            }
            int finalMsg = serverInput.read();
            if (finalMsg == SUCCESS) System.out.println("File transferred successfully");
            System.out.println("------------session end");
        }catch (Exception e){
            e.printStackTrace();
            output_message.write("Bye".getBytes());
            output_message.flush();
        }
        serverInput.close();
        echoSocket.close();
        input_message.close();
        stdIn.close();
    }

    private static long bytesToLong(byte[] bytes, int size){
        return ByteBuffer.wrap(bytes, 0, size).getLong();
    }
    private static int bytesToInt(byte[] bytes){return ByteBuffer.wrap(bytes).getInt();}
    private static byte[] intToBytes(int integer){ return ByteBuffer.allocate(Integer.BYTES).putInt(integer).array();}
    private static byte[] longToBytes(long length){
        return ByteBuffer.allocate(Long.BYTES).putLong(length).array();
    }
}
