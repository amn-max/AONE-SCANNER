package com.aonescan.scanner.Database.Documents;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DocumentDao {

    @Query("SELECT * FROM Documents")
    List<Documents> getAll();

    @Insert
    void insert(Documents documents);

    @Delete
    void delete(Documents documents);

}
