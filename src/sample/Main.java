package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("USB");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
        USB.ConnectWithDevice();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                USB.ExitUsb();
                System.out.println("Stage is closing");
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
