package cz.zcu.fav.cryptedchat.client;

import cz.zcu.fav.cryptedchat.crypto.Cypher;
import cz.zcu.fav.cryptedchat.crypto.RSA;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public class Controller implements Initializable, App.OnCloseListener {

    @FXML private Button btnDisconnect;
    @FXML private ListView listUsers;
    @FXML private TabPane tabMessages;
    @FXML private TextArea textMessage;
    @FXML private TextField txtIp;
    @FXML private TextField txtPort;
    @FXML private Button btnConnect;

    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private Cypher cypher = new RSA(1024);

    private Communicator communicator;
    private static final UnaryOperator<TextFormatter.Change> filter = change -> {
        String text = change.getText();
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return null;
            }
        }
        return change;
    };
    private final Communicator.OnDisconnectListener disconnectListener = () -> {
        running.set(false);
    };

    private void disconnect() {
        communicator.disconnect();
        running.set(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtPort.setTextFormatter(new TextFormatter<>(filter));

        btnConnect.disableProperty().bind(running);
        btnDisconnect.disableProperty().bind(running.not());
    }

    public void handleConnect(ActionEvent actionEvent) {
        String ip = txtIp.getText();
        int port = Integer.parseInt(txtPort.getText());
        running.set(true);
        communicator = new Communicator(ip, port, cypher);
        communicator.setDisconnectListener(disconnectListener);
        communicator.connect();
    }

    public void handleSendMessage(ActionEvent actionEvent) {
        String message = textMessage.getText();
        communicator.sendMessage(message.getBytes());
    }

    @Override
    public void onClose() {
        disconnect();
    }

    public void handleDisconnect(ActionEvent actionEvent) {
        disconnect();
    }
}
