package com.aonescan.scanner.database;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.aonescan.scanner.Model.Images;
import java.util.List;
import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface ImagesDao {

    @Query("SELECT * FROM images where id = :historyId")
    LiveData<List<Images>> getAllImagesByProjectId(int historyId);

    @Delete
    void delete(Images img);

    @Query("UPDATE images set image = :newPath WHERE image = :prevPath")
    void update(String prevPath,String newPath);

    @Insert(onConflict = REPLACE)
    void insert(Images images);
}
