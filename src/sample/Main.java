package sample;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main extends Application {
    private static final String SEND_FILE = "Send File";
    private static final String TRACE_ROUTE = "Trace Route";
    private static final String DIAGNOSIS = "Diagnosis";
    private static final String SETTINGS = "Settings";
    private static final String HELP = "Help";
    File toUpload;
    BorderPane border;
    String centerPane = "";
    static TextArea logstatus = new TextArea("Status: \nWelcome to miniFTP client!\n");
    TextArea traceRouteLog = new TextArea();

    @Override
    public void start(Stage primaryStage) throws Exception{
        border = new BorderPane();
        border.setTop(addHBox());
        border.setLeft(addVBox());
        border.setCenter(addFileDrop());
        border.setBottom(addStatusRegion());
        traceRouteLog.setWrapText(true);
        traceRouteLog.setEditable(false);
        traceRouteLog.appendText("Diagnosis log: \n");

        System.setOut(new PrintStream(new LogOutputStream(logstatus)));
        System.setErr(new PrintStream(new LogOutputStream(logstatus)));

        Scene scene = new Scene(border, 550, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public StackPane addTraceRouteLog(){
        StackPane base = new StackPane();
        base.setAlignment(Pos.TOP_LEFT);
        base.setPadding(new Insets(10, 20, 10, 30));

        base.getChildren().add(traceRouteLog);
        return base;
    }

    public VBox addVBox(){
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Hyperlink options[] = new Hyperlink[] {
                new Hyperlink(SEND_FILE),
                new Hyperlink(DIAGNOSIS),
                new Hyperlink(SETTINGS),
                new Hyperlink(HELP)};

//        Button options[] = new Button[] {
//                new Button(SEND_FILE),
//                new Button(TRACE_ROUTE),
//                new Button(DIAGNOSIS),
//                new Button(SETTINGS),
//                new Button(HELP)};
        options[0].setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!centerPane.equals(SEND_FILE)) {
                    border.setCenter(addFileDrop());
                    centerPane = SEND_FILE;
                }
                options[0].setVisited(false);
            }
        });
        options[1].setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!centerPane.equals(DIAGNOSIS)) {
                    border.setCenter(addTraceRouteLog());
                    centerPane = DIAGNOSIS;
                }
                options[1].setVisited(false);
            }
        });
        options[2].setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!centerPane.equals(SETTINGS)) {
                    border.setCenter(addRadioPane());
                    centerPane = SETTINGS;
                }
                options[2].setVisited(false);
            }
        });
        options[3].setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!centerPane.equals(HELP)) {
                    border.setCenter(addHelpPane());
                    centerPane = HELP;
                }
                options[3].setVisited(false);
            }
        });
        for (int i=0; i<4; i++) {
            VBox.setMargin(options[i], new Insets(0, 0, 0, 8));
            vbox.getChildren().add(options[i]);
        }

        return vbox;
    }

    public HBox addHBox(){
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.setAlignment(Pos.CENTER);
        Text title = new Text("Server IP: ");
        title.setTextAlignment(TextAlignment.CENTER);
        TextField input = new TextField();
        Button btn = new Button("Send");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (toUpload != null){
                    String serverAddress = input.getText();
                    if (serverAddress == null || serverAddress.equals("")){
                        serverAddress = "localhost";
                    }
                    logstatus.appendText("Sending file to "+serverAddress+" ... \n");
                    try {
                        Client.sendFile(toUpload, serverAddress);
                    }catch (Exception e){
                        logstatus.appendText(e.getMessage());
                    }
                }else {
                    logstatus.appendText("Please drag the file in\n");
                }
            }
        });
        Button traceRoute = new Button("trace route");
        traceRoute.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String ipAddress = input.getText();
                Thread traceThread = new Diagnosis(ipAddress, logstatus, traceRouteLog);
                logstatus.appendText("Diagnosing...\n");
                traceThread.start();
            }
        });
        hbox.getChildren().addAll(title, input, btn, traceRoute);
        return hbox;
    }

    public StackPane addHelpPane(){
        StackPane base = new StackPane();
        base.setAlignment(Pos.TOP_LEFT);
        base.setPadding(new Insets(10, 20, 10, 20));

        TextArea helpText = new TextArea();
        helpText.setText("Welcome to use mini file transfer client! \n\n" +
                "To send file: \n" +
                "   1. Type the server ip address on top\n" +
                "   2. Drag in the file\n" +
                "   3. Press \"send\"!\n\n" +
                "To diagnose network issue: \n" +
                "   1. Type in the server ip address\n" +
                "   2. Press \"trace route\"!\n" +
                "   3. View the diagnosis log in info terminal or diagnosis section\n\n" +
                "To change encryption policy:\n" +
                "   1. Go to settings\n" +
                "   2.Select the policy\n\n");
        helpText.setWrapText(true);
        helpText.setEditable(false);
        base.getChildren().add(helpText);
        return base;
    }

    public StackPane addRadioPane(){
        StackPane base = new StackPane();
        base.setAlignment(Pos.CENTER);
        base.setPadding(new Insets(20, 20, 10, 20));

        VBox vBox = new VBox();
        vBox.setSpacing(10);

        Text text = new Text("Please select encryption policy");
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton cp1 = new RadioButton("CP1");
        RadioButton cp2 = new RadioButton("CP2");
        cp1.setToggleGroup(toggleGroup);
        cp1.setSelected(Client.CP1);
        cp2.setToggleGroup(toggleGroup);
        cp2.setSelected(!Client.CP1);

        cp1.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
                if (isNowSelected) {
                    Client.CP1 = true;
                }
            }
        });

        cp2.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
                if (isNowSelected) {
                    Client.CP1 = false;
                }
            }
        });

        vBox.getChildren().addAll(text, cp1, cp2);
        base.getChildren().add(vBox);
        return base;
    }

    public StackPane addFileDrop(){
        StackPane base = new StackPane();
        base.setAlignment(Pos.CENTER);
        base.setPadding(new Insets(10, 20, 10, 10));

        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.DASHED, new CornerRadii(0.5), BorderWidths.DEFAULT)));
        pane.setPrefSize(50, 50);

        Text target = new Text("Drop your file here");
        if (toUpload != null) target.setText(toUpload.getName());
        pane.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getGestureSource() != target &&
                        event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });
        pane.setOnDragEntered(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getGestureSource() != target &&
                        event.getDragboard().hasFiles()) {
                    pane.setBackground(getBackgroundDragged());
                }
                event.consume();
            }
        });
        pane.setOnDragExited(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                pane.setBackground(restoreBackground());
                event.consume();
            }
        });
        pane.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    for (File file: db.getFiles()){
                        toUpload = file;
                        String name = file.getName();
                        target.setText(name);
                    }
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
        pane.getChildren().add(target);
        base.getChildren().add(pane);
        return base;
    }

    private Background getBackgroundDragged(){
        return new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY));
    }

    private Background restoreBackground(){
        return new Background(new BackgroundFill(Color.WHITESMOKE, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public StackPane addStatusRegion(){
        StackPane stackPane = new StackPane();
        stackPane.setPadding(new Insets(5, 10, 2, 10));
//        logstatus.setPrefHeight(100);
        logstatus.minHeightProperty().bind(border.heightProperty().divide(4));
        logstatus.setEditable(false);
        stackPane.setPadding(new Insets(0, 5, 5, 5));
        stackPane.getChildren().add(logstatus);
        stackPane.setAlignment(Pos.CENTER);
        return stackPane;
    }
}
