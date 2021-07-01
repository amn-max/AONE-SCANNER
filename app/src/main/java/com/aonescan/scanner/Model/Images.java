package com.aonescan.scanner.Model;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.aonescan.scanner.database.Project;

@Entity(foreignKeys = {@ForeignKey(entity = Project.class, parentColumns = "id", childColumns = "id", onDelete = ForeignKey.CASCADE)})
public class Images {

    @PrimaryKey(autoGenerate = true)
    private int numberId;
    private int id;
    private String image;
    private boolean isSelected = false;
    private boolean isEnhanced = false;
    private boolean isEnhancing = false;

    public Images() {

    }

    public Images(String s) {
        this.image = s;
    }

    public void setEnhancing(Boolean isEnhancing) {
        this.isEnhancing = isEnhancing;
    }

    public Boolean getIsEnhancing() {
        return isEnhancing;
    }

    public int getNumberId() {
        return numberId;
    }

    public void setNumberId(int numberId) {
        this.numberId = numberId;
    }

    public int getId() {
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

    public boolean getIsEnhanced() {
        return isEnhanced;
    }

    public void setIsEnhanced(boolean isEnhanced) {
        this.isEnhanced = isEnhanced;
    }
}
