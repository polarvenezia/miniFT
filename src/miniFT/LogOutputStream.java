package miniFT;

import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by polarvenezia on 17/4/17.
 */
public class LogOutputStream extends OutputStream{
    private TextArea textArea;
    LogOutputStream(TextArea textArea){
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException {
        textArea.appendText(String.valueOf((char) b));
    }

    public void log(String msg){
        textArea.appendText(msg);
    }
}
