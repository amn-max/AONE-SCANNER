package com.aonescan.scanner.Database.Documents;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;

@Entity(tableName = "Documents")
public class Documents {
    @PrimaryKey(autoGenerate = true) public int uid;

    @ColumnInfo(name = "filename") public ArrayList<String> fileName;

    @ColumnInfo(name = "filepath") public ArrayList<String> filePath;
}
