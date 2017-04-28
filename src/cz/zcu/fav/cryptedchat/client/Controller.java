package cz.zcu.fav.cryptedchat.client;

import cz.zcu.fav.cryptedchat.client.Communicator.OnClientsChangeListener;
import cz.zcu.fav.cryptedchat.client.widget.ClientListCell;
import cz.zcu.fav.cryptedchat.crypto.Cypher;
import cz.zcu.fav.cryptedchat.crypto.RSA;
import cz.zcu.fav.cryptedchat.shared.ClientState;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    @FXML
    private Button btnDisconnect;
    @FXML
    private ListView<Client> listUsers;
    @FXML
    private TabPane tabMessages;
    @FXML
    private TextArea textMessage;
    @FXML
    private TextField txtIp;
    @FXML
    private TextField txtPort;
    @FXML
    private Button btnConnect;

    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final ObservableList<Client> clients = FXCollections.observableArrayList();
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
    private final Communicator.OnDisconnectListener disconnectListener = () -> running.set(false);

    private final Communicator.OnClientsChangeListener clientsChangeListener = new OnClientsChangeListener() {
        @Override
        public void onListRequest(final List<Long> users) {
            Platform.runLater(() -> clients.setAll(
                users.stream().map(userId -> new Client(userId)).collect(Collectors.toList())));
        }

        @Override
        public void onClientChangeState(final long clientId, final ClientState clientState) {
            Platform.runLater(() -> {
                final Optional<Client> result = clients.stream()
                    .filter(client -> client.id.get() == clientId)
                    .findFirst();
                final boolean online = clientState == ClientState.ONLINE;
                if (result.isPresent()) {
                    final Client client = result.get();
                    client.online.set(online);
                } else {
                    clients.add(new Client(clientId, online));
                }
            });
        }
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

        listUsers.setItems(clients);
        listUsers.setCellFactory(param -> new ClientListCell());
    }

    public void handleConnect(ActionEvent actionEvent) {
        String ip = txtIp.getText();
        int port = Integer.parseInt(txtPort.getText());
        running.set(true);
        communicator = new Communicator(ip, port, cypher);
        communicator.setDisconnectListener(disconnectListener);
        communicator.setClientsChangeListener(clientsChangeListener);
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

    public class Client {

        final BooleanProperty online = new SimpleBooleanProperty(true);
        final LongProperty id = new SimpleLongProperty();

        public Client(long id) {
            this(id, true);
        }

        public Client(long id, boolean online) {
            this.id.set(id);
            this.online.set(online);
        }

        public boolean isOnline() {
            return online.get();
        }

        public BooleanProperty onlineProperty() {
            return online;
        }

        public long getId() {
            return id.get();
        }

        public LongProperty idProperty() {
            return id;
        }
    }
}
