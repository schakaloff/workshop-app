package main;
import com.jfoenix.controls.JFXDrawer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;




public class ActualWorkshopController{
    @FXML
    private JFXDrawer drawer;
    public static void main(String[] args){

    }
    public void closeMenu(ActionEvent e){
        drawer.close();
    }
}