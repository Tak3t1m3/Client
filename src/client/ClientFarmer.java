package client;

import com.jfoenix.controls.JFXToggleButton;
import controllers.ControllerFarmer;
import crops.Crop;
import helpers.TREC;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import users.Farmer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static client.Client.primaryStage;

public class ClientFarmer  {




    Thread t;
    ObjectOutputStream meOut;
    ObjectInputStream in;
    File file;
    PopOver pp;
    Client cli;
    Farmer user = new Farmer();

    ControllerFarmer cf;

    TextField name,weight,cost,quantity;
    JFXToggleButton available;
    ImageView imageView;

    TextField editWeight = new TextField();
    TextField editCost = new TextField();
    TextField editQuantity = new TextField();
    TextField editAvailable = new TextField();



    private boolean chatBoxBool = true;


    public ClientFarmer(Client client) {
        cli = client;

    }


    private void initiateChat() {


    }


    public void shift() {


        if (!cf.farmBool)
            return;

        try {
            cf.pName.setText("Name : " + user.getFullName());
            cf.pAlias.setText("Alias : " + user.getAlias());
            cf.pAddress.setText("Address : " + user.getAddress());
            cf.pEmail.setText("Email : " + user.getEmail());
            double num = user.getBalance();
            //todo format num into dollars
            cf.pBalance.setText("$"+num+"");
            cli.os.writeObject("getImage");

            cli.os.writeObject(user);


            try {
                File file = (File) cli.is.readObject();
                if(file!=null)
                cf.pImage.setImage(new Image(file.toURI().toString()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            cf.farmBool = false;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void loadFarm(AnchorPane p) {
        try {

            cf.welcome.setOpacity(0.0);
            cf.welcome.setDisable(true);
            cf.homeFarm.setOpacity(0.0);
            cf.cropFarm.setOpacity(0.0);
            cf.historyFarm.setOpacity(0.0);
            cf.chatFarm.setOpacity(0.0);

            cf.homeFarm.setDisable(true);
            cf.cropFarm.setDisable(true);
            cf.historyFarm.setDisable(true);
            cf.chatFarm.setDisable(true);

            p.setDisable(false);
            p.toFront();

            KeyValue keyValue = new KeyValue(p.opacityProperty(), 1);
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(350), keyValue));
            timeline.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setupChatBox() {
        if (!chatBoxBool)
            return;
        cli.logger.info("setting up chatbox");
        t = new Thread(new StartServer());
        t.start();
        cf.gridChat.setVgap(1);

        chatBoxBool = false;
    }


    public void sendMessage() {
        String msg = cf.chatSendBoxFarmer.getText();
        if(msg.compareTo("")==0)
            return;
        Label lab = new Label(msg);

        cf.chatSendBoxFarmer.clear();
        lab.getStylesheets().add(getClass().getResource("/views/msg sheets.css").toExternalForm());
        lab.getStyleClass().add("out");

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                cf.gridChat.addRow(cf.gridChat.getChildren().size()+1,lab);
                Animation animation = new Timeline(
                        new KeyFrame(Duration.seconds(.3),
                                new KeyValue(cf.scrollPane.vvalueProperty(), 1)));
                animation.play();
            }
        });

        try {
            meOut.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void setFarmer() {
        if (!cf.firstRun)
            return;


        shift();
        setTable();
        setupChatBox();

      cf.firstRun = false;
    }

    public void loadWhoever(Event e) {
        user = user;
        setFarmer();
        if (e.getSource() == cf.homeBtn)
            loadFarm(cf.homeFarm);
        if (e.getSource() == cf.cropBtn)
            loadFarm(cf.cropFarm);
        if (e.getSource() == cf.historyBtn)
            loadFarm(cf.historyFarm);
        if (e.getSource() == cf.chatBtn)
            loadFarm(cf.chatFarm);
    }

    public void addCrop() {


        if (checkCropValues()) {
            Crop c = new Crop(cf.cName.getText(), Double.valueOf(cf.cWeight.getText()), Double.valueOf(cf.cCost.getText()), Double.valueOf(cf.cQuantity.getText()), cf.cropAvailable.isSelected(), file);
            c.setOwner(user.getEmail());
            try {
                cli.os.writeObject("addCrop");
                cli.os.writeObject(c);
            } catch (IOException e) {
                cli.logger.error(e.getMessage());
                e.printStackTrace();
            }



        }


    }

    public void displayAddCrop() {

        addCrop();
        updateTable();
        clearCropBar();


    }

    private void clearCropBar() {
        cf.cName.clear();
        cf.cWeight.clear();
        cf.cCost.clear();
        cf.cQuantity.clear();
        cf.cropAvailable.setSelected(false);
        cf.cImage.setImage(null);

    }


    public void cropImage() throws IOException {

        FileChooser fc = new FileChooser();
        File fil = fc.showOpenDialog(primaryStage);
        if(fil==null)
            return;
        file = fil;
        Image ill = new Image(fil.toURI().toString());
        cf.cImage.setImage(ill);

    }

    private boolean checkCropValues() {

        if (cf.cName.getText().compareTo("") == 0)
            return false;
        if (cf.cWeight.getText().compareTo("") == 0)
            return false;
        if (cf.cCost.getText().compareTo("") == 0)
            return false;
        if (cf.cQuantity.getText().compareTo("") == 0)
            return false;

        return true;

    }

    private void updateTable() {
        ArrayList list = getArrayList();
        ObservableList<Object> cropList = FXCollections.observableArrayList(list);

        cf.cropTable.setItems(null);
        cf.cropTable.getColumns().removeAll();
        cf.cropTable.setItems(cropList);


    }

    private ArrayList getArrayList() {
        try {
            cli.os.writeObject("getObservableList");
            cli.os.writeObject(user);


            return (ArrayList) cli.is.readObject();
        } catch (IOException e) {
            cli.logger.error(e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            cli.logger.error(e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList();
    }

    public void upload() throws IOException {
        FileChooser fc = new FileChooser();
        File fil = fc.showOpenDialog(primaryStage);
        if (fil == null)
            return;

        Image ill = new Image(fil.toURI().toString());
        //  Image ill = new Image("http://2.bp.blogspot.com/-Ol8pLJcc9oo/TnZY6R8YJ5I/AAAAAAAACSI/YDxcIHCZhy4/s150/duke_44x80.png");
        cf.pImage.setImage(ill);
        user.setImage(ill.toString().getBytes());

        cli.os.writeObject("uploadUserImage");
        cli.os.writeObject(user);
        cli.os.writeObject(fil.getAbsoluteFile());

    }

    public void setTable() {

        if (!cf.cropBool)
            return;

        ArrayList list = getArrayList();
        ObservableList<Object> cropList = FXCollections.observableArrayList(list);
        cf.ctName.setCellValueFactory(new PropertyValueFactory("Name"));
        cf.ctWeight.setCellValueFactory(new PropertyValueFactory("Weight"));
        cf.ctCost.setCellValueFactory(new PropertyValueFactory("Cost"));
        cf.ctQuantity.setCellValueFactory(new PropertyValueFactory("Quantity"));
        cf.ctAvailable.setCellValueFactory(new PropertyValueFactory("Available"));


        TREC expans = getExpanse();

        cf.cropTable.setItems(null);
        cf.cropTable.getColumns().removeAll();
        cf.cropTable.getColumns().add(0, expans);
        cf.cropTable.setItems(cropList);



        cf.cropBool = false;
    }

    private TREC getExpanse() {
        TREC<Object> expans = new TREC<> (param -> {

            //   Crop c = (Crop) param.getTableRow().getItem();
            HBox hbox = new HBox();
            HBox hboxbtn = new HBox();
            GridPane editor = new GridPane();
            editor.setBackground(new Background(new BackgroundFill(Color.gray(0.95), CornerRadii.EMPTY, Insets.EMPTY)));


            Crop c = (Crop) param.getValue();


            editWeight.setPromptText("edit weight");
            editCost.setPromptText("edit cost");
            editQuantity.setPromptText("edit quatity");
            editAvailable.setPromptText("edit availablity");


            editWeight.setPrefSize(100, 25);
            editCost.setPrefSize(100, 25);
            editQuantity.setPrefSize(100, 25);
            editAvailable.setPrefSize(100, 25);

//
//
            hbox.getChildren().addAll(editWeight, editCost, editQuantity, editAvailable);
//
            editor.setVgap(17);
//
            Button save = new Button("Save");
            save.setOnAction(event -> {
                // save();

                if(editCost.getText().compareToIgnoreCase("")!=0){
                    c.setCost(Double.parseDouble(editCost.getText()));
                }
                if(editQuantity.getText().compareToIgnoreCase("")!=0){
                    c.setQuantity(Double.parseDouble(editQuantity.getText()));
                }
                if(editWeight.getText().compareToIgnoreCase("")!=0){
                    c.setWeight(Double.parseDouble(editWeight.getText()));
                }


                 //  Crop c = (Crop) param.getTableRow().getItem();
                try {
                    cli.os.writeObject("updateCrop");
                    cli.os.writeObject(c);
                    cli.os.writeObject(user);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //todo save something
                param.toggleExpanded();
            });
            Button cancel = new Button("Cancel");
            cancel.setOnAction(event -> {
                // save();
                param.toggleExpanded();
            });

            hboxbtn.getChildren().addAll(save, cancel);
            save.getStyleClass().add("saveAndCancel");
            cancel.getStyleClass().add("saveAndCancel");

            hbox.setSpacing(5);
            hboxbtn.setSpacing(15);
            hbox.setPrefHeight(45);
            hboxbtn.setPrefHeight(45);

            editor.addRow(1, hbox);
            editor.addRow(2, hboxbtn);

            editWeight.setPadding(new Insets(5));
            editCost.setPadding(new Insets(5));
            editQuantity.setPadding(new Insets(5));
            editAvailable.setPadding(new Insets(5));
            editor.setPadding(new Insets(5, 5, 5, 5));

            return editor;
        });

        return expans;
    }


    public void getHistory() {

        try {
            cli.os.writeObject("getHistory");
            ResultSet rs = (ResultSet) cli.is.readObject();

            while (rs.next()) {
                GridPane gp = new GridPane();
                Label cropName, customerName, quantity, amount;
                cropName = new Label(rs.getString(1));
                customerName = new Label(rs.getString(2));
                quantity = new Label(String.valueOf(rs.getDouble(3)));
                amount = new Label(String.valueOf(rs.getDouble(4)));
                gp.setMinWidth(300);
                gp.add(cropName, 1, 1);
                gp.add(customerName, 1, 2);
                gp.add(quantity, 1, 3);
                gp.add(amount, 1, 4);

            }


        } catch (IOException e) {
            cli.logger.error(e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            cli.logger.error(e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            cli.logger.error(e.getMessage());
            e.printStackTrace();
        }


    }

    public void addController(ControllerFarmer controllerFarmer) {
        cf = controllerFarmer;
    }

    public void setUser(Farmer f) {
        user = f;
    }

    public void updateChat(boolean selected) {
        try {
            cli.os.writeObject("update selected");
            cli.os.writeObject(user.getEmail());
            cli.os.writeObject(selected);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class Server implements Runnable {

        Socket socket;
        String msg = "";
        TextArea t;


        public Server(Socket socket) {
            this.socket = cli.sock;
        }

        @Override
        public void run() {


            while (msg.compareTo("0000") != 0) {

                try {
                    msg = (String) in.readObject();
                    Label lab = new Label(msg);

                    lab.getStylesheets().add(getClass().getResource("/views/msg sheets.css").toExternalForm());
                    lab.getStyleClass().add("in");
                    Pane huh = new Pane();
                    huh.setMinSize(410,19);
                    huh.setPrefSize(410,19);
                    huh.getStylesheets().add(getClass().getResource("/views/msg sheets.css").toExternalForm());
                    huh.getStyleClass().add("paneHolder");

                    huh.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                    huh.getChildren().add(lab);
                    lab.getStylesheets().add(getClass().getResource("/views/msg sheets.css").toExternalForm());
                    lab.getStyleClass().add("in");

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            cf.gridChat.addRow(cf.gridChat.getChildren().size()+1,huh);
                            Animation animation = new Timeline(
                                    new KeyFrame(Duration.seconds(.3),
                                            new KeyValue(cf.scrollPane.vvalueProperty(), 1)));
                            animation.play();
                        }
                    });


                    //cf.chatfarmer.appendText("n"+msg);


                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }


        }
    }

    private class StartServer implements Runnable {

        TextArea ta = new TextArea();

        public StartServer() {

        }

        @Override
        public void run() {


            try {
                ta = cf.chatfarmer;


                ServerSocket serverSocket = new ServerSocket(4000);
                Socket socket = serverSocket.accept();
                cli.logger.info("client server accepted");
//                chatBoxFarm.appendText("client connected");
              //  cf.chatfarmer.appendText("client connected \n\n");
                Label lab = new Label("Customer Connected");
                lab.getStylesheets().add(getClass().getResource("/views/msg sheets.css").toExternalForm());
                lab.getStyleClass().add("mid");
                lab.setTranslateX((433/2.75));
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        cf.gridChat.addRow(1,lab);
                        cf.gridChat.setVgap(2);
                        cf.scrollPane.setFitToHeight(true);
//                        cf.gridChat.add



                    }
                });
                meOut = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                Thread t = new Thread(new Server(socket));
                t.start();

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}




