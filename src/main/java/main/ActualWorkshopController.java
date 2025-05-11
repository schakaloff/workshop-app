package main;


import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXHamburger;
import javafx.fxml.FXML;

import javafx.scene.input.MouseEvent;

public class ActualWorkshopController{
    @FXML
    private JFXDrawer drawer;
    @FXML
    private JFXHamburger hamburger;
    @FXML

    public void closeMenu(MouseEvent e){
        if(drawer.isOpened()){
            drawer.close();
        }else{
            drawer.open();
        }
    }

}