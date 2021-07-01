package com.aonescan.scanner.database;


import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.ArrayList;

@Entity
public class Project implements Serializable {

    private final ArrayList<String> imagePaths = new ArrayList<>();
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String createdOn;
    private boolean isChecked;
    private String projectName;

    @Ignore
    public Project() {

    }

    @Ignore
    public Project(String createdOn, ArrayList<String> imagePaths) {
        this.createdOn = createdOn;
        this.imagePaths.addAll(imagePaths);
    }

    public Project(String createdOn, ArrayList<String> imagePaths, boolean isChecked, String projectName) {
        this.createdOn = createdOn;
        this.imagePaths.addAll(imagePaths);
        this.isChecked = isChecked;
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean getChecked() {
        return isChecked;
    }

    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public ArrayList<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(ArrayList<String> imagePaths) {
        this.imagePaths.addAll(imagePaths);
    }
}
