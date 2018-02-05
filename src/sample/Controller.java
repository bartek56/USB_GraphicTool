package sample;

import javafx.fxml.FXML;

import java.nio.charset.StandardCharsets;

public class Controller {

    @FXML
    public javafx.scene.control.Button bSend;

    @FXML
    public javafx.scene.control.Label lInfo;

    @FXML
    public javafx.scene.control.Button bConnect;

    @FXML
    public javafx.scene.control.Button bReadData;

    @FXML
    public javafx.scene.control.TextField tTextToSend;


    public void bSend_Click()
    {
        String text = tTextToSend.getText();

        byte bReq=0;
        byte[] dataToSend = text.getBytes();

        short lengthText = (short)text.length();

        System.out.println("text: "+lengthText);

        USB.SendData((byte)1,lengthText,dataToSend);
    }

    public void bReadData_Click()
    {
        String str = new String((USB.ReadData((byte)1,(short)3,8)), StandardCharsets.UTF_8);

        //byte[] data = USB.ReadData((byte)1,(short)3,5);
        System.out.println(str.toString());
        lInfo.setText(str);


    }




}
