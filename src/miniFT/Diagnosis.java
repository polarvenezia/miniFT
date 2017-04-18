package miniFT;

import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by polarvenezia on 12/4/17.
 */
public class Diagnosis extends Thread{
    private final String os = System.getProperty("os.name").toLowerCase();
    String address;
    TextArea textArea;
    TextArea traceRouteLog;
    Diagnosis(String address, TextArea textArea){
        this.address = address;
        this.textArea = textArea;
    }

    Diagnosis(String address, TextArea textArea, TextArea traceRouteLog){
        this.address = address;
        this.textArea = textArea;
        this.traceRouteLog = traceRouteLog;
    }

    @Override
    public void run() {
        String result = traceRoute(address);
        textArea.appendText(result);
        traceRouteLog.appendText(result);
    }

    public String traceRoute(String addressString) {
        String route = "Trace route diagnosis: \n";
        try {
            InetAddress address = InetAddress.getByName(addressString);
            Process traceRt;
            if (os.contains("win")) traceRt = Runtime.getRuntime().exec("tracert " + address.getHostAddress());
            else traceRt = Runtime.getRuntime().exec("traceroute " + address.getHostAddress());

            // read the output from the command
            route = readFromStream(traceRt.getInputStream());

            // read any errors from the attempted command
            String errors = readFromStream(traceRt.getErrorStream());
            if (!errors.equals("")) System.out.println(errors);
        }catch (UnknownHostException e){
            route += "\nInvalid IP address entered";
        }
        catch (IOException e) {
            String errmsg = "error while performing trace route command";
            System.out.println(errmsg);
            route += "\n"+errmsg;
            e.printStackTrace();
        }

        return route;
    }

    private String readFromStream(InputStream inputStream) throws IOException{
        String result = "";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine())!=null){
            result += line + "\n";
        }
        return result;
    }
}
