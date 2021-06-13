package com.aonescan.scanner.Model;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.aonescan.scanner.database.Project;

import java.security.PublicKey;

@Entity(foreignKeys = {@ForeignKey(entity = Project.class,parentColumns = "id",childColumns = "id",onDelete = ForeignKey.CASCADE)})
public class Images {

    @PrimaryKey(autoGenerate = true)
    private int numberId;
    private int id;
    private String image;
    private Boolean isSelected = false;
    private Boolean isEnhanced = false;

    public Images(){

    }

    public Images(String s) {
        this.image = s;
    }

    public void setNumberId(int numberId){
        this.numberId = numberId;
    }
    public int getNumberId(){
        return numberId;
    }

    public int getId(){
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean getSelected() {
        return isSelected;
    }

    public void setSelected(Boolean selected) {
        this.isSelected = selected;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setIsEnhanced(boolean isEnhanced){
        this.isEnhanced = isEnhanced;
    }

    public boolean getIsEnhanced(){
        return isEnhanced;
    }
}
