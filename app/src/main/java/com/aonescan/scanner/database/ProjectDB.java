package com.aonescan.scanner.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.aonescan.scanner.Model.Images;

@Database(entities = {Project.class, Images.class}, version = 4, exportSchema = false)

@TypeConverters({Converters.class})
public abstract class ProjectDB extends RoomDatabase {
    public abstract ProjectDao projectDao();

    public abstract ImagesDao imagesDao();
}
