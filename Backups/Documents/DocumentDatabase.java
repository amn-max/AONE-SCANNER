package com.aonescan.scanner.Database.Documents;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import org.w3c.dom.Document;


@Database(entities = {Document.class}, version = 1)
public abstract class DocumentDatabase extends RoomDatabase {
    public abstract DocumentDao documentDao();
}
